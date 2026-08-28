package com.app.pustakam.core.network

import com.app.pustakam.core.database.localdb.preferences.IAppPreferences
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.NetworkError
import com.app.pustakam.core.common.util.log_d
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.RefreshReq
import com.app.pustakam.core.model.models.response.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Result
import kotlinx.coroutines.flow.first

// 🔐 20-Aug-2026 sync: routes whose own 401 means "bad credentials", never "token expired" — refreshing on them loops
private val NON_REFRESHABLE_PATHS = listOf("/login", "/register", "/auth/refresh")

private const val BEARER_PREFIX = "Bearer "
@PublishedApi internal const val STATUS_BAD_REQUEST = 400
@PublishedApi internal const val STATUS_UNAUTHORIZED = 401
@PublishedApi internal const val STATUS_FORBIDDEN = 403
@PublishedApi internal const val STATUS_NOT_FOUND = 404
@PublishedApi internal const val STATUS_REQUEST_TIMEOUT = 408
@PublishedApi internal const val STATUS_CONFLICT = 409
@PublishedApi internal const val STATUS_PAYLOAD_TOO_LARGE = 413
@PublishedApi internal const val STATUS_TOO_MANY_REQUESTS = 429

abstract class BaseClient  : KoinComponent {
    val userPrefs : IAppPreferences by inject<IAppPreferences>()

    // 🔐 20-Aug-2026 sync: set synchronously by a refresh so the immediate retry does not race the DataStore write
    private var freshToken: String? = null

    private val refreshLock = Mutex()

    protected val httpClient: HttpClient = createHttpClient { freshToken ?: userPrefs.currentTokenOrNull() }

    protected suspend inline fun < reified T, E: Error> baseApiCall(
        crossinline actualApiCall : suspend  () -> HttpResponse,
    ) : Result<T, Error> = flow {
           var response : HttpResponse = try {
               actualApiCall.invoke()
           }
           catch (e: UnresolvedAddressException) {
               log_d("Error", "$e")
               emit(Result.Error(NetworkError.NO_INTERNET))
               return@flow
           }
           catch (e: SerializationException){
               log_d("Error", "$e")
               emit( Result.Error(NetworkError.SERIALIZATION))
               return@flow
           }
        catch (e: ConnectTimeoutException){
            log_d("Error", "$e")
            emit( Result.Error(NetworkError.CONNECTION_FAILED))
            return@flow
        }
        catch (e: Throwable){
            log_d("Error", "$e")
            emit( Result.Error(NetworkError.CONNECTION_FAILED))
            return@flow
        }

        // 🔐 20-Aug-2026 sync: one silent refresh + one replay. Only a FAILED refresh ends the session.
        if (response.status.value == STATUS_UNAUTHORIZED && isRefreshable(response)) {
            if (!refreshSession()) {
                emit(Result.Error(NetworkError.SESSION_EXPIRED))
                return@flow
            }
            response = try {
                actualApiCall.invoke()
            } catch (e: Throwable) {
                log_d("Error", "$e")
                emit(Result.Error(NetworkError.CONNECTION_FAILED))
                return@flow
            }
        }

        captureHeaderToken(response)

           when (response.status.value){
               in 200..299 -> emit(Result.Success(response.body<T>()))
               STATUS_BAD_REQUEST -> emit(Result.Error(NetworkError.BAD_REQUEST))
               STATUS_UNAUTHORIZED -> emit(Result.Error(NetworkError.UNAUTHORIZED))
               STATUS_FORBIDDEN -> emit(Result.Error(NetworkError.FORBIDDEN))
               STATUS_NOT_FOUND -> emit(Result.Error(NetworkError.NOT_FOUND))
               STATUS_CONFLICT -> emit(Result.Error(NetworkError.CONFLICT))
               STATUS_REQUEST_TIMEOUT -> emit(Result.Error(NetworkError.REQUEST_TIMEOUT))
               STATUS_PAYLOAD_TOO_LARGE -> emit(Result.Error(NetworkError.PAYLOAD_TOO_LARGE))
               STATUS_TOO_MANY_REQUESTS -> emit(Result.Error(NetworkError.TOO_MANY_REQUESTS))
               in 500 ..599 -> emit(Result.Error(NetworkError.SERVER_ERROR))
               else ->  emit(Result.Error(NetworkError.UNKNOWN))
           }

       }.flowOn(Dispatchers.IO).first()

    @PublishedApi
    internal fun isRefreshable(response: HttpResponse): Boolean {
        val path = response.call.request.url.encodedPath
        return NON_REFRESHABLE_PATHS.none { path.endsWith(it) }
    }

    // 🔧 20-Aug-2026 sync: the header carries "Bearer <jwt>"; storing it raw made every request send "Bearer Bearer <jwt>"
    @PublishedApi
    internal suspend fun captureHeaderToken(response: HttpResponse) {
        if (!userPrefs.getAuthToken().isNullOrEmpty()) return
        val header = response.headers[headerAuth] ?: response.headers[headerAuth.lowercase()] ?: return
        val token = header.removePrefix(BEARER_PREFIX).trim()
        if (token.isNotEmpty()) {
            freshToken = token
            userPrefs.setToken(token)
        }
    }

    /** 🔐 Rotates the session. Returns false only when the server rejected the refresh token — a
     *  network failure leaves the tokens alone, because offline is not the same as signed out. */
    @PublishedApi
    internal suspend fun refreshSession(): Boolean {
        val tokenBeforeWait = freshToken ?: userPrefs.currentTokenOrNull()
        return refreshLock.withLock {
            // another coroutine already rotated while we waited — its token is the good one
            if ((freshToken ?: userPrefs.currentTokenOrNull()) != tokenBeforeWait) return@withLock true

            val refreshToken = userPrefs.getRefreshToken()?.takeIf { it.isNotBlank() }
                ?: return@withLock false

            val response = try {
                httpClient.post(urlString = ApiRoute.AUTH_REFRESH.getName()) {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshReq(refreshToken))
                }
            } catch (e: Throwable) {
                log_d("BaseClient", "refresh could not reach the server: $e")
                return@withLock false
            }

            if (response.status.value !in 200..299) {
                log_d("BaseClient", "refresh rejected with ${response.status.value}")
                // 🔐 28-Aug-2026 — ONLY the server saying "this refresh token is no good" ends the
                //   session. Clearing on any non-2xx meant a 502/504 from the tunnel silently wiped
                //   the tokens: the app still looked signed in, and nothing ever synced again.
                if (response.status.value == STATUS_UNAUTHORIZED || response.status.value == STATUS_FORBIDDEN) {
                    userPrefs.clearTokens()
                    freshToken = null
                }
                return@withLock false
            }

            val user = try {
                response.body<BaseResponse<User>>().data
            } catch (e: Throwable) {
                log_d("BaseClient", "refresh body unreadable: $e")
                return@withLock false
            }

            val access = user?.accessToken?.takeIf { it.isNotBlank() } ?: return@withLock false
            freshToken = access
            userPrefs.setToken(access)
            user.refreshToken?.takeIf { it.isNotBlank() }?.let { userPrefs.setRefreshToken(it) }
            true
        }
    }
}

import kotlinx.coroutines.*
fun main(){
    co()
}

fun co() {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    GlobalScope.launch(exceptionHandler) {
        // Coroutine code
        GlobalScope.async{
            delay(2000)
        }
        delay(1000)
        GlobalScope.async{
            delay(20000)
        }.await()
        println("completed")
    }
}
import BackgroundTasks
import Foundation
import Network
import shared

/// 🔄 20-Aug-2026 — the iOS half of "came back online, sync in the background".
///
/// Connectivity and scheduling are native on purpose (NWPathMonitor + BGTaskScheduler); the sync
/// cycle itself is the shared Kotlin engine behind `SyncBridge`. Android does the same job with
/// WorkManager's NetworkType.CONNECTED constraint.
final class SyncController {

    static let shared = SyncController()

    /// Must also appear in Info.plist → BGTaskSchedulerPermittedIdentifiers, or register() throws.
    static let taskIdentifier = "com.app.pustakam.iosApp.sync"

    private static let backgroundInterval: TimeInterval = 15 * 60

    private let bridge = SyncBridge()
    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.app.pustakam.sync.path")

    private var stateHandle: Closeable?
    private var didStart = false

    /// Latest engine state, for any screen that wants to show it.
    private(set) var state: SyncRunState = SyncRunState.Idle()
    var onStateChange: ((SyncRunState) -> Void)?

    private init() {}

    // MARK: - lifecycle

    /// Call once from the app's init, after Koin is up.
    func start() {
        guard !didStart else { return }
        didStart = true

        bridge.start()

        stateHandle = bridge.observeSyncState { [weak self] newState in
            guard let self else { return }
            self.state = newState
            self.onStateChange?(newState)
        }

        // 🔄 the trigger the feature exists for: work queued offline leaves the moment there is a line
        monitor.pathUpdateHandler = { [weak self] path in
            self?.bridge.setOnline(isOnline: path.status == .satisfied)
        }
        monitor.start(queue: monitorQueue)
    }

    /// Scene became active.
    func nudge() {
        bridge.nudge()
    }

    /// 🔄 28-Aug-2026 — pull to refresh. Runs ONE full cycle and waits for it, so the spinner
    /// stops when the work is actually done. Returns nil on success, or the reason it failed —
    /// a sync that fails silently is exactly why this feature looked broken.
    @MainActor
    func syncNowAsync() async -> String? {
        await withCheckedContinuation { continuation in
            var handle: Closeable?
            var finished = false
            let finish: (String?) -> Void = { message in
                guard !finished else { return }
                finished = true
                handle?.close()
                continuation.resume(returning: message)
            }
            handle = bridge.syncNow(
                onLoading: {},
                onSuccess: { _ in finish(nil) },
                onError: { error in finish(error.message) }
            )
        }
    }

    // MARK: - background scheduling

    /// 🔄 must run before the app finishes launching, so call it from iOSApp.init().
    func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handle(task: task)
        }
    }

    func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: Self.backgroundInterval)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Simulator and "background app refresh off" both land here; neither is fatal
            print("sync: could not schedule background refresh — \(error)")
        }
    }

    private func handle(task: BGTask) {
        // 🔄 always re-arm FIRST: a task that forgets this never runs a second time
        scheduleBackgroundRefresh()

        var handle: Closeable?
        task.expirationHandler = {
            handle?.close()
            task.setTaskCompleted(success: false)
        }

        handle = bridge.syncNow(
            onLoading: {},
            onSuccess: { _ in
                handle?.close()
                task.setTaskCompleted(success: true)
            },
            onError: { error in
                print("sync: background run failed — \(error.code): \(error.message)")
                handle?.close()
                task.setTaskCompleted(success: false)
            }
        )
    }
}

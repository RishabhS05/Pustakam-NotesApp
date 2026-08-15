import SwiftUI
import shared

struct ReaderPageCurlView: UIViewControllerRepresentable {
    let pages: [ReaderPage]
    let policy: PageLayoutPolicy
    let startIndex: Int
    let onPageChanged: (Int) -> Void
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenImage: (NoteContentModel.MediaContent) -> Void = { _ in }
    // 📖 15-Aug-2026: off keeps the shipped behaviour — the list under the curl never resizes
    var inlineDocumentsEnabled: Bool = false
    var inlineDocuments: InlineDocumentState = .disabled

    private var documents: InlineDocumentState { inlineDocumentsEnabled ? inlineDocuments : .disabled }

    func makeUIViewController(context: Context) -> UIPageViewController {
        let controller = UIPageViewController(transitionStyle: .pageCurl, navigationOrientation: .horizontal)
        controller.dataSource = context.coordinator
        controller.delegate = context.coordinator
        controller.view.backgroundColor = .clear
        // pageCurl defaults to double-sided, which makes UIKit demand a second controller
        controller.isDoubleSided = false
        guard !pages.isEmpty else { return controller }
        let start = min(max(startIndex, 0), pages.count - 1)
        controller.setViewControllers([context.coordinator.pageController(at: start)],
                                      direction: .forward, animated: false)
        context.coordinator.shownCount = pages.count
        context.coordinator.shownAnchor = start
        return controller
    }

    func updateUIViewController(_ uiViewController: UIPageViewController, context: Context) {
        context.coordinator.pages = pages
        context.coordinator.documents = documents
        guard !pages.isEmpty else { return }
        let start = min(max(startIndex, 0), pages.count - 1)
        // the controller is created before pages arrive, so seed it once they do
        if uiViewController.viewControllers?.isEmpty ?? true {
            uiViewController.setViewControllers([context.coordinator.pageController(at: start)],
                                                direction: .forward, animated: false)
            context.coordinator.shownCount = pages.count
            context.coordinator.shownAnchor = start
            return
        }
        guard inlineDocumentsEnabled else { return }
        // only a document opening or closing resizes the list or moves the anchor; a page turn does neither
        if context.coordinator.shownCount != pages.count || context.coordinator.shownAnchor != start {
            context.coordinator.shownCount = pages.count
            context.coordinator.shownAnchor = start
            uiViewController.setViewControllers([context.coordinator.pageController(at: start)],
                                                direction: .forward, animated: false)
            return
        }
        // 📖 a hosted page keeps the rootView it was built with, so state that does NOT resize the
        //   list — the busy spinner, Read/Close, a file found unreadable — must be pushed in by hand
        if let host = uiViewController.viewControllers?.first as? ReaderPageHost,
           pages.indices.contains(host.index) {
            host.rootView = context.coordinator.pageView(at: host.index)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(pages: pages, policy: policy, documents: documents, onPageChanged: onPageChanged,
                    onOpenDocument: onOpenDocument, onOpenImage: onOpenImage)
    }

    // index travels on the hosting controller — no tag hacks
    final class ReaderPageHost: UIHostingController<ReaderPageView> {
        let index: Int
        init(index: Int, rootView: ReaderPageView) {
            self.index = index
            super.init(rootView: rootView)
            view.backgroundColor = .clear
        }
        @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError("not used") }
    }

    final class Coordinator: NSObject, UIPageViewControllerDataSource, UIPageViewControllerDelegate {
        var pages: [ReaderPage]
        let policy: PageLayoutPolicy
        var documents: InlineDocumentState
        // what the container is currently showing, so a resize is told apart from a page turn
        var shownCount: Int = 0
        var shownAnchor: Int = -1
        let onPageChanged: (Int) -> Void
        let onOpenDocument: (NoteContentModel.MediaContent) -> Void
        let onOpenImage: (NoteContentModel.MediaContent) -> Void

        init(pages: [ReaderPage],
             policy: PageLayoutPolicy,
             documents: InlineDocumentState,
             onPageChanged: @escaping (Int) -> Void,
             onOpenDocument: @escaping (NoteContentModel.MediaContent) -> Void,
             onOpenImage: @escaping (NoteContentModel.MediaContent) -> Void) {
            self.pages = pages
            self.policy = policy
            self.documents = documents
            self.onPageChanged = onPageChanged
            self.onOpenDocument = onOpenDocument
            self.onOpenImage = onOpenImage
        }

        func pageView(at index: Int) -> ReaderPageView {
            ReaderPageView(
                page: pages[index],
                policy: policy,
                fillHeight: true,
                documents: documents,
                onOpenDocument: onOpenDocument,
                onOpenImage: onOpenImage
            )
        }

        func pageController(at index: Int) -> UIViewController {
            guard pages.indices.contains(index) else { return UIViewController() }
            return ReaderPageHost(index: index, rootView: pageView(at: index))
        }

        func pageViewController(_ pvc: UIPageViewController, viewControllerBefore vc: UIViewController) -> UIViewController? {
            guard let host = vc as? ReaderPageHost, host.index > 0, !pages.isEmpty else { return nil }
            return pageController(at: host.index - 1)
        }

        func pageViewController(_ pvc: UIPageViewController, viewControllerAfter vc: UIViewController) -> UIViewController? {
            guard let host = vc as? ReaderPageHost, host.index < pages.count - 1, !pages.isEmpty else { return nil }
            return pageController(at: host.index + 1)
        }

        func pageViewController(_ pvc: UIPageViewController, didFinishAnimating finished: Bool,
                                previousViewControllers: [UIViewController], transitionCompleted completed: Bool) {
            guard completed, let host = pvc.viewControllers?.first as? ReaderPageHost else { return }
            onPageChanged(host.index)
        }
    }
}

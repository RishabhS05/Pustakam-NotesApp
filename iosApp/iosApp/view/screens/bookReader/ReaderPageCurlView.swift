import SwiftUI
import shared

struct ReaderPageCurlView: UIViewControllerRepresentable {
    let pages: [ReaderPage]
    let policy: PageLayoutPolicy
    let startIndex: Int
    let onPageChanged: (Int) -> Void
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenImage: (NoteContentModel.MediaContent) -> Void = { _ in }

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
        return controller
    }

    func updateUIViewController(_ uiViewController: UIPageViewController, context: Context) {
        context.coordinator.pages = pages
        guard !pages.isEmpty else { return }
        // the controller is created before pages arrive, so seed it once they do
        if uiViewController.viewControllers?.isEmpty ?? true {
            let start = min(max(startIndex, 0), pages.count - 1)
            uiViewController.setViewControllers([context.coordinator.pageController(at: start)],
                                                direction: .forward, animated: false)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(pages: pages, policy: policy, onPageChanged: onPageChanged,
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
        let onPageChanged: (Int) -> Void
        let onOpenDocument: (NoteContentModel.MediaContent) -> Void
        let onOpenImage: (NoteContentModel.MediaContent) -> Void

        init(pages: [ReaderPage],
             policy: PageLayoutPolicy,
             onPageChanged: @escaping (Int) -> Void,
             onOpenDocument: @escaping (NoteContentModel.MediaContent) -> Void,
             onOpenImage: @escaping (NoteContentModel.MediaContent) -> Void) {
            self.pages = pages
            self.policy = policy
            self.onPageChanged = onPageChanged
            self.onOpenDocument = onOpenDocument
            self.onOpenImage = onOpenImage
        }

        func pageController(at index: Int) -> UIViewController {
            guard pages.indices.contains(index) else { return UIViewController() }
           return ReaderPageHost(
                index: index,
                rootView: ReaderPageView(
                    page: pages[index],
                    policy: policy,
                    fillHeight: true,
                    onOpenDocument: onOpenDocument,
                    onOpenImage: onOpenImage
                )
            )
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

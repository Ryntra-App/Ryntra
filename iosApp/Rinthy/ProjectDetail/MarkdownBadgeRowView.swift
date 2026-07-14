import RinthyShared
import SwiftUI
import UIKit
import WebKit

struct MarkdownBadgeRowView: UIViewRepresentable {
    let images: [MarkdownImage]

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.navigationDelegate = context.coordinator
        view.isOpaque = false
        view.backgroundColor = .clear
        view.scrollView.backgroundColor = .clear
        view.scrollView.showsHorizontalScrollIndicator = false
        view.scrollView.showsVerticalScrollIndicator = false
        return view
    }

    func updateUIView(_ view: WKWebView, context: Context) {
        let payload = encodedPayload()
        guard context.coordinator.payload != payload else { return }
        context.coordinator.payload = payload
        view.loadHTMLString(Self.html(payload: payload), baseURL: nil)
    }

    private func encodedPayload() -> String {
        let values = images.map { image in
            [
                "url": image.url,
                "alt": image.alt,
                "link": image.linkUrl ?? "",
            ]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: values) else { return "W10=" }
        return data.base64EncodedString()
    }

    private static func html(payload: String) -> String {
        """
        <!doctype html>
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        *{box-sizing:border-box}html,body{margin:0;padding:0;background:transparent;overflow:hidden}
        #row{height:42px;display:flex;align-items:center;gap:6px;white-space:nowrap;overflow-x:auto;overflow-y:hidden}
        img{display:block;height:36px;width:auto;max-width:none;border-radius:5px}
        a{display:block;flex:0 0 auto}
        </style></head><body><div id="row"></div><script>
        const items=JSON.parse(new TextDecoder().decode(Uint8Array.from(atob('(payload)'),c=>c.charCodeAt(0))));
        const row=document.getElementById('row');
        for(const item of items){
          const image=document.createElement('img'); image.src=item.url; image.alt=item.alt; image.loading='eager';
          if(item.link){const link=document.createElement('a');link.href=item.link;link.appendChild(image);row.appendChild(link)}
          else{row.appendChild(image)}
        }
        </script></body></html>
        """
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        var payload: String?

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard navigationAction.navigationType == .linkActivated else {
                decisionHandler(.allow)
                return
            }
            guard let url = navigationAction.request.url,
                  let scheme = url.scheme?.lowercased(),
                  scheme == "http" || scheme == "https" else {
                decisionHandler(.cancel)
                return
            }
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
        }
    }
}

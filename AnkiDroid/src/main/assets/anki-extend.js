function sessionStorageSet(key, value) {
  sessionStorage.setItem(key, value);
}

function sessionStorageGet(key) {
  return sessionStorage.getItem(key);
}

function sessionStorageRemove(key) {
  return sessionStorage.removeItem(key);
}

String.prototype.hashCode = function () {
  var hash = 0, i, chr;
  if (this.length === 0) return hash;
  for (i = 0; i < this.length; i++) {
    chr = this.charCodeAt(i);
    hash = ((hash << 5) - hash) + chr;
    hash |= 0;
  }
  return hash;
};

function setupWebViewJavascriptBridge(callback) {
  if (window.WebViewJavascriptBridge) {
    return callback(WebViewJavascriptBridge);
  }
  if (window.WVJBCallbacks) {
    return window.WVJBCallbacks.push(callback);
  }
  window.WVJBCallbacks = [callback];
  var WVJBIframe = document.createElement('iframe');
  WVJBIframe.style.display = 'none';
  WVJBIframe.src = 'https://__bridge_loaded__';
  document.documentElement.appendChild(WVJBIframe);
  setTimeout(function () {
    document.documentElement.removeChild(WVJBIframe)
  }, 0)
}

setupWebViewJavascriptBridge(function (bridge) {
  if (window.akBridge === undefined) {
    window.akBridge = bridge;
    window.akBridge.registerHandler("akSetHtmlContent", (data) => {
      $('body').html(JSON.parse(data).body);
      // window.akBridge.callHandler("akSetHtmlContentCompleted");
    });
  }
});

Ext.ns('NX');

NX.global = (function() {
  if (window !== undefined) {
    return window;
  }
  if (global !== undefined) {
    return global;
  }
  Ext.Error.raise('Unable to determine global object');
}());

Ext.Loader.setConfig({
  enabled: true,
  paths: {
    NX: './src/NX'
  }
});

(function() {
  var method = Ext.Loader.getPath;
  Ext.Loader.getPath = function() {
    var path = method.apply(this, arguments);
    if (path.indexOf('static/rapture/NX') === 0) {
      return path.replace('static/rapture/NX', './src/NX');
    }
    else {
      return path;
    }
  };
})();

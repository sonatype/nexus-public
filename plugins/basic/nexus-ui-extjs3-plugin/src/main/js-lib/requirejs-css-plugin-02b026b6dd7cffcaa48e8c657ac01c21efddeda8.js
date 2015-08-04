/*
 * https://github.com/guybedford/require-css uses MIT license.
 */
/*
 * css.normalize.js
 *
 * CSS Normalization
 *
 * CSS paths are normalized based on an optional basePath and the RequireJS config
 *
 * Usage:
 *   normalize(css, fromBasePath, toBasePath);
 *
 * css: the stylesheet content to normalize
 * fromBasePath: the absolute base path of the css relative to any root (but without ../ backtracking)
 * toBasePath: the absolute new base path of the css relative to the same root
 * 
 * Absolute dependencies are left untouched.
 *
 * Urls in the CSS are picked up by regular expressions.
 * These will catch all statements of the form:
 *
 * url(*)
 * url('*')
 * url("*")
 * 
 * @import '*'
 * @import "*"
 *
 * (and so also @import url(*) variations)
 *
 * For urls needing normalization
 *
 */

define('normalize', ['require', 'module'], function(require, module) {
  
  // regular expression for removing double slashes
  // eg http://www.example.com//my///url/here -> http://www.example.com/my/url/here
  var slashes = /([^:])\/+/g
  var removeDoubleSlashes = function(uri) {
    return uri.replace(slashes, '$1/');
  }

  // given a relative URI, and two absolute base URIs, convert it from one base to another
  function convertURIBase(uri, fromBase, toBase) {
    uri = removeDoubleSlashes(uri);
    // absolute urls are left in tact
    if (uri.match(/^\/|([^\:\/]*:)/))
      return uri;
    return relativeURI(absoluteURI(uri, fromBase), toBase);
  };
  
  // given a relative URI, calculate the absolute URI
  function absoluteURI(uri, base) {
    if (uri.substr(0, 2) == './')
      uri = uri.substr(2);    
    
    var baseParts = base.split('/');
    var uriParts = uri.split('/');
    
    baseParts.pop();
    
    while (curPart = uriParts.shift())
      if (curPart == '..')
        baseParts.pop();
      else
        baseParts.push(curPart);
    
    return baseParts.join('/');
  };


  // given an absolute URI, calculate the relative URI
  function relativeURI(uri, base) {
    
    // reduce base and uri strings to just their difference string
    var baseParts = base.split('/');
    baseParts.pop();
    base = baseParts.join('/') + '/';
    i = 0;
    while (base.substr(i, 1) == uri.substr(i, 1))
      i++;
    while (base.substr(i, 1) != '/')
      i--;
    base = base.substr(i + 1);
    uri = uri.substr(i + 1);

    // each base folder difference is thus a backtrack
    baseParts = base.split('/');
    var uriParts = uri.split('/');
    out = '';
    while (baseParts.shift())
      out += '../';
    
    // finally add uri parts
    while (curPart = uriParts.shift())
      out += curPart + '/';
    
    return out.substr(0, out.length - 1);
  };
  
  var normalizeCSS = function(source, fromBase, toBase) {

    fromBase = removeDoubleSlashes(fromBase);
    toBase = removeDoubleSlashes(toBase);

    var urlRegEx = /@import\s*("([^"]*)"|'([^']*)')|url\s*\(\s*(\s*"([^"]*)"|'([^']*)'|[^\)]*\s*)\s*\)/ig;
    var result, url, source;

    while (result = urlRegEx.exec(source)) {
      url = result[3] || result[2] || result[5] || result[6] || result[4];
      var newUrl = convertURIBase(url, fromBase, toBase);
      var quoteLen = result[5] || result[6] ? 1 : 0;
      source = source.substr(0, urlRegEx.lastIndex - url.length - quoteLen - 1) + newUrl + source.substr(urlRegEx.lastIndex - quoteLen - 1);
      urlRegEx.lastIndex = urlRegEx.lastIndex + (newUrl.length - url.length);
    }
    
    return source;
  };
  
  normalizeCSS.convertURIBase = convertURIBase;
  
  return normalizeCSS;
});
/*
 * css! loader plugin
 * Allows for loading stylesheets with the 'css!' syntax.
 *
 * in Chrome 19+, IE10+, Firefox 9+, Safari 6+ <link> tags are used with onload support
 * in all other environments, <style> tags are used with injection to mimic onload support
 *
 * <link> tag can be enforced with the configuration useLinks, although support cannot be guaranteed.
 *
 * External stylesheets loaded with link tags, unless useLinks explicitly set to false assuming CORS support.
 *
 * Stylesheet parsers always use <style> injection even on external urls, which may cause origin issues.
 *
 */
define('css', ['normalize', 'module'], function(normalize, module) {
  if (typeof window == 'undefined')
    return { load: function(n, r, load){ load() } };
  
  var head = document.getElementsByTagName('head')[0];

  var agentMatch = window.navigator.userAgent.match(/Chrome\/([^ \.]*)|MSIE ([^ ;]*)|Firefox\/([^ ;]*)|Version\/([\d\.]*) Safari\//);

  var useLinks, browserEngine;

  if (window.opera) {
    browserEngine = 'opera';
    useLinks = true;
  }

  if (agentMatch) {
    if (agentMatch[4])
      browserEngine = 'webkit'
    if (agentMatch[3])
      browserEngine = 'mozilla';
    else if (agentMatch[2])
      browserEngine = 'ie';
    else if (agentMatch[1])
      browserEngine = 'webkit';

    useLinks = (browserEngine && (parseInt(agentMatch[4]) > 5 || parseInt(agentMatch[3]) > 8 || parseInt(agentMatch[2]) > 9 || parseInt(agentMatch[1]) > 18)) || undefined;
  }

  var config = module.config();
  if (config && config.useLinks !== undefined)
    useLinks = config.useLinks;
  
  /* XHR code - copied from RequireJS text plugin */
  var progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];
  var fileCache = {};
  var get = function(url, callback, errback) {
    if (fileCache[url]) {
      callback(fileCache[url]);
      return;
    }

    var xhr, i, progId;
    if (typeof XMLHttpRequest !== 'undefined')
      xhr = new XMLHttpRequest();
    else if (typeof ActiveXObject !== 'undefined')
      for (i = 0; i < 3; i += 1) {
        progId = progIds[i];
        try {
          xhr = new ActiveXObject(progId);
        }
        catch (e) {}
  
        if (xhr) {
          progIds = [progId];  // so faster next time
          break;
        }
      }
    
    xhr.open('GET', url, requirejs.inlineRequire ? false : true);
  
    xhr.onreadystatechange = function (evt) {
      var status, err;
      //Do not explicitly handle errors, those should be
      //visible via console output in the browser.
      if (xhr.readyState === 4) {
        status = xhr.status;
        if (status > 399 && status < 600) {
          //An http 4xx or 5xx error. Signal an error.
          err = new Error(url + ' HTTP status: ' + status);
          err.xhr = xhr;
          errback(err);
        }
        else {
          fileCache[url] = xhr.responseText;
          callback(xhr.responseText);
        }
      }
    };
    
    xhr.send(null);
  }
  
  //main api object
  var cssAPI = {};
  
  cssAPI.pluginBuilder = './css-builder';
  
  //uses the <style> load method
  var stylesheet = document.createElement('style');
  stylesheet.type = 'text/css';
  head.appendChild(stylesheet);
  
  if (stylesheet.styleSheet)
    cssAPI.inject = function(css) {
      stylesheet.styleSheet.cssText += css;
    }
  else
    cssAPI.inject = function(css) {
      stylesheet.appendChild(document.createTextNode(css));
    }


  var webkitLoadCheck = function(link, callback) {
    setTimeout(function() {
      for (var i = 0; i < document.styleSheets.length; i++) {
        var sheet = document.styleSheets[i];
        if (sheet.href == link.href)
          return callback();
      }
      webkitLoadCheck(link, callback);
    }, 10);
  }

  var mozillaLoadCheck = function(style, callback) {
    setTimeout(function() {
      try {
        style.sheet.cssRules;
        return callback();
      } catch (e){}
      mozillaLoadCheck(style, callback);
    }, 10);
  }

  // uses the <link> load method
  var createLink = function(url) {
    var link = document.createElement('link');
    link.type = 'text/css';
    link.rel = 'stylesheet';
    link.href = url;
    return link;
  }

  cssAPI.linkLoad = function(url, callback) {
    var timeout = setTimeout(callback, waitSeconds * 1000 - 100);
    var _callback = function() {
      clearTimeout(timeout);
      callback();
    }
    if (browserEngine == 'webkit') {
      var link = createLink(url);
      webkitLoadCheck(link, _callback);
      head.appendChild(link);
    }
    // onload support only in firefox 18+
    else if (browserEngine == 'mozilla' && parseInt(agentMatch[3]) < 18) {
      var style = document.createElement('style');
      style.textContent = '@import "' + url + '"';
      mozillaLoadCheck(style, _callback);
      head.appendChild(style);
    }
    else {
      var link = createLink(url);
      link.onload = _callback;
      head.appendChild(link);
    }
  }


  cssAPI.inspect = function() {
    if (stylesheet.styleSheet)
      return stylesheet.styleSheet.cssText;
    else if (stylesheet.innerHTML)
      return stylesheet.innerHTML;
  }
  
  cssAPI.normalize = function(name, normalize) {
    if (name.substr(name.length - 4, 4) == '.css')
      name = name.substr(0, name.length - 4);
    
    return normalize(name);
  }

  // NB add @media query support for media imports
  var importRegEx = /@import\s*(url)?\s*(('([^']*)'|"([^"]*)")|\(('([^']*)'|"([^"]*)"|([^\)]*))\))\s*;?/g;

  var pathname = window.location.pathname.split('/');
  pathname.pop();
  pathname = pathname.join('/') + '/';

  var loadCSS = function(fileUrl, callback, errback) {

    //make file url absolute
    if (fileUrl.substr(0, 1) != '/')
      fileUrl = '/' + normalize.convertURIBase(fileUrl, pathname, '/');

    get(fileUrl, function(css) {

      // normalize the css (except import statements)
      css = normalize(css, fileUrl, pathname);

      // detect all import statements in the css and normalize
      var importUrls = [];
      var importIndex = [];
      var importLength = [];
      var match;
      while (match = importRegEx.exec(css)) {
        var importUrl = match[4] || match[5] || match[7] || match[8] || match[9];

        // add less extension if necessary
        if (importUrl.indexOf('.less') === -1)
          importUrl += '.less';

        importUrls.push(importUrl);
        importIndex.push(importRegEx.lastIndex - match[0].length);
        importLength.push(match[0].length);
      }

      // load the import stylesheets and substitute into the css
      var completeCnt = 0;
      for (var i = 0; i < importUrls.length; i++)
        (function(i) {
          loadCSS(importUrls[i], function(importCSS) {
            css = css.substr(0, importIndex[i]) + importCSS + css.substr(importIndex[i] + importLength[i]);
            var lenDiff = importCSS.length - importLength[i];
            for (var j = i + 1; j < importUrls.length; j++)
              importIndex[j] += lenDiff;
            completeCnt++;
            if (completeCnt == importUrls.length) {
              callback(css);
            }
          }, errback);
        })(i);

      if (importUrls.length == 0)
        callback(css);
    }, errback);
  }
  
  var waitSeconds;
  cssAPI.load = function(cssId, req, load, config, parse) {
    waitSeconds = waitSeconds || config.waitSeconds || 7;

    var fileUrl = cssId;
    
    if (fileUrl.substr(fileUrl.length - 4, 4) != '.css' && !parse)
      fileUrl += '.css';
    
    fileUrl = req.toUrl(fileUrl);

    // determine if it is the same domain or not
    var sameDomain = true,
    domainCheck = /^(\w+:)?\/\/([^\/]+)/.exec(fileUrl);
    if (domainCheck) {
      sameDomain = domainCheck[2] === window.location.host;
      if (domainCheck[1])
        sameDomain &= domainCheck[1] === window.location.protocol;
    }
    
    //external url -> add as a <link> tag to load
    if (!parse && useLinks !== false && (!sameDomain || useLinks)) {
      cssAPI.linkLoad(fileUrl, function() {
        load(cssAPI);
      });
    }
    //internal url or parsing -> inject into <style> tag
    else {
      loadCSS(fileUrl, function(css) {
        // run parsing last - since less is a CSS subset this works fine
        if (parse)
          css = parse(css);

        cssAPI.inject(css);

        load(cssAPI);
      }, load.error);
    }
  }
  
  return cssAPI;
});

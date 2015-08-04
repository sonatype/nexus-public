/**
 * The OWASP CSRFGuard Project, BSD License
 * Eric Sheridan (eric@infraredsecurity.com), Copyright (c) 2011 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. Neither the name of OWASP nor the names of its contributors may be used
 *       to endorse or promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
(function() {
	/**
	 * Code to ensure our event always gets triggered when the DOM is updated.
	 * @param obj
	 * @param type
	 * @param fn
	 * @source http://www.dustindiaz.com/rock-solid-addevent/
	 */
	function addEvent( obj, type, fn ) {
	    if (obj.addEventListener) {
	        obj.addEventListener( type, fn, false );
	        EventCache.add(obj, type, fn);
	    }
	    else if (obj.attachEvent) {
	        obj["e"+type+fn] = fn;
	        obj[type+fn] = function() { obj["e"+type+fn]( window.event ); }
	        obj.attachEvent( "on"+type, obj[type+fn] );
	        EventCache.add(obj, type, fn);
	    }
	    else {
	        obj["on"+type] = obj["e"+type+fn];
	    }
	}
	
	var EventCache = function(){
	    var listEvents = [];
	    return {
	        listEvents : listEvents,
	        add : function(node, sEventName, fHandler){
	            listEvents.push(arguments);
	        },
	        flush : function(){
	            var i, item;
	            for(i = listEvents.length - 1; i >= 0; i = i - 1){
	                item = listEvents[i];
	                if(item[0].removeEventListener){
	                    item[0].removeEventListener(item[1], item[2], item[3]);
	                };
	                if(item[1].substring(0, 2) != "on"){
	                    item[1] = "on" + item[1];
	                };
	                if(item[0].detachEvent){
	                    item[0].detachEvent(item[1], item[2]);
	                };
	                item[0][item[1]] = null;
	            };
	        }
	    };
	}();
	
	/** string utility functions **/
	String.prototype.startsWith = function(prefix) {
		return this.indexOf(prefix) === 0;
	};

	String.prototype.endsWith = function(suffix) {
		return this.match(suffix+"$") == suffix;
	};

	/** hook using standards based prototype **/
	function hijackStandard() {
		XMLHttpRequest.prototype._open = XMLHttpRequest.prototype.open;
		XMLHttpRequest.prototype.open = function(method, url, async, user, pass) {
			this.url = url;
			
			this._open.apply(this, arguments);
		};
		
		XMLHttpRequest.prototype._send = XMLHttpRequest.prototype.send;
		XMLHttpRequest.prototype.send = function(data) {
			if(this.onsend != null) {
				this.onsend.apply(this, arguments);
			}
			
			this._send.apply(this, arguments);
		};
	}

	/** ie does not properly support prototype - wrap completely **/
	function hijackExplorer() {
		var _XMLHttpRequest = window.XMLHttpRequest;
		
		function alloc_XMLHttpRequest() {
			this.base = _XMLHttpRequest ? new _XMLHttpRequest : new window.ActiveXObject("Microsoft.XMLHTTP");
		}
		
		function init_XMLHttpRequest() {
			return new alloc_XMLHttpRequest;
		}
		
		init_XMLHttpRequest.prototype = alloc_XMLHttpRequest.prototype;
		
		/** constants **/
		init_XMLHttpRequest.UNSENT = 0;
		init_XMLHttpRequest.OPENED = 1;
		init_XMLHttpRequest.HEADERS_RECEIVED = 2;
		init_XMLHttpRequest.LOADING = 3;
		init_XMLHttpRequest.DONE = 4;
		
		/** properties **/
		init_XMLHttpRequest.prototype.status = 0;
		init_XMLHttpRequest.prototype.statusText = "";
		init_XMLHttpRequest.prototype.readyState = init_XMLHttpRequest.UNSENT;
		init_XMLHttpRequest.prototype.responseText = "";
		init_XMLHttpRequest.prototype.responseXML = null;
		init_XMLHttpRequest.prototype.onsend = null;
		
		init_XMLHttpRequest.url = null;
		init_XMLHttpRequest.onreadystatechange = null;

		/** methods **/
		init_XMLHttpRequest.prototype.open = function(method, url, async, user, pass) {
			var self = this;
			this.url = url;
			
			this.base.onreadystatechange = function() {
				try { self.status = self.base.status; } catch (e) { }
				try { self.statusText = self.base.statusText; } catch (e) { }
				try { self.readyState = self.base.readyState; } catch (e) { }
				try { self.responseText = self.base.responseText; } catch(e) { }
				try { self.responseXML = self.base.responseXML; } catch(e) { }
				
				if(self.onreadystatechange != null) {
					self.onreadystatechange.apply(this, arguments);
				}
			}
			
			this.base.open(method, url, async, user, pass);
		};
		
		init_XMLHttpRequest.prototype.send = function(data) {
			if(this.onsend != null) {
				this.onsend.apply(this, arguments);
			}
			
			this.base.send(data);
		};
		
		init_XMLHttpRequest.prototype.abort = function() {
			this.base.abort();
		};
		
		init_XMLHttpRequest.prototype.getAllResponseHeaders = function() {
			return this.base.getAllResponseHeaders();
		};
		
		init_XMLHttpRequest.prototype.getResponseHeader = function(name) {
			return this.base.getResponseHeader(name);
		};
		
		init_XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
			return this.base.setRequestHeader(name, value);
		};
		
		/** hook **/
		window.XMLHttpRequest = init_XMLHttpRequest;
	}

	/** check if valid domain based on domainStrict **/
	function isValidDomain(current, target) {
		var result = false;
		
		/** check exact or subdomain match **/
		if(current == target) {
			result = true;
		} else if(%DOMAIN_STRICT% == false) {
			if(target.charAt(0) == '.') {
				result = current.endsWith(target);
			} else {
				result = current.endsWith('.' + target);
			}
		}
		
		return result;
	}

	/** determine if uri/url points to valid domain **/
	function isValidUrl(src) {
		var result = false;
		
		/** parse out domain to make sure it points to our own **/
		if(src.substring(0, 7) == "http://" || src.substring(0, 8) == "https://") {
			var token = "://";
			var index = src.indexOf(token);
			var part = src.substring(index + token.length);
			var domain = "";
			
			/** parse up to end, first slash, or anchor **/
			for(var i=0; i<part.length; i++) {
				var character = part.charAt(i);
				
				if(character == '/' || character == ':' || character == '#') {
					break;
				} else {
					domain += character;
				}
			}
			
			result = isValidDomain(document.domain, domain);
			/** explicitly skip anchors **/
		} else if(src.charAt(0) == '#') {
			result = false;
			/** ensure it is a local resource without a protocol **/
		} else if(!src.startsWith("//") && (src.charAt(0) == '/' || src.indexOf(':') == -1)) {
			result = true;
		}
		
		return result;
	}

	/** parse uri from url **/
	function parseUri(url) {
		var uri = "";
		var token = "://";
		var index = url.indexOf(token);
		var part = "";
		
		/**
		 * ensure to skip protocol and prepend context path for non-qualified
		 * resources (ex: "protect.html" vs
		 * "/Owasp.CsrfGuard.Test/protect.html").
		 */
		if(index > 0) {
			part = url.substring(index + token.length);
		} else if(url.charAt(0) != '/') {
			part = "%CONTEXT_PATH%/" + url;
		} else {
			part = url;
		}
		
		/** parse up to end or query string **/
		var uriContext = (index == -1);
		
		for(var i=0; i<part.length; i++) {
			var character = part.charAt(i);
			
			if(character == '/') {
				uriContext = true;
			} else if(uriContext == true && (character == '?' || character == '#')) {
				uriContext = false;
				break;
			}
			
			if(uriContext == true) {
				uri += character;
			}
		}
		
		return uri;
	}

	/** inject tokens as hidden fields into forms **/
	function injectTokenForm(form, tokenName, tokenValue, pageTokens) {
		var action = form.getAttribute("action");
		
		if(action != null && isValidUrl(action)) {
			var uri = parseUri(action);
			var hidden = document.createElement("input");
			
			hidden.setAttribute("type", "hidden");
			hidden.setAttribute("name", tokenName);
			hidden.setAttribute("value", (pageTokens[uri] != null ? pageTokens[uri] : tokenValue));
			
			form.appendChild(hidden);
		}
	}

	/** inject tokens as query string parameters into url **/
	function injectTokenAttribute(element, attr, tokenName, tokenValue, pageTokens) {
		var location = element.getAttribute(attr);
		
		if(location != null && isValidUrl(location)) {
			var uri = parseUri(location);
			var value = (pageTokens[uri] != null ? pageTokens[uri] : tokenValue);
			
			if(location.indexOf('?') != -1) {
				location = location + '&' + tokenName + '=' + value;
			} else {
				location = location + '?' + tokenName + '=' + value;
			}

			try {
				element.setAttribute(attr, location);
			} catch (e) {
				// attempted to set/update unsupported attribute
			}
		}
	}

	/** inject csrf prevention tokens throughout dom **/
	function injectTokens(tokenName, tokenValue) {
		/** obtain reference to page tokens if enabled **/
		var pageTokens = {};
		
		if(%TOKENS_PER_PAGE% == true) {
			pageTokens = requestPageTokens();
		}
		
		/** iterate over all elements and injection token **/
		var all = document.all ? document.all : document.getElementsByTagName('*');
		var len = all.length;

		for(var i=0; i<len; i++) {
			var element = all[i];
			
			/** inject into form **/
			if(element.tagName.toLowerCase() == "form") {
				if(%INJECT_FORMS% == true) {
					injectTokenForm(element, tokenName, tokenValue, pageTokens);
					injectTokenAttribute(element, "action", tokenName, tokenValue, pageTokens);
				}
				/** inject into attribute **/
			} else if(%INJECT_ATTRIBUTES% == true) {
				injectTokenAttribute(element, "src", tokenName, tokenValue, pageTokens);
				injectTokenAttribute(element, "href", tokenName, tokenValue, pageTokens);
			}
		}
	}

	/** obtain array of page specific tokens **/
	function requestPageTokens() {
		var xhr = window.XMLHttpRequest ? new window.XMLHttpRequest : new window.ActiveXObject("Microsoft.XMLHTTP");
		var pageTokens = {};
		
		xhr.open("POST", "%SERVLET_PATH%", false);
		xhr.send(null);
		
		var text = xhr.responseText;
		var name = "";
		var value = "";
		var nameContext = true;
		
		for(var i=0; i<text.length; i++) {
			var character = text.charAt(i);
			
			if(character == ':') {
				nameContext = false;
			} else if(character != ',') {
				if(nameContext == true) {
					name += character;
				} else {
					value += character;
				}
			}
			
			if(character == ',' || (i + 1) >= text.length) {
				pageTokens[name] = value;
				name = "";
				value = "";
				nameContext = true;
			}
		}
		
		return pageTokens;
	}
	
	/**
	 * Only inject the tokens if the JavaScript was referenced from HTML that
	 * was served by us. Otherwise, the code was referenced from malicious HTML
	 * which may be trying to steal tokens using JavaScript hijacking techniques.
	 */
	if(isValidDomain(document.domain, "%DOMAIN_ORIGIN%")) {
		/** optionally include Ajax support **/
		if(%INJECT_XHR% == true) {
			if(navigator.appName == "Microsoft Internet Explorer") {
				hijackExplorer();
			} else {
				hijackStandard();
			}

      // Sonatype changes: Code bellow are changes required for deferred pushing of token after login
      XMLHttpRequest.tokenName = "%TOKEN_NAME%";
      XMLHttpRequest.tokenValue = function () {
        return "%TOKEN_VALUE%";
      };
      // Sonatype changes: end
			
			XMLHttpRequest.prototype.onsend = function(data) {
				if(isValidUrl(this.url)) {
					this.setRequestHeader("X-Requested-With", "%X_REQUESTED_WITH%");
          // Sonatype changes: use function to get token value
					this.setRequestHeader("%TOKEN_NAME%", XMLHttpRequest.tokenValue());
          // Sonatype changes: end
				}
			};
		}
		
		/** update nodes in DOM after load **/
		addEvent(window,'unload',EventCache.flush);
    // Sonatype changes: use function to get token value
		addEvent(window,'load', function() {
			injectTokens("%TOKEN_NAME%", XMLHttpRequest.tokenValue());
		});
    // Sonatype changes: end
	} else {
		alert("OWASP CSRFGuard JavaScript was included from within an unauthorized domain!");
	}
})();
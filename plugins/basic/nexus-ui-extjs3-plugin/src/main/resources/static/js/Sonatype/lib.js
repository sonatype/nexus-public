/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global define, escape, unescape*/
/*jslint eqeq: true*/
///* jslint bitwise: true, eqeq: true*/
define('Sonatype/lib',['extjs', 'sonatype'], function(Ext, Sonatype) {

  Sonatype.lib.Permissions = {
    READ : 1, // 0001
    EDIT : 2, // 0010
    DELETE : 4, // 0100
    CREATE : 8, // 1000
    ALL : 15, // 1111
    NONE : 0, // 0000

    // returns bool indicating if value has all perms
    // all values are base 10 representations of the n-bit representation
    // Example: for 4-bit permissions: 3 (base to) represents 0011 (base 2)
    checkPermission : function(value, perm /* , perm... */) {
      var p = perm, perms;

      if (Sonatype.user.curr.repoServer)
      {
        Ext.each(Sonatype.user.curr.repoServer, function(item, i, arr) {
              if (item.id === value)
              {
                value = item.value;
                return false;
              }
            });
      }

      if (arguments.length > 2)
      {
        perms = [].slice.call(arguments, 2);
        Ext.each(perms, function(item, i, arr) {
              p = p | item;
            });
      }

      return ((p & value) == p);
    }
  };

  /*
   * Adapted from ExtJS v2.0.2 Ext.state.CookieProvider; removed inheritance
   * from Ext.state.Provider @cfg {String} path The path for which the cookie is
   * active (defaults to root '/' which makes it active for all pages in the
   * site) @cfg {Date} expires The cookie expiration date (defaults to 7 days
   * from now) @cfg {String} domain The domain to save the cookie for. Note that
   * you cannot specify a different domain than your page is on, but you can
   * specify a sub-domain, or simply the domain itself like 'extjs.com' to
   * include all sub-domains if you need to access cookies across different
   * sub-domains (defaults to null which uses the same domain the page is
   * running on including the 'www' like 'www.extjs.com') @cfg {Boolean} secure
   * True if the site is using SSL (defaults to false) @constructor Create a new
   * CookieProvider @param {Object} config The configuration object
   */
  Sonatype.lib.CookieProvider = function(config) {
    this.namePrefix = 'st-'; // added alternate default prefix
    this.path = "/";
    this.expires = new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 7)); // 7
                                                                                // days
    this.domain = null;
    this.secure = false;
    Ext.apply(this, config);
    this.state = this.readCookies();
  };

  Sonatype.lib.CookieProvider.prototype = {
    /**
     * Returns the current value for a key
     * 
     * @param {String}
     *          name The key name
     * @param {Mixed}
     *          defaultValue A default value to return if the key's value is not
     *          found
     * @return {Mixed} The state data
     */
    get : function(name, defaultValue) {
      return this.state[name] || defaultValue;
    },

    // private
    set : function(name, value) {
      if (!value)
      {
        this.clear(name);
        return;
      }
      this.setCookie(name, value);
      this.state[name] = value;
    },

    // private
    clear : function(name) {
      this.clearCookie(name);
      delete this.state[name];
    },

    // private
    readCookies : function() {
      var
            cookies = {},
            c = document.cookie + ";",
            re = /\s?(.*?)=(.*?);/g,
            matches, name, value;

      while ((matches = re.exec(c)) != null)
      {
        name = matches[1];
        value = matches[2];

        if (name && name.substring(0, 3) === this.namePrefix)
        {
          cookies[name.substr(3)] = this.decodeValue(value);
        }
      }
      return cookies;
    },

    // private
    setCookie : function(name, value) {
      document.cookie = this.namePrefix + name + "=" + this.encodeValue(value) + ((this.expires == null) ? "" : ("; expires=" + this.expires.toGMTString())) + ((this.path == null) ? "" : ("; path=" + this.path))
          + ((this.domain == null) ? "" : ("; domain=" + this.domain)) + ((this.secure == true) ? "; secure" : "");
    },

    // private
    clearCookie : function(name) {
      document.cookie = this.namePrefix + name + "=null; expires=Thu, 01-Jan-70 00:00:01 GMT" + ((this.path == null) ? "" : ("; path=" + this.path)) + ((this.domain == null) ? "" : ("; domain=" + this.domain)) + ((this.secure == true) ? "; secure" : "");
    },

    /**
     * Decodes a string previously encoded with {@link #encodeValue}.
     * 
     * @param {String}
     *          value The value to decode
     * @return {Mixed} The decoded value
     */
    decodeValue : function(cookie) {
      var all, values, i, len, kv, re, matches, type, v;
      re = /^(a|n|d|b|s|o)\:(.*)$/;
      matches = re.exec(unescape(cookie));
      if (!matches || !matches[1]) {
        return null; // non state cookie
      }
      type = matches[1];
      v = matches[2];

      switch (type)
      {
        case "n" :
          return parseFloat(v);
        case "d" :
          return new Date(Date.parse(v));
        case "b" :
          return (v === "1");
        case "a" :
                all = [];
                values = v.split("^");
          for (i = 0, len = values.length; i < len; i=i+1)
          {
            all.push(this.decodeValue(values[i]));
          }
          return all;
        case "o" :
          all = {};
          values = v.split("^");
          for (i = 0, len = values.length; i < len; i=i+1)
          {
            kv = values[i].split("=");
            all[kv[0]] = this.decodeValue(kv[1]);
          }
          return all;
        default :
          return v;
      }
    },

    /**
     * Encodes a value including type information. Decode with
     * {@link #decodeValue}.
     * 
     * @param {Mixed}
     *          value The value to encode
     * @return {String} The encoded value
     */
    encodeValue : function(v) {
      var enc, i, flat = "", key, len;
      if (typeof v === "number")
      {
        enc = "n:" + v;
      }
      else if (typeof v === "boolean")
      {
        enc = "b:" + (v ? "1" : "0");
      }
      else if (Ext.isDate(v))
      {
        enc = "d:" + v.toGMTString();
      }
      else if (Ext.isArray(v))
      {
        for (i = 0, len = v.length; i < len; i=i+1)
        {
          flat += this.encodeValue(v[i]);
          if (i !== len - 1) {
            flat += "^";
          }
        }
        enc = "a:" + flat;
      }
      else if (typeof v === "object")
      {
        for (key in v)
        {
          if (v.hasOwnProperty(key) && typeof v[key] !== "function" && v[key] !== undefined)
          {
            flat += key + "=" + this.encodeValue(v[key]) + "^";
          }
        }
        enc = "o:" + flat.substring(0, flat.length - 1);
      }
      else
      {
        enc = "s:" + v;
      }
      return escape(enc);
    }
  };

});
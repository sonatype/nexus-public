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
/*global define,Ext,NX,global */

define('NX/base', ['require'], function(require) {
  Ext.ns('NX');

  /**
   * Basic framework helpers.
   *
   * @singleton
   */
  Ext.apply(NX, {
      /**
       * Global object reference; never undefined.
       *
       * @type {Object}
       */
      global: (function() {
          if (window !== undefined) {
              return window;
          }
          if (global !== undefined) {
              return global;
          }
          throw new Error('Unable to determine global object');
      }()), // assign return value of function

      /**
       * Reference an object by its global name.
       *
       * @param path
       * @return {*}
       * @throws Error    No object at path
       */
      obj: function (path) {
          var context = NX.global;
          if (typeof path === 'string') {
            Ext.each(path.split('.'), function (part) {
                context = context[part];
                if (context === undefined) {
                    throw new Error('No object at path: ' + path + '; part is undefined: ' + part);
                }
            });
          }
          return context;
      },

      /**
       * Check if an object exists by its global name.
       *
       * @param path
       * @return {boolean}    True if it exists, false if it does not.
       */
      isobj: function(path) {
          var context = NX.global;
          Ext.each(path.split('.'), function (part) {
              context = context[part];
              if (context === undefined) {
                  return false; // break
              }
          });
          return context !== undefined;
      },

      /**
       * Turns input into an array of strings.
       *
       * @param input
       * @return {Array}
       */
      arrayify: function (input) {
          var i, list = [];

          if (Ext.isArray(input)) {
              for (i = 0; i < input.length; i++) {
                  if (Ext.isString(input[i])) {
                      list.push(input[i]);
                  }
                  else {
                      throw new Error('Invalid entry: ' + input[i]);
                  }
              }
          }
          else if (Ext.isString(input)) {
              list.push(input);
          }
          else if (input !== undefined) {
              throw new Error('Invalid value: ' + input);
          }

          return list;
      },

      /**
       * Helper to construct an object from a constructor.
       *
       * @param {Function} constructor
       * @param {Array} args
       * @return {*}
       */
      construct: function (constructor, args) {
          function F() {
              return constructor.apply(this, args);
          }

          F.prototype = constructor.prototype;
          return new F();
      },

      /**
       * Create a new instance of a class.
       *
       * @param name
       * @return {*}
       */
      create: function (name /*, [arg1,arg2,...,argN] */) {
          var args,
              type;

          // pull out varargs, strip off class name
          args = Array.prototype.slice.call(arguments);
          args.shift();

          NX.log.debug('Creating instance:', name);

          // Get a reference to the class
          type = NX.obj(name);

          // Require the class if its not defined
          if (type === undefined) {
              type = require(name.replaceAll('.', '/'));
          }

          return NX.construct(type, args);
      },

      htmlRenderer: (function() {
        var div = document.createElement('div');

        return function(value) {
          while (div.hasChildNodes()) {
            div.removeChild(div.firstChild);
          }
          div.appendChild(document.createTextNode(value));
          return div.innerHTML;
        }
      })()
  });
});

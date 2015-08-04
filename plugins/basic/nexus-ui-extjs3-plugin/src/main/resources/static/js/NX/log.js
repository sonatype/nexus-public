/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global define,Ext,NX,Nexus */

define('NX/log', ['NX/base'], function() {
  Ext.ns('NX');

  /**
   * Logging helper.
   *
   * @singleton
   */
  NX.log = (function () {
      var logger;

      // FIXME: Really would like a better/more-powerful logging API here
      // FIXME: Including the ability to remote back to the server to capture UI events
      //
      // http://log4javascript.org/index.html
      // http://www.gscottolson.com/blackbirdjs
      // http://log4js.berlios.de

      logger = {
          enabled: true,

          levels: {
              trace:  false,
              debug:  false,
              group:  false,
              info:   true,
              warn:   true,
              error:  true
          },

          isEnabled: function(level) {
              return this.enabled && (this.levels[level] || NX.global.location.search === '?debug');
          }

          // FIXME: Add ExtJS 4.x compatible(ish) log() helper
          //log: function(/*[options],[message]*/) {
          //}
      };

      /**
       * @private
       * @param target {Object} The logging implementation
       * @param name {String} The logger to use
       * @param isLoggerEnabled {Function} A function that returns true when the logger for target/name is enabled, false otherwise.
       * @return {Function} The logger.
       */
      function createLogger(target, name, isLoggerEnabled) {
        var log;
        if (Ext.isDefined(target)) {
          if (Ext.isFunction(target[name])) { // most browser have proper console.log, console.debug, ... as functions
            log = function() {
              if (isLoggerEnabled()) {
                target[name].apply(target, arguments);
              }
            };
          } else if (Ext.isDefined(target[name])) { // IE9: console.log is reported as an object, not a function, and does not have #apply
            log = function() {
              if (isLoggerEnabled()) {
                var args = [];
                Ext.each(arguments, function(item) {
                  args.push(item);
                });
                target[name](args.join(' '));
              }
            };
          } else if (Ext.isDefined(target.log)) { // if e.g. console.debug is not available, use console.log
            log = createLogger(target, 'log', isLoggerEnabled);
          } else { // nothing we can do, don't log.
            log = Ext.emptyFn;
          }
        }
        return log;
      }

      function safeProxy(target, name) {
        return createLogger(target, name, function() {
          return logger.enabled;
        });
      }

      function levelAwareProxy(target, name) {
        return createLogger(target, name, function() {
          return logger.isEnabled(name);
        });
      }

      if ( !NX.global.console ) { // IE without webdev has no console object, so we create an empty one
        NX.global.console = {
          log : Ext.emptyFn
        };
      } else if ( !NX.global.console.log ) { // We rely on existence of console.log below
        NX.global.console.log = Ext.emptyFn;
      }

      Ext.each([
          'trace',
          //'log', skipping; as we may want to make an Ext.log compatible method
          'debug',
          'group',
          'info',
          'warn',
          'error'
      ], function (name) {
          logger[name] = levelAwareProxy(NX.global.console, name);
      });

      Ext.each([
          'groupEnd'
      ], function (name) {
          logger[name] = safeProxy(NX.global.console, name);
      });

      return logger;
  }());

  NX.log.debug('Logging initialized');

  // Compatibility
  Ext.ns('Nexus');
  Nexus.Log = NX.log;
  Nexus.log = NX.log.debug;

});

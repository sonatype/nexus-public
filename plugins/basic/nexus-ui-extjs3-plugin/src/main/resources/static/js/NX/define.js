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
/*global define,Ext,NX */

define('NX/define', ['NX/base', 'NX/assert', 'NX/log'], function() {
  Ext.ns('NX');

  /**
   * Used as the super-class for all NX.define classes which not not provide a class to extend.
   * Do not put anything special in here, since not everything will use this as its super-class.
   *
   * @constructor
   *
   * @private
   */
  NX.Base = function (config) {
      Ext.apply(this, config);
  };

  /**
   * Define a new class, in Extjs-4 compatible manner.
   *
   * @static
   *
   * @param {String} className        The name of the class to define.
   * @param {Object} data             Configuration for the class.
   * @param {Function} [createdFn]    Function to execute when class has been defined.
   *
   * @cfg {String} extend             Super-class name.
   * @cfg {*} statics                 Static members.
   * @cfg {Array} mixins              Mixin class names, does not support object configuration like extjs4 ATM.
   * @cfg {*} requirejs               Array of requirejs module dependencies, custom... defs not extjs4 compatible.
   * @cfg {*} requires                Array of class names (created by Ext.define) which are dependencies.
   * @cfg {boolean} requireSuper      Flag to enable/disable automatic dependency on super-class;
   *                                  Tries to auto-detect but just inc-ase you can enable/disable it.
   * @cfg {String} xtype              Automatically register class as Ext xtype component.
   * @cfg {boolean} singleton         Defines a singleton instead of a class.
   *                                  Given class name will reference singleton object and createdFn 'this' will be the singleton object.
   */
  NX.define = function (className, data, createdFn) {
      data = data || {};

      var i,
          nameSpace,
          simpleClassName,
          superName,
          type,
          requireSuper,
          superClass,
          statics,
          mixins,
          mixin,
          requiredClassNames,
          requiredModulePaths,
          moduleName,
          singleton,
          xtype,
          tmp;

      // Find the namespace (if any) for the new class
      i = className.lastIndexOf('.');
      if (i !== -1) {
          nameSpace = className.substring(0, i);
          simpleClassName = className.substring(i + 1);
      }
      else {
          simpleClassName = className;
      }

      requireSuper = data.requireSuper;
      delete data.requireSuper;

      // Determine the super-class
      if (data.extend !== undefined) {
          superName = data.extend;
          delete data.extend;

          // require super module if there is not already a defined class of that name for legacy support
          if (requireSuper === undefined) {
              requireSuper = !NX.isobj(superName);
          }
      }
      else {
          superName = 'NX.Base';
          requireSuper = false;
      }

      statics = data.statics;
      delete data.statics;

      mixins = data.mixins;
      delete data.mixins;

      singleton = data.singleton;
      delete data.singleton;

      xtype = data.xtype;
      delete data.xtype;

      // Extract requirejs dependencies
      requiredModulePaths = NX.arrayify(data.requirejs);
      delete data.requirejs;

      // Extract class dependencies (which were defined using Ext.define)
      requiredClassNames = NX.arrayify(data.requires);
      delete data.requires;

      // Require super if enabled
      if (requireSuper === true) {
          requiredClassNames.push(superName);
      }

      // Require mixins
      if (mixins !== undefined) {
          for(i=0; i<mixins.length; i++) {
              requiredClassNames.push(mixins[i]);
          }
      }

      // append translated dependency classes on to required modules
      for (i=0; i < requiredClassNames.length; i++) {
          tmp = requiredClassNames[i].replaceAll('.', '/');
          requiredModulePaths.push(tmp);
      }

      // Translate class name into module name
      moduleName = className.replaceAll('.', '/');

      if (requiredModulePaths.length !== 0) {
          NX.log.debug('Defining module:', moduleName, 'depends:', requiredModulePaths);
      }
      else {
          NX.log.debug('Defining module:', moduleName);
      }

      define(moduleName, requiredModulePaths, function()
      {
          NX.log.debug('Defining class:', className, 'super:', superName);

          // Sanity check required classes exist
          Ext.each(requiredClassNames, function(className) {
              NX.assert(NX.isobj(className), 'Missing required class: ' + className);
          });

          // Create namespace if required
          if (nameSpace) {
              Ext.namespace(nameSpace);
          }

          // Get a reference to the super-class
          superClass = NX.obj(superName);

          // When no constructor given in configuration (its always there due to picking up from Object.prototype), use a synthetic version
          if (data.constructor === Object.prototype.constructor) {
              data.constructor = function () {
                  superClass.prototype.constructor.apply(this, arguments);
              };
          }

          // Create the sub-class
          type = Ext.extend(superClass, data);

          // Remember class name (full and simple)
          type.$className = className;
          type.$simpleClassName = simpleClassName;
          type.prototype.$className = className;
          type.prototype.$simpleClassName = simpleClassName;

          // replace toString if its the default Object.toString
          if (type.prototype.toString === Object.prototype.toString) {
              type.prototype.toString = function() {
                  return '[object ' + className + ']';
              };
          }

          // Handle mixins
          if (mixins !== undefined) {
              for(i=0; i<mixins.length; i++) {
                  mixin = mixins[i]; // name
                  NX.log.debug('Applying mixin:', mixin);
                  mixin = NX.obj(mixin); // class ref
                  Ext.applyIf(type.prototype, mixin.prototype);
              }
          }

          // Apply any static members
          if (statics !== undefined) {
              Ext.apply(type, statics);
          }

          // Register xtype for class
          if (xtype !== undefined) {
              Ext.reg(xtype, type);
          }


          // When singleton; type becomes new instance
          // NOTE: `type` is passed into the creation callback function later on, so the variable must hold the actual instance for singletons
          if (singleton !== undefined) {
              type = new type();
          }

          // Assign to global namespace
          NX.obj(nameSpace)[simpleClassName] = type;


          // Call post-define hook
          if (createdFn !== undefined) {
              // Scope to created type, empty args seems to be required here
              createdFn.call(type, []);
          }

          return type;
      });
  };

  // Compatibility
  Ext.define = NX.define;
});
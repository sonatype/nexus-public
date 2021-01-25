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
/*global Ext, NX*/

/**
 * Wizard step.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.wizard.Step', {
  alias: 'widget.nx-wizard-step',
  requires: [
    'NX.Assert'
  ],
  mixins: {
    observable: 'Ext.util.Observable',
    logAware: 'NX.LogAware'
  },

  config: {
    /**
     * Class name of screen.
     *
     * @cfg {String}
     */
    screen: undefined,

    /**
     * Automatically reset step when moving back.
     *
     * @cfg {Boolean}
     */
    resetOnBack: false,

    /**
     * Set to false to disable step.
     *
     * @cfg {Boolean}
     */
    enabled: true
  },

  /**
   * The controller which the step is attached to.
   *
   * @protected {NX.wizard.Controller}
   */
  controller: undefined,

  /**
   * Step name.
   *
   * @private {String}
   */
  name: undefined,

  /**
   * Screen class.
   *
   * @private {Ext.Class}
   */
  screenClass: undefined,

  /**
   * Screen xtype.
   *
   * @private {String}
   */
  screenXtype: undefined,

  /**
   * @constructor
   */
  constructor: function (config) {
    var me = this;
    me.mixins.observable.constructor.call(me, config);
    me.initConfig(config);

    me.name = Ext.getClassName(me);
    me.screenClass = Ext.ClassManager.get(me.getScreen());
    // TODO: Sort out if this is proper api to get xtype from class, this may not be portable to new versions of extjs
    me.screenXtype = me.screenClass.xtype;
  },

  /**
   * Returns step name.  Defaults to class-name.
   *
   * @return {String}
   */
  getName: function () {
    return this.name;
  },

  /**
   * Returns screen class.
   *
   * @return {Ext.Class}
   */
  getScreenClass: function () {
    return this.screenClass;
  },

  /**
   * Returns screen xtype.
   *
   * @return {String}
   */
  getScreenXtype: function () {
    return this.screenXtype;
  },

  /**
   * Returns screen component.
   *
   * @return {NX.wizard.Screen|undefined}
   */
  getScreenCmp: function () {
    var xtype = this.screenXtype,
        matches = Ext.ComponentQuery.query(xtype);

    if (matches.length === 0) {
      return undefined;
    }

    //<if assert>
    NX.Assert.assert(matches.length === 1, 'Expected 1 component matching:', xtype, '; found:', matches.length);
    //</if>

    return matches[0];
  },

  /**
   * Create screen component or component configuration.
   *
   * @return {Object|NX.wizard.Screen}
   */
  createScreenCmp: function () {
    return {
      xtype: this.screenXtype
    };
  },

  //
  // Lifecycle
  //

  /**
   * Attach step to controller and initialize step.
   *
   * @param {NX.wizard.Controller} controller
   */
  attach: function (controller) {
    var me = this;

    me.controller = controller;

    // TODO: We could probably simplify this by logic in controller for nx-wizard-screen
    // TODO: and could probably do with some better eventing between screen + step and avoid this global listener bloat

    // setup core screen event handlers
    me.controller.control(me.screenXtype, {
      activate: {
        fn: me.doActivate,
        scope: me
      }
    });

    // initialize step
    me.init();

    //<if debug>
    me.logDebug('Attached');
    //</if>
  },

  /**
   * Initialize state.
   *
   * @protected
   * @template
   */
  init: Ext.emptyFn,

  /**
   * @private
   * @type {boolean}
   */
  prepared: false,

  /**
   * @private
   */
  doActivate: function() {
    var me = this;

    //<if debug>
    me.logDebug('Activate');
    //</if>

    if (!me.prepared) {
      //<if debug>
      me.logDebug('Preparing');
      //</if>

      me.prepare();
      me.prepared = true;
    }
  },

  /**
   * Prepare state.
   *
   * @protected
   * @template
   */
  prepare: Ext.emptyFn,

  /**
   * Refresh state.
   *
   * @template
   */
  refresh: Ext.emptyFn,

  /**
   * Reset state.
   */
  reset: function() {
    this.prepared = false;
    this.setEnabled(true);

    //<if debug>
    this.logDebug('Reset');
    //</if>
  },

  //
  // Events
  //

  /**
   * Helper to register listeners relative to screen component.
   *
   * Special handling for '$screen' selector to reference the screen itself.
   *
   * @protected
   * @param {Object} selectors
   */
  control: function (selectors) {
    var me = this,
        xtype = me.screenXtype,
        ctrl = me.controller;

    Ext.Object.each(selectors, function (selector, listeners) {
      var q;
      if (selector === '$screen') {
        q = xtype;
      }
      else {
        q = xtype + ' ' + selector;
      }
      ctrl.control(q, me.normalizeListeners(listeners));
    });
  },

  /**
   * Normalize listeners to ensure scope to step, unless otherwise configured.
   *
   * @private
   * @param {Object} listeners
   * @return {Object}
   */
  normalizeListeners: function(listeners) {
    var me = this,
        entry,
        listener;

    for (entry in listeners) {
      if (listeners.hasOwnProperty(entry)) {
        listener = listeners[entry];

        // if listener is a function convert to object and apply scope
        if (Ext.isFunction(listener)) {
          listener = {
            fn: listener,
            scope: me
          };
          listeners[entry] = listener;
        }
        else {
          // else apply scope if not specified
          Ext.applyIf(listener, {
            scope: me
          });
        }
      }
    }

    return listeners;
  },

  /**
   * Helper to register listeners on controller.
   *
   * @protected
   * @param {Object} to
   */
  listen: function (to) {
    var me = this,
        domain,
        selectors,
        selector,
        ctrl = me.controller;

    // normalize all listeners
    for (domain in to) {
      if (to.hasOwnProperty(domain)) {
        selectors = to[domain];
        for (selector in selectors) {
          if (selectors.hasOwnProperty(selector)) {
            me.normalizeListeners(selectors[selector]);
          }
        }
      }
    }

    ctrl.listen(to);
  },

  //
  // Context
  //

  /**
   * Get the shared context.
   *
   * @returns {Ext.util.MixedCollection}
   */
  getContext: function() {
    return this.controller.getContext();
  },

  /**
   * Get shared context value.
   *
   * @protected
   * @param {String} name
   * @return {Object|undefined}
   */
  get: function (name) {
    return this.getContext().get(name);
  },

  /**
   * Set shared context value.
   *
   * @protected
   * @param {String} name
   * @param {Object} value
   */
  set: function (name, value) {
    this.getContext().add(name, value);
  },

  /**
   * Unset shared context value.
   *
   * @protected
   * @param {String} name
   */
  unset: function (name) {
    this.getContext().removeAtKey(name);
  },

  //
  // Navigation
  //

  /**
   * Request move to next step.
   *
   * @protected
   */
  moveNext: function () {
    this.controller.moveNext();
  },

  /**
   * Request move to previous step.
   *
   * @protected
   */
  moveBack: function () {
    this.controller.moveBack();

    // optionally reset when moving back
    if (this.getResetOnBack()) {
      this.reset();
    }
  },

  /**
   * Request cancel.
   *
   * @protected
   */
  cancel: function () {
    this.controller.cancel();
  },

  /**
   * Inform finished.
   *
   * @protected
   */
  finish: function () {
    this.controller.finish();
  },

  //
  // Helpers
  //

  /**
   * Return a store from controller by name.
   *
   * @protected
   * @param {String} name
   * @returns {Ext.data.Store}
   */
  getStore: function(name) {
    return this.controller.getStore(name);
  },

  /**
   * Display content mask.
   *
   * @protected
   * @param {String} message
   */
  mask: function (message) {
    this.controller.mask(message);
  },

  /**
   * Remove content mask.
   *
   * @protected
   */
  unmask: function () {
    this.controller.unmask();
  }
});

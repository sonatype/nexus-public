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
/*global Ext*/

/**
 * Access point for available {NX.util.condition.Condition}s.
 *
 * @since 3.0
 */
Ext.define('NX.Conditions', {
  singleton: true,
  requires: [
    'NX.util.condition.Conjunction',
    'NX.util.condition.Disjunction',
    'NX.util.condition.FormHasRecord',
    'NX.util.condition.FormIs',
    'NX.util.condition.GridHasSelection',
    'NX.util.condition.IsPermitted',
    'NX.util.condition.StoreHasRecords',
    'NX.util.condition.MultiListener',
    'NX.util.condition.WatchState',
    'NX.util.condition.NeverSatisfied',
    'NX.util.condition.HasNoFirewall'
  ],

  /**
   * @param {String} permission permission
   * @returns {NX.util.condition.IsPermitted}
   */
  isPermitted: function (permission) {
    return Ext.create('NX.util.condition.IsPermitted', { permission: permission });
  },

  /**
   * @param {String} store id of store that should have records
   * @returns {NX.util.condition.StoreHasRecords}
   */
  storeHasRecords: function (store) {
    return Ext.create('NX.util.condition.StoreHasRecords', { store: store });
  },

  /**
   * @param {Ext.app.Controller} the controller to use to detect events on the grid
   * @param {String} grid a grid selector as specified by {@link Ext.ComponentQuery#query}
   * @param {Function} [fn] to be called when grid has a selection to perform additional checks on the passed in model
   * @returns {NX.util.condition.GridHasSelection}
   */
  gridHasSelection: function (grid, fn) {
    return Ext.create('NX.util.condition.GridHasSelection', { grid: grid, fn: fn });
  },

  /**
   * @param {String} form a {@link NX.view.SettingsForm} selector as specified by {@link Ext.ComponentQuery#query}
   * @param {Function} [fn] to be called when form has a record to perform additional checks on the passed in model
   * @returns {NX.util.condition.FormHasRecord}
   */
  formHasRecord: function (form, fn) {
    return Ext.create('NX.util.condition.FormHasRecord', { form: form, fn: fn });
  },

  /**
   * @param {Array} [listenerConfigs] An array of objects { observable: o, events:[] }
   * @param {Function} [fn] A function to be called when an event occurs.
   * @returns {NX.util.condition.MultiListener}
   */
  watchEvents: function (listenerConfigs, fn) {
    return Ext.create('NX.util.condition.MultiListener', { fn: fn, listenerConfigs: listenerConfigs });
  },

  /**
   * @param {String|Ext.Component} [form] A selector or a reference to a form
   * @param {Function} [condition] A function to evaluate against the form to fire the satisfied or unsatisfied event
   * @returns {NX.util.condition.FormIs}
   */
  formIs: function (form, condition) {
    return Ext.create('NX.util.condition.FormIs', { form: form, condition: condition });
  },

  /**
   * @param {String} key state value key
   * @param {Function} [fn] An optional function to be called when a state value changes. If not specified, a boolean
   * check against value will be performed
   * @returns {NX.util.condition.WatchState}
   */
  watchState: function (key, fn) {
    return Ext.create('NX.util.condition.WatchState', { key: key, fn: fn });
  },

  /**
   * Takes as parameter {@link NX.util.condition.Condition}s to be AND-ed.
   *
   * @returns {NX.util.condition.Conjunction}
   */
  and: function () {
    return Ext.create('NX.util.condition.Conjunction', { conditions: Array.prototype.slice.call(arguments) });
  },

  /**
   * Takes as parameter {@link NX.util.condition.Condition}s to be OR-ed.
   *
   * @returns {NX.util.condition.Disjunction}
   */
  or: function () {
    return Ext.create('NX.util.condition.Disjunction', { conditions: Array.prototype.slice.call(arguments) });
  },

  /**
   * No-op condition that is never satisfied.
   *
   * @returns {NX.util.condition.NeverSatisfied}
   */
  never: function() {
    return Ext.create('NX.util.condition.NeverSatisfied');
  },

  /**
   * Checks if this NXRM instance does not have firewall
   *
   * @returns
   */
  hasNoFirewall: function() {
    return Ext.create('NX.util.condition.HasNoFirewall');
  }

});

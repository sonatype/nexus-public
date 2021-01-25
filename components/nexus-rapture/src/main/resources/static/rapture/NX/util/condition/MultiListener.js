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
 * A {@link NX.util.condition.Condition} that calls a function after event(s) fire.
 *
 * @since 3.14
 */
Ext.define('NX.util.condition.MultiListener', {
  extend: 'NX.util.condition.Condition',

  /**
   * @cfg {Array}
   *
   * An array of objects with an observable property and an array of event names
   */
  listenerConfigs: undefined,

  /**
   * The function to be called when an event occurs.
   *
   * @cfg {Function}
   */
  fn: undefined,

  /**
   * @override
   * @returns {NX.util.condition.MultiListener}
   */
  bind: function () {
    var me = this;

    if (!me.bounded) {
      if (!Ext.isFunction(me.fn)) {
        throw "fn must be a valid function";
      }
      if (!Ext.isArray(me.listenerConfigs)) {
        throw "listenerConfigs must be an array";
      }

      me.listenerConfigs.forEach(function(listenerConfig) {
        listenerConfig.events.forEach(function(event) {
          listenerConfig.observable.on(event, me.evaluate, me);
        });
      });

      me.callParent();
      me.evaluate();
    }

    return me;
  },

  /**
   * @private
   */
  evaluate: function () {
    var me = this;

    if (me.bounded) {
      me.setSatisfied(me.fn());
    }
  },

  /**
   * @override
   * @returns {String}
   */
  toString: function () {
    return this.self.getName() + '{ key=' + this.key + ' }';
  }

});

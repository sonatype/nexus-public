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
 * A {@link NX.util.condition.Condition} that is satisfied applying a function on a state value.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.WatchState', {
  extend: 'NX.util.condition.Condition',
  requires: [
    'NX.State'
  ],

  /**
   * @cfg {String}
   *
   * State value key.
   */
  key: undefined,

  /**
   * An optional function to be called when a state value changes. If not specified, a boolean check
   * against value will be performed.
   *
   * @cfg {Function}
   */
  fn: undefined,

  /**
   * @override
   * @returns {NX.util.condition.WatchState}
   */
  bind: function () {
    var me = this,
        controller, listeners;

    if (!me.bounded) {
      if (!Ext.isDefined(me.fn)) {
        me.fn = function (value) {
          return value;
        };
      }
      controller = NX.getApplication().getController('State');
      listeners = { scope: me };
      listeners[me.key.toLowerCase() + 'changed'] = me.evaluate;
      me.mon(controller, listeners);
      me.callParent();
      me.evaluate(NX.State.getValue(me.key));
    }

    return me;
  },

  /**
   * @private
   */
  evaluate: function (value, oldValue) {
    var me = this;

    if (me.bounded) {
      me.setSatisfied(me.fn(value, oldValue));
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
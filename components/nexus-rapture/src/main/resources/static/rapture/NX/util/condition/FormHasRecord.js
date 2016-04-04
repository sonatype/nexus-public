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
/*global Ext*/

/**
 * A {@link NX.util.condition.Condition} that is satisfied when a {@link NX.view.SettingsForm}, specified by its
 * selector, exists and has a record. Optionally, a function could be used to provide additional checking when form has
 * a record.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.FormHasRecord', {
  extend: 'NX.util.condition.Condition',

  /**
   * A form selector as specified by (@link Ext.ComponentQuery#query}.
   *
   * @cfg {String}
   */
  form: undefined,

  /**
   * An optional function to be called when form has a record to perform additional checks on the passed in model.
   *
   * @cfg {Function}
   */
  fn: undefined,

  /**
   * @override
   * @returns {NX.util.condition.FormHasRecord}
   */
  bind: function () {
    var me = this,
        components = {}, queryResult;

    if (!me.bounded) {
      components[me.form] = {
        afterrender: me.evaluate,
        recordloaded: me.evaluate,
        destroy: me.evaluate
      };
      Ext.app.EventBus.listen({ component: components }, me);
      me.callParent();
      queryResult = Ext.ComponentQuery.query(me.form);
      if (queryResult && queryResult.length > 0) {
        me.evaluate(queryResult[0]);
      }
    }

    return me;
  },

  /**
   * @private
   */
  evaluate: function (form) {
    var me = this,
        satisfied = false,
        model;

    if (me.bounded) {
      if (Ext.isDefined(form) && form.isXType('form')) {
        model = form.getRecord();
        if (model) {
          satisfied = true;
          if (Ext.isFunction(me.fn)) {
            satisfied = me.fn(model) === true;
          }
        }
      }
      me.setSatisfied(satisfied);
    }
  },

  /**
   * @override
   * @returns {String}
   */
  toString: function () {
    return this.self.getName() + '{ form=' + this.form + ' }';
  }

});
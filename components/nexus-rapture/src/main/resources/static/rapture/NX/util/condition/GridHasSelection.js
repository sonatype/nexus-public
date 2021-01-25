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
 * A {@link NX.util.condition.Condition} that is satisfied when a grid, specified by its selector, exists and has a
 * selection. Optionally, a function could be used to provide additional checking when grid has a selection.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.GridHasSelection', {
  extend: 'NX.util.condition.Condition',

  /**
   * A grid selector as specified by (@link Ext.ComponentQuery#query}.
   *
   * @cfg {String}
   */
  grid: undefined,

  /**
   * An optional function to be called when grid has a selection to perform additional checks on the
   * passed in model.
   *
   * @cfg {Function}
   */
  fn: undefined,

  /**
   * @override
   * @returns {NX.util.condition.GridHasSelection}
   */
  bind: function () {
    var me = this,
        gridQueryResult = Ext.ComponentQuery.query(me.grid),
        cmp = gridQueryResult && gridQueryResult.length ? gridQueryResult[0] : null;

    if (!me.bounded) {
      cmp.on({
        cellclick: me.cellclick,
        selectionchange: me.selectionchange,
        destroy: me.destroy,
        scope: me
      });
      me.callParent();
    }

    return me;
  },

  /**
   * @private
   */
  cellclick: function (cmp, td, cellIndex, model) {
    this.evaluate(cmp, model);
  },

  /**
   * @private
   */
  selectionchange: function (cmp, selected) {
    this.evaluate(cmp, selected ? selected[0] : null);
  },

  /**
   * @private
   */
  destroy: function (cmp) {
    var me = this;
    if (cmp.getSelectionModel().getSelected()) {
      me.evaluate(cmp, cmp.getSelection());
    }
    else {
      me.evaluate(cmp, null);
    }
  },

  /**
   * @private
   */
  evaluate: function (cmp, selection) {
    var me = this,
        satisfied = false;

    if (cmp && selection) {
      satisfied = true;
      if (Ext.isFunction(me.fn)) {
        satisfied = me.fn(selection) === true;
      }
    }
    me.setSatisfied(satisfied);
  },

  /**
   * @override
   * @returns {String}
   */
  toString: function () {
    return this.self.getName() + '{ grid=' + this.grid + ' }';
  }

});
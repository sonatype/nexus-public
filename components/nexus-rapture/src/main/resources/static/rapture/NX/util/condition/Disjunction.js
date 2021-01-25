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
 * A {@link NX.util.condition.Condition} that is satisfied when any of OR-ed {@link NX.util.condition.Condition}s
 * is satisfied.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.Disjunction', {
  extend: 'NX.util.condition.Condition',

  /**
   * Array of conditions to be OR-ed.
   *
   * @cfg {NX.util.condition.Condition[]}
   */
  conditions: undefined,

  /**
   * @override
   * @returns {NX.util.condition.Disjunction}
   */
  bind: function () {
    var me = this;

    if (!me.bounded) {
      me.callParent();
      me.evaluate();
      Ext.each(me.conditions, function (condition) {
        me.mon(condition, {
          satisfied: me.evaluate,
          unsatisfied: me.evaluate,
          scope: me
        });
      });
    }

    return me;
  },

  /**
   * @private
   */
  evaluate: function () {
    var me = this,
        satisfied = false;

    if (me.bounded) {
      Ext.each(me.conditions, function (condition) {
        if(condition.satisfied){
          satisfied = true;
          return false;
        }
        return true;
      });
      me.setSatisfied(satisfied);
    }
  },

  /**
   * @override
   * @returns {String}
   */
  toString: function () {
    return this.conditions.join(' OR ');
  }

});

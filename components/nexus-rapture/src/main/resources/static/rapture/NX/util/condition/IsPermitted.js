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
 * A {@link NX.util.condition.Condition} that is satisfied when user has a specified permission.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.IsPermitted', {
  extend: 'NX.util.condition.Condition',

  /**
   * @private
   * @property {String}
   */
  permission: undefined,

  /**
   * @override
   * @returns {NX.util.condition.IsPermitted}
   */
  bind: function () {
    var me = this,
        controller;

    if (!me.bounded) {
      controller = NX.getApplication().getController('Permissions');
      me.mon(controller, {
        changed: me.evaluate,
        scope: me
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
      me.setSatisfied(NX.Permissions.check(me.permission));
    }
  },

  /**
   * @override
   * @returns {String}
   */
  toString: function () {
    return this.self.getName() + '{ permission=' + this.permission + ' }';
  },

  /**
   * Sets permission and re-evaluate.
   *
   * @public
   * @param {String} permission
   */
  setPermission: function(permission) {
    this.permission = permission;
    this.evaluate();
  }

});

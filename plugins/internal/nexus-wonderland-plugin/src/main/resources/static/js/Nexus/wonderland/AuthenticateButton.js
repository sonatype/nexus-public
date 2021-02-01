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
/**
 * Authenticate current user button.
 */
NX.define('Nexus.wonderland.AuthenticateButton', {
  extend: 'Ext.Button',

  requires: [
    'Nexus.siesta',
    'Nexus.wonderland.Icons',
    'Nexus.wonderland.view.AuthenticateWindow'
  ],

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  xtype: 'nx-wonderland-button-authenticate',
  cls: 'nx-wonderland-button-authenticate',

  /**
   * @cfg message
   * @type String
   */
  message: 'You have requested an operation which requires validation of your credentials.',

  /**
   * @cfg noPopUps
   * @type Boolean
   */
  noPopUps: false,

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.wonderland.Icons;

    Ext.apply(me, {
      cls: 'x-btn-text-icon',
      iconCls: icons.get('lock').cls,
      scope: me,
      handler: me.showWindow
    });

    me.constructor.superclass.initComponent.apply(me, arguments);

    // register events
    me.addEvents(
        /**
         * @event authenticated
         * @param {Button} self
         * @param {String} authentication ticket
         */
        'authenticated'
    );
  },

  /**
   * @private
   */
  showWindow: function () {
    var me = this;

    // bypass pop-up window for external users?
    if (me.noPopUps === true && Sonatype.user.curr.loggedInUserSource !== 'default') {
      me.fireEvent('authenticated', me, null);
    }
    else {
      var win = NX.create('Nexus.wonderland.view.AuthenticateWindow', {
        message: me.message,
        animateTarget: me.getEl(),
        listeners: {
          // HACK: propagate event from window to button, controller does not have context of the button
          'authenticated': function(window, authticket) {
            me.fireEvent('authenticated', me, authticket);
          }
        }
      });

      win.show();
    }
  }
});
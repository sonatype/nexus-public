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
/*global Ext, NX*/

/**
 * UI Session Timeout controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.UiSessionTimeout', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.ux.ActivityMonitor',
    'NX.Messages',
    'NX.Security',
    'NX.State',
    'NX.I18n',
    'NX.State'
  ],

  views: [
    'ExpireSession'
  ],

  refs: [
    {
      ref: 'expireSessionWindow',
      selector: 'nx-expire-session'
    }
  ],

  SECONDS_TO_EXPIRE: 30,

  activityMonitor: undefined,

  expirationTicker: undefined,

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#State': {
          userchanged: me.setupTimeout,
          uisettingschanged: me.onUiSettingsChanged,
          receivingchanged: me.setupTimeout
        }
      },
      component: {
        'nx-expire-session': {
          afterrender: me.startTicking
        },
        'nx-expire-session button[action=cancel]': {
          click: me.setupTimeout
        }
      }
    });
  },

  /**
   * @override
   */
  onLaunch: function () {
    this.setupTimeout();
  },

  /**
   * Reset UI session timeout when uiSettings.sessionTimeout changes.
   *
   * @private
   * @param {Object} uiSettings
   * @param {Number} uiSettings.sessionTimeout
   * @param {Object} oldUiSettings
   * @param {Number} oldUiSettings.sessionTimeout
   */
  onUiSettingsChanged: function (uiSettings, oldUiSettings) {
    uiSettings = uiSettings || {};
    oldUiSettings = oldUiSettings || {};

    if (uiSettings.sessionTimeout !== oldUiSettings.sessionTimeout) {
      this.setupTimeout();
    }
  },

  /**
   * @private
   */
  setupTimeout: function () {
    var me = this,
        user = NX.State.getUser(),
        uiSettings = NX.State.getValue('uiSettings') || {},
        sessionTimeout = user ? uiSettings['sessionTimeout'] : undefined;

    me.cancelTimeout();
    if ((user && NX.State.isReceiving()) && sessionTimeout > 0) {
      //<if debug>
      me.logDebug('Session expiration enabled for', sessionTimeout, 'minutes');
      //</if>

      me.activityMonitor = Ext.create('Ext.ux.ActivityMonitor', {
        // check every second
        interval: 1000,
        maxInactive: ((sessionTimeout * 60) - me.SECONDS_TO_EXPIRE) * 1000,
        isInactive: Ext.bind(me.showExpirationWindow, me)
      });
      me.activityMonitor.start();
    }
  },

  /**
   * @private
   */
  cancelTimeout: function () {
    var me = this,
        expireSessionView = me.getExpireSessionWindow();

    // close the window if the session has not yet expired or if the server is disconnected
    if (expireSessionView && (!expireSessionView.sessionExpired() || !NX.State.isReceiving())) {
      expireSessionView.close();
    }

    if (me.activityMonitor) {
      me.activityMonitor.stop();
      delete me.activityMonitor;

      //<if debug>
      me.logDebug('Activity monitor disabled');
      //</if>
    }

    if (me.expirationTicker) {
      me.expirationTicker.destroy();
      delete me.expirationTicker;

      //<if debug>
      me.logDebug('Session expiration disabled');
      //</if>
    }
  },

  /**
   * @private
   */
  showExpirationWindow: function () {
    NX.Messages.add({text: NX.I18n.get('UiSessionTimeout_Expire_Message'), type: 'warning'});
    this.getExpireSessionView().create();
  },

  /**
   * @private
   */
  startTicking: function (win) {
    var me = this;

    me.expirationTicker = Ext.util.TaskManager.newTask({
      run: function (count) {
        win.down('label').setText(NX.I18n.format('UiSessionTimeout_Expire_Text', me.SECONDS_TO_EXPIRE - count));
        if (count === me.SECONDS_TO_EXPIRE) {
          win.down('label').setText(NX.I18n.get('SignedOut_Text'));
          win.down('button[action=close]').show();
          win.down('button[action=signin]').show();
          win.down('button[action=cancel]').hide();
          NX.Messages.add({
            text: NX.I18n.format('UiSessionTimeout_Expired_Message', NX.State.getValue('uiSettings')['sessionTimeout']),
            type: 'warning'
          });
          NX.Security.signOut();
        }
      },
      interval: 1000,
      repeat: me.SECONDS_TO_EXPIRE
    });
    me.expirationTicker.start();
  }

});

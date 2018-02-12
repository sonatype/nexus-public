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
 * CLM controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Clm', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State',
    'NX.Messages',
    'NX.Permissions',
    'NX.Windows',
    'NX.I18n'
  ],

  stores: [
    'ClmApplication'
  ],
  views: [
    'clm.ClmSettings',
    'clm.ClmDashboard',
    'clm.ClmSettingsTestResults'
  ],
  refs: [
    {
      ref: 'clmSettingsForm',
      selector: 'nx-coreui-clm-settings'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    // preload icon to avoid a grey icon when clm dashboard feature will be registered
    me.getApplication().getIconController().addIcons({
      'feature-clm-dashboard': {
        file: 'clm_dashboard.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/IQ/Server',
      text: NX.I18n.get('Clm_Text'),
      description: NX.I18n.get('Clm_Description'),
      view: { xtype: 'nx-coreui-clm-settings' },
      iconConfig: {
        file: 'clm_server.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, me);

    me.onClmStateChanged(NX.State.getValue('clm'), undefined, true);

    me.listen({
      controller: {
        '#State': {
          clmchanged: me.onClmStateChanged
        }
      },
      component: {
        'nx-coreui-clm-settings button[action=verify]': {
          click: me.verifyConnection
        },
        'nx-coreui-clm-settings nx-settingsform': {
          submitted: me.onSubmitted
        },
        'nx-coreui-clm-dashboard': {
          activate: me.openDashboardWindow
        },
        'nx-coreui-clm-dashboard button[action=open]': {
          click: me.openDashboardWindow
        }
      }
    });
  },

  /**
   * Verify the connection to the IQ Server.
   * @private
   */
  verifyConnection: function (button) {
    var form = button.up('form'),
        values = form.getForm().getFieldValues();

    form.getEl().mask('Checking connection to ' + values.url);

    NX.direct.clm_CLM.verifyConnection(values, function (response) {
      form.getEl().unmask();
      if (Ext.isDefined(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('Clm_Connection_Success', values.url), type: 'success' });
        Ext.widget('nx-coreui-clm-settings-testresults', {applications: response.data});
      }
    });
  },

  /**
   * @private
   */
  onSubmitted: function(form, action) {
    NX.State.setValue('clm', Ext.apply(Ext.clone(NX.State.getValue('clm', {})), {
      enabled: action.result.data.enabled,
      url: action.result.data.url
    }));
  },

  /**
   * Update the global UI state to register/unregister this Feature.
   * @param newState
   * @param oldState
   * @param avoidMenuRefresh
   * @private
   */
  onClmStateChanged: function (newState, oldState, avoidMenuRefresh) {
    var me = this,
        features = me.getApplication().getFeaturesController(),
        shouldRefreshMenu = false;

    if (oldState && oldState.enabled && oldState.url) {
      features.unregisterFeature({
        mode: 'admin',
        path: '/IQ/Dashboard'
      });
      shouldRefreshMenu = true;
    }
    if (newState && newState.enabled && newState.url) {
      features.registerFeature({
        mode: 'admin',
        path: '/IQ/Dashboard',
        text: NX.I18n.get('Clm_Dashboard_Title'),
        description: NX.I18n.get('Clm_Dashboard_Description'),
        view: { xtype: 'nx-coreui-clm-dashboard' },
        weight: 10,
        // use preloaded icon to avoid a grey icon
        iconName: 'feature-clm-dashboard',
        visible: function () {
          var clmState = NX.State.getValue('clm');
          return NX.Permissions.check('nexus:settings:read') && clmState && clmState.enabled && clmState.url;
        }
      }, me);
      shouldRefreshMenu = true;
    }
    if (!avoidMenuRefresh && shouldRefreshMenu) {
      me.getController('Menu').refreshMenu();
    }
  },

  /**
   * @private
   */
  openDashboardWindow: function() {
    var state = NX.State.getValue('clm');

    NX.Windows.open(state.url);
  },

  /**
   * Load the list of applications available from the configured server and show it in a modal window.
   * @private
   */
  loadApplications: function () {
    this.getClmClmApplicationsView().create().show();
  }

});

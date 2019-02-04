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
    'clm.ClmSettingsTestResults'
  ],
  refs: [
    {
      ref: 'clmSettingsForm',
      selector: 'nx-coreui-clm-settings'
    },
    {
      ref: 'openDashboardButton',
      selector: 'nx-coreui-clm-settings button[action=open]'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/IQ',
      text: NX.I18n.get('Clm_Text'),
      description: NX.I18n.get('Clm_Description'),
      view: { xtype: 'nx-coreui-clm-settings' },
      iconConfig: {
        file: 'three_tags.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, me);

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
        'nx-coreui-clm-settings button[action=open]': {
          afterrender: me.onClmStateChanged,
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
   * Enable/disable the openDashboardButton
   * @private
   */
  onClmStateChanged: function () {
    var clmState = NX.State.getValue('clm'),
        openDashboardButton = this.getOpenDashboardButton(),
        enableOpenDashboardButton = NX.Permissions.check('nexus:settings:read') && clmState && clmState.enabled && clmState.url;

    // when Capability is modified but page isn't loaded
    if (!openDashboardButton) {
      return;
    }

    if (enableOpenDashboardButton) {
      openDashboardButton.enable();
    } else {
      openDashboardButton.disableWithTooltip(NX.I18n.get('Clm_Dashboard_Disabled_Tooltip'));
    }
  },

  /**
   * @private
   */
  openDashboardWindow: function() {
    var state = NX.State.getValue('clm');

    NX.Windows.open(state.url);
  }

});

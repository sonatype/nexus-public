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
 * Licensing controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Licensing', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.Permissions',
    'NX.Messages',
    'NX.Security',
    'NX.I18n'
  ],

  views: [
    'licensing.LicenseAgreement',
    'licensing.LicensingDetails'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-coreui-licensing-details'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/System/Licensing',
      text: NX.I18n.get('Licensing_Text'),
      description: NX.I18n.get('Licensing_Description'),
      view: { xtype: 'nx-coreui-licensing-details' },
      iconConfig: {
        file: 'license_key.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:licensing:read');
      }
    }, me);

    me.listen({
      component: {
        'nx-coreui-licensing-details button[action=install]': {
          click: me.install,
          afterrender: me.bindInstallButton
        },
        'nx-coreui-licensing-agreement button[action=agree]': {
          click: me.onAgree
        }
      }
    });
  },

  /**
   * @private
   */
  showAgreementWindow: function () {
    return Ext.widget('nx-coreui-licensing-agreement');
  },

  /**
   * @private
   * Install license, as user agreed.
   */
  onAgree: function (button) {
    var me = this,
        win = button.up('window'),
        form = win.licenseForm,
        authToken = win.authToken;

    win.close();
    form.submit({
      params: {
        authToken: authToken
      },
      success: function () {
        NX.Messages.add({ text: NX.I18n.get('Licensing_Install_Success'), type: 'success' });
        me.getPanel().down('nx-settingsform').load(); //reload to pick up server changes
        me.getPanel().down('nx-settingsform').show();
      }
    });
  },

  /**
   * @override
   * @private
   * Enable 'Install' when user has 'create' permission.
   */
  bindInstallButton: function (button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:licensing:create')
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Install a license after successful authentication.
   */
  install: function (button) {
    var me = this,
        form = button.up('form');
    NX.Security.doWithAuthenticationToken(
        NX.I18n.format('Licensing_Authentication_Validation', 'Installing'),
        {
          success: function (authToken) {
            Ext.apply(me.showAgreementWindow(), { licenseForm: form, authToken: authToken });
            me.getPanel().down('form').show();
          }
        }
    );
  }
});

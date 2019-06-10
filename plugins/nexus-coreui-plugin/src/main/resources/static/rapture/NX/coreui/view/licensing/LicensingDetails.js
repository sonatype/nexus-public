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
 * Licensing details panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.licensing.LicensingDetails', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-licensing-details',
  requires: [
    'NX.I18n',
    'Ext.util.Cookies'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var dateFormat = 'D, M d, Y';
    Ext.apply(this, {
      settingsForm: [
        {
          xtype: 'nx-settingsform',
          api: {
            load: 'NX.direct.licensing_Licensing.read'
          },
          listeners: {
            'actioncomplete': function(form, action, eOpts) {
              var fieldValues = form.getFieldValues();
              if(!fieldValues.contactCompany) {
                this.hide();  
              }
            }
          }, 
          itemId: 'details',
          defaults: {
            xtype: 'textfield',
            readOnly: true
          },
          items: [
            {
              name: 'contactCompany',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_Company_FieldLabel'),
              xtype: 'displayfield'
            },
            {
              name: 'contactName',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_Name_FieldLabel'),
              xtype: 'displayfield'
            },
            {
              name: 'contactEmail',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_Email_FieldLabel'),
              xtype: 'displayfield'
            },
            {
              name: 'effectiveDate',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_EffectiveDate_FieldLabel'),
              xtype: 'nx-datedisplayfield',
              format: dateFormat
            },
            {
              name: 'expirationDate',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_ExpirationDate_FieldLabel'),
              xtype: 'nx-datedisplayfield',
              format: dateFormat
            },
            {
              name: 'licenseType',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_Type_FieldLabel'),
              xtype: 'displayfield'
            },
            {
              name: 'licensedUsers',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_LicensedUsers_FieldLabel'),
              xtype: 'displayfield'
            },
            {
              name: 'fingerprint',
              fieldLabel: NX.I18n.get('Licensing_LicensingDetails_Fingerprint_FieldLabel'),
              xtype: 'displayfield'
            }
          ],
          buttons: undefined
        },
        {
          xtype: 'form',
          title: NX.I18n.get('Licensing_LicensingDetails_InstallLicense_Title'),
          ui: 'nx-subsection',
          frame: true,

          api: {
            submit: 'NX.direct.licensing_Licensing.install'
          },
          baseParams: {
            'NX-ANTI-CSRF-TOKEN': Ext.util.Cookies.get('NX-ANTI-CSRF-TOKEN')
          },

          items: [
            {
              xtype: 'label',
              html: NX.I18n.get('Licensing_LicensingDetails_InstallLicense_Html')
            },
            {
              xtype: 'fileuploadfield',
              name: 'license',
              allowBlank: false,
              buttonText: NX.I18n.get('Licensing_LicensingDetails_LicenseSelect_Button'),
              buttonConfig: {
                glyph: 'xf016@FontAwesome' /* fa-file-o */
              }
            }
          ],

          buttonAlign: 'left',
          buttons: [
            { text: NX.I18n.get('Licensing_LicensingDetails_LicenseInstall_Button'), action: 'install', formBind: true, ui: 'nx-primary', glyph: 'xf023@FontAwesome' /* fa-lock */ }
          ]
        }
      ]
    });

    this.callParent();
  }

});

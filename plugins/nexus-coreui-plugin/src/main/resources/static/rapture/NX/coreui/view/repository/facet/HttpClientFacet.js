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
 * Configuration specific to Http connections for repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.HttpClientFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-httpclient-facet',
  requires: [
    'NX.I18n'
  ],
  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_HttpClientFacet_Title'),

        items: [
          {
            xtype: 'nx-optionalfieldset',
            title: NX.I18n.get('Repository_Facet_HttpClientFacet_Authentication_Title'),
            checkboxToggle: true,
            checkboxName: 'authEnabled',
            collapsed: true,
            items: [
              {
                xtype: 'combo',
                name: 'attributes.httpclient.authentication.type',
                fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_FieldLabel'),
                editable: false,
                store: [
                  ['username', NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_Username')],
                  ['ntlm', NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_NTLM')]
                ],
                value: 'username' ,
                listeners: {
                  'change': function(combo) {
                    var ntlmFields = this.up('form').down('#ntlmFields');

                    if(combo.getValue() === 'ntlm') {
                      ntlmFields.show();
                      ntlmFields.enable();
                    }
                    else {
                      ntlmFields.hide();
                      ntlmFields.disable();
                    }
                  }
                }
              },
              {
                xtype:'textfield',
                name: 'attributes.httpclient.authentication.username',
                fieldLabel: NX.I18n.get('System_AuthenticationSettings_Username_FieldLabel'),
                allowBlank: false
              },
              {
                xtype: 'textfield',
                inputType: 'password',
                name: 'attributes.httpclient.authentication.password',
                fieldLabel: NX.I18n.get('System_AuthenticationSettings_Password_FieldLabel'),
                allowBlank: false
              },
              {
                xtype: 'fieldcontainer',
                itemId: 'ntlmFields',
                hidden: true,
                items:[
                  {
                    xtype:'textfield',
                    name: 'attributes.httpclient.authentication.ntlmHost',
                    fieldLabel: NX.I18n.get('System_AuthenticationSettings_WindowsNtlmHostname_FieldLabel')
                  },
                  {
                    xtype:'textfield',
                    name: 'attributes.httpclient.authentication.ntlmDomain',
                    fieldLabel: NX.I18n.get('System_AuthenticationSettings_WindowsNtlmDomain_FieldLabel')
                  }
                ]
              }
            ]
          },
          {
            xtype: 'nx-optionalfieldset',
            title: NX.I18n.get('Repository_Facet_HttpClientFacet_HTTP_Title'),
            checkboxToggle: true,
            checkboxName: 'httpRequestSettings',
            collapsed: true,
            items: [
              {
                xtype: 'textfield',
                name: 'attributes.httpclient.connection.userAgentSuffix',
                fieldLabel: NX.I18n.get('System_HttpRequestSettings_UserAgentCustomization_FieldLabel'),
                helpText: NX.I18n.get('System_HttpRequestSettings_UserAgentCustomization_HelpText')
              },
              {
                xtype: 'numberfield',
                name: 'attributes.httpclient.connection.retries',
                fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_ConnectionRetries_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_HttpClientFacet_ConnectionRetries_HelpText'),
                allowDecimals: false,
                allowExponential: false,
                minValue: 0,
                maxValue: 10
              },
              {
                xtype: 'numberfield',
                name: 'attributes.httpclient.connection.timeout',
                fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_ConnectionTimeout_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_HttpClientFacet_ConnectionTimeout_HelpText'),
                allowDecimals: false,
                allowExponential: false,
                minValue: 0,
                maxValue: 3600
              }
            ]
          }
        ]
      }
    ];

    me.callParent(arguments);
  }

});

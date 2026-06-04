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

  authFields: function() {
    var me = this;
    return [
      {
        xtype: 'combo',
        name: 'attributes.httpclient.authentication.type',
        fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_FieldLabel'),
        editable: false,
        store: me.getAuthTypeStore(),
        value: 'username',
        listeners: {
          'change': me.authTypeChanged,
          'afterrender': me.authTypeChanged
        }
      },
      {
        xtype: 'textfield',
        itemId: 'attributes_httpclient_authentication_username',
        name: 'attributes.httpclient.authentication.username',
        fieldLabel: NX.I18n.get('System_AuthenticationSettings_Username_FieldLabel'),
        allowBlank: false
      },
      {
        xtype: 'textfield',
        itemId: 'attributes_httpclient_authentication_password',
        inputType: 'password',
        name: 'attributes.httpclient.authentication.password',
        fieldLabel: NX.I18n.get('System_AuthenticationSettings_Password_FieldLabel'),
        allowBlank: false
      },
      {
        xtype: 'fieldcontainer',
        itemId: 'ntlmFields',
        hidden: true,
        items: [
          {
            xtype: 'textfield',
            name: 'attributes.httpclient.authentication.ntlmHost',
            fieldLabel: NX.I18n.get('System_AuthenticationSettings_WindowsNtlmHostname_FieldLabel')
          },
          {
            xtype: 'textfield',
            name: 'attributes.httpclient.authentication.ntlmDomain',
            fieldLabel: NX.I18n.get('System_AuthenticationSettings_WindowsNtlmDomain_FieldLabel')
          }
        ]
      }
    ];
  }, /**
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
            items: this.authFields(me)
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
              },
              {
                xtype: 'checkbox',
                name: 'attributes.httpclient.connection.enableCircularRedirects',
                fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_EnableCircularRedirects_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_HttpClientFacet_EnableCircularRedirects_HelpText')
              },
              {
                xtype: 'checkbox',
                name: 'attributes.httpclient.connection.enableCookies',
                fieldLabel: NX.I18n.get('Repository_Facet_HttpClientFacet_EnableCookies_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_HttpClientFacet_EnableCookies_HelpText')
              }
            ]
          }
        ]
      }
    ];

    me.callParent();

    me.on('afterrender', function() {
      var form = me.up('form');
      if (!form) {
        return;
      }
      var remoteUrlField = form.down('#remoteUrl');
      if (!remoteUrlField) {
        return;
      }
      remoteUrlField.on('change', function(field, newValue) {
        me.checkRemoteUrlOriginChange(field, newValue);
      });

      var proxyFieldset = form.down('#proxyFieldSet');
      if (proxyFieldset) {
        proxyFieldset.on('add', function() {
          var warning = form.down('#remoteUrlOriginWarning');
          if (warning && warning.isVisible()) {
            me.ensureWarningAfterRemoteUrl(form, warning);
          }
        });
      }

      form.on('load', function() {
        var authCheckbox = form.down('[name=authEnabled]');
        me.hadAuthOnLoad = authCheckbox && authCheckbox.getValue();
        me.originChangeTriggered = false;
        var warning = form.down('#remoteUrlOriginWarning');
        if (warning) {
          warning.hide();
        }
      }, me);
    });
  },

  authTypeChanged: function(combo) {
    var ntlmFields = this.up('form').down('#ntlmFields');

    if(combo.getValue() === 'ntlm') {
      ntlmFields.show();
      ntlmFields.enable();
    }
    else {
      ntlmFields.hide();
      ntlmFields.disable();
    }
  },

  getAuthTypeStore: function() {
    return [
      ['username', NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_Username')],
      ['ntlm', NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_NTLM')]
    ];
  },

  checkRemoteUrlOriginChange: function(field, newValue) {
    var me = this,
        form = me.up('form'),
        warning = form ? form.down('#remoteUrlOriginWarning') : null;

    if (!form || !warning) {
      return;
    }

    var originalValue = field.originalValue;
    if (!originalValue || !newValue) {
      return;
    }

    var urlChanged = me.isUrlChanged(originalValue, newValue);

    if (!urlChanged && me.originChangeTriggered) {
      warning.hide();
      me.originChangeTriggered = false;
      return;
    }

    if (me.originChangeTriggered) {
      return;
    }

    // Skip if no credentials existed when the form loaded — nothing to exfiltrate
    var authEnabledCheckbox = form.down('[name=authEnabled]');
    if (!authEnabledCheckbox || !authEnabledCheckbox.getValue()) {
      if (!me.hadAuthOnLoad) {
        return;
      }
    }

    if (!urlChanged) {
      return;
    }

    me.originChangeTriggered = true;
    warning.show();
    me.ensureWarningAfterRemoteUrl(form, warning);
    me.resetAuthFields();
  },

  ensureWarningAfterRemoteUrl: function(form, warning) {
    var proxyFieldset = form.down('#proxyFieldSet');
    if (!proxyFieldset) {
      return;
    }
    var remoteUrlField = proxyFieldset.down('#remoteUrl');
    if (!remoteUrlField) {
      return;
    }
    var urlIdx = proxyFieldset.items.indexOf(remoteUrlField);
    var warningIdx = proxyFieldset.items.indexOf(warning);
    if (warningIdx !== urlIdx + 1) {
      proxyFieldset.move(warningIdx, urlIdx + 1);
    }
  },

  isUrlChanged: function(url1, url2) {
    return url1 !== url2;
  },

  resetAuthFields: function() {
    var me = this,
        form = me.up('form'),
        authFieldset, authTypeCombo, usernameField, passwordField, bearerField;

    if (!form) {
      return;
    }

    authFieldset = form.down('nx-optionalfieldset[checkboxName=authEnabled]');
    if (authFieldset) {
      authFieldset.expand();
      if (authFieldset.checkboxCmp) {
        authFieldset.checkboxCmp.setValue(true);
      }
    }

    authTypeCombo = form.down('[name=attributes.httpclient.authentication.type]');
    if (authTypeCombo) {
      authTypeCombo.suspendEvents();
      authTypeCombo.setValue('username');
      authTypeCombo.resumeEvents();
      me.authTypeChanged(authTypeCombo);
    }

    usernameField = form.down('#attributes_httpclient_authentication_username');
    if (usernameField) {
      usernameField.setValue('');
    }

    passwordField = form.down('#attributes_httpclient_authentication_password');
    if (passwordField) {
      passwordField.setValue('');
    }

    bearerField = form.down('#attributes_httpclient_authentication_bearerToken');
    if (bearerField) {
      bearerField.setValue('');
    }

    var ntlmHost = form.down('[name=attributes.httpclient.authentication.ntlmHost]');
    if (ntlmHost) {
      ntlmHost.setValue('');
    }
    var ntlmDomain = form.down('[name=attributes.httpclient.authentication.ntlmDomain]');
    if (ntlmDomain) {
      ntlmDomain.setValue('');
    }

    form.isValid();
  }

});

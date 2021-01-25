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
 * Configuration specific to Http connections for npm repositories including bearer token support.
 *
 * @since 3.20
 */
Ext.define('NX.coreui.view.repository.facet.BearerHttpClientFacet', {
  extend: 'NX.coreui.view.repository.facet.HttpClientFacet',
  alias: 'widget.nx-coreui-repository-httpclient-facet-with-bearer-token',
  requires: [
    'NX.I18n'
  ],

  authFields: function() {
    var me = this;
    var parentAuthFields = me.callParent(arguments);
    parentAuthFields.push(
        {
          xtype: 'textfield',
          inputType: 'password',
          itemId: 'attributes_httpclient_authentication_bearerToken',
          name: 'attributes.httpclient.authentication.bearerToken',
          fieldLabel: NX.I18n.get('System_AuthenticationSettings_Bearer_Token_FieldLabel'),
          helpText: NX.I18n.get('System_AuthenticationSettings_Bearer_Token_HelpText'),
          hidden: true,
          disabled: true,
          allowBlank: false
        });
    return parentAuthFields;
  },

  authTypeChanged: function(combo) {
    var form = this.up('form'),
        bearerTokenField = form.down('#attributes_httpclient_authentication_bearerToken'),
        usernameField = form.down('#attributes_httpclient_authentication_username'),
        passwordField = form.down('#attributes_httpclient_authentication_password');

    this.callParent(arguments);

    if (combo.getValue() === 'bearerToken') {
      usernameField.hide();
      usernameField.disable();
      usernameField.allowBlank = true;

      passwordField.hide();
      passwordField.disable();
      passwordField.allowBlank = true;

      bearerTokenField.show();
      bearerTokenField.enable();
      bearerTokenField.allowBlank = false;

      form.isValid();
    }
    else {
      bearerTokenField.hide();
      bearerTokenField.disable();
      bearerTokenField.allowBlank = true;

      usernameField.show();
      usernameField.enable();
      usernameField.allowBlank = false;

      passwordField.show();
      passwordField.enable();
      passwordField.allowBlank = false;

      form.isValid();
    }
  },

  getAuthTypeStore: function() {
    var me = this;
    var parentAuthTypes = me.callParent(arguments);
    parentAuthTypes.push([
        'bearerToken',
        NX.I18n.get('Repository_Facet_HttpClientFacet_AuthenticationType_Bearer_Token')
    ]);
    return parentAuthTypes;
  }

});

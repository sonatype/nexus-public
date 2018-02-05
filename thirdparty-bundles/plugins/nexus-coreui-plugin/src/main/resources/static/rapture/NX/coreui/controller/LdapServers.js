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
 * LDAP controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.LdapServers', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-ldapserver-list'
  ],
  models: [
    'LdapServer'
  ],
  stores: [
    'LdapServer',
    'LdapSchemaTemplate'
  ],
  views: [
    'ldap.LdapServerChangeOrder',
    'ldap.LdapServerFeature',
    'ldap.LdapServerList',
    'ldap.LdapServerConnection',
    'ldap.LdapServerConnectionFieldSet',
    'ldap.LdapServerConnectionForm',
    'ldap.LdapServerUserAndGroup',
    'ldap.LdapServerUserAndGroupFieldSet',
    'ldap.LdapServerUserAndGroupForm',
    'ldap.LdapServerUserAndGroupLoginCredentials',
    'ldap.LdapServerUserAndGroupMappingTestResults',
    'ldap.LdapServerConnectionAdd',
    'ldap.LdapServerUserAndGroupAdd'
  ],
  refs: [
    { ref: 'main', selector: 'nx-main' },
    { ref: 'feature', selector: 'nx-coreui-ldapserver-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-ldapserver-list' },
    { ref: 'connection', selector: 'nx-coreui-ldapserver-feature nx-coreui-ldapserver-connection' },
    { ref: 'userAndGroup', selector: 'nx-coreui-ldapserver-feature nx-coreui-ldapserver-userandgroup' }
  ],
  icons: {
    'ldapserver-default': {
      file: 'book_addresses.png',
      variants: ['x16', 'x32']
    },
    'ldap-security-source': {
      file: 'book_addresses.png',
      variants: ['x16']
    }
  },

  permission: 'nexus:ldap',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Security/LDAP',
      text: NX.I18n.get('LdapServers_Text'),
      description: NX.I18n.get('LdapServers_Description'),
      view: {xtype: 'nx-coreui-ldapserver-feature'},
      iconConfig: {
        file: 'book_addresses.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:ldap:read');
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#LdapServer': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-ldapserver-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-ldapserver-list button[action=new]': {
          click: me.showConnectionPanel
        },
        'nx-coreui-ldapserver-connection-add button[action=next]': {
          click: me.showUserAndGroupPanel
        },
        'nx-coreui-ldapserver-userandgroup-form button[action=add]': {
          click: me.createServer
        },
        'nx-coreui-ldapserver-connection-form button[action=save]': {
          click: me.updateServer
        },
        'nx-coreui-ldapserver-userandgroup-form button[action=save]': {
          click: me.updateServer
        },
        'nx-coreui-ldapserver-list button[action=changeorder]': {
          click: me.showChangeOrder,
          afterrender: me.bindChangeOrderButton
        },
        'nx-coreui-ldapserver-list button[action=clearcache]': {
          click: me.clearCache,
          afterrender: me.bindClearCacheButton
        },
        'nx-coreui-ldapserver-changeorder button[action=save]': {
          click: me.changeOrder
        },
        'nx-coreui-ldapserver-connection-add button[action=verifyconnection]': {
          click: me.verifyConnection
        },
        'nx-coreui-ldapserver-connection button[action=verifyconnection]': {
          click: me.verifyConnection
        },
        'nx-coreui-ldapserver-userandgroup-add button[action=verifyusermapping]': {
          click: me.verifyUserMapping
        },
        'nx-coreui-ldapserver-userandgroup button[action=verifyusermapping]': {
          click: me.verifyUserMapping
        },
        'nx-coreui-ldapserver-userandgroup-add button[action=verifylogin]': {
          click: me.showLoginCredentialsWindow
        },
        'nx-coreui-ldapserver-userandgroup button[action=verifylogin]': {
          click: me.showLoginCredentialsWindow
        },
        'nx-coreui-ldapserver-userandgroup-login-credentials button[action=verifylogin]': {
          click: me.verifyLogin
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    var me = this;

    if (Ext.isDefined(model)) {
      Ext.suspendLayouts();

      me.getConnection().loadRecord(model);
      me.getUserAndGroup().loadRecord(model);
      me.getUserAndGroup().down('#template').setValue(null);

      Ext.resumeLayouts(true);
    }
  },

  /**
   * @private
   */
  showConnectionPanel: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('LdapServers_CreateConnection_Title'));
    me.loadCreateWizard(1, true, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-ldapserver-connection-add',
          flex: 1
        }
      ]
    }));
  },

  /**
   * @private
   */
  showUserAndGroupPanel: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(2, NX.I18n.get('LdapServers_CreateUsersAndGroups_Title'));
    me.loadCreateWizard(2, true, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-ldapserver-userandgroup-add',
          flex: 1
        }
      ]
    }));
  },

  /**
   * @private
   */
  showChangeOrder: function() {
    Ext.widget('nx-coreui-ldapserver-changeorder');
  },

  /**
   * @private
   * Update an existing LDAP entry
   */
  updateServer: function() {
    var me = this,
      feature = me.getFeature(),
      connectionForm = feature.down('nx-coreui-ldapserver-connection').down('nx-coreui-ldapserver-connection-form'),
      userGroupForm = feature.down('nx-coreui-ldapserver-userandgroup').down('nx-coreui-ldapserver-userandgroup-form'),
      values = {};

    // Get fields from all relevant forms
    Ext.apply(values, connectionForm.getForm().getFieldValues());
    Ext.apply(values, userGroupForm.getForm().getFieldValues());

    var modelData = connectionForm.getForm().getRecord().getData(false);

    Object.keys(values).forEach(function(field) {
      delete modelData[field];
    });
    Ext.apply(modelData, values);

    // Apply masks
    me.getContent().getEl().mask(NX.I18n.get('LdapServers_Update_Mask'));
    NX.direct.ldap_LdapServer.update(modelData, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('LdapServers_Update_Success',
              me.getDescription(me.getLdapServerModel().create(response.data))),
            type: 'success'
          });
          me.getStore('LdapServer').load();
        }
      }
    });
  },

  /**
   * @private
   * Create a new LDAP entry
   */
  createServer: function() {
    var me = this,
      feature = me.getFeature(),
      connectionForm = feature.down('nx-coreui-ldapserver-connection-add').down('nx-coreui-ldapserver-connection-form'),
      userGroupForm = feature.down('nx-coreui-ldapserver-userandgroup-add').down('nx-coreui-ldapserver-userandgroup-form'),
      values = {};

    // Get fields from all relevant forms
    Ext.apply(values, connectionForm.getForm().getFieldValues());
    Ext.apply(values, userGroupForm.getForm().getFieldValues());

    me.getContent().getEl().mask(NX.I18n.get('LdapServers_Create_Mask'));
    NX.direct.ldap_LdapServer.create(values, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('LdapServers_Create_Success',
              me.getDescription(me.getLdapServerModel().create(response.data))),
            type: 'success'
          });
          me.getStore('LdapServer').load();
        }
      }
    });
  },

  /**
   * @private
   * Enable 'Change Order' when user has 'update' permission.
   */
  bindChangeOrderButton: function(button) {
    button.mon(
        NX.Conditions.isPermitted(this.permission + ':update'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Enable 'ClearCache' when user has 'delete' permission and there is at least one LDAP server configured.
   */
  bindClearCacheButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':delete'),
            NX.Conditions.storeHasRecords('LdapServer')
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
   * @override
   * Deletes a LDAP server.
   * @param {NX.coreui.model.LdapServer} model to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.ldap_LdapServer.remove(model.getId(), function(response) {
      me.getStore('LdapServer').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('LdapServers_Delete_Success', description), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Change LDAP servers order.
   */
  changeOrder: function(button) {
    var me = this,
        win = button.up('window'),
        order = button.up('form').down('nx-itemorderer').getValue();

    NX.direct.ldap_LdapServer.changeOrder(order, function(response) {
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({ text: NX.I18n.get('LdapServers_ChangeOrder_Success'), type: 'success' });
        me.getStore('LdapServer').load();
      }
    });
  },

  /**
   * @private
   * Clear LDAP cache.
   */
  clearCache: function(button) {
    NX.direct.ldap_LdapServer.clearCache(function(response) {
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.get('LdapServers_ClearCache_Success'), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Verify LDAP server connection.
   */
  verifyConnection: function(button) {
    var form = button.up('form'),
        values = form.getForm().getFieldValues(),
        url = values.protocol + '://' + values.host + ':' + values.port;

    form.getEl().mask(NX.I18n.format('LdapServers_VerifyConnection_Mask', url));

    NX.direct.ldap_LdapServer.verifyConnection(values, function(response) {
      form.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('LdapServers_VerifyConnection_Success', url), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Verify LDAP user mapping.
   */
  verifyUserMapping: function() {
    var me = this,
        values = me.getValues(),
        url = values.protocol + '://' + values.host + ':' + values.port;

    me.getMain().getEl().mask(NX.I18n.format('LdapServers_VerifyMapping_Mask', url));

    NX.direct.ldap_LdapServer.verifyUserMapping(values, function(response) {
      me.getMain().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({text: NX.I18n.format('LdapServers_VerifyMapping_Success', url), type: 'success'});
        Ext.widget('nx-coreui-ldapserver-userandgroup-testresults', {mappedUsers: response.data});
      }
    });
  },

  /**
   * @private
   */
  showLoginCredentialsWindow: function(button) {
    Ext.widget('nx-coreui-ldapserver-userandgroup-login-credentials');
  },

  /**
   * @private
   * Verify LDAP login.
   */
  verifyLogin: function(button) {
    var win = button.up('window'),
        form = button.up('form'),
        loginValues = form.getForm().getFieldValues(),
        userName = NX.util.Base64.encode(loginValues.username),
        userPass = NX.util.Base64.encode(loginValues.password),
        values = this.getValues(),
        url = values.protocol + '://' + values.host + ':' + values.port;

    form.getEl().mask(NX.I18n.format('LdapServers_VerifyLogin_Mask', url));

    NX.direct.ldap_LdapServer.verifyLogin(values, userName, userPass, function(response) {
      form.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({ text: NX.I18n.format('LdapServers_VerifyLogin_Success', url), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Get form values from connection adn user/group form.
   */
  getValues: function() {
    var feature = this.getFeature(),
        values = {}, url, connectionForm, userGroupForm;

    if (feature.down('nx-coreui-ldapserver-connection-add')) {
      connectionForm = feature.down('nx-coreui-ldapserver-connection-add').down('nx-coreui-ldapserver-connection-form');
    }
    else {
      connectionForm = feature.down('nx-coreui-ldapserver-connection').down('nx-coreui-ldapserver-connection-form');
    }
    if (feature.down('nx-coreui-ldapserver-userandgroup-add')) {
      userGroupForm = feature.down('nx-coreui-ldapserver-userandgroup-add').down('nx-coreui-ldapserver-userandgroup-form');
    }
    else {
      userGroupForm = feature.down('nx-coreui-ldapserver-userandgroup').down('nx-coreui-ldapserver-userandgroup-form');
    }

    // Get fields from all relevant forms
    Ext.apply(values, connectionForm.getForm().getFieldValues());
    Ext.apply(values, userGroupForm.getForm().getFieldValues());

    return values;
  }

});

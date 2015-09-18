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
 * LDAP Server "Connection" field set.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerConnectionFieldSet', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-ldapserver-connection-fieldset',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    xtype: 'textfield',
    allowBlank: false,
    bindGroup: 'connection'
  },

  items: [
    {
      xtype: 'hiddenfield',
      name: 'id'
    },
    {
      name: 'name',
      itemId: 'name',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_Name_FieldLabel')
    },
    {
      xtype: 'label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Address_Text'),
      style: {
        fontWeight: 'bold',
        display: 'block',
        marginTop: '10px',
        marginBottom: '5px'
      }
    },
    {
      xtype: 'label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Address_HelpText'),
      style: {
        fontSize: '10px',
        display: 'block',
        marginBottom: '1px'
      }
    },
    {
      xtype: 'combo',
      name: 'protocol',
      itemId: 'protocol',
      cls: 'nx-float-left',
      blankText: 'Required',
      width: 85,
      emptyText: NX.I18n.get('LdapServersConnectionFieldSet_Protocol_EmptyText'),
      editable: false,
      store: [
        ['ldap', NX.I18n.get('LdapServersConnectionFieldSet_Protocol_PlainItem')],
        ['ldaps', NX.I18n.get('LdapServersConnectionFieldSet_Protocol_SecureItem')]
      ],
      queryMode: 'local',
      listeners: {
        change: function(){
          var protocol = this.up('form').down('#port');
          protocol.fireEvent('change', protocol, protocol.getValue(), protocol.getValue());
        }
      }
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: '://'
    },
    {
      name: 'host',
      itemId: 'host',
      cls: 'nx-float-left',
      blankText: 'Required',
      width: 405,
      emptyText: NX.I18n.get('LdapServersConnectionFieldSet_Host_EmptyText'),
      listeners: {
        change: function(){
          var protocol = this.up('form').down('#port');
          protocol.fireEvent('change', protocol, protocol.getValue(), protocol.getValue());
        }
      }
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: ':'
    },
    {
      xtype: 'numberfield',
      name: 'port',
      itemId: 'port',
      cls: 'nx-float-left',
      blankText: 'Required',
      width: 75,
      emptyText: NX.I18n.get('LdapServersConnectionFieldSet_Port_EmptyText'),
      minValue: 1,
      maxValue: 65535,
      allowDecimals: false,
      allowExponential: false,
      useTrustStore: function (field) {
        var form = field.up('form');
        if (form.down('#protocol').getValue() === 'ldaps' && form.down('#host').getValue() && field.getValue()) {
          return {
            name: 'useTrustStore',
            host: form.down('#host'),
            port: form.down('#port')
          };
        }
        return undefined;
      }
    },
    {
      name: 'searchBase',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_Base_FieldLabel'),
      helpText: NX.I18n.get('LdapServersConnectionFieldSet_Base_HelpText'),
      cls: 'nx-clear-both'
    },
    {
      xtype: 'combo',
      name: 'authScheme',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_FieldLabel'),
      emptyText: NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_EmptyText'),
      editable: false,
      store: [
        ['simple', NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_SimpleItem')],
        ['none', NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_AnonymousItem')],
        ['DIGEST-MD5', NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_DigestItem')],
        ['CRAM-MD5', NX.I18n.get('LdapServersConnectionFieldSet_AuthMethod_CramItem')]
      ],
      queryMode: 'local',
      listeners: {
        change: function (combo, newValue) {
          this.up('panel').showOrHide('authScheme', newValue);
        }
      }
    },
    {
      name: 'authRealm',
      itemId: 'authRealm',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_SaslRealm_FieldLabel'),
      helpText: NX.I18n.get('LdapServersConnectionFieldSet_SaslRealm_HelpText'),
      allowBlank: true,
      authScheme: ['DIGEST-MD5', 'CRAM-MD5']
    },
    {
      name: 'authUsername',
      itemId: 'authUsername',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_Username_FieldLabel'),
      helpText: NX.I18n.get('LdapServersConnectionFieldSet_Username_HelpText'),
      authScheme: ['simple', 'DIGEST-MD5', 'CRAM-MD5']
    },
    {
      xtype: 'nx-password',
      name: 'authPassword',
      itemId: 'authPassword',
      fieldLabel: NX.I18n.get('LdapServersConnectionFieldSet_Password_FieldLabel'),
      inputType: 'password',
      helpText: NX.I18n.get('LdapServersConnectionFieldSet_Password_HelpText'),
      authScheme: ['simple', 'DIGEST-MD5', 'CRAM-MD5']
    },
    {
      xtype: 'label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_Text'),
      style: {
        fontWeight: 'bold',
        display: 'block',
        marginTop: '10px',
        marginBottom: '5px'
      }
    },
    {
      xtype: 'label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_HelpText'),
      style: {
        fontSize: '10px',
        display: 'block',
        marginBottom: '1px'
      }
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_Text1')
    },
    {
      xtype: 'numberfield',
      name: 'connectionTimeout',
      cls: 'nx-float-left',
      width: 70,
      value: 30
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_Text2')
    },
    {
      xtype: 'numberfield',
      name: 'connectionRetryDelay',
      cls: 'nx-float-left',
      width: 70,
      value: 300
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_Text3')
    },
    {
      xtype: 'numberfield',
      name: 'maxIncidentsCount',
      cls: 'nx-float-left',
      width: 55,
      value: 3
    },
    {
      xtype: 'label',
      cls: 'nx-float-left nx-interstitial-label',
      text: NX.I18n.get('LdapServersConnectionFieldSet_Rules_Text4')
    }
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.callParent(arguments);

    me.showOrHide('authScheme', undefined);
  },

  /**
   * @private
   * Show & enable or hide and disable components that have attributes that matches the specified value.
   * @param attribute name of attribute
   * @param value to be matched in order to show
   */
  showOrHide: function (attribute, value) {
    var me = this,
        form = me.up('form'),
        components = me.query('component[' + attribute + ']');

    Ext.iterate(components, function (component) {
      if (value && component[attribute].indexOf(value) > -1) {
        component.enable();
        component.show();
      }
      else {
        component.disable();
        component.hide();
      }
    });
    if (form) {
      form.isValid();
    }
  }

});

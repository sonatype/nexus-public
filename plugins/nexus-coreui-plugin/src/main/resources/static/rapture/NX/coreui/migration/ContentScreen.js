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
 * Migration content-options screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.ContentScreen', {
  extend: 'NX.wizard.FormScreen',
  alias: 'widget.nx-coreui-migration-content',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      description: NX.I18n.render(me, 'Description'),

      fields: [
        {
          xtype: 'checkboxgroup',
          columns: 1,
          allowBlank: false,
          items: [
            {
              xtype: 'checkboxgroup',
              fieldLabel: NX.I18n.render(me, 'Repositories_FieldLabel'),
              columns: 1,
              allowBlank: true,
              items: [
                {
                  xtype: 'checkbox',
                  name: 'repositories.usermanaged',
                  boxLabel: NX.I18n.render(me, 'Repositories_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'repository.targets',
                  boxLabel: NX.I18n.render(me, 'RepositoryTargets_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'capability.healthcheck',
                  boxLabel: NX.I18n.render(me, 'Repositories_HealthCheck_BoxLabel'),
                  checked: true
                }
              ]
            },
            {
              xtype: 'checkboxgroup',
              fieldLabel: NX.I18n.render(me, 'Security_FieldLabel'),
              columns: 1,
              allowBlank: true,
              items: [
                {
                  xtype: 'checkbox',
                  name: 'security.anonymous',
                  boxLabel: NX.I18n.render(me, 'Security_Anonymous_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: (NX.State.requiresLicense() && NX.State.isLicenseValid()) ? 'ldapPro' : 'ldap',
                  boxLabel: NX.I18n.render(me, 'Security_LDAP_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'nuget.apikey',
                  boxLabel: NX.I18n.render(me, 'Security_NuGet_API_Key_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.realms',
                  boxLabel: NX.I18n.render(me, 'Security_Realms_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.roles',
                  boxLabel: NX.I18n.render(me, 'Security_Roles_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.target-privileges',
                  boxLabel: NX.I18n.render(me, 'Security_TargetPrivileges_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.trust',
                  boxLabel: NX.I18n.render(me, 'Security_SSL_Certificates_BoxLabel'),
                  checked: true,
                  hidden: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.user-tokens',
                  boxLabel: NX.I18n.render(me, 'Security_User_Tokens_BoxLabel'),
                  checked: true,
                  hidden: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.crowd',
                  boxLabel: NX.I18n.render(me, 'Security_Crowd_BoxLabel'),
                  checked: true,
                  hidden: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.users',
                  boxLabel: NX.I18n.render(me, 'Security_Users_BoxLabel'),
                  checked: true
                }
              ]
            },
            {
              xtype: 'checkboxgroup',
              fieldLabel: NX.I18n.render(me, 'System_FieldLabel'),
              columns: 1,
              allowBlank: true,
              items: [
                {
                  xtype: 'checkbox',
                  name: 'system.email',
                  boxLabel: NX.I18n.render(me, 'System_Email_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'system.http',
                  boxLabel: NX.I18n.render(me, 'System_HTTP_BoxLabel'),
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'capability.iq',
                  boxLabel: NX.I18n.render(me, 'Repositories_Clm_BoxLabel'),
                  checked: true,
                  hidden: true
                }
              ]
            }
          ]
        }
      ],

      buttons: ['back', 'next', 'cancel']
    });

    me.callParent();
    me.down('form').settingsForm = true;
  },

  /**
   * Returns the state of the screen form
   *
   * @return {boolean}
   */
  isDirty: function() {
    return this.down('form').isDirty();
  }
});

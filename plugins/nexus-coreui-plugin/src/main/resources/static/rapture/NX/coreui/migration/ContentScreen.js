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

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: 'Content',

      description: '<p>Select the contents to migrate from remote server.</p>',

      fields: [
        {
          xtype: 'checkboxgroup',
          columns: 1,
          allowBlank: false,
          items: [
            {
              xtype: 'checkboxgroup',
              fieldLabel: 'Security',
              columns: 1,
              allowBlank: true,
              items: [
                {
                  xtype: 'checkbox',
                  name: 'security.anonymous',
                  boxLabel: 'Anonymous',
                  checked: true
                },
                {
                  xtype: 'checkbox',
                  name: 'security.realms',
                  boxLabel: 'Realms',
                  checked: false
                },
                {
                  xtype: 'checkbox',
                  name: 'security.users',
                  boxLabel: 'Users',
                  checked: false
                },
                {
                  xtype: 'checkbox',
                  name: 'security.roles',
                  boxLabel: 'Roles',
                  checked: false
                },
                {
                  xtype: 'checkbox',
                  name: 'ldap',
                  boxLabel: 'LDAP Configuration',
                  checked: false
                }
              ]
            },
            {
              xtype: 'checkboxgroup',
              fieldLabel: 'Repositories',
              columns: 1,
              allowBlank: true,
              items: [
                {
                  xtype: 'checkbox',
                  name: 'repositories.usermanaged',
                  boxLabel: 'User-managed repositories',
                  checked: true
                }
              ]
            }
          ]
        }
      ],

      buttons: ['back', 'next', 'cancel']
    });

    me.callParent();
  }
});

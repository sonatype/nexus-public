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
 * Migration agent connection screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.AgentScreen', {
  extend: 'NX.wizard.FormScreen',
  alias: 'widget.nx-coreui-migration-agent',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: 'Agent Connection',

      description: "<p>Configure the connection to remote server's migration-agent.<br/>" +
      'The remote server must have a migration-agent previously configured and enabled.</p>',

      fields: [
        {
          xtype: 'nx-url',
          name: 'url',
          fieldLabel: 'URL',
          helpText: "The URL of the remote server's migration-agent endpoint.",
          allowBlank: false,

          // HACK: testing, avoid retyping this all the time
          value: 'http://localhost:8082/nexus/service/siesta/migrationagent'
        },
        {
          xtype: 'textfield',
          name: 'accessToken',
          fieldLabel: 'Access Token',
          helpText: "The access token copied from remote server's migration-agent settings.",
          allowBlank: false,

          // HACK: testing, avoid retyping this all the time
          value: 'test'
        }
      ],

      buttons: [ 'back', 'next', 'cancel' ]
    });

    me.callParent();
  }
});

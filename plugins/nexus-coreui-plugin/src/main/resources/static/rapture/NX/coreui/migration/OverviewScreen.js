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
 * ???
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.OverviewScreen', {
  extend: 'NX.wizard.Screen',
  alias: 'widget.nx-coreui-migration-overview',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: 'Overview',

      description: '<p>This wizard will help you setup migration from a remote server.</p>' +
      '<p>Many aspects of a server can be migrated <strong>automatically</strong>:' +
      '<ul>' +
      '<li>Security: users, roles, privileges</li>' +
      '<li>Repositories in supported formats: maven2, nuget, npm, site</li>' +
      '</ul>' +
      '</p>' +
      '<p>Some aspects are <strong>incompatible</strong> and can not be automatically migrated:' +
      '<ul>' +
      '<li>Unsupported repository formats: yum, p2, obr</li>' +
      '<li>Unsupported security: roles and privileges</li>' +
      '<li>Scheduled tasks</li>' +
      '<li>Capabilities</li>' +
      '</ul>' +
      '</p>' +
      '<p>Migration is incremental. We recommend migrating one or two things first to ensure that the process works, then repeat the process and migrate the rest. Keep in mind that repository migration could take <strong>considerable time</strong> and needs <strong>special consideration</strong> for finalization upon completion.' +
      '</p>',

      buttons: ['next']
    });

    me.callParent();
  }
});

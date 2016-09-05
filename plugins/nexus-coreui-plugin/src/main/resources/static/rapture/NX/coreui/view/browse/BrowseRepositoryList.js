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
 * Repository grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.browse.BrowseRepositoryList', {
  extend: 'NX.coreui.view.repository.RepositoryListTemplate',
  alias: 'widget.nx-coreui-browse-repository-list',
  requires: [
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-browse-repository-list',

  /**
   * @override
   */
  initComponent: function() {

    this.store = Ext.create('NX.coreui.store.RepositoryReference', { remoteFilter: true });
    this.store.addFilter([
      { property: 'applyPermissions', value: true }
    ], false);

    // filter will install into toolbar, ensure its properly styled for drilldown
    this.tbar = {
      xtype: 'nx-actions'
    };

    this.callParent();
  }

});

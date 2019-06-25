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
/**
 * Repository settings form for an apt repository
 *
 * @since 3.17
 */
Ext.define('NX.coreui.view.repository.recipe.AptProxy', {
  extend: 'NX.coreui.view.repository.RepositorySettingsForm',
  alias: 'widget.nx-coreui-repository-apt-proxy',
  requires: [
    'NX.coreui.view.repository.facet.AptFacet',
    'NX.coreui.view.repository.facet.ProxyFacet',
    'NX.coreui.view.repository.facet.RoutingRuleFacet',
    'NX.coreui.view.repository.facet.StorageFacet',
    'NX.coreui.view.repository.facet.HttpClientFacet',
    'NX.coreui.view.repository.facet.NegativeCacheFacet',
    'NX.coreui.view.repository.facet.CleanupPolicyFacet'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {xtype: 'nx-aptui-repository-apt-facet'},
      {xtype: 'nx-coreui-repository-proxy-facet'},
      {xtype: 'nx-coreui-repository-routing-rule-facet'},
      {xtype: 'nx-coreui-repository-storage-facet'},
      {xtype: 'nx-coreui-repository-negativecache-facet'},
      {xtype: 'nx-coreui-repository-cleanup-policy-facet'},
      {xtype: 'nx-coreui-repository-httpclient-facet'}
    ];

    me.callParent();
  }
});

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
 * Configuration specific to Http connections for repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.NugetProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-nugetproxy-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    itemCls: 'required-field'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_NugetProxyFacet_Title'),

        items: [
          {
            xtype: 'numberfield',
            name: 'attributes.nugetProxy.queryCacheSize',
            fieldLabel: NX.I18n.get('Repository_Facet_NegativeCacheFacet_QueryCacheSize_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_NegativeCacheFacet_QueryCacheSize_HelpText'),
            minValue: 0,
            value: 300
          },
          {
            xtype: 'numberfield',
            name: 'attributes.nugetProxy.queryCacheItemMaxAge',
            fieldLabel: NX.I18n.get('Repository_Facet_NugetProxyFacet_ItemMaxAge_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_NugetProxyFacet_ItemMaxAge_HelpText'),
            minValue: 0,
            value: 3600
          }
        ]
      }
    ];

    me.callParent(arguments);
  }

});

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
 * Configuration for Yum hosted repodata level.
 *
 * @since 3.8
 */
Ext.define('NX.coreui.view.repository.facet.YumHostedFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-yum-hosted-facet',
  requires: [
    'NX.I18n',
    'Ext.form.ComboBox'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_YumHostedFacet_Title'),
        items: [
          {
            xtype: 'combo',
            name: 'attributes.yum.repodataDepth',
            fieldLabel: NX.I18n.get('Repository_Facet_YumHostedFacet_RepodataDepth_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_YumHostedFacet_RepodataDepth_HelpText'),
            forceSelection: true,
            editable: false,
            allowBlank: false,
            store : [0, 1, 2, 3, 4, 5]
          },
          {
            xtype: 'combo',
            name: 'attributes.yum.deployPolicy',
            fieldLabel: NX.I18n.get('Repository_Facet_YumHostedFacet_DeployPolicy_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_YumHostedFacet_DeployPolicy_HelpText'),
            emptyText: NX.I18n.get('Repository_Facet_YumHostedFacet_DeployPolicy_EmptyText'),
            editable: false,
            store: [
              ['STRICT', NX.I18n.get('Repository_Facet_YumHostedFacet_DeployPolicy_StrictItem')],
              ['PERMISSIVE', NX.I18n.get('Repository_Facet_YumHostedFacet_DeployPolicy_PermissiveItem')]
            ],
            value: 'STRICT'
          }
        ]
      }
    ];

    me.callParent();
  }
});


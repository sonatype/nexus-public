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
 * Configuration for repository storage write policy.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.StorageFacetHosted', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-storage-hosted-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    allowBlank: false,
    itemCls: 'required-field'
  },

  /**
   * @cfg String
   * Set the write policy of storage, defaults to ALLOW_ONCE.
   */
  writePolicy: 'ALLOW_ONCE',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_StorageFacetHosted_Title'),

        items: [
          {
            xtype: 'combo',
            name: 'attributes.storage.writePolicy',
            itemId: 'writePolicy',
            fieldLabel: NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_HelpText'),
            emptyText: NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_EmptyText'),
            editable: false,
            store: [
              ['ALLOW', NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_AllowItem')],
              ['ALLOW_ONCE', NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_DisableItem')],
              ['DENY', NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_ReadOnlyItem')]
            ],
            value: me.writePolicy,
            queryMode: 'local'
          }
        ]
      }
    ];

    me.callParent(arguments);
  }

});

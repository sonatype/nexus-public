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
 * Configuration specific to Maven repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.Maven2Facet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-maven2-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    allowBlank: false,
    queryMode: 'local',
    itemCls: 'required-field'
  },

  // Default to inline for existing maven repos
  contentDisposition: 'INLINE',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_Maven2Facet_Title'),

        items: [
          {
            xtype: 'combo',
            name: 'attributes.maven.versionPolicy',
            itemId: 'versionPolicy',
            fieldLabel: NX.I18n.get('Maven2Facet_VersionPolicy_FieldLabel'),
            helpText: NX.I18n.get('Maven2Facet_VersionPolicy_HelpText'),
            emptyText: NX.I18n.get('Maven2Facet_VersionPolicy_EmptyText'),
            editable: false,
            store: [
              ['RELEASE', NX.I18n.get('Maven2Facet_VersionPolicy_ReleaseItem')],
              ['SNAPSHOT', NX.I18n.get('Maven2Facet_VersionPolicy_SnapshotItem')],
              ['MIXED', NX.I18n.get('Maven2Facet_VersionPolicy_MixedItem')]
            ],
            value: 'RELEASE',
            readOnlyOnUpdate: true
          },
          {
            xtype: 'combo',
            name: 'attributes.maven.layoutPolicy',
            fieldLabel: NX.I18n.get('Repository_Facet_Maven2Facet_LayoutPolicy_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_Maven2Facet_LayoutPolicy_HelpText'),
            emptyText: NX.I18n.get('Repository_Facet_Maven2Facet_LayoutPolicy_EmptyText'),
            editable: false,
            store: [
              ['STRICT', NX.I18n.get('Repository_Facet_Maven2Facet_LayoutPolicy_StrictItem')],
              ['PERMISSIVE', NX.I18n.get('Repository_Facet_Maven2Facet_LayoutPolicy_PermissiveItem')]
            ],
            value: 'STRICT'
          },
          {
            xtype: 'combo',
            name: 'attributes.maven.contentDisposition',
            itemId: 'contentDisposition',
            allowBlank: false,
            fieldLabel: NX.I18n.get('Repository_Facet_Maven2Facet_ContentDisposition_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_Maven2Facet_ContentDisposition_HelpText'),
            editable: false,
            store: [
              ['INLINE', NX.I18n.get('Repository_Facet_Maven2Facet_ContentDisposition_Inline')],
              ['ATTACHMENT', NX.I18n.get('Repository_Facet_Maven2Facet_ContentDisposition_Attachment')]
            ],
            value: me.contentDisposition,
            queryMode: 'local'
          }
        ]
      }
    ];

    me.callParent();
  }

});

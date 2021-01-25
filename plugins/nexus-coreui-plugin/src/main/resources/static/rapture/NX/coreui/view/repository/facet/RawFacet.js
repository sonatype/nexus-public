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
 * Configuration specific to raw repos to set content-disposition
 *
 * @since 3.25
 */
Ext.define('NX.coreui.view.repository.facet.RawFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-raw-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    itemCls: 'required-field'
  },

  // Default to inline for existing raw repos
  contentDisposition: 'INLINE',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    // Newly added repos should default to being Attachment
    if (me.up("nx-coreui-repository-add") != null) {
      me.contentDisposition = 'ATTACHMENT';
    }

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_Raw_Title'),

        items: [
          {
            xtype: 'combo',
            name: 'attributes.raw.contentDisposition',
            itemId: 'contentDisposition',
            allowBlank: false,
            fieldLabel: NX.I18n.get('Repository_Facet_Raw_ContentDisposition_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_Raw_ContentDisposition_HelpText'),
            editable: false,
            store: [
              ['INLINE', NX.I18n.get('Repository_Facet_Raw_ContentDisposition_Inline')],
              ['ATTACHMENT', NX.I18n.get('Repository_Facet_Raw_ContentDisposition_Attachment')]
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

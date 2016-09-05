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
 * Configuration specific to proxy repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.ProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-proxy-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    allowBlank: false,
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
        itemId: 'proxyFieldSet',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_ProxyFacet_Title'),

        items: [
          {
            xtype: 'nx-url',
            itemId: 'remoteUrl',
            name: 'attributes.proxy.remoteUrl',
            fieldLabel: NX.I18n.get('Repository_Facet_ProxyFacet_Remote_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_ProxyFacet_Remote_HelpText'),
            emptyText: NX.I18n.get('Repository_Facet_ProxyFacet_Remote_EmptyText'),
            useTrustStore: function (field) {
              if (Ext.String.startsWith(field.getValue(), 'https://')) {
                return {
                  name: 'attributes.httpclient.connection.useTrustStore',
                  url: field
                };
              }
              return undefined;
            }
          },
          {
            xtype: 'checkbox',
            name: 'attributes.httpclient.blocked',
            fieldLabel: NX.I18n.get('Repository_Facet_ProxyFacet_Blocked_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_ProxyFacet_Blocked_HelpText'),
            value: false
          },
          {
            xtype: 'checkbox',
            name: 'attributes.httpclient.autoBlock',
            fieldLabel: NX.I18n.get('Repository_Facet_ProxyFacet_Autoblock_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_ProxyFacet_Autoblock_HelpText'),
            value: true
          },
          {
            xtype: 'numberfield',
            name: 'attributes.proxy.contentMaxAge',
            fieldLabel: NX.I18n.get('Repository_Facet_ProxyFacet_ArtifactAge_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_ProxyFacet_ArtifactAge_HelpText'),
            minValue: -1,
            value: 1440
          },
          {
            xtype: 'numberfield',
            name: 'attributes.proxy.metadataMaxAge',
            fieldLabel: NX.I18n.get('Repository_Facet_ProxyFacet_MetadataAge_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_ProxyFacet_MetadataAge_HelpText'),
            minValue: -1,
            value: 1440
          }
        ]
      }
    ];

    me.callParent();
  }

});

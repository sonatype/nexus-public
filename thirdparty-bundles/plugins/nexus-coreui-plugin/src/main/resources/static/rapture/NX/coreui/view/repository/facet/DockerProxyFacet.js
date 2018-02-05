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
 * Configuration for Docker proxy repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.DockerProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-docker-proxy-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldcontainer',
        fieldLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_IndexType_FieldLabel'),
        defaultType: 'radiofield',
        layout: 'vbox',
        items: [
          {
            boxLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_IndexTypeRegistry_BoxLabel'),
            name: 'attributes.dockerProxy.indexType',
            itemId: 'indexTypeRegistry',
            inputValue: 'REGISTRY',
            checked: true,
            listeners: {
              change: function(radio) {
                var remoteUrl = radio.up('form').down('#remoteUrl'),
                    indexUrl = radio.up('form').down('#indexUrl');
                if (radio.getValue()) {
                  indexUrl.setValue(remoteUrl.getValue());
                  indexUrl.setDisabled(true);
                  indexUrl.setVisible(false);
                }
              }
            }
          },
          {
            boxLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_IndexTypeHub_BoxLabel'),
            name: 'attributes.dockerProxy.indexType',
            itemId: 'indexTypeHub',
            inputValue: 'HUB',
            listeners: {
              change: function(radio) {
                var indexUrl = radio.up('form').down('#indexUrl');
                if (radio.getValue()) {
                  indexUrl.setValue('https://index.docker.io/');
                  indexUrl.setDisabled(true);
                  indexUrl.setVisible(true);
                }
              }
            }
          },
          {
            boxLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_IndexTypeCustom_BoxLabel'),
            name: 'attributes.dockerProxy.indexType',
            itemId: 'indexTypeCustom',
            inputValue: 'CUSTOM',
            listeners: {
              change: function(radio) {
                var indexUrl = radio.up('form').down('#indexUrl');
                if (radio.getValue()) {
                  indexUrl.setDisabled(false);
                  indexUrl.setVisible(true);
                }
              }
            }
          }
        ]
      },
      {
        xtype: 'nx-url',
        itemId: 'indexUrl',
        name: 'attributes.dockerProxy.indexUrl',
        helpText: NX.I18n.get('Repository_Facet_DockerProxyFacet_IndexUrl_HelpText'),
        disabled: true,
        hidden: true,
        allowBlank: false,
        useTrustStore: function(field) {
          var remoteUrl = field.up('form').down('#remoteUrl');
          if (Ext.String.startsWith(field.getValue(), 'https://') && field.getValue() !== remoteUrl.getValue()) {
            return {
              name: 'attributes.dockerProxy.useTrustStoreForIndexAccess',
              url: field
            };
          }
          return undefined;
        }
      }
    ];

    me.callParent();
  }

});

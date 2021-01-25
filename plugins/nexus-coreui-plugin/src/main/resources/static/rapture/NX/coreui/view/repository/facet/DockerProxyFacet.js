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

  formLoad: false,

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
                  me.deselectDefaultOption(radio);
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
                  me.deselectDefaultOption(radio);
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
      },
      {
        xtype: 'checkbox',
        name: 'attributes.dockerProxy.cacheForeignLayers',
        itemId: 'cacheForeignLayers',
        fieldLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayers_FieldLabel'),
        helpText: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayers_HelpText'),
        value: false,
        listeners: {
          change: function(chkbox) {
            var whitelistActive = chkbox.getValue(),
                whitelistSection = chkbox.up('form').down('#foreignLayerWhitelist');

            whitelistSection.setDisabled(!whitelistActive);
            whitelistSection.setVisible(whitelistActive);
            if (!me.formLoad && me.getWhitelistRows().length === 0) {
              me.addWhitelistRow();
            }
          }
        }
      },
      {
        xtype: 'fieldset',
        itemId: 'foreignLayerWhitelist',
        title: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_FieldLabel'),
        hidden: true,
        items: [
          {
            xtype: 'label',
            text: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_HelpText')
          },
          {
            xtype: 'fieldcontainer',
            itemId: 'foreignLayerWhitelistRows'
          },
          {
            xtype: 'button',
            iconCls: 'x-fa fa-plus-circle',
            text: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_AddButton'),
            tooltip: NX.I18n.get('Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_AddButton'),
            handler: function() {
              me.addWhitelistRow();
              var rows = me.getWhitelistRows();
              var row = rows[rows.length - 1];
              row.validate();
              row.focus();
            }
          }
        ]
      }
    ];

    me.callParent();
  },

  addWhitelistRow: function(value) {
    var me = this,
        form = me.up('nx-coreui-repository-settings-form'),
        idx = me.getWhitelistRows().length,
        val = me.getWhitelistRows().length === 0 ? (value || '.*') : (value || ''),
        row = {
          xtype: 'panel',
          cls: ['nx-repeated-row', 'whitelist-url'],
          layout: 'column',
          items: [
            {
              xtype: 'textfield',
              allowBlank: false,
              name: 'attributes.dockerProxy.whitelistUrl[' + idx + ']',
              width: 'calc(100% - 36px)',
              value: val
            },
            {
              xtype: 'button',
              cls: 'nx-remove-whitelist-row-button',
              iconCls: 'x-fa fa-trash',
              style: {
                marginLeft: '10px'
              },
              handler: function(button) {
                var whitelist = me.getWhitelistRowsContainer();
                whitelist.add({
                  xtype: 'hiddenfield',
                  isDirty: function() {
                    return true;
                  }
                });
                form.fireEvent('dirtychange', form.getForm());
                whitelist.remove(button.up());
                me.setFirstRemoveButtonState();
              }
            }
          ]
        };

    me.getWhitelistRowsContainer().add(row);
    me.setFirstRemoveButtonState();
  },

  getWhitelistRowsContainer: function() {
    return this.down('#foreignLayerWhitelistRows');
  },

  getWhitelistRows: function() {
    return this.getWhitelistRowsContainer().query('textfield');
  },

  setFirstRemoveButtonState: function() {
    var removeButtons = this.getWhitelistRowsContainer().query('button[cls=nx-remove-whitelist-row-button]');

    if (removeButtons.length > 1) {
      removeButtons[0].enable();
    }
    else {
      removeButtons[0].disable();
    }
  },

  resetWhitelist: function() {
    var whitelist = this.getWhitelistRowsContainer(),
        urls = this.items && this.query('panel[cls=whitelist-url]') || [],
        dirtyHiddenFields = this.items && this.query('hiddenfield') || [];

    dirtyHiddenFields.forEach(function(field) {
      whitelist.remove(field);
    });

    urls.forEach(function(row) {
      whitelist.remove(row);
    });
  },

  deselectDefaultOption: function(radio) {
    var indexTypeRegistryRadio = radio.up('form').down('#indexTypeRegistry');
    if (indexTypeRegistryRadio.getValue()) {
      //this is working around an apparent bug in ext where 2 radiobuttons from same group are being
      //selected apparently because we have the 'checked' property set on one of them
      indexTypeRegistryRadio.setValue(false);
    }
  }

});

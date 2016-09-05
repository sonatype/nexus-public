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
 * Content selector preview window, listing the components that match the selector in the selected repository.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.selector.SelectorPreviewWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-selector-preview-window',
  requires: [
    'NX.I18n',
    'NX.coreui.store.AllRepositoriesReference'
  ],

  config: {
    /**
     * @cfg {String} jexl selector.
     */
    jexl: undefined
  },

  resizable: true,
  closable: true,
  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },
  height: 480,
  ui: 'nx-inset',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.get('SelectorPreviewWindow_Title'),
      width: NX.view.ModalDialog.LARGE_MODAL,
      height: 540,
      buttonAlign: 'left',
      buttons: [
        {text: NX.I18n.get('Button_Close'), handler: me.close, action: 'close', scope: me}
      ],
      items: [
        {
          xtype: 'form',
          buttonAlign: 'left',
          items: [
            {
              xtype: 'textareafield',
              name: 'jexl',
              itemId: 'jexl',
              fieldLabel: NX.I18n.get('SelectorPreviewWindow_jexl_FieldLabel'),
              allowBlank: false,
              value: me.jexl
            },
            {
              xtype: 'combo',
              name: 'selectedRepository',
              fieldLabel: NX.I18n.get('SelectorPreviewWindow_repository_FieldLabel'),
              helpText: NX.I18n.get('SelectorPreviewWindow_repository_HelpText'),
              emptyText: NX.I18n.get('SelectorPreviewWindow_repository_EmptyText'),
              editable: false,
              store: Ext.create('NX.coreui.store.AllRepositoriesReference', {remote: true, autoLoad: true}),
              valueField: 'id',
              displayField: 'name',
              allowBlank: false
            }
          ],
          buttons: [
            {
              text: NX.I18n.get('SelectorPreviewWindow_Preview_Button'),
              action: 'preview',
              ui: 'nx-primary',
              formBind: true
            }
          ]
        },
        {
          xtype: 'gridpanel',
          store: me.assetStore,
          flex: 1,
          viewConfig: {
            emptyText: NX.I18n.get('SelectorPreviewWindow_EmptyText_View'),
            emptyTextFilter: NX.I18n.get('SelectorPreviewWindow_EmptyText_Filter'),
            deferEmptyText: false
          },

          columns: [
            {
              xtype: 'nx-iconcolumn',
              dataIndex: 'contentType',
              width: 36,
              iconVariant: 'x16',
              iconNamePrefix: 'asset-type-',
              iconName: function(value) {
                var assetType;

                if (value) {
                  assetType = value.replace('/', '-');
                  if (NX.getApplication().getIconController().findIcon('asset-type-' + assetType, 'x16')) {
                    return assetType;
                  }
                }
                return 'default';
              }
            },
            {
              text: NX.I18n.get('SelectorPreviewWindow_Name_Column'),
              dataIndex: 'name',
              stateId: 'name',
              flex: 1
            }
          ],

          tbar: {
            xtype: 'nx-actions',
            items: [
              '->',
              {
                xtype: 'nx-searchbox',
                itemId: 'filter',
                emptyText: NX.I18n.get('Grid_Plugin_FilterBox_Empty'),
                width: 200
              }
            ]
          },

          plugins: {
            ptype: 'bufferedrenderer',
            trailingBufferZone: 20,
            leadingBufferZone: 50
          }
        }
      ]
    });

    me.callParent();
    me.center();
  }

});
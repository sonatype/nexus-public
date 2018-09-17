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
Ext.define('NX.coreui.view.cleanuppolicy.CleanupPolicyPreviewWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-cleanuppolicy-preview-window',
  requires: [
    'NX.I18n',
    'NX.coreui.store.AllRepositoriesReference'
  ],

  config: {
    format: undefined
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

    me.repositoryStore = Ext.create('NX.coreui.store.RepositoryReference', {
      remote: true,
      remoteFilter: true,
      sorters: {
        property: 'name',
        direction: 'ASC'
      }
    });

    if(me.format !== NX.I18n.get('Repository_Formats_All')) {
      me.repositoryStore.addFilter([
        {property: 'format', value: me.format},
        {property: 'type', value: '!group'}
      ]);
    } else {
      me.repositoryStore.addFilter([
        {property: 'type', value: '!group'}
      ]);
    }

    Ext.apply(me, {
      title: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Title'),
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
              xtype: 'combo',
              name: 'selectedRepository',
              fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_repository_FieldLabel'),
              helpText: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_repository_HelpText'),
              emptyText: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_repository_EmptyText'),
              editable: false,
              store: me.repositoryStore,
              valueField: 'id',
              displayField: 'name',
              allowBlank: false
            }
          ],
          buttons: [
            {
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Preview_Button'),
              action: 'preview',
              ui: 'nx-primary',
              formBind: true
            }
          ]
        },
        {
          xtype: 'panel',
          layout: 'hbox',
          ui: 'nx-drilldown-message',
          cls: 'nx-drilldown-warning',
          iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
          title: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Warning'),
          style: {
            marginTop: '10px',
            marginBottom: '5px'
          }
        },
        {
          xtype: 'gridpanel',
          store: 'CleanupPreview',
          flex: 1,
          viewConfig: {
            emptyText: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_EmptyText_View'),
            emptyTextFilter: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_EmptyText_Filter'),
            deferEmptyText: false
          },

          columns: [
            {
              xtype: 'nx-iconcolumn',
              dataIndex: 'contentType',
              width: 36,
              iconVariant: 'x16',
              iconName: function() { return 'tree-component'; }
            },
            {
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Name_Column'),
              dataIndex: 'name',
              stateId: 'name',
              flex: 1
            },
            {
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Group_Column'),
              dataIndex: 'group',
              stateId: 'group',
              flex: 1
            },
            {
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Version_Column'),
              dataIndex: 'version',
              stateId: 'version',
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
        },
        {
          xtype: 'panel',
          itemId: 'componentCountPanel',
          hidden: true,
          layout: 'hbox',
          style: {
            marginTop: '10px',
            marginBottom: '5px'
          },
          items: [
            {
              xtype: 'label',
              style: {
                marginRight: '2px'
              },
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Total_Component_Count')
            },
            {
              xtype: 'label',
              id: 'currentComponentCount',
              name: 'currentComponentCount'
            },
            {
              xtype: 'label',
              style: {
                marginLeft: '2px',
                marginRight: '2px'
              },
              text: NX.I18n.get('CleanupPolicy_CleanupPolicyPreviewWindow_Total_Component_Count_Out_Of')
            },
            {
              xtype: 'label',
              id: 'totalComponentCount',
              name: 'totalComponentCount'
            }
          ]
        }
      ]
    });

    me.callParent();
    me.center();
  }
});

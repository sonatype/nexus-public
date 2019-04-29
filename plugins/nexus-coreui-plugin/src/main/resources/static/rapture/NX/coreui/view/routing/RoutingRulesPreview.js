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
 * Global Routing Rule Preview window.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.routing.RoutingRulesPreview', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-routing-rules-preview',
  requires: [
    'NX.I18n',
    'NX.Conditions',
    'NX.coreui.store.RoutingRulePreview',
    'NX.coreui.view.routing.RoutingRulesSettingsForm',
    'NX.ext.grid.column.Renderers'
  ],
  ui: 'nx-inset',
  scrollable: true,

  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'form',
        buttonAlign: 'left',
        ui: 'nx-subsection',
        frame: true,
        items: [
          {
            xtype: 'combo',
            name: 'filter',

            editable: false,
            forceSelection: true,
            value: 'all',
            valueField: 'value',
            store: {
              data: [
                {
                  text: NX.I18n.get('RoutingRules_PreviewContext_AllRepositories'),
                  value: 'all'
                },
                {
                  text: NX.I18n.get('RoutingRules_PreviewContext_AllGroups'),
                  value: 'groups'
                },
                {
                  text: NX.I18n.get('RoutingRules_PreviewContext_AllProxies'),
                  value: 'proxies'
                }
              ]
            }
          },
          {
            xtype: 'fieldcontainer',
            cls: 'nx-routing-rules-test-path-container',
            itemId: 'pathContainer',
            fieldLabel: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Path_FieldLabel'),
            helpText: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Path_HelpText'),
            layout: 'hbox',
            items: [
              {
                xtype: 'label',
                cls: 'nx-routing-rules-path-prefix',
                text: '/'
              },
              {
                xtype: 'textfield',
                name: 'path',
                emptyText: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Path_EmptyText'),
                flex: 1
              }
            ]
          }
        ],
        buttons: [
          {
            text: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Test'),
            tooltip: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Test_Tooltip'),
            action: 'preview',
            ui: 'nx-primary',
            bindToEnter: true
          }
        ]
      },
      {
        xtype: 'panel',
        layout: 'hbox',
        ui: 'nx-subsection',
        frame: true,
        items: [
          {
            xtype: 'treepanel',
            rootVisible: false,
            ui: 'nx-subsection',
            cls: 'grid-with-border',
            title: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_TestResult'),
            emptyText: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_EmptyText'),
            store: Ext.create('NX.coreui.store.RoutingRulePreview'),
            flex: 1,
            columns: [
              {
                xtype: 'treecolumn',
                text: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Column_Repository'),
                dataIndex: 'repository',
                flex: 2,
                renderer: Ext.htmlEncode
              },
              {
                text: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Column_RoutingRule'),
                dataIndex: 'rule',
                flex: 2,
                renderer: NX.ext.grid.column.Renderers.optionalRule
              },
              {
                text: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Column_AllowedBlocked'),
                dataIndex: 'allowed',
                flex: 1,
                renderer: NX.ext.grid.column.Renderers.allowedBlocked
              }
            ],
            plugins: [
              {
                ptype: 'gridfilterbox',
                emptyText: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Grid_Filter_EmptyText')
              }
            ]
          },
          {
            xtype: 'nx-coreui-routing-rules-settings-form',
            ui: 'nx-subsection-with-background',
            frame: true,
            style: {
              margin: '75px 0 0 10px',
              border: '1px solid #DDDDDD'
            },
            title: NX.I18n.get('RoutingRules_GlobalRoutingPreview_SelectedRule_Title'),
            flex: 1,
            hidden: true,
            readOnly: true,
            buttons: undefined,
            border: true
          }
        ]
      }
    ];

    me.callParent();
  }
});

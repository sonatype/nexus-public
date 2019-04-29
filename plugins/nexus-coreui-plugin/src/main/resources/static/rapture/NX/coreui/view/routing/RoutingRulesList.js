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
 * Routing Rules grid.
 *
 * @since 3.16
 */
Ext.define('NX.coreui.view.routing.RoutingRulesList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-routing-rules-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-routing-rules-list',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.store = 'RoutingRule';

    me.columns = [
      {
        header: NX.I18n.get('RoutingRules_Name_Label'),
        dataIndex: 'name',
        stateId: 'name',
        flex: 1,
        renderer: Ext.htmlEncode
      },
      {
        header: NX.I18n.get('RoutingRules_Description_Label'),
        dataIndex: 'description',
        stateId: 'description',
        flex: 1,
        renderer: Ext.htmlEncode
      },
      {
        header: NX.I18n.get('RoutingRules_UsedBy_Label'),
        dataIndex: 'assignedRepositoryCount',
        stateId: 'assignedRepositoryCount',
        tdCls: 'usedByTdCls',
        flex: 1,
        renderer: function(value) {
          var text = Ext.util.Format.plural(
                  value,
                  NX.I18n.get('RoutingRule_UsedBy_Repository_Singular'),
                  NX.I18n.get('RoutingRule_UsedBy_Repository_Plural')
              );
          if (value === 0) {
            text += ', ' + NX.I18n.get('RoutingRule_Assign_Repositories');
          }
          return text;
        },
        sorter: function(model1, model2) {
          var numRepos1 = model1.get('assignedRepositoryCount') || 0,
              numRepos2 = model2.get('assignedRepositoryCount') || 0;
          return numRepos1 - numRepos2;
        }
      }
    ];

    me.viewConfig = {
      emptyText: NX.I18n.get('RoutingRules_List_EmptyText'),
      deferEmptyText: false
    };

    me.plugins = [
      {
        ptype: 'gridfilterbox',
        emptyText: NX.I18n.get('RoutingRules_List_Filter_EmptyText')
      }
    ];

    me.dockedItems = [
      {
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('RoutingRules_Create_Button'),
            glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
            action: 'new'
          },
          {
            xtype: 'button',
            text: NX.I18n.get('RoutingRules_GlobalRoutingPreview_Button'),
            glyph: 'xf0b0@FontAwesome' /* fa-filter */,
            action: 'test'
          }
        ]
      }
    ];

    me.listeners = {
      render: me.createTooltip
    };

    me.callParent();
  },

  createTooltip: function(grid) {
    var view = grid.getView();

    grid.tip = Ext.create('Ext.tip.ToolTip', {
      target: view.getId(),
      delegate: view.itemSelector + ' .usedByTdCls',
      trackMouse: true,
      listeners: {
        beforeshow: function(tip) {
          var tipGridView = tip.target.component,
              record = tipGridView.getRecord(tip.triggerElement),
              repositoryCount = record.get('assignedRepositoryCount'),
              repositoryNames = record.get('assignedRepositoryNames');

          if (!repositoryCount) {
            return false;
          }

          tip.update(
              NX.I18n.format(repositoryCount !== repositoryNames.length ?
                  'RoutingRule_UsedBy_Info_Tooltip' :
                  'RoutingRule_UsedBy_Info_Tooltip_Permitted',
                  Ext.htmlEncode(repositoryNames.join(', '))));
        }
      }
    });
  }
});

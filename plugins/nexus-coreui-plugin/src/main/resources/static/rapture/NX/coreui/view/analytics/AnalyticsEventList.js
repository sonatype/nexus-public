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
 * Analytics Event grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.analytics.AnalyticsEventList', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-analytics-event-list',
  requires: [
    'Ext.XTemplate',
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-analytics-event-list',

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      store: 'AnalyticsEvent',

      viewConfig: {
        stripeRows: true
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          iconName: function (value, meta, record) {
            var type = record.get('type');
            switch (type) {
              case 'REST':
                return 'analyticsevent-rest';
              case 'Ext.Direct':
                return 'analyticsevent-ui';
              default:
                return 'analyticsevent-default';
            }
          }
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_Type_Header'),
          dataIndex: 'type',
          stateId: 'type',
          flex: 1
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_Timestamp_Header'),
          tooltip: NX.I18n.get('Analytics_AnalyticsEventList_Timestamp_Tooltip'),
          dataIndex: 'timestamp',
          stateId: 'timestamp',
          flex: 1
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_Sequence_Header'),
          dataIndex: 'sequence',
          stateId: 'sequence',
          flex: 1
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_Duration_Header'),
          tooltip: NX.I18n.get('Analytics_AnalyticsEventList_Duration_Tooltip'),
          dataIndex: 'duration',
          stateId: 'duration',
          flex: 1
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_User_Header'),
          dataIndex: 'userId',
          stateId: 'userId',
          flex: 1
        },
        {
          header: NX.I18n.get('Analytics_AnalyticsEventList_Attributes_Header'),
          dataIndex: 'attributes',
          stateId: 'attributes',
          flex: 3,
          renderer: function (value) {
            var text = '';
            Ext.Object.each(value, function (name, value) {
              if (text !== '') {
                text += ', ';
              }
              text += name + '=' + value;
            });
            return text;
          }
        }
      ],

      dockedItems: [{
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('Analytics_AnalyticsEventList_Clear_Button'),
            glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
            action: 'clear',
            disabled: true
          },
          {
            xtype: 'button',
            text: NX.I18n.get('Analytics_AnalyticsEventsList_Export_Button'),
            glyph: 'xf019@FontAwesome' /* fa-download */,
            action: 'export'
          },
          '-',
          {
            xtype: 'button',
            text: NX.I18n.get('Analytics_AnalyticsEventList_Submit_Button'),
            glyph: 'xf0ee@FontAwesome' /* fa-cloud-upload */,
            action: 'submit',
            disabled: true
          },
          '-',
          {
            xtype: 'pagingtoolbar',
            store: 'AnalyticsEvent',
            border: false
          }
        ]
      }],

      plugins: [
        {
          ptype: 'rowexpander',
          rowBodyTpl: Ext.create('Ext.XTemplate',
            '<table class="nx-rowexpander">',
            '<tpl for="this.attributes(values)">',
            '<tr>',
            '<td class="x-selectable">{name}</td>',
            '<td class="x-selectable">{value}</td>',
            '</tr>',
            '</tpl>',
            '</table>',
            {
              compiled: true,

              /**
               * Convert attributes field to array of name/value pairs for rendering in template.
               */
              attributes: function (values) {
                var result = [];
                Ext.iterate(values.attributes, function (name, value) {
                  result.push({ name: name, value: value });
                });
                return result;
              }
            })
        },
        { ptype: 'gridfilterbox', emptyText: NX.I18n.get('Analytics_AnalyticsEventList_Filter_EmptyText') }
      ]
    });

    this.callParent();
  }

});

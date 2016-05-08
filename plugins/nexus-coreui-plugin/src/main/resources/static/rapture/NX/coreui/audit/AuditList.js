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
 * Audit list.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.audit.AuditList', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-audit-list',
  requires: [
    'Ext.XTemplate',
    'NX.I18n',
    'NX.util.DateFormat',
    'NX.coreui.audit.AuditStore'
  ],

  stateful: true,
  stateId: 'nx-coreui-audit-list',

  /**
   * @override
   */
  initComponent: function() {
    var me = this,
      iconCtl = NX.getApplication().getController('Icon');

    Ext.apply(me, {
      store: 'NX.coreui.audit.AuditStore',

      viewConfig: {
        stripeRows: true,
        emptyText: NX.I18n.render(me, 'EmptyText'),
        deferEmptyText: false
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          /**
           * Attempt to resolve icon for audit-[domain], use if exists.
           */
          iconName: function (value, meta, record) {
            var iconName = 'audit-' + record.get('domain');
            var icon = iconCtl.findIcon(iconName, 'x16');
            if (icon !== null) {
              return iconName;
            }
            return 'audit-default';
          }
        },
        {
          header: NX.I18n.render(me, 'Domain'),
          dataIndex: 'domain',
          stateId: 'domain',
          flex: 1
        },
        {
          header: NX.I18n.render(me, 'Type'),
          dataIndex: 'type',
          stateId: 'type',
          flex: 1
        },
        {
          header: NX.I18n.render(me, 'Context'),
          dataIndex: 'context',
          stateId: 'context',
          flex: 1
        },
        {
          xtype: 'datecolumn',
          header: NX.I18n.render(me, 'Timestamp'),
          dataIndex: 'timestamp',
          stateId: 'timestamp',
          flex: 1
        },
        {
          header: NX.I18n.render(me, 'NodeId'),
          dataIndex: 'nodeId',
          stateId: 'nodeId',
          flex: 1,
          hidden: true
        },
        {
          header: NX.I18n.render(me, 'Initiator'),
          dataIndex: 'initiator',
          stateId: 'initiator',
          flex: 1
        }
      ],

      dockedItems: [{
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.render(me, 'Clear_Button'),
            glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
            action: 'clear',
            disabled: true
          },
          '-',
          {
            xtype: 'pagingtoolbar',
            store: 'NX.coreui.audit.AuditStore',
            border: false
          }
        ]
      }],

      plugins: [
        {
          ptype: 'rowexpander',

          rowBodyTpl: Ext.create('Ext.XTemplate',
            '<table class="nx-rowexpander">',
            '<tpl for="this.data(values)">',
            '<tr>',
            '<td class="x-selectable">{name}</td>',
            '<td class="x-selectable">{value}</td>',
            '</tr>',
            '</tpl>',
            '</table>',
            {
              compiled: true,

              data: function (values) {
                var result = [
                  { name: NX.I18n.render(me, 'Domain'), value: values.domain },
                  { name: NX.I18n.render(me, 'Type'), value: values.type },
                  { name: NX.I18n.render(me, 'Context'), value: values.context },
                  { name: NX.I18n.render(me, 'Timestamp'), value: NX.util.DateFormat.timestamp(values.timestamp) },
                  { name: NX.I18n.render(me, 'NodeId'), value: values.nodeId },
                  { name: NX.I18n.render(me, 'Initiator'), value: values.initiator }
                ];

                Ext.iterate(values.attributes, function (name, value) {
                  result.push({ name: NX.I18n.render(me, 'Attribute', name), value: value });
                });

                return result;
              }
            })
        },
        { ptype: 'gridfilterbox', emptyText: NX.I18n.render(me, 'Filter_EmptyText') }
      ]
    });

    me.callParent();
  }

});

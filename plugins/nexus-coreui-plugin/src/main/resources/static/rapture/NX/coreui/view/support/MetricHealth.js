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
 * Metric Health grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.MetricHealth', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-support-metric-health',
  requires: [
    'NX.I18n',
    'NX.coreui.model.MetricHealth',
    'NX.coreui.store.MetricHealth'
  ],

  stateful: true,
  stateId: 'nx-coreui-support-metric-health',

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      store: Ext.create('NX.coreui.store.MetricHealth'),

      viewConfig: {
        stripeRows: true
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          iconName: function (value, meta, record) {
            return record.get('healthy') ? 'message-success' : 'message-danger'; 
          }
        },
        {
          header: NX.I18n.get('Metric_Health_Name_Header'),
          dataIndex: 'name',
          stateId: 'name',
          flex: 1
        },
        {
          header: NX.I18n.get('Metric_Health_Message_Header'),
          dataIndex: 'message',
          stateId: 'message',
          flex: 1,
          renderer: function (val){
            return '<div style="white-space:normal !important;">'+ val +'</div>';
          }
        },
        {
          header: NX.I18n.get('Metric_Health_Error_Header'),
          dataIndex: 'error',
          stateId: 'error',
          flex: 1
        }
      ]
    });

    this.callParent();
  }

});

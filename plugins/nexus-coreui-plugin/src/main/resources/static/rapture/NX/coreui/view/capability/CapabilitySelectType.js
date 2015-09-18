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
 * Select capability type window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.capability.CapabilitySelectType', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-capability-selecttype',
  requires: [
    'NX.I18n'
  ],

  store: 'CapabilityType',
  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function() {
        return 'capability-default';
      }
    },
    { header: NX.I18n.get('Capability_CapabilitySelectType_Type_Header'), dataIndex: 'name', flex: 1 },
    { header: NX.I18n.get('Capability_CapabilitySelectType_Description_Header'), dataIndex: 'about', flex: 2,
      renderer: function(val) {
        var i;
        if (val) {
          i = val.indexOf('.');
          if (i > 0) {
            val = val.substring(0, i);
          }
          // replace HTML
          return val.replace(/(<([^>]+)>)/ig, '');
        }
        return val;
      }
    }
  ],

  initComponent: function() {
    var me = this;

    me.on('afterrender', function(grid) {
        var view = grid.getView();
        grid.tip = Ext.create('Ext.tip.ToolTip', {
          target: view.el,
          delegate: view.itemSelector,
          trackMouse: true,
          renderTo: Ext.getBody(),
          dismissDelay: 0,
          listeners: {
            beforeshow: function updateTipBody(tip) {
              tip.update(view.getRecord(tip.triggerElement).get('about'));
            }
          }
        });
      }
    );

    me.callParent(arguments);
  }

});

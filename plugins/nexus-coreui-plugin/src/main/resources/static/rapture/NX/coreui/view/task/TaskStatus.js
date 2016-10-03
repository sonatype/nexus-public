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
 * Task "Status" panel.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.task.TaskStatus', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-task-status',
  ui: 'nx-inset',
  requires: [
    'NX.I18n'
  ],

  initComponent: function() {
    var me = this;

    me.items = {
      xtype: 'grid',
          columns: [
        { text: NX.I18n.get('TaskFeature_Status_Node_Column'), dataIndex: 'nodeId', width: 400},
        { text: NX.I18n.get('TaskFeature_Status_Status_Column'), dataIndex: 'statusDescription', width: 200 },
        { text: NX.I18n.get('TaskFeature_Status_LastResult_Column'), dataIndex: 'lastRunResult', flex: 1 }
      ]
    };

    me.callParent();
  },

  getGrid: function() {
    var me = this;
    return me.down('grid');
  }
});

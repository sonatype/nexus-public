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
 * Task "Summary" panel.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.task.TaskSummary', {
  extend: 'Ext.Panel',
  alias: 'widget.nx-coreui-task-summary',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.title = NX.I18n.get('TaskFeature_Summary_Title');
    me.autoScroll = true;

    me.layout = {
      type: 'vbox',
      align: 'stretch'
    };

    me.items = {
      xtype: 'panel',
      ui: 'nx-inset',
      items: [
        {
          xtype: 'panel',
          ui: 'nx-subsection',
          itemId: 'nx-coreui-task-summary-subsection',
          frame: true,
          layout: 'column',
          weight: 10,
          items: [
            {
              xtype: 'nx-info',
              columnWidth: 1
            }
          ]
        },
        {
          xtype: 'nx-coreui-task-status',
          ui: 'nx-subsection',
          frame: true,
          title: NX.I18n.get('TaskFeature_Summary_Status_Section_Title'),
          weight: 20
        }
      ]
    };

    me.callParent();
  },

  showInfo: function (info) {
    this.down('nx-info').showInfo(info);
  },

  getStatuses: function() {
    var me = this;
    return me.down('nx-coreui-task-status');
  }
});

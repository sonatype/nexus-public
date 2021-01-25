/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
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
  cls: 'nx-hr',

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
        }
      ]
    };

    me.callParent();
  },

  showInfo: function (info) {
    this.down('nx-info').showInfo(info);
  }
});

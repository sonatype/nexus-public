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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Log panel.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.view.Log', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  xtype: 'nx-logging-view-log',

  title: 'Log',
  id: 'nx-logging-view-log',
  cls: 'nx-logging-view-log',
  layout: 'fit',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.logging.Icons,
        sp = Sonatype.lib.Permissions;

    Ext.apply(me, {
      id: 'nx-logging-view-log',

      items: {
        xtype: 'textarea',
        readOnly: true,
        hideLabel: true,
        emptyText: 'Refresh to display log',
        anchor: '100% 100%'
      },

      tbar: [
        {
          id: 'nx-logging-button-refresh-log',
          text: 'Refresh',
          tooltip: 'Refresh log',
          iconCls: icons.get('log_refresh').cls
        },
        {
          id: 'nx-logging-button-download-log',
          text: 'Download',
          tooltip: 'Download log file',
          iconCls: icons.get('log_download').cls
        },
        '-',
        {
          id: 'nx-logging-button-mark',
          text: 'Mark',
          tooltip: 'Add a mark in Nexus log file',
          iconCls: icons.get('log_mark').cls,
          disabled: !sp.checkPermission('nexus:logconfig', sp.EDIT)
        },
        '->',
        {
          xtype: 'combo',
          id: 'nx-logging-combo-refresh-period',
          triggerAction: 'all',
          lazyRender: true,
          mode: 'local',
          emptyText: 'Select...',
          editable: false,
          value: 0,
          store: NX.create('Ext.data.ArrayStore', {
            id: 0,
            fields: [
              'seconds',
              'text'
            ],
            data: [
              [0, 'Refresh manually'],
              [20, 'Refresh every 20 seconds'],
              [60, 'Refresh every minute'],
              [120, 'Refresh every 2 minutes'],
              [300, 'Refresh every 5 minutes']
            ]
          }),
          valueField: 'seconds',
          displayField: 'text'
        },
        {
          xtype: 'combo',
          id: 'nx-logging-combo-refresh-size',
          triggerAction: 'all',
          lazyRender: true,
          mode: 'local',
          emptyText: 'Select...',
          editable: false,
          width: 90,
          value: 25,
          store: NX.create('Ext.data.ArrayStore', {
            id: 0,
            fields: [
              'kb',
              'text'
            ],
            data: [
              [25, 'Last 25KB'],
              [50, 'Last 50KB'],
              [100, 'Last 100KB']
            ]
          }),
          valueField: 'kb',
          displayField: 'text'
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);

    me.retrieveLogTask = {
      interval: 0,
      started: false,

      changeInterval: function (millis) {
        me.retrieveLogTask.interval = millis;
        me.retrieveLogTask.stop();
        me.retrieveLogTask.start();
      },

      start: function () {
        if (me.retrieveLogTask.run) {
          if (me.retrieveLogTask.interval > 0) {
            Ext.TaskMgr.start(me.retrieveLogTask);
            me.retrieveLogTask.started = true;
            me.logDebug('Started refreshing log every ' + me.retrieveLogTask.interval / 1000 + ' seconds');
          }
          else {
            me.retrieveLogTask.run();
          }
        }
      },

      stop: function () {
        if (me.retrieveLogTask.started) {
          Ext.TaskMgr.stop(me.retrieveLogTask);
          me.retrieveLogTask.started = false;
          me.logDebug('Stopped refreshing log');
        }
      }
    };
  },

  /**
   * Display log content.
   * @param {String} text log content
   */
  showLog: function (text) {
    var textarea = this.down('textarea');
    textarea.setValue(text);
    // scroll to the bottom
    textarea.getEl().dom.scrollTop = 1000000;
  }
});
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
 * Metrics controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Metrics', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.Ajax',
    'NX.Messages',
    'NX.Permissions',
    'NX.util.Url',
    'NX.util.DownloadHelper',
    'NX.I18n'
  ],

  views: [
    'support.Metrics'
  ],
  refs: [
    {
      ref: 'metrics',
      selector: 'nx-coreui-support-metrics'
    },
    {
      ref: 'content',
      selector: 'nx-feature-content'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Metrics',
      view: { xtype: 'nx-coreui-support-metrics' },
      text: NX.I18n.get('Metrics_Text'),
      description: NX.I18n.get('Metrics_Description'),
      iconConfig: {
        file: 'chart_pie.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:metrics:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-support-metrics': {
          afterrender: me.load
        },
        'nx-coreui-support-metrics button[action=download]': {
          'click': me.downloadMetrics
        },
        'nx-coreui-support-metrics button[action=threads]': {
          'click': me.downloadThreads
        }
      }
    });
  },

  /**
   * Load metrics information and update charts.
   *
   * @private
   */
  load: function () {
    var me = this,
        panel = me.getMetrics();

    if (!panel) {
      return;
    }

    me.getContent().getEl().mask(NX.I18n.get('Metrics_Load_Mask'));

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/metrics/data'),
      method: 'GET',
      headers: {
        'accept': 'application/json'
      },
      scope: me,
      suppressStatus: true,

      callback: function (response) {
        me.getContent().getEl().unmask();
      },

      failure: function (response) {
        NX.Messages.add({ type: 'warning', text: NX.I18n.get('Metrics_Refresh_Warning') });
      },

      success: function (response) {
        var data = Ext.decode(response.responseText);
        var memoryTotalMax = gaugeValue('jvm.memory.total.max');
        var memoryUsed = gaugeValue('jvm.memory.total.used');
        var memoryAvailable = memoryTotalMax - memoryUsed;
        var memoryHeap = gaugeValue('jvm.memory.heap.used');
        var memoryHeapPercentage = Math.round((memoryHeap / memoryTotalMax) * 100);
        var memoryNonHeap = memoryUsed - memoryHeap;
        var memoryNonHeapPercentage = Math.round((memoryNonHeap / memoryTotalMax) * 100);

        // return gauge value
        function gaugeValue(name) {
          return data.gauges[name].value;
        }
        
        // return count from counter
        function counterValue(name) {
          return data.counters[name].count;
        }
       
        // return count from meter
        function meterCountValue(name) {
          return data.meters[name].count;
        }

        // return count from timer
        function timerCountValue(name) {
          return data.timers[name].count;
        }

        // update memory charts
        panel.setMemoryUsageData([
          {
            name: NX.I18n.get('Support_Metrics_Heap_Title'),
            data: memoryHeap,
            percentage: memoryHeapPercentage
          },
          {
            name: NX.I18n.get('Metrics_Heap_NonHeapItem'),
            data: memoryNonHeap,
            percentage: memoryNonHeapPercentage
          },
          {
            name: NX.I18n.get('Metrics_Heap_Available'),
            data: memoryAvailable,
            percentage: 100 - memoryHeapPercentage - memoryNonHeapPercentage
          }
        ]);

        // update threads charts
        panel.setThreadStatesData([
          { name: NX.I18n.get('Metrics_ThreadStates_New'), data: gaugeValue('jvm.thread-states.new.count') },
          { name: NX.I18n.get('Metrics_ThreadStates_Terminated'), data: gaugeValue('jvm.thread-states.terminated.count') },
          { name: NX.I18n.get('Metrics_ThreadStates_Blocked'), data: gaugeValue('jvm.thread-states.blocked.count') },
          { name: NX.I18n.get('Metrics_ThreadStates_Runnable'), data: gaugeValue('jvm.thread-states.runnable.count') },
          { name: NX.I18n.get('Metrics_ThreadStates_TimedWaiting'), data: gaugeValue('jvm.thread-states.timed_waiting.count') },
          { name: NX.I18n.get('Metrics_ThreadStates_Waiting'), data: gaugeValue('jvm.thread-states.waiting.count') }
        ]);
        
        // update active requests
        panel.setActiveRequestsData([
          { name: 'active-requests', data: counterValue('org.eclipse.jetty.webapp.WebAppContext.active-requests') },
          { name: 'active-dispatches', data: counterValue('org.eclipse.jetty.webapp.WebAppContext.active-dispatches') },
          { name: 'active-suspended', data: counterValue('org.eclipse.jetty.webapp.WebAppContext.active-suspended') }
        ]);
        
        // update web response codes
        panel.setResponseCodeData([
          {name: '1xx', data: meterCountValue('org.eclipse.jetty.webapp.WebAppContext.1xx-responses')},
          {name: '2xx', data: meterCountValue('org.eclipse.jetty.webapp.WebAppContext.2xx-responses')},
          {name: '3xx', data: meterCountValue('org.eclipse.jetty.webapp.WebAppContext.3xx-responses')},
          {name: '4xx', data: meterCountValue('org.eclipse.jetty.webapp.WebAppContext.4xx-responses')},
          {name: '5xx', data: meterCountValue('org.eclipse.jetty.webapp.WebAppContext.5xx-responses')}
        ]);
        
        panel.setWebRequestData([
          {name: 'GET', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.get-requests')},    
          {name: 'PUT', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.put-requests')},    
          {name: 'POST', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.post-requests')},    
          {name: 'DELETE', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.delete-requests')},    
          {name: 'OTHER', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.other-requests')},    
          {name: 'OPTIONS', data: timerCountValue('org.eclipse.jetty.webapp.WebAppContext.options-requests')}    
        ]);
      }
    });
  },

  /**
   * @private
   * Download metrics data.
   */
  downloadMetrics: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('service/metrics/data?pretty=true&download=true'));
  },

  /**
   * @private
   * Download thread dump.
   */
  downloadThreads: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('service/metrics/threads?download=true'));
  }

});

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
 * @since 3.13
 */
Ext.define('NX.coreui.controller.MetricHealth', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Messages',
    'NX.Permissions',
    'NX.util.Url',
    'NX.I18n'
  ],

  views: [
    'support.MetricHealth'
  ],
  refs: [
    {
      ref: 'healthchecks',
      selector: 'nx-coreui-support-metric-health'
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
      path: '/Support/Status',
      view: { xtype: 'nx-coreui-support-metric-health' },
      text: NX.I18n.get('Metric_Health_Text'),
      description: NX.I18n.get('Metric_Health_Description'),
      iconConfig: {
        file: 'server_chart.png',
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
        'nx-coreui-support-metric-health': {
          afterrender: me.load
        }
      }
    });
  },

  /**
   * Load metrics health information and populate Store.
   *
   * @private
   */
  load: function () {
    var me = this,
        panel = me.getHealthchecks(),
        loadStore = function (response) {
          var data = Ext.decode(response.responseText);
          var healthchecks = [];
          var keys = Ext.Object.getKeys(data);
          Ext.each(keys, function (key) {
            healthchecks.push({
              name: key,
              message: data[key].message,
              healthy: data[key].healthy,
              error: data[key].error
            });
          });
          panel.getStore().loadRawData(healthchecks);
        };
    
    if (!panel) {
      return;
    }

    me.getContent().getEl().mask(NX.I18n.get('Metric_Health_Load_Mask'));

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/metrics/healthcheck'),
      method: 'GET',
      headers: {
        'accept': 'application/json'
      },
      scope: me,
      suppressStatus: true,

      callback: function (response) {
        me.getContent().getEl().unmask();
      },

      success: function (response) {
        loadStore(response);
      },
      failure: function (response) {
        // the server will respond with a 500 code if any checks fail, but the data is still returned
        loadStore(response);
      }
    });
  }

});

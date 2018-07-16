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
 * Metrics panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.Metrics', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-support-metrics',
  requires: [
    'Ext.chart.Chart',
    'Ext.chart.theme.*',
    'Ext.data.ArrayStore',
    'NX.Assert',
    'NX.I18n'
  ],
  scrollable: true,
  cls: 'nx-coreui-support-metrics',

  /**
   * @private
   */
  widgetPanel: function (title) {
    return {
      xtype: 'panel',
      ui: 'nx-subsection',
      cls: 'metricwidget',
      frame: true,
      height: 240,
      width: 300,
      layout: 'fit',
      colspan: 2,
      title: title
    };
  },

  /**
   * @private
   */
  nameDataTip: function(suffix) {
    return function(tooltip, record, context) {
      var title = record.get('name') + ': ' + record.get('data') + ' ' + suffix;
      var percentage = record.get('percentage');

      if (percentage) {
        title += " (" + percentage + "%)";
      }

      tooltip.setTitle(title);
    }
  },

  /**
   * @private
   */
  highlightConfig: {
    opacity: 0.8,
    stroke: '#55c',
    'stroke-width': 3
  },

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      dockedItems: [{
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('Support_Metrics_Download_Button'),
            tooltip: NX.I18n.get('Metrics_Download_Tooltip'),
            glyph: 'xf019@FontAwesome' /* fa-download */,
            action: 'download'
          },
          '-',
          {
            xtype: 'button',
            text: NX.I18n.get('Support_Metrics_Dump_Button'),
            tooltip: NX.I18n.get('Support_Metrics_Dump_Tooltip'),
            glyph: 'xf019@FontAwesome' /* fa-download */,
            action: 'threads'
          }
        ]
      }],

      items: {
        xtype: 'panel',
        cls: [
          'nx-inset',
          'nx-hr'
        ],
        layout: 'column',

        // FIXME: Remove use of ui 'nx-subsection', adjust scss style of .nx-coreui-support-metrics .metricwidget

        items: [
          Ext.apply(this.widgetPanel(NX.I18n.get('Support_Metrics_MemoryUsage_Title')),
              {
                items: [
                  {
                    xtype: 'polar',
                    itemId: 'memoryUsage',
                    animate: false,
                    downloadServerUrl: ' ', // NEXUS-16479 avoids console warnings about security  //NOSONAR
                    insetPadding: 20,
                    colors: ['#5f7f1c', '#8ebd2b', '#ddd'],
                    store: Ext.create('Ext.data.ArrayStore', {
                      fields: ['name', 'data', 'percentage']
                    }),
                    series: [
                      {
                        type: 'pie',
                        angleField: 'data',
                        showInLegend: true,

                        label: {
                          field: 'name',
                          display: 'none'
                        },

                        tips: {
                          trackMouse: true,
                          renderer: this.nameDataTip('bytes')
                        }
                      }
                    ],
                    legend: {
                      docked: 'right'
                    }
                  }
                ]
              }
          ),
          Ext.apply(this.widgetPanel(NX.I18n.get('Support_Metrics_ThreadStates_Title')),
              {
                items: [
                  {
                    xtype: 'polar',
                    itemId: 'threadStates',
                    animate: false,
                    downloadServerUrl: ' ', // NEXUS-16479 avoids console warnings about security  //NOSONAR
                    insetPadding: 20,
                    theme: 'Base',
                    store: Ext.create('Ext.data.ArrayStore', {
                      fields: ['name', 'data']
                    }),
                    series: [
                      {
                        type: 'pie',
                        angleField: 'data',
                        showInLegend: true,

                        label: {
                          field: 'name',
                          display: 'none'
                        },

                        tips: {
                          trackMouse: true,
                          renderer: this.nameDataTip('')
                        }
                      }
                    ],
                    legend: {
                      docked: 'right',
                      boxStrokeWidth: 0
                    }
                  }
                ]
              }
          ),
          Ext.apply(this.widgetPanel(NX.I18n.get('Support_Metrics_Dispatches_Title')),
              {
                items: [
                  {
                    xtype: 'chart',
                    itemId: 'activeRequests',
                    animate: false,
                    downloadServerUrl: ' ', // NEXUS-16479 avoids console warnings about security  //NOSONAR
                    insetPadding: 20,
                    theme: 'Base',
                    flipXY: true,
                    store: Ext.create('Ext.data.ArrayStore', {
                      fields: ['name', 'data']
                    }),
                    axes: [
                      {
                        type: 'category',
                        position: 'left',
                        fields: ['name']
                      }
                    ],
                    series: [
                      {
                        type: 'bar',
                        axis: 'bottom',
                        highlight: true,
                        highlightCfg: this.highlightConfig,
                        label: {
                          display: 'insideEnd',
                          field: 'data',
                          renderer: Ext.util.Format.numberRenderer('0'),
                          orientation: 'horizontal',
                          color: '#333',
                          'text-anchor': 'middle'
                        },
                        xField: 'name',
                        yField: 'data',
                        tips: {
                          trackMouse: true,
                          renderer: this.nameDataTip('requests')
                        }
                      }
                    ]
                  }
                ]
              }
          ),
          Ext.apply(this.widgetPanel(NX.I18n.get('Support_Metrics_ResponseCode_Title')),
              {
                items: [
                  {
                    xtype: 'chart',
                    itemId: 'responseCodes',
                    animate: false,
                    downloadServerUrl: ' ', // NEXUS-16479 avoids console warnings about security  //NOSONAR
                    insetPadding: 20,
                    theme: 'Category1',
                    flipXY: true,
                    store: Ext.create('Ext.data.ArrayStore', {
                      fields: ['name', 'data'],
                      sorters: {property: 'name', direction: 'DESC'}
                    }),
                    axes: [
                      {
                        type: 'category',
                        position: 'left',
                        fields: ['name']
                      }
                    ],
                    series: [
                      {
                        type: 'bar',
                        axis: 'bottom',
                        highlight: true,
                        highlightCfg: this.highlightConfig,
                        tips: {
                          trackMouse: true,
                          renderer: this.nameDataTip('responses')
                        },
                        label: {
                          display: 'insideEnd',
                          field: 'data',
                          renderer: Ext.util.Format.numberRenderer('0'),
                          orientation: 'horizontal',
                          color: '#333',
                          'text-anchor': 'middle'
                        },
                        xField: 'name',
                        yField: 'data',
                        renderer: function (sprite, config, rendererData, index) {
                          var record = rendererData.store.getAt(index);
                          var color;
                          switch (record.get('name')) {
                            case '5xx':
                              color = 'rgb(255, 0, 0)';
                              break;
                            default: // covers all but error cases
                              color = 'rgb(68,220,225)';
                              break;
                          }
                          return Ext.apply(config, {
                            fill: color
                          });
                        }
                      }
                    ]
                  }
                ]
              }
          ),
          Ext.apply(this.widgetPanel(NX.I18n.get('Support_Metrics_Requests_Title')),
              {
                items: [
                  {
                    xtype: 'chart',
                    itemId: 'webRequests',
                    animate: false,
                    downloadServerUrl: ' ', // NEXUS-16479 avoids console warnings about security  //NOSONAR
                    insetPadding: 20,
                    theme: 'Category6',
                    flipXY: true,
                    store: Ext.create('Ext.data.ArrayStore', {
                      fields: ['name', 'data'],
                      sorters: {property: 'name', direction: 'DESC'}
                    }),
                    axes: [
                      {
                        type: 'category',
                        position: 'left',
                        fields: ['name']
                      }
                    ],
                    series: [
                      {
                        type: 'bar',
                        axis: 'bottom',
                        highlight: true,
                        highlightCfg: this.highlightConfig,
                        tips: {
                          trackMouse: true,
                          renderer: this.nameDataTip('requests')
                        },
                        label: {
                          display: 'insideEnd',
                          field: 'data',
                          renderer: Ext.util.Format.numberRenderer('0'),
                          orientation: 'horizontal',
                          color: '#333',
                          'text-anchor': 'middle'
                        },
                        xField: 'name',
                        yField: 'data'
                      }
                    ]
                  }
                ]
              }
          )
        ]
      }
    });

    this.callParent();
  },

  /**
   * Load the data into the store of the component referred to by the query.
   *
   * @private
   * @param data to be loaded
   * @param query used to find the component
   */
  loadStoreByQuery: function (data, query) {
    if (!this.rendered) {
      return;
    }

    var p = this.down(query);
    //<if assert>
    NX.Assert.assert(p, "Expected this.down('" + query + "') to return component");
    //</if>
    if (p) {
      p.getStore().loadData(data);
    }
  },

  /**
   * @public
   */
  setMemoryUsageData: function (data) {
    this.loadStoreByQuery(data, 'panel #memoryUsage');
  },

  /**
   * @public
   */
  setThreadStatesData: function (data) {
    this.loadStoreByQuery(data, 'panel #threadStates');
  },

  /**
   * @public
   */
  setActiveRequestsData: function(data) {
    this.loadStoreByQuery(data, 'panel #activeRequests');
  },

  /**
   * @public
   */
  setResponseCodeData: function(data) {
    this.loadStoreByQuery(data, 'panel #responseCodes');
  },

  /**
   * @public
   */
  setWebRequestData: function(data) {
    this.loadStoreByQuery(data, 'panel #webRequests');
  }
});

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

Ext.define('NX.controller.AnalyticsOptOut', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State'
  ],

  refs: [
    {ref: 'analytics', selector: 'nx-footer-analytics-opt-out'},
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      component: {
        'nx-footer-analytics-opt-out': {
          beforerender: me.renderTextAndButton
        }
      },
      controller: {
        '#Permissions': {
          changed: me.maybeSetVisible
        }
      }
    });

    me.callParent();
  },

  maybeSetVisible: function() {
    this.getAnalytics().maybeSetVisible()
  },

  dismiss: function() {
    var analyticsView = this.getAnalytics();
    NX.direct.capability_Capability.create(
        {enabled: true, typeId: 'analytics-configuration', properties: {'submitAnalytics': 'true'}},
        function(response) {
          analyticsView.setVisible(false);
          NX.State.setValue('acknowledgeAnalytics.required', false);
        }
    );
  },

  optOut: function() {
    var analyticsView = this.getAnalytics();
    NX.direct.capability_Capability.create(
        {enabled: false, typeId: 'analytics-configuration', properties: {'submitAnalytics': 'false'}},
        function(response) {
          analyticsView.setVisible(false);
          NX.State.setValue('acknowledgeAnalytics.required', false);
        }
    );
  },

  renderTextAndButton: function() {
    var me = this;
    var analyticsView = this.getAnalytics();

    analyticsView.add({
      width: 800,
      html: '<div class="nx-coreui-component-analyticsoptout-banner-text">' +
            'Sonatype will start to collect anonymous, non-sensitive usage metrics ' +
            'and performance information to shape the future of Nexus Repository.  ' +
            '<a class="nx-coreui-component-analyticsoptout-banner-text-link" ' +
            'href="https://help.sonatype.com/en/in-product-analytics-capability.html">Learn more</a> ' +
            'about the information we collect or '+
            '<span><a id="nx-analytics-opt-out-button" class="nx-coreui-component-analyticsoptout-banner-text-link">decline</a></span>.</div>',
      listeners: {
        click: {
          element: 'el',
          delegate: 'span',
          fn: function() {
            me.optOut();
          }
        }
      }
    });

    analyticsView.add(
        {
          html: '<div class="nx-coreui-component-analyticsoptout-banner-rectangle">' +
              '<span class="nx-coreui-component-analyticsoptout-banner-button-text">OK</span>' +
              '</div>',
          listeners: {
            click: {
              element: 'el',
              delegate: 'div',
              fn: function() {
                me.dismiss();
              }
            }
          }
        });
  }
});

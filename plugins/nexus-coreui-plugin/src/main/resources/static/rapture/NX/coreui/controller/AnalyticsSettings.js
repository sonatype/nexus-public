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
 * Analytics controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.AnalyticsSettings', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.State',
    'NX.I18n'
  ],

  views: [
    'analytics.AnalyticsSettings'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Analytics',
      text: NX.I18n.get('AnalyticsSettings_Text'),
      description: NX.I18n.get('AnalyticsSettings_Description'),
      view: { xtype: 'nx-coreui-analytics-settings' },
      iconConfig: {
        file: 'system_monitor.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:analytics:read');
      }
    }, me);

    me.listen({
      component: {
        'nx-coreui-analytics-settings nx-settingsform': {
          submitted: me.onSubmitted
        }
      }
    });
  },

  /**
   * @private
   * Set "analytics" state on save.
   */
  onSubmitted: function (form, action) {
    NX.State.setValue('analytics', { enabled: action.result.data.collection });
  }

});

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
 * Analytics settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.analytics.AnalyticsSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-analytics-settings',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-settingsform',
      settingsFormSuccessMessage: NX.I18n.get('Analytics_AnalyticsSettings_Update_Success'),
      api: {
        load: 'NX.direct.analytics_Settings.read',
        submit: 'NX.direct.analytics_Settings.update'
      },
      editableCondition: NX.Conditions.isPermitted('nexus:analytics:update'),
      editableMarker: NX.I18n.get('Analytics_AnalyticsSettings_Update_Error'),

      items: [
        {
          xtype: 'container',
          html: NX.I18n.get('Analytics_AnalyticsSettings_HelpText')
        },
        {
          xtype: 'checkbox',
          name: 'collection',
          boxLabel: NX.I18n.get('Analytics_AnalyticsSettings_Collection_BoxLabel')
        },
        {
          xtype: 'checkbox',
          name: 'autosubmit',
          boxLabel: NX.I18n.get('Analytics_AnalyticsSettings_Submission_BoxLabel')
        }
      ]
    };

    me.callParent();
  }
});

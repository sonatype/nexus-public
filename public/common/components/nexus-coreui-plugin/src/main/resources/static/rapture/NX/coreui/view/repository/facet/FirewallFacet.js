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
 * Configuration for Sonatype Nexus Firewall repository facet.
 */
Ext.define('NX.coreui.view.repository.facet.FirewallFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-firewall-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * Set to true for repository formats that support PCCS mode (npm and pypi).
   */
  pccsEnabled: false,

  /**
   * Set to true for Docker proxy repositories (to show layer download timeout field).
   */
  dockerProxy: false,

  /**
   * @override
   */
  initComponent: function() {
    var me = this,
        modeStore = [
          ['DISABLED', NX.I18n.get('Repository_Facet_FirewallFacet_Mode_Disabled')],
          ['AUDIT', NX.I18n.get('Repository_Facet_FirewallFacet_Mode_Audit')],
          ['QUARANTINE', NX.I18n.get('Repository_Facet_FirewallFacet_Mode_Quarantine')]
        ];

    if (me.pccsEnabled) {
      modeStore.push(['PCCS', NX.I18n.get('Repository_Facet_FirewallFacet_Mode_PCCS')]);
    }

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_FirewallFacet_Title'),

        items: [
          {
            xtype: 'panel',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-info',
            iconCls: NX.Icons.cls('drilldown-info', 'x16'),
            title: NX.I18n.get('Repository_Facet_FirewallFacet_Info_Message')
          },
          {
            xtype: 'combo',
            name: 'attributes.firewall.mode',
            itemId: 'firewallMode',
            fieldLabel: NX.I18n.get('Repository_Facet_FirewallFacet_Mode_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_FirewallFacet_Mode_HelpText'),
            editable: false,
            store: modeStore,
            queryMode: 'local',
            allowBlank: true,
            value: 'DISABLED',
            listeners: {
              change: function(combo, newValue) {
                var timeoutField = me.down('#layerDownloadTimeout');
                if (me.dockerProxy && timeoutField) {
                  // Show timeout field only when firewall is AUDIT, QUARANTINE, or PCCS (not DISABLED)
                  var showField = newValue && newValue !== 'DISABLED';
                  timeoutField.setDisabled(!showField);
                  timeoutField.setVisible(showField);
                }
              }
            }
          },
          // Layer Download Timeout field for Docker proxy repositories
          // Only visible when dockerProxy=true AND firewall mode is not DISABLED
          {
            xtype: 'numberfield',
            name: 'attributes.dockerProxy.layerDownloadTimeoutMinutes',
            itemId: 'layerDownloadTimeout',
            fieldLabel: NX.I18n.get('Repository_Facet_FirewallFacet_LayerDownloadTimeout_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_FirewallFacet_LayerDownloadTimeout_HelpText'),
            minValue: 1,
            maxValue: 1440,
            allowDecimals: false,
            allowBlank: true,
            value: null,
            emptyText: '10',
            // Initially hidden when not dockerProxy OR when mode is DISABLED (default value)
            hidden: !me.dockerProxy,
            disabled: !me.dockerProxy,
            validator: function(value) {
              if (!value || value === '') {
                return true; // Empty is valid (will use default)
              }
              var num = parseInt(value, 10);
              if (isNaN(num)) {
                return NX.I18n.get('Repository_Facet_FirewallFacet_LayerDownloadTimeout_ValidationError_NaN');
              }
              if (num < 1) {
                return NX.I18n.get('Repository_Facet_FirewallFacet_LayerDownloadTimeout_ValidationError_Min');
              }
              if (num > 1440) {
                return NX.I18n.get('Repository_Facet_FirewallFacet_LayerDownloadTimeout_ValidationError_Max');
              }
              return true;
            }
          }
        ]
      }
    ];

    me.callParent();

    // Sync initial visibility with the current combo value after the form is rendered
    // This ensures the timeout field is correctly shown/hidden when editing an existing repository
    if (me.dockerProxy) {
      var combo = me.down('#firewallMode');
      if (combo) {
        combo.fireEvent('change', combo, combo.getValue());
      }
    }
  }
});

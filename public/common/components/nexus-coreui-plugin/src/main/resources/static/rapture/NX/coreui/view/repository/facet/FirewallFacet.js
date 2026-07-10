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
            allowBlank: true
          }
        ]
      }
    ];

    me.callParent();
  }
});

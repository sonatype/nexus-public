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
 * Capability feature panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.capability.CapabilityFeature', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-capability-feature',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      iconName: 'capability-default',

      masters: [
        { xtype: 'nx-coreui-capability-list' }
      ],

      tabs: [
        { xtype: 'nx-coreui-capability-summary', weight: 10 },
        { xtype: 'nx-coreui-capability-settings', title: NX.I18n.get('Capability_CapabilitySettings_Title'), weight: 20 }
      ],

      nxActions: [
        {
          xtype: 'button',
          text: NX.I18n.get('Capability_CapabilityFeature_Delete_Button'),
          action: 'delete',
          disabled: true,
          iconCls: 'x-fa fa-trash'
        },
        '-',
        {
          xtype: 'button',
          text: NX.I18n.get('Capability_CapabilityFeature_Enable_Button'),
          action: 'enable',
          handler: function(button) {
            button.fireEvent('runaction');
          },
          disabled: true,
          iconCls: 'x-fa fa-play'
        },
        {
          xtype: 'button',
          text: NX.I18n.get('Capability_CapabilityFeature_Disable_Button'),
          action: 'disable',
          handler: function(button) {
            button.fireEvent('runaction');
          },
          disabled: true,
          iconCls: 'x-fa fa-stop'
        }
      ]
    });

    this.callParent();
  }
});

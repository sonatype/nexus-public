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
 * Routing Rules feature panel.
 *
 * @since 3.16
 */
Ext.define('NX.coreui.view.routing.RoutingRulesFeature', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-routing-rules-feature',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      iconName: 'routing-rules-default',

      masters: [
        {xtype: 'nx-coreui-routing-rules-list'}
      ],

      tabs: [
        {
          xtype: 'nx-coreui-routing-rules-edit',
          title: NX.I18n.get('RoutingRules_Settings_Title'),
          weight: 10
        }
      ],

      nxActions: [
        {
          xtype: 'nx-button',
          text: NX.I18n.get('RoutingRules_Delete_Button'),
          glyph: 'xf1f8@FontAwesome' /* fa-trash */,
          action: 'delete',
          disabled: true
        }
      ]
    });

    this.callParent();
  }
});

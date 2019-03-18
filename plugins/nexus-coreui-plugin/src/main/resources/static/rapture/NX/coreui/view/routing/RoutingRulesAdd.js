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
 * Add Routing Rule window.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.routing.RoutingRulesAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-routing-rules-add',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.coreui.view.routing.RoutingRulesSinglePreview'
  ],

  defaultFocus: 'name',

  initComponent: function() {
    this.settingsForm = {
      xtype: 'nx-coreui-routing-rules-settings-form',
      settingsFormSuccessMessage: function(data) {
        return NX.I18n.format('RoutingRule_Create_Message', data['name']);
      },
      editableCondition: NX.Conditions.watchState('routingRules'),
      editableMarker: NX.I18n.get('RoutingRule_Create_Error'),

      buttons: [
        {text: NX.I18n.get('RoutingRules_Create_Button'), action: 'create', formBind: true, ui: 'nx-primary'},
        {text: NX.I18n.get('Add_Cancel_Button'), action: 'back'}
      ]
    };

    this.callParent();

    this.down('nx-coreui-routing-rules-settings-form').addMatcherRow();
    this.items.get(0).add({xtype: 'nx-coreui-routing-rules-single-preview'});
  }

});

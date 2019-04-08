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
 * Configuration for the repository routing rule.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.repository.facet.RoutingRuleFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-routing-rule-facet',
  requires: [
    'NX.I18n',
    'NX.coreui.store.RoutingRule',
    'NX.coreui.view.repository.facet.RoutingRuleFacetViewController'
  ],

  controller: 'routingRuleViewController',

  viewModel: {
    stores: {
      RoutingRules: {
        source: 'RoutingRule'
      }
    }
  },

  items: [
    {
      xtype: 'fieldset',
      cls: 'nx-form-section',
      bind: {
        title: '{title}'
      },

      items: [
        {
          xtype: 'combo',
          reference: 'routingRuleCombo',
          name: 'routingRuleId',
          displayField: 'name',
          valueField: 'id',
          bind: {
            store: '{RoutingRules}'
          },
          queryMode: 'local',
          typeAhead: true,
          forceSelection: true
        }
      ]
    }
  ],

  constructor: function() {
    if (!NX.State.getValue('routingRules', false)) { // nexus.routing.rules.enabled
      this.config.viewModel.stores.RoutingRules = {
        data: []
      };
    }

    this.callParent();
  }
});

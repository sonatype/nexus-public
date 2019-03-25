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
Ext.define('NX.coreui.view.repository.facet.RoutingRuleFacetViewController', {
  extend: 'Ext.app.ViewController',
  alias: 'controller.routingRuleViewController',
  requires: [
    'NX.State',
    'NX.I18n'
  ],

  control: {
    '#': {
      beforeRender: 'onBeforeRender',
      afterRender: 'onAfterRender'
    }
  },

  onBeforeRender: function() {
    this.lookupReference('routingRuleCombo').setHelpText(NX.I18n.get('Repository_Facet_RoutingRuleFacet_HelpText'));
    this.getViewModel().set('title', NX.I18n.get('Repository_Facet_RoutingRuleFacet_Title'));

    this.getView().setVisible(NX.State.getValue('routingRules', false)); // nexus.routing.rules.enabled
  },

  onAfterRender: function() {
    if (!NX.State.getValue('routingRules', false)) { // nexus.routing.rules.enabled
      return;
    }

    var combo = this.lookupReference('routingRuleCombo'),
        chainedStore = this.getStore('RoutingRules'),
        sourceStore = chainedStore.getSource(),
        model = chainedStore.getModel();

    sourceStore.load(function() {
      chainedStore.insert(0, model.create({
        id: '',
        name: 'None'
      }));

      if (chainedStore.find('id', combo.getValue()) === -1) {
        combo.setValue('');
      }
    });
  }
});

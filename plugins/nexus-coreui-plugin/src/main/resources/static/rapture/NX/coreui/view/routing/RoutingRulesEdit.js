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
 * Routing Rules "Settings" panel.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.routing.RoutingRulesEdit', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-routing-rules-edit',

  initComponent: function() {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-coreui-routing-rules-settings-form',
      buttons: [
        {
          text: NX.I18n.get('SettingsForm_Save_Button'),
          action: 'save', ui: 'nx-primary',
          bindToEnter: false
        },
        {
          text: NX.I18n.get('SettingsForm_Discard_Button'),
          handler: me.onDiscardClick.bind(me)
        }
      ]
    };

    me.callParent();
  },

  loadRecord: function(record) {
    var matcherStrings = record.get('matchers'),
        form = this.down('nx-coreui-routing-rules-settings-form');

    form.resetMatchersSection();

    this.callParent(arguments);

    matcherStrings.forEach(function(matcherString) {
      form.addMatcherRow(matcherString);
    });

    this.currentRecord = record;
  },

  onDiscardClick: function() {
    var form = this.down('nx-coreui-routing-rules-settings-form');

    this.loadRecord(this.currentRecord);

    form.fireEvent('dirtychange', form.getForm());
  }
});

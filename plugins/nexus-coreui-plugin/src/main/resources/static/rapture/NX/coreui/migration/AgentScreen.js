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
 * Migration agent connection screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.AgentScreen', {
  extend: 'NX.wizard.FormScreen',
  alias: 'widget.nx-coreui-migration-agent',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, "Title"),

      description: NX.I18n.render(me, "Description"),

      fields: [
        {
          xtype: 'nx-url',
          name: 'url',
          fieldLabel: NX.I18n.render(me, "Endpoint_FieldLabel"),
          helpText: NX.I18n.render(me, "Endpoint_HelpText"),
          allowBlank: false
        },
        {
          xtype: 'textfield',
          inputType: 'password',
          inputAttrTpl: 'autocomplete="new-password"',
          name: 'accessToken',
          fieldLabel: NX.I18n.render(me, "Token_FieldLabel"),
          helpText: NX.I18n.render(me, "Token_HelpText"),
          allowBlank: false
        },
        {
          xtype: 'numberfield',
          name: 'fetchSize',
          fieldLabel: NX.I18n.render(me, "FetchSize_FieldLabel"),
          helpText: NX.I18n.render(me, "FetchSize_HelpText"),
          allowBlank: false,
          allowDecimals: false,
          allowExponential: false,
          minValue: 1,
          value: 100
        }
      ],

      buttons: [ 'back', 'next', 'cancel' ]
    });

    me.callParent();

    me.down('form').settingsForm = true;
  },

  /**
   * Returns the state of the screen form
   *
   * @return {boolean}
   */
  isDirty: function() {
    return this.down('form').isDirty();
  }
});

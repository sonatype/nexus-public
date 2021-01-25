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
 * Form for showing Node information, allowing admins to set a friendly name.
 *
 * @since 3.6
 */
Ext.define('NX.coreui.view.system.NodeSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-system-node-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.util.Validator'
  ],

  api: {
    submit: 'NX.direct.proui_Node.update'
  },

  settingsFormSuccessMessage: function(data) {
    return NX.I18n.get('Nodes_NodeSettingsForm_Update_Success') + data['friendlyName'];
  },

  initComponent: function() {
    var me = this;

    me.editableMarker = NX.I18n.get('Nodes_NodeSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:nodes:update');

    me.items = [
      {
        name: 'nodeIdentity',
        itemId: 'nodeIdentity',
        readOnly: true,
        fieldLabel: NX.I18n.get('Nodes_NodeSettingsForm_ID_FieldLabel'),
        helpText: NX.I18n.get('Nodes_NodeSettingsForm_ID_HelpText')
      },
      {
        name: 'socketAddress',
        itemId: 'socketAddress',
        readOnly: true,
        fieldLabel: NX.I18n.get('Nodes_NodeSettingsForm_SocketAddress_FieldLabel'),
        helpText: NX.I18n.get('Nodes_NodeSettingsForm_SocketAddress_HelpText')
      },
      {
        name: 'friendlyName',
        itemId: 'friendlyName',
        fieldLabel: NX.I18n.get('Nodes_NodeSettingsForm_FriendlyName_FieldLabel'),
        helpText: NX.I18n.get('Nodes_NodeSettingsForm_FriendlyName_HelpText'),
        allowBlank: true,
        vtype: 'nx-name'
      }
    ];

    me.callParent();
  }

});

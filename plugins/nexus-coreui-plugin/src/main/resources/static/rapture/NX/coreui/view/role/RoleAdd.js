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
 * Add role window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.role.RoleAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-role-add',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  defaultFocus: 'id',

  initComponent: function() {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-coreui-role-settings-form',
      api: {
        submit: 'NX.direct.coreui_Role.create'
      },
      settingsFormSuccessMessage: function(data) {
        return NX.I18n.get('Role_RoleAdd_Create_Success') + data['name'];
      },
      editableCondition: NX.Conditions.isPermitted('nexus:roles:create'),
      editableMarker: NX.I18n.get('Role_RoleAdd_Create_Error'),
      source: me.source,

      buttons: [
        { text: NX.I18n.get('Role_RoleList_New_Button'), action: 'add', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('Add_Cancel_Button'), action: 'back' }
      ]
    };

    me.callParent(arguments);

    me.down('#id').setReadOnly(false);
  }

});

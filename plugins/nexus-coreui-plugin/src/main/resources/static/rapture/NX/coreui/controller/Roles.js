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
 * Roles controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Roles', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-role-list'
  ],
  models: [
    'Role'
  ],
  stores: [
    'Role',
    'RoleSource',
    'RoleBySource',
    'PrivilegeReference'
  ],
  views: [
    'role.RoleAdd',
    'role.RoleFeature',
    'role.RoleList',
    'role.RoleSettings',
    'role.RoleSettingsForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-role-feature' },
    { ref: 'list', selector: 'nx-coreui-role-list' },
    { ref: 'settings', selector: 'nx-coreui-role-feature nx-coreui-role-settings' }
  ],
  icons: {
    'role-default': {
      file: 'user_policeman.png',
      variants: ['x16', 'x32']
    },
    'role-externalmapping': {
      file: 'shield.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:roles',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Security/Roles',
      text: NX.I18n.get('Roles_Text'),
      description: NX.I18n.get('Roles_Description'),
      view: {xtype: 'nx-coreui-role-feature'},
      iconConfig: {
        file: 'user_policeman.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:roles:read') && NX.Permissions.check('nexus:privileges:read');
      },
      weight: 20
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Role': {
          load: me.reselect
        },
        '#RoleSource': {
          load: me.onRoleSourceLoad
        }
      },
      component: {
        'nx-coreui-role-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-role-list menuitem[action=newrole]': {
          click: me.showAddWindowRole
        },
        'nx-coreui-role-list menuitem[action=newmapping]': {
          click: me.showAddWindowMapping
        },
        'nx-coreui-role-settings-form': {
          submitted: me.onSettingsSubmitted,
          beforerecordloaded: me.onBeforeRecordLoaded
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  onSelection: function(list, model) {
    if (Ext.isDefined(model)) {
      this.getSettings().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindowRole: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Roles_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-role-add'));
  },

  /**
   * @private
   */
  showAddWindowMapping: function(menuItem) {
    var me = this;

    me.getStore('RoleBySource').load({
      params: {
        source: menuItem.source
      }
    });

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Roles_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-role-add', { source: menuItem.source }));
  },

  /**
   * @private
   * (Re)create external role mapping entries.
   */
  onRoleSourceLoad: function(store) {
    var list = this.getList(),
        newButton, menuItems = [];

    if (list) {
      newButton = list.down('button[action=new]');
      if (newButton.menu.items.length > 1) {
        newButton.menu.remove(1);
      }
      store.each(function(source) {
        menuItems.push({
          text: source.get('name'),
          iconCls: NX.Icons.cls(source.getId().toLowerCase() + '-security-source', 'x16'),
          action: 'newmapping',
          source: source.getId()
        });
      });
      newButton.menu.add({
        text: NX.I18n.get('Roles_New_ExternalRoleItem'),
        menu: menuItems,
        iconCls: NX.Icons.cls('role-externalmapping', 'x16')
      });
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and role is not read only.
   */
  bindDeleteButton: function (button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.watchEvents([
              { observable: me.getStore('Role'), events: ['load']},
              { observable: Ext.History, events: ['change']}
            ], me.watchEventsHandler())
        ),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );
  },

  /**
   * @private
   */
  watchEventsHandler: function () {
    var me = this,
        store = me.getStore('Role');

    return function() {
      var roleId = me.getModelIdFromBookmark(),
          model = roleId ? store.findRecord('id', roleId, 0, false, true, true) : undefined;

      if (model) {
        return !model.get('readOnly');
      }

      return false;
    };
  },

  /**
   * @private
   */
  onSettingsSubmitted: function(form, action) {
    this.getStore('Role').load();
  },

  /**
   * @private
   * @override
   * Deletes a role.
   * @param model role to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Role.remove(model.getId(), function(response) {
      me.getStore('Role').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Roles_Delete_Message', description), type: 'success'
        });
      }
    });
  },

  onBeforeRecordLoaded: function(roleSettingsForm, editingRole) {
    var roleStore = roleSettingsForm.down('#roles').getStore();
    roleStore.load();
    roleStore.clearFilter(true);
    roleStore.filterBy(function(role) {
      return role.get('id') !== editingRole.get('id');
    });
  }

});

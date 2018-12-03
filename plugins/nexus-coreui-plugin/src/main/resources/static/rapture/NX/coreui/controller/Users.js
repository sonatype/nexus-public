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
 * Users controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Users', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.State',
    'NX.Permissions',
    'NX.Security',
    'NX.Icons',
    'NX.Messages',
    'NX.Dialogs',
    'NX.I18n',
    'NX.util.Filter',
    'Ext.History'
  ],
  masters: [
    'nx-coreui-user-list'
  ],
  models: [
    'User'
  ],
  stores: [
    'User',
    'UserSource',
    'Role'
  ],
  views: [
    'user.UserAccount',
    'user.UserAdd',
    'user.UserChangePassword',
    'user.UserFeature',
    'user.UserList',
    'user.UserSearchBox',
    'user.UserSettings',
    'user.UserSettingsForm',
    'user.UserSettingsExternal',
    'user.UserSettingsExternalForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-user-feature' },
    { ref: 'list', selector: 'nx-coreui-user-list' },
    { ref: 'userSearchBox', selector: 'nx-coreui-user-list nx-coreui-user-searchbox' },
    { ref: 'settings', selector: 'nx-coreui-user-feature nx-coreui-user-settings' },
    { ref: 'externalSettings', selector: 'nx-coreui-user-feature nx-coreui-user-settings-external' }
  ],
  icons: {
    'user-default': {
      file: 'user.png',
      variants: ['x16', 'x32']
    },
    'default-security-source': {
      file: 'user.png',
      variants: ['x16']
    },
    'allconfigured-security-source': {
      file: 'user.png',
      variants: ['x16']
    }
  },

  permission: 'nexus:users',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = [
      {
        mode: 'admin',
        path: '/Security/Users',
        text: NX.I18n.get('User_Text'),
        description: NX.I18n.get('User_Description'),
        view: {xtype: 'nx-coreui-user-feature'},
        iconConfig: {
          file: 'group.png',
          variants: ['x16', 'x32']
        },
        visible: function() {
          return NX.Permissions.check('nexus:users:read') && NX.Permissions.check('nexus:roles:read');
        },
        weight: 30
      },
      {
        mode: 'user',
        path: '/Account',
        text: NX.I18n.get('Users_Text'),
        description: NX.I18n.get('Users_Description'),
        view: {xtype: 'nx-coreui-user-account'},
        iconConfig: {
          file: 'user.png',
          variants: ['x16', 'x32']
        },
        visible: function() {
          return NX.Security.hasUser();
        }
      }
    ];

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        },
        '*': {
          userSettingsAddTab: me.addTab,
          userSettingsRemoveTab: me.removeTab
        }
      },
      store: {
        '#User': {
          load: me.reselect
        },
        '#UserSource': {
          load: me.onUserSourceLoad
        }
      },
      component: {
        'nx-coreui-user-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-user-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-user-settings-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-user-settings-external-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-user-list menuitem[action=filter]': {
          click: me.onSourceChanged
        },
        'nx-coreui-user-feature button[action=setpassword]': {
          afterrender: me.bindSetPasswordButton,
          click: me.showChangePasswordWindowForSelection
        },
        'nx-coreui-user-list nx-coreui-user-searchbox': {
          search: me.loadStore,
          searchcleared: me.onSearchCleared
        },
        'nx-coreui-user-account button[action=changepassword]': {
          click: me.showChangePasswordWindowForUserAccount,
          afterrender: me.bindChangePasswordButton
        },
        'nx-coreui-user-changepassword button[action=changepassword]': {
          click: me.changePassword
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('firstName') + ' ' + model.get('lastName');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    var me = this,
        settingsPanel = me.getSettings(),
        externalSettingsPanel = me.getExternalSettings();

    if (Ext.isDefined(model)) {
      if (model.get('external')) {
        if (!externalSettingsPanel) {
          me.addTab({ xtype: 'nx-coreui-user-settings-external', title: 'Settings', weight: 10 });
          externalSettingsPanel = me.getExternalSettings();
        }
        externalSettingsPanel.loadRecord(model);
      }
      else {
        if (!settingsPanel) {
          me.addTab({ xtype: 'nx-coreui-user-settings', title: 'Settings', weight: 10 });
          settingsPanel = me.getSettings();
        }
        settingsPanel.loadRecord(model);
      }

      if (model.get('external')) {
        if (settingsPanel) {
          me.removeTab(settingsPanel);
        }
      }
      else {
        if (externalSettingsPanel) {
          me.removeTab(externalSettingsPanel);
        }
      }
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Users_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-user-add'));
  },

  /**
   * @private
   */
  showChangePasswordWindowForSelection: function() {
    var list = this.getList(),
        userId = list.getSelectionModel().getSelection()[0].get('userId');

    NX.Security.doWithAuthenticationToken(
        'Changing password requires validation of your credentials.',
        {
          success: function(authToken) {
            Ext.widget('nx-coreui-user-changepassword', { userId: userId, authToken: authToken });
          }
        }
    );
  },

  /**
   * @private
   */
  showChangePasswordWindowForUserAccount: function(button) {
    var userId = button.up('form').down('#userId').getValue();

    NX.Security.doWithAuthenticationToken(
        'Changing password requires validation of your credentials.',
        {
          success: function(authToken) {
            Ext.widget('nx-coreui-user-changepassword', { userId: userId, authToken: authToken });
          }
        }
    );
  },

  /**
   * @override
   * Override in order to filter users by selected source (user manager) and user id.
   */
  loadStore: function(cb) {
    var me = this,
        list = me.getList(),
        userSourceButton;

    if (list) {
      userSourceButton = list.down('button[action=filter]');
      if (!userSourceButton.sourceId) {
        userSourceButton.sourceId = 'default';
      }
      me.updateEmptyText();
      me.getStore('User').load({
        params: {
          filter: [
            { property: 'source', value: userSourceButton.sourceId },
            { property: 'userId', value: me.getUserSearchBox().getValue() }
          ]
        },
        callback: function(records, operation, success) {
          if (Ext.isFunction(cb)) {
            cb(records, operation, success);
          }
        }
      });
    }
  },

  /**
   * @override
   * Load all of the stores associated with this controller
   */
  loadStores: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.loadStore();
      me.getStore('UserSource').load();
      me.getStore('Role').load();
    }
  },

  /**
   * @private
   * (Re)create user source filters.
   */
  onUserSourceLoad: function(store) {
    var list = this.getList(),
        userSourceButton;

    if (list) {
      userSourceButton = list.down('button[action=filter]');
      if (userSourceButton.menu.items.length > 1) {
        userSourceButton.menu.removeAll();
      }
      if (!userSourceButton.sourceId) {
        userSourceButton.sourceId = 'default';
      }
      store.each(function(source) {
        var iconCls = NX.Icons.cls(source.getId().toLowerCase() + '-security-source', 'x16');
        userSourceButton.menu.add({
          text: source.get('name'),
          iconCls: iconCls,
          group: 'usersource',
          checked: userSourceButton.sourceId === source.getId(),
          action: 'filter',
          source: source
        });
        if (userSourceButton.sourceId === source.getId()) {
          userSourceButton.setText(source.get('name'));
          userSourceButton.setIconCls(iconCls);
        }
      });
    }
  },

  /**
   * @private
   */
  onSearchCleared: function() {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]');

    if (userSourceButton.sourceId === 'default') {
      me.loadStore();
    }
    else {
      me.updateEmptyText();
      me.getStore('User').removeAll();
    }
  },

  /**
   * @private
   */
  onSourceChanged: function(menuItem) {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]');

    userSourceButton.setText(menuItem.source.get('name'));
    userSourceButton.setIconCls(menuItem.iconCls);
    userSourceButton.sourceId = menuItem.source.getId();

    me.getUserSearchBox().setValue(undefined);
    me.loadStore();
  },

  /**
   * @private
   * Update grid empty text based on source/user id from search box.
   */
  updateEmptyText: function() {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]'),
        userId = me.getUserSearchBox().getValue(),
        emptyText = '';

    if (userSourceButton.sourceId === 'default') {
      if (userId) {
        emptyText += 'No user matched query criteria "$filter"';
      }
      else {
        emptyText += 'No users defined';
      }
    }
    else {
      emptyText += 'No ' + userSourceButton.getText() + ' user matched query criteria';
      if (userId) {
        emptyText += ' "$filter"';
      }
    }

    list.getView().emptyText = NX.util.Filter.buildEmptyResult(userId, emptyText);
  },

  /**
   * @private
   */
  onSettingsSubmitted: function(form, action) {
    var me = this,
        list = me.getList(),
        selection = list.getSelection();

    me.loadStore(function(records) {
      var selectedId;
      if (selection && selection.length > 0) {
        selectedId = selection[0].get('userId');
        records.forEach(function(candidate) {
          if (candidate.get('userId') === selectedId) {
            list.setSelection(candidate);
          }
        });
      }
    });
  },

  /**
   * @private
   */
  watchEventsHandler: function (skipCurrentUser) {
    var me = this,
        store = me.getStore('User');

    return function() {
      var userId = me.getModelIdFromBookmark(),
          enableButton = false,
          selectedModel;

      if (userId) {
        selectedModel = store.findRecord('userId', userId, 0, false, true, true);

        enableButton = selectedModel && !selectedModel.get('external') && (selectedModel.get('userId') !== NX.State.getValue('anonymousUsername'));

        if (enableButton && skipCurrentUser) {
          enableButton = (selectedModel.get('userId') !== NX.State.getUser().id);
        }
      }

      return enableButton;
    };
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and user is not read only and user is not the current signed on
   * used or the anonymous user.
   */
  bindDeleteButton: function(button) {
    var arr = [
      { observable: this.getUserStore(), events: ['load']},
      { observable: Ext.History, events: ['change']}
    ];

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':delete'),
            NX.Conditions.watchEvents(arr, this.watchEventsHandler(true))
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
   * @override
   * Deletes a user.
   * @param model user to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_User.remove(model.get('userId'), model.get('realm'), function(response) {
      me.getStore('User').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Users_Delete_Success', description), type: 'success'
        });
      }
    });
  },

  /**
   * @protected
   * Enable 'ChangePassword' button as appropriate for user's permissions.
   */
  bindSetPasswordButton: function(button) {
    var arr = [
      { observable: this.getUserStore(), events: ['load']},
      { observable: Ext.History, events: ['change']}
    ];

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:userschangepw:create'),
            NX.Conditions.watchEvents(arr, this.watchEventsHandler())
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
   * @override
   * @private
   * Enable 'Change Password' when user has 'nexus:userschangepw:create' permission.
   */
  bindChangePasswordButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:userschangepw:create')
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
  changePassword: function(button) {
    var win = button.up('window'),
        password = button.up('form').down('#password').getValue();

    NX.direct.coreui_User.changePassword(win.authToken, win.userId, password, function(response) {
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({ text: NX.I18n.get('Users_Change_Success'), type: 'success' });
      }
    });
  },

  /**
   * @override
   * @private
   * Get an ID from a model. Override if using a model with a synthetic ID
   */
  getModelId: function(model) {
    return model.get('userId');
  },

  reselect: function () {
    var userSearchBox = this.getUserSearchBox();
    if (userSearchBox && !userSearchBox.hasFocus) {
      this.callParent();
    }
  }
});

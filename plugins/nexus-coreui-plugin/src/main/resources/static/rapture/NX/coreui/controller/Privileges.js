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
 * Privilege controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Privileges', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-privilege-list'
  ],
  stores: [
    'Privilege',
    'PrivilegeType'
  ],
  models: [
    'Privilege',
    'PrivilegeType'
  ],
  views: [
    'privilege.PrivilegeAdd',
    'privilege.PrivilegeFeature',
    'privilege.PrivilegeList',
    'privilege.PrivilegeSelectType',
    'privilege.PrivilegeSettings',
    'privilege.PrivilegeSettingsForm',
    'formfield.SettingsFieldSet'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-privilege-feature'},
    {ref: 'content', selector: 'nx-feature-content' },
    {ref: 'list', selector: 'nx-coreui-privilege-list'},
    {ref: 'settings', selector: 'nx-coreui-privilege-feature nx-coreui-privilege-settings'}
  ],
  icons: {
    'privilege-default': {
      file: 'medal_gold_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-application': {
      file: 'medal_gold_green.png',
      variants: ['x16', 'x32']
    },
    'privilege-wildcard': {
      file: 'medal_gold_blue.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository': {
      file: 'database.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-admin': {
      file: 'database_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-content-selector': {
      file: 'content_selector.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-view': {
      file: 'database.png',
      variants: ['x16', 'x32']
    },
    'privilege-script': {
      file: 'script_text.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:privileges',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Security/Privileges',
      text: NX.I18n.get('Privileges_Text'),
      description: NX.I18n.get('Privileges_Description'),
      view: {xtype: 'nx-coreui-privilege-feature'},
      iconConfig: {
        file: 'medal_gold_green.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:privileges:read') &&
            !NX.State.getValue('nexus.react.privileges', false);
      },
      weight: 10
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Privilege': {
          load: function() {
            var focusedElement = window.document.activeElement;

            me.reselect(arguments);

            // Calling reselect() removes the focus from the Filter box.
            // This prevents the Filter box from losing focus while the
            // user is typing in it - NEXUS-16975, NEXUS-12693
            if (focusedElement) {
              focusedElement.focus();
            }
          }
        }
      },
      component: {
        'nx-coreui-privilege-list': {
          beforerender: me.loadPrivilegeStores
        },
        'nx-coreui-privilege-list button[action=new]': {
          click: me.showSelectTypePanel
        },
        'nx-coreui-privilege-selecttype': {
          cellclick: me.showAddPanel
        },
        'nx-coreui-privilege-add button[action=add]': {
          click: me.createPrivilege
        },
        'nx-coreui-privilege-settings button[action=save]': {
          click: me.updatePrivilege
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function (list, model) {
    var settingsPanel = this.getSettings();
    if (Ext.isDefined(model)) {
      settingsPanel.loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddPanel: function(list, td, cellIndex, model) {
    var me = this,
        panel;

    // Shows the first panel in the create wizard, and sets the breadcrumb
    me.setItemName(2, NX.I18n.format('Privileges_Create_Title', model.get('name')));
    me.loadCreateWizard(2, panel = Ext.create('widget.nx-coreui-privilege-add'));
    var m = me.getPrivilegeModel().create({ type: model.getId(), readonly: false });
    panel.down('nx-settingsform').loadRecord(m);
  },

  /**
   * @override
   * @protected
   * Enables 'New' button when user has 'create' permission and there is at least one privilege type.
   */
  bindNewButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':create'),
            NX.Conditions.storeHasRecords('PrivilegeType')
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
   * @protected
   * Enables 'Delete' when user has 'delete' permission and privilege is not read only.
   */
  bindDeleteButton: function (button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.watchEvents([
              { observable: me.getStore('Privilege'), events: ['load']},
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
        store = me.getStore('Privilege');

    return function() {
      var privilegeId = me.getModelIdFromBookmark(),
          model = privilegeId ? store.findRecord('id', privilegeId, 0, false, true, true) : undefined;

      if (model) {
        return !model.get('readOnly');
      }

      return false;
    };
  },

  /**
   * @private
   * Creates a privlege.
   */
  createPrivilege: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    NX.direct.coreui_Privilege.create(values, function(response) {
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.success(NX.I18n.format('Privileges_Create_Success',
                me.getDescription(me.getPrivilegeModel().create(response.data))));
          me.getStore('Privilege').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @private
   * Updates privilege.
   */
  updatePrivilege: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    me.getContent().getEl().mask(NX.I18n.get('Privileges_Update_Mask'));
    NX.direct.coreui_Privilege.update(values, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.success(NX.I18n.format('Privileges_Update_Success',
                me.getDescription(me.getPrivilegeModel().create(response.data))));
          form.fireEvent('submitted', form);
          me.getStore('Privilege').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @private
   * @override
   * Deletes a privilege.
   * @param model privilege to be deleted
   */
  deleteModel: function (model) {
    var me = this;
    NX.direct.coreui_Privilege.remove(model.getId(), function (response) {
      me.getStore('Privilege').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.success(NX.I18n.format('Privileges_Delete_Success', model.get('name')));
      }
    });
  },

  /**
   * @private
   */
  showSelectTypePanel: function() {
    var me = this;

    // Shows the first panel in the create wizard, and sets the breadcrumb
    me.setItemName(1, NX.I18n.get('Privileges_Select_Title'));
    me.loadCreateWizard(1, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-privilege-selecttype',
          flex: 1
        }
      ]
    }));
  },

  loadPrivilegeStores: function() {
    var me = this;

    Ext.each(this.storesForLoad, function(store){
      me.getStore(store).clearFilter(true);
    });
    me.loadStores();
  }
});

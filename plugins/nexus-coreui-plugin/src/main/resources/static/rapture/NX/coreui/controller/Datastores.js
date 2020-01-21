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
 * Datastores controller.
 *
 * @since 3.19
 */
Ext.define('NX.coreui.controller.Datastores', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-datastore-list'
  ],
  models: [
    'Datastore'
  ],
  stores: [
    'DatastoreSource',
    'DatastoreType',
    'Datastore',
    'ModifiableDatastoreSource',
    'ContentDatastore'
  ],
  views: [
    'datastore.DatastoreAdd',
    'datastore.DatastoreFeature',
    'datastore.DatastoreList',
    'datastore.DatastoreSettings',
    'datastore.DatastoreSettingsForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-datastore-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-datastore-list' },
    { ref: 'settings', selector: 'nx-coreui-datastore-feature nx-coreui-datastore-settings' }
  ],
  icons: {
    'datastore-default': {
      file: 'database_share.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:datastores',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Repository/Datastores',
      text: NX.I18n.get('Datastores_Text'),
      description: NX.I18n.get('Datastores_Description'),
      view: {xtype: 'nx-coreui-datastore-feature'},
      iconConfig: {
        file: 'database_share.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:datastores:read') && !Ext.isEmpty(NX.State.getUser());
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Datastore': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-datastore-list': {
          beforerender: me.loadRecipe
        },
        'nx-coreui-datastore-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-datastore-settings button[action=save]': {
          click: me.updateDatastore
        },
        'nx-coreui-datastore-settings-form': {
          submitted: me.loadStores
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

  /**
   * @override
   */
  onSelection: function(list, model) {
    if (Ext.isDefined(model)) {
      this.getSettings().loadRecord(model);
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission for selected data store.
   */
  bindDeleteButton: function(button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':delete'),
            NX.Conditions.watchEvents([
              { observable: me.getStore('Datastore'), events: ['load']},
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
        store = me.getStore('Datastore');

    return function() {
      var datastoreId = me.getModelIdFromBookmark(),
          model = datastoreId ? store.findRecord('name', datastoreId, 0, false, true, true) : undefined;

      if (model) {
        var inUse = model.get('inUse');
        if (inUse) {
          me.showInfo(NX.I18n.format('Datastore_DatastoreFeature_Delete_Disabled_Message'));
          return false;
        }

        me.clearInfo();
        return true;
      }

      return false;
    };
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Datastores_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-datastore-add'));
  },

  /**
   * @private
   */
  loadRecipe: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getStore('Datastore').clearFilter();
      me.getStore('DatastoreSource').load();
      me.getStore('DatastoreType').load();
    }
  },

  /**
   * @private
   * Updates data store.
   */
  updateDatastore: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    NX.Dialogs.askConfirmation(NX.I18n.get('Datastore_DatastoreFeature_Update_Title'),
                               NX.I18n.get('Datastore_DatastoreFeature_Update_Warning'),
                               function () {
      me.getContent().getEl().mask(NX.I18n.get('Datastores_Update_Mask'));
      NX.direct.coreui_Datastore.update(values, function(response) {
        me.getContent().getEl().unmask();
        if (Ext.isObject(response)) {
          if (response.success) {
            NX.Messages.success(NX.I18n.format('Datastores_Update_Success',
                  me.getDescription(me.getDatastoreModel().create(response.data))));
            me.getStore('Datastore').load();
          }
          else if (Ext.isDefined(response.errors)) {
            form.markInvalid(response.errors);
          }
        }
      });
    }, {scope: me});
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    me.getContent().getEl().mask(NX.I18n.get('Datastores_Delete_Mask'));
    NX.direct.coreui_Datastore.remove(model.getId(), function(response) {
      me.getContent().getEl().unmask();
      me.getStore('Datastore').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.success(NX.I18n.format('Datastores_Delete_Success', description));
      }
    });
  }
});

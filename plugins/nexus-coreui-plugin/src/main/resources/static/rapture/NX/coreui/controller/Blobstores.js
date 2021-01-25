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
 * Blobstores controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Blobstores', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-blobstore-list'
  ],
  models: [
    'Blobstore'
  ],
  stores: [
    'Repository',
    'Blobstore',
    'BlobstoreType',
    'BlobStoreQuotaType'
  ],
  views: [
    'blobstore.BlobstoreAdd',
    'blobstore.BlobstoreFeature',
    'blobstore.BlobstoreList',
    'blobstore.BlobstoreSettings',
    'blobstore.BlobstoreSettingsForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-blobstore-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-blobstore-list' },
    { ref: 'settings', selector: 'nx-coreui-blobstore-feature nx-coreui-blobstore-settings' }
  ],
  icons: {
    'blobstore-default': {
      file: 'drive_network.png',
      variants: ['x16', 'x32']
    },
    'blobstore-failure': {
      file: 'drive_network_error.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:blobstores',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Repository/Blobstores',
      text: NX.I18n.get('Blobstores_Text'),
      description: NX.I18n.get('Blobstores_Description'),
      view: {xtype: 'nx-coreui-blobstore-feature'},
      iconConfig: {
        file: 'drive_network.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:blobstores:read') &&
            !Ext.isEmpty(NX.State.getUser()) &&
            !NX.State.getValue('nexus.react.blobstores', false);
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
        '#Blobstore': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-blobstore-list': {
          beforerender: me.loadRecipe
        },
        'nx-coreui-blobstore-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-blobstore-settings button[action=save]': {
          click: me.updateBlobstore
        },
        'nx-coreui-blobstore-feature button[action=promoteToGroup]': {
          click: me.promoteToGroup,
          afterrender: me.bindIfUpdatableAndGroupsEnabled
        },
        'nx-coreui-blobstore-settings-form': {
          submitted: me.loadStores
        },
        //Note that this component is from the Task UI
        'combobox[name=property_fromGroup]': {
          change: me.removeGroupMemberTaskFromGroupChanged
        },
        'combobox[name=property_moveRepositoryName]': {
          change: me.moveRepositoryTaskRepositoryNameChanged
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
   * Enable 'Delete' when user has 'delete' permission for selected blobstore.
   */
  bindDeleteButton: function(button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':delete'),
            NX.Conditions.watchEvents([
              { observable: me.getStore('Blobstore'), events: ['load']},
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
        store = me.getStore('Blobstore');

    return function() {
      var blobstoreId = me.getModelIdFromBookmark(),
          model = blobstoreId ? store.findRecord('name', blobstoreId, 0, false, true, true) : undefined;

      if (model) {
        var inUse = model.get('inUse');
        var taskUseCount = model.get('taskUseCount');
        if (inUse || taskUseCount > 0) {
          var repositoryUseCount = model.get('repositoryUseCount');
          var blobStoreUseCount = model.get('blobStoreUseCount');
          me.showInfo(NX.I18n.format('Blobstore_BlobstoreFeature_Delete_Disabled_Message',
              Ext.util.Format.plural(repositoryUseCount, 'repository', 'repositories'),
              Ext.util.Format.plural(blobStoreUseCount, 'other blob store', 'other blob stores'),
              Ext.util.Format.plural(taskUseCount, 'task', 'tasks')));
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
    me.setItemName(1, NX.I18n.get('Blobstores_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-blobstore-add'));
  },

  /**
   * @private
   */
  loadRecipe: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getStore('Blobstore').clearFilter();
      me.getStore('BlobstoreType').load();
      me.getStore('BlobStoreQuotaType').load();
    }
  },

  /**
   * @private
   * Updates blobstore.
   */
  updateBlobstore: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    NX.Dialogs.askConfirmation(NX.I18n.get('Blobstore_BlobstoreFeature_Update_Title'),
                               NX.I18n.get('Blobstore_BlobstoreFeature_Update_Warning'),
                               function () {
      me.getContent().getEl().mask(NX.I18n.get('Blobstores_Update_Mask'));
      NX.direct.coreui_Blobstore.update(values, function(response) {
        me.getContent().getEl().unmask();
        if (Ext.isObject(response)) {
          if (response.success) {
            NX.Messages.success(NX.I18n.format('Blobstores_Update_Success',
                  me.getDescription(me.getBlobstoreModel().create(response.data))));
            me.getStore('Blobstore').load();
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

    me.getContent().getEl().mask(NX.I18n.get('Blobstores_Delete_Mask'));
    NX.direct.coreui_Blobstore.remove(model.getId(), function(response) {
      me.getContent().getEl().unmask();
      me.getStore('Blobstore').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.success('Blobstore deleted: ' + description);
      }
    });
  },

  /**
   * @private
   * Converts file blob store to group.
   */
  promoteToGroup: function(button) {
    var me = this,
        model = me.getList().getSelectionModel().getLastSelected();
    NX.Dialogs.askConfirmation(NX.I18n.get('Blobstore_BlobstoreFeature_Confirm_Title'),
        NX.I18n.get('Blobstore_BlobstoreFeature_Confirm_Warning'),
        function() {
          NX.direct.coreui_Blobstore.promoteToGroup(model.get('name'), function(response) {
            if (Ext.isObject(response) && response.success) {
              NX.Messages.success(NX.I18n.format('Blobstore_BlobstoreFeature_Promote_Success', response.data.name));
              me.getStore('Blobstore').load();
              button.disable();
            }
          });
        });
  },

  /**
   * @private
   */
  watchEventsForNonGroups: function () {
    var me = this,
        store = me.getStore('Blobstore');

    return function() {
      var blobstoreId = me.getModelIdFromBookmark(),
          model = blobstoreId ? store.findRecord('name', blobstoreId, 0, false, true, true) : undefined;

      if (model) {
        return model.get('promotable');
      }

      return false;
    };
  },

  /**
   * @private
   * checks that blobstore is not a blobstore group and that user has permissions before activating button
   */
  bindIfUpdatableAndGroupsEnabled: function(button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':update'),
            NX.Conditions.watchEvents([
              { observable: me.getStore('Blobstore'), events: ['load']},
              { observable: Ext.History, events: ['change']}
            ], me.watchEventsForNonGroups())
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

    // check that Groups are enabled before showing the promote button
    button.mon(
        NX.Conditions.watchEvents([
          { observable: me.getStore('BlobstoreType'), events: ['load']},
          { observable: Ext.History, events: ['change']}
        ], function() {
          return me.getStore('BlobstoreType').findRecord('name', 'Group', false, true, true) != null;
        }),
        {
          satisfied: function () {
            button.show();
          },
          unsatisfied: function () {
            button.hide();
          }
        }
    );
  },

  removeGroupMemberTaskFromGroupChanged: function(groupComboBox, newVal, old) {
    var members = groupComboBox.up().query('[name=property_memberToRemove]')[0];
    var selectedGroup = groupComboBox.getStore().getById(newVal);
    var data = Ext.Array.map(selectedGroup.data.attributes.group.members, function(m) {return {name: m, id: m};});
    members.setValue(null);
    members.getStore().setData(data);
    if(!old) {
      members.reset();
    }
  },

  moveRepositoryTaskRepositoryNameChanged: function(moveRepoComboBox, newVal, old) {
    this.getStore('Repository').load({
      scope: this,
      callback: function() {
        this.getStore('Blobstore').load({
          scope: this,
          callback: function() {
            var me = this,
                repoStore = me.getStore('Repository'),
                selectedRepo = repoStore.findRecord('name', newVal);

            if (selectedRepo) {
              var blobstoreStore = me.getStore('Blobstore'),
                  oldSelection,
                  validSelection = false,
                  blobstoresCombo = moveRepoComboBox.up().query('[name=property_moveTargetBlobstore]')[0],
                  currentBlobStore = selectedRepo.data.attributes.storage.blobStoreName,
                  validBlobstores = blobstoreStore.getRange().filter(function(item) {
                    return item.data.name !== currentBlobStore;
                  }).map(function(item) {
                    return {name: item.data.name, id: item.data.name};
                  });

              // Check if selected value was valid, if not clean
              oldSelection = blobstoresCombo.getValue()
              for (var i = 0; i < validBlobstores.length; i++) {
                if (validBlobstores[i].id === oldSelection) {
                  oldSelection = blobstoresCombo.getValue()
                  validSelection = true;
                  break;
                }
              }

              blobstoresCombo.getStore().setData(validBlobstores);
              if (!old) {
                blobstoresCombo.reset();
              }
              if (validSelection) {
                blobstoresCombo.setValue(oldSelection);
              } else {
                blobstoresCombo.setValue(null);
              }
            }
          }
        });
      }
    });
  }
});

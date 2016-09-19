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
    'Blobstore',
    'BlobstoreType'
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
    { ref: 'list', selector: 'nx-coreui-blobstore-list' },
    { ref: 'settings', selector: 'nx-coreui-blobstore-feature nx-coreui-blobstore-settings' }
  ],
  icons: {
    'blobstore-default': {
      file: 'drive_network.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Repository/Blobstores',
    text: NX.I18n.get('Blobstores_Text'),
    description: NX.I18n.get('Blobstores_Description'),
    view: { xtype: 'nx-coreui-blobstore-feature' },
    iconConfig: {
      file: 'drive_network.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('nexus:blobstores:read') && NX.State.getUser();
    }
  },
  permission: 'nexus:blobstores',

  /**
   * @override
   */
  init: function() {
    var me = this;

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
        'nx-coreui-blobstore-settings-form': {
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
   * @override
   */
  bindDeleteButton: function (button) {
    var me = this;
    button.mon(
      NX.Conditions.and(
        NX.Conditions.isPermitted(this.permission + ':delete'),
        NX.Conditions.gridHasSelection('nx-coreui-blobstore-list', function(model) {
          var repositoryUseCount = model.get('repositoryUseCount');
          if (repositoryUseCount > 0) {
            me.showInfo(NX.I18n.format('Blobstore_BlobstoreFeature_Delete_Disabled_Message',
                Ext.util.Format.plural(repositoryUseCount, 'repository', 'repositories')));
          }
          else {
            me.clearInfo();
          }
          return !repositoryUseCount > 0;
        })
      ),
      {
        satisfied: button.enable,
        unsatisfied: button.disable,
        scope: button
      }
    );
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Blobstores_Create_Title'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-blobstore-add'));
  },

  /**
   * @private
   */
  loadRecipe: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getStore('BlobstoreType').load();
    }
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Blobstore.remove(model.getId(), function(response) {
      me.getStore('Blobstore').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: 'Blobstore deleted: ' + description, type: 'success' });
      }
    });
  }

});

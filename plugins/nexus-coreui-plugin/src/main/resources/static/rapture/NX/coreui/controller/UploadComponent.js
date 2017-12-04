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
 * Upload controller.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.controller.UploadComponent', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-uploadcomponentfeature nx-coreui-browse-repository-list',
    'nx-coreui-uploadcomponentfeature nx-coreui-upload-component'
  ],
  stores: [
    'RepositoryReference',
    'UploadComponentDefinition'
  ],
  models: [
    'UploadComponentDefinition',
    'RepositoryReference'
  ],

  views: [
    'upload.UploadComponentFeature',
    'upload.UploadComponent',
    'browse.BrowseRepositoryList'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-uploadcomponentfeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-uploadcomponentfeature nx-coreui-browse-repository-list'},
    {ref: 'uploadComponent', selector: 'nx-coreui-uploadcomponentfeature nx-coreui-upload-component'},
    {ref: 'successMessage', selector: '#nx-coreui-upload-success-message'}
  ],

  icons: {
    'upload': {
      file: 'upload.png',
      variants: ['x16', 'x32']
    }
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'browse',
      path: '/Upload',
      text: NX.I18n.get('FeatureGroups_Upload_Text'),
      description: NX.I18n.get('FeatureGroups_Upload_Description'),
      view: 'NX.coreui.view.upload.UploadComponentFeature',
      group: false,
      iconConfig: {
        file: 'upload.png',
        variants: ['x16', 'x32']
      },
      authenticationRequired: false,
      visible: function() {
          return NX.Permissions.check('nexus:component:add');
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      component: {
        'nx-coreui-uploadcomponentfeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
        },
        'nx-coreui-upload-component button[action=remove_upload_asset]': {
          click: me.removeUploadAsset
        },
        'nx-coreui-upload-component button[action=upload]': {
          click: me.doUpload
        },
        'nx-coreui-upload-component button[action=discard]': {
          click: me.discardUpload
        },
        'nx-coreui-upload-component button[action=add_asset]': {
          click: me.addAsset
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
   * When a list managed by this controller is clicked, route the event to the proper handler
   */
  onSelection: function(list, model) {
    this.onRepositorySelection(model);
  },

  /**
   * Load upload definition for selected repository.
   *
   * @private
   * @param {NX.coreui.model.RepositoryReference} model selected repository
   */
  onRepositorySelection: function(model) {
    var uploadComponentDefinition = this.getStore('UploadComponentDefinition').getById(model.get('format'));
    this.getUploadComponent().loadRecord(uploadComponentDefinition, model);
  },

  /**
   * @override
   */
  onNavigate: function() {
    if (this.getFeature()) {
      this.onBeforeRender();
    }
  },

  /**
   * @private
   * Load stores based on the bookmarked URL
   */
  onBeforeRender: function() {
    var me = this,
        uploadComponent = me.getUploadComponent(),
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoStore = me.getRepositoryList().getStore(),
        repoModel;

    // If the list hasn’t loaded, don't do anything
    if (!uploadComponent) {
      return;
    }

    repoStore.removeAll();

    this.getStore('UploadComponentDefinition').load(function (store, results) {
        var formats = '';
        results.getResultSet().records.forEach(function(record){
            if (formats.length > 0) {
                formats += ',';
            }
            formats += record.get('format');
        });

        repoStore.addFilter([{property: 'format', value: formats},{property: 'type', value: 'hosted'}]);
        repoStore.load(function () {
            // Load the asset upload page
            if (list_ids[1]) {
                repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
                me.onModelChanged(0, repoModel);
                me.onRepositorySelection(repoModel);
                uploadComponent.getStore().load(function () {
                    me.reselect();
                });
            }
            // Load the asset list view or repository list view
            else {
                me.reselect();
            }
        });
    });
  },

  removeUploadAsset: function(button) {
    button.up('#nx-coreui-upload-component-assets').remove(button.up());
  },

  doUpload: function(button) {
    var me = this;
    var fp = button.up('form');
    if(fp.getForm().isValid()) {
      fp.getForm().submit({
        waitMsg: NX.I18n.get('FeatureGroups_Upload_Wait_Message'),
        success: function(form, action){
          NX.Messages.add({text: NX.I18n.get('FeatureGroups_Upload_Successful'), type: 'success'});
          me.getSuccessMessage().update(NX.util.Url.asLink('#browse/search=' + encodeURIComponent('keyword="' + action.result.data + '"'),
              NX.I18n.get('FeatureGroups_Upload_Successful_Link_Text'), '_self'));
        }
      });
    }
  },

  discardUpload: function() {
    var me = this;
    me.showChild(0, true);
  },

  addAsset: function() {
    var me = this;
    me.getUploadComponent().addAssetRow();
  }
});

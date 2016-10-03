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
 * Browse controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.BrowseAssets', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n',
    'NX.coreui.controller.Assets'
  ],
  masters: [
    'nx-coreui-browseassetfeature nx-coreui-browse-repository-list',
    'nx-coreui-browseassetfeature nx-coreui-browse-asset-list'
  ],
  stores: [
    'Repository',
    'Asset'
  ],
  models: [
    'Asset',
    'RepositoryReference'
  ],

  views: [
    'browse.BrowseAssetFeature',
    'browse.BrowseAssetList',
    'browse.BrowseRepositoryList'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-browseassetfeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-browseassetfeature nx-coreui-browse-repository-list'},
    {ref: 'assetList', selector: 'nx-coreui-browseassetfeature nx-coreui-browse-asset-list'},
    {ref: 'assetContainer', selector: 'nx-coreui-browseassetfeature nx-coreui-component-assetcontainer'}
  ],

  icons: {
    'browse-asset-default': {
      file: 'page_white.png',
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
      path: '/Browse/Assets',
      text: NX.I18n.get('Browse_Assets_Title_Feature'),
      description: NX.I18n.get('Browse_Assets_Description_Feature'),
      view: 'NX.coreui.view.browse.BrowseAssetFeature',
      iconConfig: {
        file: 'page_white_stack.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.checkExistsWithPrefix('nexus:repository-view');
      },
      authenticationRequired: false
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      component: {
        'nx-coreui-browseassetfeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
        },
        'nx-coreui-browseassetfeature nx-coreui-component-assetcontainer': {
          updated: me.setAssetIcon
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
    var modelType = list.getStore().model;

    if (modelType === this.getRepositoryReferenceModel()) {
      this.onRepositorySelection(model);
    }
    else if (modelType === this.getAssetModel()) {
      this.onAssetSelection(model);
    }
  },

  /**
   * Load assets for selected repository.
   *
   * @private
   * @param {NX.coreui.model.RepositoryReference} model selected repository
   */
  onRepositorySelection: function(model) {
    var me = this,
        assetStore = me.getStore('Asset'),
        assetList = me.getAssetList(),
        filter = assetList.findPlugin('remotegridfilterbox');

    // If the list hasn’t loaded, don't do anything
    if (!assetList) {
      return;
    }

    assetStore.filters.removeAtKey('filter');
    filter.clearSearch();
    assetList.getSelectionModel().deselectAll();
    assetStore.addFilter([
      {
        id: 'repositoryName',
        property: 'repositoryName',
        value: model.get('name')
      }
    ]);
  },

  /**
   * Load asset container for selected asset
   *
   * @private
   * @param {NX.coreui.model.Asset} model selected asset
   */
  onAssetSelection: function(model) {
    this.getController('NX.coreui.controller.Assets').updateAssetContainer(null, null, null, model);
  },

  /**
   * Finds asset by looking it up on server side.
   *
   * @override
   */
  findAndSelectModel: function(index, modelId) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        store = lists[index].getStore(),
        modelType = store.model,
        repoSelection = lists[0].getSelectionModel().getSelection();

    if (modelType === me.getAssetModel() && repoSelection && repoSelection[0]) {
      NX.direct.coreui_Component.readAsset(modelId, repoSelection[0].get('name'), function(response) {
        if (Ext.isObject(response) && response.success && response.data) {
          me.selectModel(index, me.getAssetModel().create(response.data));
        }
        else {
          me.self.superclass.findAndSelectModel.call(me, index, modelId);
        }
      });
    }
  },

  /**
   * @override
   * Load all of the stores associated with this controller.
   */
  loadStores: function() {
    var me = this;
    if (me.getFeature()) {
      if (me.currentIndex === 0) {
        me.getRepositoryList().getStore().load();
      }
      if (me.currentIndex >= 1) {
        me.getAssetList().getStore().load(function () {
          me.reselect();
        });
      }
    }
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
        assetList = me.getAssetList(),
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoStore = me.getRepositoryList().getStore(),
        repoModel;

    // If the list hasn’t loaded, don't do anything
    if (!assetList) {
      return;
    }

    repoStore.load(function() {
      // Load the asset detail view
      if (list_ids[1]) {
        repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, repoModel);
        me.onRepositorySelection(repoModel);
        assetList.getStore().load(function() {
          me.reselect();
        });
      }
      // Load the asset list view or repository list view
      else {
        me.reselect();
      }
    });
  },

  /**
   * Set the appropriate breadcrumb icon.
   *
   * @private
   * @param {NX.coreui.view.component.AssetContainer} container asset container
   * @param {NX.coreui.model.Asset} assetModel selected asset
   */
  setAssetIcon: function(container, assetModel) {
    if (assetModel) {
      // Set the appropriate breadcrumb icon
      this.setItemClass(2, container.iconCls);
    }
  },

  /**
   * @override
   */
  loadView: function (index, animate, model) {
    this.callParent(arguments);
    this.loadStores();
  }

});

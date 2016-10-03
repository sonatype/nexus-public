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
 * Browse components controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.BrowseComponents', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list',
    'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list',
    'nx-coreui-browsecomponentfeature nx-coreui-component-asset-list'
  ],
  stores: [
    'Component',
    'ComponentAsset'
  ],
  models: [
    'Asset',
    'Component',
    'RepositoryReference'
  ],

  views: [
    'browse.BrowseComponentFeature',
    'browse.BrowseComponentList',
    'browse.BrowseRepositoryList'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-browsecomponentfeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list'},
    {ref: 'componentList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list'},
    {ref: 'assetList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-component-asset-list'},
    {ref: 'componentDetails', selector: 'nx-coreui-browsecomponentfeature nx-coreui-component-details'},
  ],

  icons: {
    'browse-component-default': {file: 'database.png', variants: ['x16', 'x32']},
    'browse-component': {file: 'box_front.png', variants: ['x16', 'x32']},
    'browse-component-detail': {file: 'box_front_open.png', variants: ['x16', 'x32']}
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'browse',
      path: '/Browse/Components',
      text: NX.I18n.get('Browse_Components_Title_Feature'),
      description: NX.I18n.get('Browse_Components_Description_Feature'),
      view: 'NX.coreui.view.browse.BrowseComponentFeature',
      iconConfig: {
        file: 'box_front.png',
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
      store: {
        '#Repository': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
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
    else if (modelType === this.getComponentModel()) {
      this.onComponentSelection(model);
    }
    else if (modelType === this.getAssetModel()) {
      this.onAssetSelection(model);
    }
  },

  /**
   * @private
   *
   * Load browse results for selected repository.
   *
   * @param {NX.coreui.model.Repository} model selected repository
   */
  onRepositorySelection: function(model) {
    var me = this,
        componentStore = me.getStore('Component'),
        componentList = me.getComponentList(),
        filter = componentList.findPlugin('remotegridfilterbox');

    // If the list hasn’t loaded, don't do anything
    if (!componentList) {
      return;
    }

    componentStore.filters.removeAtKey('filter');
    filter.clearSearch();
    componentList.getSelectionModel().deselectAll();
    componentStore.addFilter([
      {
        id: 'repositoryName',
        property: 'repositoryName',
        value: model.get('name')
      }
    ]);
  },

  /**
   * @private
   *
   * Show component details and load assets for selected component.
   *
   * @param {NX.coreui.model.Component} model selected component
   */
  onComponentSelection: function(model) {
    var me = this;

    me.getComponentDetails().setComponentModel(model);
    me.getAssetList().setComponentModel(model);
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
   * Finds component by looking it up on server side.
   *
   * @override
   */
  findAndSelectModel: function(index, modelId) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        store = lists[index].getStore(),
        modelType = store.model,
        repoSelection = lists[0].getSelectionModel().getSelection();

    if (modelType === me.getComponentModel() && repoSelection && repoSelection[0]) {
      NX.direct.coreui_Component.readComponent(modelId, repoSelection[0].get('name'), function(response) {
        if (Ext.isObject(response) && response.success && response.data) {
          me.selectModel(index, me.getComponentModel().create(response.data));
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
      if (me.currentIndex === 1) {
        me.getComponentList().getStore().load();
      }
      if (me.currentIndex >= 2) {
        me.getAssetList().getStore().load(function() {
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
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoStore = me.getRepositoryList().getStore(),
        repoModel,
        componentStore = me.getComponentList().getStore(),
        componentId, componentModel;

    repoStore.load(function() {
      // Load the asset detail view
      if (list_ids[2]) {
        repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, repoModel);
        me.onRepositorySelection(repoModel);
        componentStore.load(function() {
          componentId = decodeURIComponent(list_ids[1]);
          NX.direct.coreui_Component.readComponent(componentId, repoModel.get('name'), function(response) {
            if (Ext.isObject(response) && response.success && response.data) {
              componentModel = me.getComponentModel().create(response.data);
              me.onModelChanged(1, componentModel);
              me.onComponentSelection(componentModel);
              me.getAssetList().getStore().load(function() {
                me.reselect();
              });
            }
          });
        });
      }
      // Load the asset list view
      else if (list_ids[1]) {
        repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, repoModel);
        me.onRepositorySelection(repoModel);
        componentStore.load(function() {
          me.reselect();
        });
      }
      // Load the component list view or repository list view
      else {
        me.reselect();
      }
    });
  },

  /**
   * @override
   */
  loadView: function (index, animate, model) {
    this.callParent(arguments);
    this.loadStores();
  }

});

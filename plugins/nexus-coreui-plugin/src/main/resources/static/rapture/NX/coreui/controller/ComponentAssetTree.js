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
 * Component/Asset tree browser controller.
 *
 * @since 3.6
 */
Ext.define('NX.coreui.controller.ComponentAssetTree', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n',
    'NX.State'
  ],
  mixins: {
    componentUtils: 'NX.coreui.mixin.ComponentUtils'
  },
  masters: [
    'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list'
  ],
  stores: [
    'Repository',
    'ComponentAssetTree',
    'UploadComponentDefinition'
  ],
  models: [
    'RepositoryReference',
    'ComponentAssetTree',
    'Component',
    'Asset',
    'UploadComponentDefinition'
  ],

  views: [
    'browse.BrowseRepositoryList',
    'browse.ComponentAssetTreeFeature',
    'browse.ComponentAssetTree',
    'component.ComponentInfo',
    'component.ComponentAssetInfo',
    'component.ComponentFolderInfo'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-componentassettreefeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list'},
    {ref: 'componentAssetTree', selector: 'nx-coreui-componentassettreefeature nx-coreui-component-asset-tree'},
    {ref: 'componentAssetTreePanel', selector: 'nx-coreui-componentassettreefeature nx-coreui-component-asset-tree treepanel'},
    {ref: 'treeFilterBox', selector: 'nx-coreui-componentassettreefeature nx-searchbox'},
    {ref: 'advancedSearchLink', selector: 'nx-coreui-componentassettreefeature #nx-coreui-component-asset-tree-advanced-search'},
    {ref: 'uploadButton', selector: 'nx-coreui-componentassettreefeature button[action=upload]'},
    {ref: 'htmlViewLink', selector: 'nx-coreui-componentassettreefeature #nx-coreui-component-asset-tree-html-view'},
    {ref: 'componentInfo', selector: 'nx-coreui-component-componentinfo'},
    {ref: 'componentAssetInfo', selector: 'nx-coreui-component-componentassetinfo'},
    {ref: 'componentFolderInfo', selector: 'nx-coreui-component-componentfolderinfo'},
    {ref: 'deleteComponentButton', selector: 'nx-coreui-component-componentinfo button[action=deleteComponent]'},
    {ref: 'deleteAssetButton', selector: 'nx-coreui-component-componentassetinfo button[action=deleteAsset]'},
    {ref: 'deleteAssetFolderButton', selector: 'nx-coreui-component-componentassetinfo button[action=deleteFolder]'},
    {ref: 'deleteFolderButton', selector: 'nx-coreui-component-componentfolderinfo button[action=deleteFolder]'},
    {ref: 'analyzeApplicationButton', selector: 'nx-coreui-component-componentinfo button[action=analyzeApplication]'},
    {ref: 'analyzeApplicationWindow', selector: 'nx-coreui-component-analyze-window'},
    {ref: 'rootContainer', selector: 'nx-main'},
    {ref: 'treeWarning', selector: 'nx-coreui-componentassettreefeature nx-coreui-component-asset-tree #warning'}
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
      path: '/Browse',
      text: NX.I18n.get('FeatureGroups_Browse_Text'),
      description: NX.I18n.get('FeatureGroups_Browse_Description'),
      view: 'NX.coreui.view.browse.ComponentAssetTreeFeature',
      iconCls: 'x-fa fa-database',
      authenticationRequired: false
    };

    me.callParent();

    me.listen({
      store: {
        '#ComponentAssetTree': {
          beforeload: me.showTreeMask,
          load: me.browseNodesLoaded
        }
      },
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        },
        '#State': {
          changed: me.stateChanged,
          userchanged: me.loadStores
        }
      },
      component: {
        'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
        },
        'nx-coreui-componentassettreefeature nx-drilldown-item > container': {
          beforedeactivate: me.onBeforeDeactivate
        },
        'nx-coreui-componentassettreefeature treepanel': {
          select: me.selectNode,
          itemkeydown: me.itemKeyDown
        },
        'nx-coreui-componentassettreefeature nx-searchbox': {
          search: me.onFilterChanged,
          searchcleared: me.onFilterChanged
        },
        'nx-coreui-component-componentinfo button[action=deleteComponent]': {
          click: me.deleteComponent
        },
        'nx-coreui-component-componentinfo button[action=analyzeApplication]': {
          click: me.mixins.componentUtils.openAnalyzeApplicationWindow
        },
        'nx-coreui-component-componentassetinfo button[action=deleteAsset]': {
          click: me.deleteAsset
        },
        'nx-coreui-component-componentassetinfo button[action=deleteFolder]': {
          click: me.deleteAssetFolder
        },
        'nx-coreui-component-componentfolderinfo button[action=deleteFolder]': {
          click: me.deleteFolder
        },
        'nx-coreui-component-analyze-window button[action=analyze]': {
          click: me.analyzeAsset
        },
        'nx-coreui-component-analyze-window combobox[name="asset"]': {
          select: me.selectedApplicationChanged
        },
        'nx-coreui-componentassettreefeature button[action=upload]': {
          click: me.onClickUploadButton
        },
        'nx-coreui-componentassettreefeature #nx-coreui-component-asset-tree-html-view': {
          render: function () { me.updateHtmlLink(); }
        }
      }
    });

    me.getApplication().getIconController().addIcons({
      'tree-folder': {
        file: 'folder.png',
        variants: ['x16']
      },
      'tree-component': {
        file: 'box_front.png',
        variants: ['x16']
      },
      'tree-asset': {
        file: 'page_white_stack.png',
        variants: ['x16']
      },
      'tree-asset-folder': {
        file: 'folder_page_white.png',
        variants: ['x16', 'x32']
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
  },

  /**
   * @public
   * @param {NX.Bookmark} bookmark to navigate to
   */
  navigateTo: function (bookmark) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        list_ids = bookmark.getSegments().slice(1),
        modelId, store;

    // Don’t navigate if the feature view hasn’t loaded
    if (!me.getFeature || !me.getFeature()) {
      return;
    }

    if (lists.length && list_ids.length) {
      //<if debug>
      me.logDebug('Navigate to: ' + bookmark.getSegments().join(':'));
      //</if>

      modelId = decodeURIComponent(list_ids.shift());
      store = lists[0].getStore();

      if (store.isLoading() || !store.isLoaded()) {
        // The store hasn’t yet loaded, load it when ready
        me.mon(store, 'load', function() {
          me.selectModelById(0, modelId);
          me.mun(store, 'load');
        });
      } else {
        me.selectModelById(0, modelId);
      }
    } else {
      me.loadView(0);
    }
  },

  /**
   * @private
   * Handle setting up the tree store with proper repo name parameter and reset the tree for reloading
   */
  onRepositorySelection: function(model) {
    var me = this, componentAssetTreeStore = me.getStore('ComponentAssetTree');

    if (!me.selectedRepository || me.selectedRepository.id !== model.id) {
      //reset the filter
      componentAssetTreeStore.proxy.setExtraParam('filter');
      componentAssetTreeStore.proxy.setExtraParam('repositoryName', model.get('name'));
    }

    // Update HTML View link
    me.updateHtmlLink(model);
    me.updateUploadButton(model);

    me.reloadNodes();

    me.expandTree();

    me.selectedRepository = model;

    me.updateWarningMessage(model.get('name'));
  },

  expandTree: function() {
    var me = this,
        treePanel = me.getComponentAssetTreePanel(),
        segments = window.location.hash.split(':'),
        hasPath = segments && segments.length === 3,
        path;

    if (treePanel.getStore().isLoading()) {
      treePanel.getStore().on({
        load: me.expandTree,
        scope: me,
        single: true
      });
    }
    else if (hasPath) {
      path = decodeURIComponent(segments[2]);
      treePanel.selectPath('/Root/' + path, 'text', '/', function (successful, lastNode) {
        if (!successful) {
          NX.Messages.error(NX.I18n.get('Component_Asset_Tree_Expand_Failure'));
        }
        else {
          lastNode.expand();
        }
      });
    }
  },

  showTreeMask: function() {
    var me = this,
      treePanel = me.getComponentAssetTreePanel(),
      treeStore = me.getStore('ComponentAssetTree'),
      maskTask = new Ext.util.DelayedTask(function() {
        if (treeStore.isLoading()) {
          // Show the loading mask
          treePanel.setLoading(true);

          // Wait for the store to load before unmasking the tree
          treeStore.on({
            single: true,
            load: function() {
              treePanel.setLoading(false);
            }
          });
        }
      });

    // Wait 0.1 seconds before showing the loading mask
    maskTask.delay(100);
  },

  browseNodesLoaded: function(store, node, records) {
    var message = { type: 'warning', text: NX.I18n.get('Component_Asset_Tree_Results_Warning')};
    if (records && records.length === NX.State.getValue('browseTreeMaxNodes') && !NX.Messages.messageExists(message)) {
      NX.Messages.add(message);
    }
  },

  /**
   * @private
   * Handle when the filter changes, so the tree will be reloaded and future node requests will contain the filter
   * parameter
   */
  onFilterChanged: function(filterBox, value) {
    var me = this,
        componentAssetTreeStore = me.getStore('ComponentAssetTree'),
        emptyText = me.getComponentAssetTreePanel().view.emptyText,
        advancedSearchLink = me.getAdvancedSearchLink(),
        treePanel = me.getComponentAssetTreePanel(),
        url;

    if (me.selectedRepository) {
      // repository selected, filter the tree
      if (value) {
        url = 'browse/search=' + encodeURIComponent('keyword=' + value);
        emptyText = emptyText.replace(/>.*</, '>' + NX.I18n.get('Component_Asset_Tree_Filtered_EmptyText_View') + '<');
        emptyText = emptyText.replace('browse/search', url);

        advancedSearchLink.setText(advancedSearchLink.initialConfig.html.replace('browse/search', url), false);
      } else {
        emptyText = emptyText.replace(/>.*</, '>' + NX.I18n.get('Component_Asset_Tree_EmptyText_View') + '<');

        advancedSearchLink.setText(advancedSearchLink.initialConfig.html, false);
      }
      treePanel.view.emptyText = emptyText;

      componentAssetTreeStore.proxy.setExtraParam('filter', value);

      me.reloadNodes();
    }
  },

  /**
   * @override
   * Load all of the stores associated with this controller.
   */
  loadStores: function() {
    var me = this;
    if (me.getFeature()) {
      if (me.atRepositoryPage()) {
        me.getRepositoryList().getStore().load();
      }
      else if (me.atTreePage()) {
        me.onRepositorySelection(me.getCurrentRepository());
      }
    }
  },

  stateChanged: function() {
    var currentRepository = this.getCurrentRepository(),
        repositoryName = currentRepository ? currentRepository.get('name') : null;

    this.updateUploadButton();
    this.updateWarningMessage(repositoryName);
  },

  updateWarningMessage: function(repositoryName) {
    var warning = this.getTreeWarning(),
        rebuildingRepositories = NX.State.getValue('rebuildingRepositories') || [];

    if (!warning) {
      return;
    }

    if (rebuildingRepositories.indexOf(repositoryName) !== -1) {
      warning.setTitle(NX.I18n.format('ComponentDetails_Rebuild_Warning'));
      warning.show();
    }
    else {
      warning.hide();
    }
  },

  bookmarkNode: function(node) {
    const ROOT_LENGTH = '/Root/'.length;
    var baseUrl = '#browse/browse:' + encodeURIComponent(this.getCurrentRepository().get('name')),
        encodedId = node ? encodeURIComponent(node.getPath('text').substring(ROOT_LENGTH)) : null;

    //if we don't have the replaceState method, don't bother doing anything
    if (window.history.replaceState && window.location.hash.indexOf(baseUrl) === 0) {
      window.history.replaceState({}, null, baseUrl + (encodedId ? (':' + encodedId) : ''));
    }
  },

  selectNode: function(view, node) {
    var me = this,
        componentInfoPanel,
        componentInfoPanelTitleText,
        assetInfoPanel,
        isFolder = !node.get('leaf');

    me.removeSideContent();
    me.bookmarkNode(node);

    if ('component' === node.get('type')) {
      componentInfoPanelTitleText = me.buildPathString(node);
      componentInfoPanel = me.getComponentInfo();
      componentInfoPanel.setTitle(componentInfoPanelTitleText);
      componentInfoPanel.setIconCls(me.mixins.componentUtils.getIconForAsset(node).get('cls'));
      componentInfoPanel.getDependencySnippetPanel().hide();
      componentInfoPanel.show();
      componentInfoPanel.mask(NX.I18n.get('ComponentDetails_Loading_Mask'));

      NX.direct.coreui_Component.readComponent(node.get('componentId'), me.getCurrentRepository().get('name'), function(response) {
        var componentModel;
        me.maybeUnmask(componentInfoPanel);
        if (me.isPanelVisible(componentInfoPanel) && me.isResponseSuccessful(response)) {
          componentModel = me.getComponentModel().create(response.data);
          me.setComponentModel(componentModel);
         }
      });
    }
    else if ('asset' === node.get('type')) {
      assetInfoPanel = me.getComponentAssetInfo();
      assetInfoPanel.setIconCls(me.mixins.componentUtils.getIconForAsset(node).get('cls'));
      assetInfoPanel.getDependencySnippetPanel().hide();
      assetInfoPanel.show();
      assetInfoPanel.mask(NX.I18n.get('ComponentDetails_Loading_Mask'));

      NX.direct.coreui_Component.readAsset(node.get('assetId'), me.getCurrentRepository().get('name'), function(response) {
        if (me.isPanelVisible(assetInfoPanel) && me.isResponseSuccessful(response)) {
          me.setInfoPanelModel(assetInfoPanel, me.getAssetModel().create(response.data), isFolder);
        }
        else {
          me.maybeUnmask(assetInfoPanel);
        }
      });
    }
    else if ('folder' === node.get('type')) {
      var folderInfoPanel = me.getComponentFolderInfo();
      folderInfoPanel.setTitle(Ext.util.Format.htmlEncode(decodeURI(node.getId())));
      folderInfoPanel.setIconCls(me.mixins.componentUtils.getIconForAsset(node).get('cls'));
      folderInfoPanel.show();

      folderInfoPanel.mask(NX.I18n.get('ComponentDetails_Loading_Mask'));
      me.getDeleteFolderButton().show();
      me.getDeleteFolderButton().enable();
      folderInfoPanel.setModel({repositoryName: me.getCurrentRepository().get('name'), folderName: node.get('text'), path: node.get('id')});
      me.updateDeleteFolderButton(me.getDeleteFolderButton(), me.getCurrentRepository(), node.get('id'));
      me.maybeUnmask(folderInfoPanel);
    }
  },

  setComponentModel: function(componentModel) {
    var componentInfoPanel = this.getComponentInfo();

    componentInfoPanel.setModel(componentModel);
    this.updateDeleteComponentButton(this.getCurrentRepository(), componentModel);
    this.updateAnalyzeButton(componentModel);
    this.setDependencySnippets(componentInfoPanel.getDependencySnippetPanel(), componentModel);
  },

  setInfoPanelModel: function(assetInfoPanel, asset, isFolder) {
    var me = this,
        componentModel;

    if (asset.get('componentId')) {
      NX.direct.coreui_Component.readComponent(asset.get('componentId'), me.getCurrentRepository().get('name'), function (response) {
        me.maybeUnmask(assetInfoPanel);
        if (me.isPanelVisible(assetInfoPanel) && me.isResponseSuccessful(response)) {
          componentModel = me.getComponentModel().create(response.data);
          assetInfoPanel.setModel(asset, componentModel);
          me.updateDeleteAssetButton(me.getCurrentRepository(), asset, isFolder);
          me.setDependencySnippets(assetInfoPanel.getDependencySnippetPanel(), componentModel, asset);
        }
      });
    }
    else {
      me.maybeUnmask(assetInfoPanel);
      if (me.isPanelVisible(assetInfoPanel)) {
        componentModel = me.getComponentModel().create({});
        assetInfoPanel.setModel(asset, componentModel);
        me.updateDeleteAssetButton(me.getCurrentRepository(), asset, isFolder);
        me.setDependencySnippets(assetInfoPanel.getDependencySnippetPanel(), componentModel, asset);
      }
    }
  },

  setDependencySnippets: function(dependencySnippetPanel, componentModel, assetModel) {
    var format, dependencySnippets;

    if (componentModel) {
      format = componentModel.get('format');
      dependencySnippets = NX.getApplication().getDependencySnippetController()
          .getDependencySnippets(format, componentModel, assetModel);

      dependencySnippetPanel.setDependencySnippets(format, dependencySnippets);

      if (dependencySnippets && dependencySnippets.length > 0) {
        dependencySnippetPanel.show();
      }
    }
  },

  isPanelVisible : function(panel) {
    return panel && panel.isVisible();
  },

  isResponseSuccessful : function(response) {
    return Ext.isObject(response) && response.success && response.data;
  },

  maybeUnmask : function(panel) {
    var me = this;
    if (me.isPanelVisible(panel)) {
      panel.getEl().unmask();
    }
  },

  itemKeyDown: function(view, model, el, index, event) {
    var key = event.getKey();
    if (key === event.ENTER || key === event.RETURN || key === event.SPACE || key === event.RIGHT) {
      view.getSelectionModel().select(model);
    }
  },

  removeSideContent: function() {
    var me = this,
        componentInfo = me.getComponentInfo(),
        componentAssetInfo = me.getComponentAssetInfo(),
        componentFolderInfo = me.getComponentFolderInfo();

    componentInfo.hide();
    componentAssetInfo.hide();
    componentFolderInfo.hide();
  },

  buildPathString: function(node) {
    var path = '';
    //node.parentNode check will skip the trees root node (labeld Root and hidden)
    while (node != null && node.parentNode != null) {
      path = node.get('text') + '/' + path;
      node = node.parentNode;
    }

    return path;
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
   */
  atRepositoryPage: function() {
    var me = this;
    return me.currentIndex === 0;
  },

  /**
   * @private
   */
  atTreePage: function() {
    var me = this;
    return me.currentIndex === 1;
  },

  /**
   * @private
   * Load stores based on the bookmarked URL
   */
  onBeforeRender: function() {
    var me = this,
        repoList = me.getRepositoryList(),
        repoModel;

    if (repoList) {
      repoList.getStore().on({
        load: function() {
          repoModel = me.getCurrentRepository();

          if (repoModel) {
            //0 references the first list in the Drilldown parent (the repository list)
            me.onModelChanged(0, repoModel);
            me.onRepositorySelection(repoModel);
          }

          me.reselect();
        },
        single: true
      });

      // In theory we should be able to just pass in the above load listener here, but for some reason it isn't being called
      repoList.getStore().load();
    }
  },

  /**
   * @private
   * Clears the filter box before the view is changed
   */
  onBeforeDeactivate: function(oldCard) {
    var filterBox = oldCard.down('nx-searchbox');

    filterBox && filterBox.clearSearch && filterBox.clearSearch();
  },

  /**
   * @private
   * Opens the Upload UI for current repository
   */
  onClickUploadButton: function() {
    var me = this,
        repository = me.getCurrentRepository(),
        uploadUrl = '#browse/upload:' + encodeURIComponent(repository.get('name'));

    window.open(uploadUrl, '_self');
  },

  /**
   * @private
   * Updates the href for the HTML Tree view
   */
  updateHtmlLink: function(repository) {
    var me = this,
        htmlViewLink = me.getHtmlViewLink(),
        currentRepository = repository || me.getCurrentRepository(),
        repositoryName = currentRepository && currentRepository.get('name');

    if (htmlViewLink && htmlViewLink.el && repositoryName) {
      htmlViewLink.el.select('a').set({
        href: NX.util.Url.urlOf('/service/rest/repository/browse/'+ encodeURIComponent(repositoryName))
      });
    }
  },

  /**
   * @private
   * Updates the visibility of the upload button.
   */
  updateUploadButton: function(repo) {
    var me = this,
        uploadButton = me.getUploadButton(),
        store = me.getStore('UploadComponentDefinition'),
        repository = repo || me.getCurrentRepository();

    if (uploadButton && repository) {
      if (NX.State.getValue('upload') &&
          NX.Permissions.check('nexus:component:add') &&
          repository.getData().type === 'hosted' &&
          repository.getData().versionPolicy !== 'SNAPSHOT') {
        store.load(function (store, results) {
          var isSupported = Ext.Array.some(results.getRecords(), function (item) {
            return item.getData().format === repository.getData().format;
          });
          uploadButton.setVisible(isSupported);
        });
      }
      else {
        uploadButton.setVisible(false);
      }
    }
  },

  /**
   * @pivate
   * Retrieve the current repository model as defined in the bookmark
   */
  getCurrentRepository: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoList = me.getRepositoryList();

    if (repoList && list_ids && list_ids.length > 0) {
      return repoList.getStore().getById(decodeURIComponent(list_ids[0]));
    }

    return null;
  },

  /**
   * Remove selected component.
   *
   * @private
   */
  deleteComponent: function() {
    var me = this,
        treePanel = me.getComponentAssetTreePanel(),
        componentInfo = me.getComponentInfo(),
        selectedNode = treePanel.getSelectionModel().getSelection()[0],
        componentModel, componentId;

    if (componentInfo) {
      componentModel = componentInfo.componentModel;
      componentId = componentModel.get('name') + '/' + componentModel.get('version');
      NX.Dialogs.askConfirmation(NX.I18n.get('ComponentDetails_Delete_Title'), Ext.htmlEncode(NX.I18n.format('ComponentDetails_Delete_Body', componentId)), function() {
        NX.direct.coreui_Component.deleteComponent(JSON.stringify(componentModel.getData()), function(response) {
          if (Ext.isObject(response) && response.success && Ext.isArray(response.data)) {
            me.removeNodeFromTree(selectedNode);
            Ext.each(response.data, function (nodeId) {
              var node = treePanel.getStore().findNode('id', nodeId);
              if (node) {
                me.removeNodeFromTree(node);
              }
            });
            NX.Messages.add({text: NX.I18n.format('ComponentDetails_Delete_Success', componentId), type: 'success'});
          }
        });
      });
    }
  },

  /**
   * @private
   * Remove selected asset.
   */
  deleteAsset: function () {
    var me = this,
        treePanel = me.getComponentAssetTreePanel(),
        componentAssetInfo = me.getComponentAssetInfo();

    if (componentAssetInfo) {
      var asset = componentAssetInfo.assetModel;
      NX.Dialogs.askConfirmation(NX.I18n.get('AssetInfo_Delete_Title'), Ext.htmlEncode(asset.get('name')), function () {
        NX.direct.coreui_Component.deleteAsset(asset.getId(), asset.get('repositoryName'), function (response) {
          if (Ext.isObject(response) && response.success) {
            var selectedRecord = treePanel.getSelectionModel().getSelection()[0];
            if (selectedRecord.get('leaf')) {
              me.removeSideContent();
              me.removeNodeFromTree(selectedRecord);
            }
            else {
              selectedRecord.set('type', 'folder');
              selectedRecord.set('iconCls', me.mixins.componentUtils.getIconForAsset(selectedRecord).get('cls'));
            }
            NX.Messages.add({text: NX.I18n.format('AssetInfo_Delete_Success', asset.get('name')), type: 'success'});
          }
        });
      });
    }
  },

  /**
   * @private
   * Remove selected Folder.
   */
  deleteFolder: function() {
    var me = this,
        componentFolderInfo = me.getComponentFolderInfo();

    if (componentFolderInfo) {
      var model = componentFolderInfo.folderModel;
      NX.Dialogs.askConfirmation(
          NX.I18n.get('FolderInfo_Delete_Title'),
          NX.I18n.format('FolderInfo_Delete_Text', Ext.htmlEncode(model.folderName)),
          function() {
            NX.direct.coreui_Component.deleteFolder(model.path, model.repositoryName,
                function(response) {
                  if (Ext.isObject(response) && response.success) {
                    NX.Messages.add({text: NX.I18n.format('FolderInfo_Delete_Success'), type: 'success'});
                  }
                });
          });
    }
  },

  /**
   * @private
   * Remove selected Folder.
   */
  deleteAssetFolder: function() {
    var componentAssetInfo = this.getComponentAssetInfo();

    if (componentAssetInfo) {
      var asset = componentAssetInfo.assetModel;
      NX.Dialogs.askConfirmation(
          NX.I18n.get('FolderInfo_Delete_Title'),
          NX.I18n.format('FolderInfo_Delete_Text', Ext.htmlEncode(asset.get('name'))),
          function() {
            NX.direct.coreui_Component.deleteFolder(asset.get('name'), asset.get('repositoryName'),
                function(response) {
                  if (Ext.isObject(response) && response.success) {
                    NX.Messages.add({text: NX.I18n.format('FolderInfo_Delete_Success'), type: 'success'});
                  }
                });
          });
    }
  },

  fetchComponentModelFromView: function() {
    return this.getComponentInfo().componentModel;
  },

  /**
   * Analyze a component using the AHC service
   *
   * @private
   */
  analyzeAsset: function(button) {
    var me = this,
        componentInfo = me.getComponentInfo(),
        win,
        form,
        formValues,
        repositoryName,
        assetId;

    if (componentInfo) {
      win = button.up('window');
      form = button.up('form');
      formValues = form.getForm().getValues();
      repositoryName = componentInfo.componentModel.get('repositoryName');
      assetId = form.down('combo[name="asset"]').getValue();
      NX.direct.ahc_Component.analyzeAsset(repositoryName, assetId, formValues.emailAddress, formValues.password,
          formValues.proprietaryPackages, formValues.reportLabel, function (response) {
            if (Ext.isObject(response) && response.success) {
              win.close();
              NX.Messages.add({text: NX.I18n.get('ComponentDetails_Analyze_Success'), type: 'success'});
            }
          });
    }
  },

  /**
   * When app changes, update the reportName as well
   */
  selectedApplicationChanged: function(combo) {
    var me = this,
        componentInfo = me.getComponentInfo(),
        labelField;

    if (componentInfo) {
      labelField = me.getAnalyzeApplicationWindow().down('textfield[name="reportLabel"]');
      if (!labelField.isDirty()) {
        //I am setting the original value so it won't be marked dirty unless user touches it
        labelField.originalValue = combo.getRawValue();
        labelField.setValue(combo.getRawValue());
      }
    }
  },

  removeNodeFromTree: function(node) {
    var me = this,
        panel = me.getComponentAssetTreePanel(),
        selectedNode;

    while(node) {
      var parentNode = node.parentNode;
      node.remove();
      node = parentNode;
      //if after removing a node there are still children, thats the one we want to select
      if (node && node.hasChildNodes()) {
        panel.getSelectionModel().select(node);
        selectedNode = true;
        break;
      }
    }

    //we found no node to select, so everything must be gone, thus make sure to update the address bar
    if (!selectedNode) {
      me.bookmarkNode(null);
    }
  },

  loadView: function (index, model) {
    var me = this,
      lists = Ext.ComponentQuery.query('nx-drilldown-master'),
      hasPath = NX.Bookmarks.getBookmark().getSegments().length > 2;

    // Don’t load the view if the feature is not ready
    if (!me.getFeature()) {
      return;
    }

    // Model specified, select it in the previous list
    if (model && index > 0) {
      lists[index - 1].fireEvent('selection', lists[index - 1], model);
      me.onModelChanged(index - 1, model);
    }

    // Set all child bookmarks
    for (var i = 0; i <= index; ++i) {
      me.setItemBookmark(i, NX.Bookmarks.fromSegments(NX.Bookmarks.getBookmark().getSegments().slice(0, i + 1)), me);
    }

    // Show the next view in line
    me.showChild(index);
    if (!hasPath) {
      me.bookmark(index, model);
    }
  },

  reloadNodes: function() {
    var me = this, componentAssetTreeStore = me.getStore('ComponentAssetTree');

    //remove the side panel which if there, is showing old data
    me.removeSideContent();

    if (!componentAssetTreeStore.isLoading()) {
      //this will trigger the tree to be reloaded
      componentAssetTreeStore.setRootNode({
          expanded: true
      });
    }
  }

});

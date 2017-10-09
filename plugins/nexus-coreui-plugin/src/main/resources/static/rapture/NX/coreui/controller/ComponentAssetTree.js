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
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n',
    'NX.State'
  ],
  masters: [
    'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list'
  ],
  stores: [
    'Repository',
    'ComponentAssetTree'
  ],
  models: [
    'RepositoryReference',
    'ComponentAssetTree',
    'Component',
    'Asset'
  ],

  views: [
    'browse.BrowseRepositoryList',
    'browse.ComponentAssetTreeFeature',
    'browse.ComponentAssetTree',
    'component.ComponentInfo',
    'component.ComponentAssetInfo'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-componentassettreefeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list'},
    {ref: 'componentAssetTree', selector: 'nx-coreui-componentassettreefeature nx-coreui-component-asset-tree'},
    {ref: 'componentAssetTreePanel', selector: 'nx-coreui-componentassettreefeature treepanel'},
    {ref: 'treeFilterBox', selector: 'nx-coreui-componentassettreefeature nx-searchbox'},
    {ref: 'advancedSearchLink', selector: 'nx-coreui-componentassettreefeature #nx-coreui-component-asset-tree-advanced-search'},
    {ref: 'htmlViewLink', selector: 'nx-coreui-componentassettreefeature #nx-coreui-component-asset-tree-html-view'},
    {ref: 'componentInfo', selector: 'nx-coreui-component-componentinfo'},
    {ref: 'componentAssetInfo', selector: 'nx-coreui-component-componentassetinfo'},
    {ref: 'deleteComponentButton', selector: 'nx-coreui-component-componentinfo button[action=deleteComponent]'},
    {ref: 'deleteAssetButton', selector: 'nx-coreui-component-componentassetinfo button[action=deleteAsset]'},
    {ref: 'analyzeApplicationWindow', selector: 'nx-coreui-component-analyze-window'},
    {ref: 'rootContainer', selector: 'nx-main'}
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
      iconConfig: {
        file: 'database_share.png',
        variants: ['x16', 'x32']
      },
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
        }
      },
      component: {
        'nx-coreui-componentassettreefeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
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
          click: me.openAnalyzeApplicationWindow
        },
        'nx-coreui-component-componentassetinfo button[action=deleteAsset]': {
          click: me.deleteAsset
        },
        'nx-coreui-component-analyze-window button[action=analyze]': {
          click: me.analyzeAsset
        },
        'nx-coreui-component-analyze-window combobox[name="asset"]': {
          select: me.selectedApplicationChanged
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

      if (store.isLoading()) {
        // The store hasn’t yet loaded, load it when ready
        me.mon(store, 'load', function() {
          me.selectModelById(0, modelId);
          me.mun(store, 'load');
        });
      } else {
        me.selectModelById(0, modelId);
      }
    } else {
      me.loadView(0, false);
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

    me.reloadNodes();

    me.expandTree();

    me.selectedRepository = model;
  },

  expandTree: function() {
    var me = this,
        treePanel = me.getComponentAssetTreePanel(),
        segments = window.location.hash.split(':');

    if (segments && segments.length === 3) {
      // Extract the filter object from the URI and select it in the tree
      treePanel.selectPath('/Root/' + decodeURIComponent(segments.pop()), 'text', '/', function (successful) {
        if (!successful) {
          NX.Messages.error(NX.I18n.get('Component_Asset_Tree_Expand_Failure'));
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
    if (records.length === NX.State.getValue('browseTreeMaxNodes') && !NX.Messages.messageExists(message)) {
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

  bookmarkNode: function(nodeId) {
    var baseUrl = '#browse/browse:' + encodeURIComponent(this.getCurrentRepository().get('name')),
        encodedId = nodeId ? encodeURIComponent(nodeId) : null;

    //if we don't have the replaceState method, don't bother doing anything
    if (window.history.replaceState && window.location.hash.indexOf(baseUrl) === 0) {
      window.history.replaceState({}, null, baseUrl + (encodedId ? (':' + encodedId) : ''));
    }
  },

  selectNode: function(view, model) {
    var me = this,
        containerView = me.getComponentAssetTree(),
        componentInfoPanel,
        assetInfoPanel,
        currentRepository;

    me.removeSideContent();
    me.bookmarkNode(model.get('id'));

    if ('component' === model.get('type')) {
      componentInfoPanel = containerView.add(me.getComponentComponentInfoView().create({
        title: me.buildPathString(model),
        iconCls: 'nx-icon-tree-component-x16',
        flex: 2
      }));
      componentInfoPanel.getEl() && componentInfoPanel.getEl().mask(NX.I18n.get('ComponentDetails_Loading_Mask'));
      currentRepository = me.getCurrentRepository();
      if (currentRepository && currentRepository.get('type') !== 'group') {
        me.getDeleteComponentButton().show();
      }
      NX.direct.coreui_Component.readComponent(model.get('componentId'), me.getCurrentRepository().get('name'), function(response) {
        me.maybeUnmask(componentInfoPanel);
        if (me.isPanelVisible(componentInfoPanel) && me.isResponseSuccessful(response)) {
          componentInfoPanel.setModel(me.getComponentModel().create(response.data));
         }
      });
    }
    else if ('asset' === model.get('type')) {
      assetInfoPanel = containerView.add(me.getComponentComponentAssetInfoView().create({
        flex: 2,
        iconCls: 'nx-icon-tree-asset-x16'
      }));
      assetInfoPanel.getEl() && assetInfoPanel.getEl().mask(NX.I18n.get('ComponentDetails_Loading_Mask'));
      currentRepository = me.getCurrentRepository();
      if (currentRepository && currentRepository.get('type') !== 'group') {
        me.getDeleteAssetButton().show();
      }

      NX.direct.coreui_Component.readAsset(model.get('assetId'), me.getCurrentRepository().get('name'), function(response) {
        if (me.isPanelVisible(assetInfoPanel) && me.isResponseSuccessful(response)) {
          me.setInfoPanelModel(assetInfoPanel, me.getAssetModel().create(response.data));
        }
        else {
          me.maybeUnmask(assetInfoPanel);
        }
      });
    }
  },

  setInfoPanelModel: function(assetInfoPanel, asset) {
    var me = this;
    if (asset.get('componentId')) {
      NX.direct.coreui_Component.readComponent(asset.get('componentId'), me.getCurrentRepository().get('name'), function (response) {
        me.maybeUnmask(assetInfoPanel);
        if (me.isPanelVisible(assetInfoPanel) && me.isResponseSuccessful(response)) {
          assetInfoPanel.setModel(asset, me.getComponentModel().create(response.data));
        }
      });
    }
    else {
      me.maybeUnmask(assetInfoPanel);
      if (me.isPanelVisible(assetInfoPanel)) {
        assetInfoPanel.setModel(asset, me.getComponentModel().create({}));
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
        containerView = me.getComponentAssetTree();

    while (containerView.items.getCount() > 1) {
      containerView.remove(containerView.items.getAt(1));
    }
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
      repoList.getStore().load(function() {
        repoModel = me.getCurrentRepository();

        if (repoModel) {
          //0 references the first list in the Drilldown parent (the repository list)
          me.onModelChanged(0, repoModel);
          me.onRepositorySelection(repoModel);
        }

        me.reselect();
      });
    }
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
        href: NX.util.Url.urlOf('/service/siesta/repository/browse/'+ encodeURIComponent(repositoryName))
      });
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
        componentInfo = me.getComponentInfo(),
        treePanel = me.getComponentAssetTreePanel(),
        componentModel, componentId, repositoryName;

    if (componentInfo) {
      componentModel = componentInfo.componentModel;
      componentId = componentModel.get('name') + '/' + componentModel.get('version');
      repositoryName = componentModel.get('repositoryName');
      NX.Dialogs.askConfirmation(NX.I18n.get('ComponentDetails_Delete_Title'), componentId, function() {
        NX.direct.coreui_Component.deleteComponent(componentModel.getId(), repositoryName, function(response) {
          if (Ext.isObject(response) && response.success) {
            me.removeSideContent();
            me.removeNodeFromTree(treePanel.getSelectionModel().getSelection()[0]);
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
        componentAssetInfo = me.getComponentAssetInfo(),
        treePanel = me.getComponentAssetTreePanel();

    if (componentAssetInfo) {
      var asset = componentAssetInfo.assetModel;
      NX.Dialogs.askConfirmation(NX.I18n.get('AssetInfo_Delete_Title'), asset.get('name'), function () {
        NX.direct.coreui_Component.deleteAsset(asset.getId(), asset.get('repositoryName'), function (response) {
          if (Ext.isObject(response) && response.success) {
            me.removeSideContent();
            me.removeNodeFromTree(treePanel.getSelectionModel().getSelection()[0]);
            NX.Messages.add({text: NX.I18n.format('AssetInfo_Delete_Success', asset.get('name')), type: 'success'});
          }
        });
      });
    }
  },

  /**
   * Open the analyze application form window
   *
   * @private
   */
  openAnalyzeApplicationWindow: function() {
    var me = this,
        componentInfo = me.getComponentInfo(),
        componentId = componentInfo.componentModel.getId(),
        repositoryName = componentInfo.componentModel.get('repositoryName');

    function doOpenAnalyzeWindow(response) {
      var widget = Ext.widget('nx-coreui-component-analyze-window');
      var form = widget.down('form');
      form.getForm().setValues(response.data);
      //I am setting the original value so it won't be marked dirty unless user touches it
      form.down('textfield[name="reportLabel"]').originalValue = response.data.reportLabel;

      var assetKeys = response.data.assetMap ? Ext.Object.getKeys(response.data.assetMap) : [];

      if (assetKeys.length < 1) {
        widget.close();
        NX.Dialogs.showError(NX.I18n.get('AnalyzeApplicationWindow_No_Assets_Error_Title'),
            NX.I18n.get('AnalyzeApplicationWindow_No_Assets_Error_Message'));
      }
      else if (assetKeys.length === 1) {
        widget.down('combo[name="asset"]').setValue(response.data.selectedAsset);
      }
      else {
        var data = [];
        for (var i = 0; i < assetKeys.length; i++) {
          data.push([assetKeys[i], response.data.assetMap[assetKeys[i]]]);
        }
        var combo = widget.down('combo[name="asset"]');
        combo.getStore().loadData(data, false);
        combo.setValue(response.data.selectedAsset);
        combo.show();
      }
    }

    me.getRootContainer().getEl().mask(NX.I18n.get('AnalyzeApplicationWindow_Loading_Mask'));
    NX.direct.ahc_Component.getPredefinedValues(componentId, repositoryName, function(response) {
      me.getRootContainer().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        if (response.data.tosAccepted) {
          doOpenAnalyzeWindow(response);
        }
        else {
          Ext.widget('nx-coreui-healthcheck-eula', {
            acceptFn: function() {
              NX.direct.ahc_Component.acceptTermsOfService(function() {
                doOpenAnalyzeWindow(response);
              });
            }
          });
        }
      }
    });
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

  loadView: function (index, animate, model) {
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
    me.showChild(index, animate);
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

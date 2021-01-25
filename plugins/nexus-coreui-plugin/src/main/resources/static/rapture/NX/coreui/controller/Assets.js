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
 * Assets controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Assets', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Bookmarks',
    'NX.coreui.util.Maven2ComponentActionHandler',
    'NX.Dialogs',
    'NX.I18n',
    'Ext.util.Format'
  ],

  mixins: {
    componentUtils: 'NX.coreui.mixin.ComponentUtils'
  },

  views: [
    'component.AssetContainer',
    'component.AssetInfo',
    'component.AssetAttributes',
    'component.AssetList',
    'component.ComponentDetails',
    'component.AnalyzeApplicationWindow'
  ],

  refs: [
    {ref: 'assetContainer', selector: 'nx-coreui-component-assetcontainer'},
    {ref: 'assetList', selector: 'grid[assetContainerSource=true]'},
    {ref: 'assetInfo', selector: 'nx-coreui-component-assetinfo'},
    {ref: 'deleteAssetButton', selector: 'nx-coreui-component-assetcontainer button[action=deleteAsset]'},
    {ref: 'componentAssetInfo', selector: 'nx-coreui-component-componentassetinfo'},
    {ref: 'viewVulnerabilitiesButton', selector: 'nx-coreui-component-componentassetinfo button[action=viewVulnerabilities]'},
    {ref: 'componentList', selector: 'grid[componentList=true]'},
    {ref: 'componentDetails', selector: 'nx-coreui-component-details'},
    {ref: 'deleteComponentButton', selector: 'nx-coreui-component-details button[action=deleteComponent]'},
    {ref: 'analyzeApplicationButton', selector: 'nx-coreui-component-details button[action=analyzeApplication]'},
    {ref: 'browseComponentButton', selector: 'nx-coreui-component-details button[action=browseComponent]'},
    {ref: 'analyzeApplicationWindow', selector: 'nx-coreui-component-analyze-window'},
    {ref: 'rootContainer', selector: 'nx-main'}
  ],

  DEPENDENCY_SNIPPET_PANEL_ID: 'snippetPanel',

  /**
   * @override
   */
  init: function () {
    var me = this;

    var fileExtensionIcons = [{},
          '3gp', '7z', 'ace', 'ai', 'aif', 'aiff', 'amr', 'asf', 'asx', 'bat', 'bin', 'bmp', 'bup', 'cab', 'cbr', 'cda',
          'cdl', 'cdr', 'chm', 'dat', 'divx', 'dll', 'dmg', 'doc', 'dss', 'dvf', 'dwg', 'eml', 'eps', 'exe', 'fla',
          'flv', 'gif', 'gz', 'hqx', 'htm', 'html', 'ifo', 'indd', 'iso', 'jar', 'jpeg', 'jpg', 'lnk', 'log', 'm4a',
          'm4b', 'm4p', 'm4v', 'mcd', 'mdb', 'mid', 'mov', 'mp2', 'mp4', 'mpeg', 'mpg', 'msi', 'mswmm', 'ogg', 'pdf',
          'png', 'pps', 'ps', 'psd', 'pst', 'ptb', 'pub', 'qbb', 'qbw', 'qxd', 'ram', 'rar', 'rm', 'rmvb', 'rtf', 'sea',
          'ses', 'sit', 'sitx', 'ss', 'swf', 'tgz', 'thm', 'tif', 'tmp', 'torrent', 'ttf', 'txt', 'vcd', 'vob', 'wav',
          'wma', 'wmv', 'wps', 'xls', 'xpi', 'zip'
    ].reduce(function(icons, extension) {
      icons['asset-type-' + extension] = {
        file: 'file_extension_' + extension + '.png',
        variants: ['x16', 'x32']
      };
      return icons;
    });

    ['debian', 'json'].forEach(function(fileName) {
      fileExtensionIcons['asset-type-' + fileName] = {
        file: fileName + '.png',
        variants: ['x16', 'x32']
      };
    });

    var formatIcons = [{}, 'code', 'ruby'].reduce(function(icons, extension) {
      icons['asset-type-' + extension] = {
        file: 'page_white_' + extension + '.png',
        variants: ['x16', 'x32']
      };
      return icons;
    });

    me.getApplication().getIconController().addIcons(fileExtensionIcons);
    me.getApplication().getIconController().addIcons(formatIcons);
    me.getApplication().getIconController().addIcons({
      'asset-type-default': {
        file: 'page_white.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      component: {
        'nx-coreui-component-assetcontainer': {
          updated: me.showAssetInfo.bind(me)
        },
        'nx-coreui-component-details': {
          updated: me.showComponentDetails
        },
        'nx-coreui-component-asset-list': {
          updated: me.loadAssets
        },
        'nx-coreui-component-assetcontainer button[action=deleteAsset]': {
          click: me.deleteAsset
        },
        'nx-coreui-component-componentassetinfo button[action=viewVulnerabilities]': {
          click: me.mixins.componentUtils.viewVulnerabilities
        },
        'nx-coreui-component-details button[action=deleteComponent]': {
          click: me.deleteComponent
        },
        'nx-coreui-component-details button[action=browseComponent]': {
          click: me.browseComponent
        },
        'nx-coreui-component-details button[action=analyzeApplication]': {
          click: me.mixins.componentUtils.openAnalyzeApplicationWindow
        },
      }
    });

    me.repositoryStore = Ext.create('NX.coreui.store.RepositoryReference', {remote: true, autoLoad: true});
  },

  /**
   * Shows information about selected component/asset.
   *
   * @private
   * @param {NX.coreui.view.component.AssetContainer} container asset container
   * @param {NX.coreui.model.Asset} assetModel selected asset
   */
  showAssetInfo: function (container, assetModel) {
    var info = container.down('nx-coreui-component-assetinfo'),
        attributes = container.down('nx-coreui-component-assetattributes'),
        componentDetails = this.getComponentDetails(),
        componentModel = componentDetails && componentDetails.componentModel,
        format, dependencySnippets;

    if (!info) {
      container.addTab(
          {
            xtype: 'nx-coreui-component-assetinfo',
            title: NX.I18n.get('Component_AssetInfo_Info_Title'),
            itemId: 'assetInfo',
            weight: 10
          }
      );
      info = container.down('nx-coreui-component-assetinfo');
    }
    info.setAssetModel(assetModel);

    if (!attributes) {
      container.addTab(
          {
            xtype: 'panel',
            ui: 'nx-inset',
            title: NX.I18n.get('Component_AssetInfo_Attributes_Title'),
            itemId: 'attributeInfo',
            weight: 30,
            autoScroll: true,
            items: [
              {xtype: 'nx-coreui-component-assetattributes'}
            ]
          }
      );

      attributes = container.down('nx-coreui-component-assetattributes');
    }
    attributes.setAssetModel(assetModel);

    if (componentModel) {
      format = componentModel.get('format');
      dependencySnippets = NX.getApplication().getDependencySnippetController()
          .getDependencySnippets(format, componentModel, assetModel);

      if (dependencySnippets && dependencySnippets.length > 0) {
        this.getDependencySnippetPanel(container).setDependencySnippets(format, dependencySnippets);
        container.showTab(this.DEPENDENCY_SNIPPET_PANEL_ID);
      } else {
        container.hideTab(this.DEPENDENCY_SNIPPET_PANEL_ID);
      }
    }
  },

  getDependencySnippetPanel: function(container) {
    if (!container.down('nx-info-dependency-snippet-panel')) {
      container.addTab(
          {
            xtype: 'nx-info-dependency-snippet-panel',
            title: NX.I18n.get('DependencySnippetPanel_Title'),
            collapsible: false,
            itemId: this.DEPENDENCY_SNIPPET_PANEL_ID,
            weight: 20
          }
      );
    }

    return container.down('nx-info-dependency-snippet-panel');
  },

  showComponentDetails: function (container, componentModel) {
    var repositoryInfo = {},
        componentInfo = {};

    if (componentModel) {
      repositoryInfo[NX.I18n.get('Search_Assets_Repository')] = Ext.htmlEncode(componentModel.get('repositoryName'));
      repositoryInfo[NX.I18n.get('Search_Assets_Format')] = Ext.htmlEncode(componentModel.get('format'));
      componentInfo[NX.I18n.get('Search_Assets_Group')] = Ext.htmlEncode(componentModel.get('group'));
      componentInfo[NX.I18n.get('Search_Assets_Name')] = Ext.htmlEncode(componentModel.get('name'));
      componentInfo[NX.I18n.get('Search_Assets_Version')] = Ext.htmlEncode(componentModel.get('version'));

      container.down('#repositoryInfo').showInfo(repositoryInfo);
      container.down('#componentInfo').showInfo(componentInfo);
    }

    this.updateDeleteButtonVisibility();
    this.updateBrowseButtonVisibility();
    this.updateAnalyzeButton(componentModel);
  },

  /**
   * @private
   *
   * Filter asset store based on component model.
   *
   * @param {NX.coreui.view.component.AssetList} grid assets grid
   * @param {NX.coreui.model.Component} componentModel component owning the assets to be loaded
   */
  loadAssets: function (grid, componentModel) {
    var assetStore = grid.getStore();

    if (componentModel) {
      assetStore.clearFilter(true);
      assetStore.addFilter([
        {
          property: 'repositoryName',
          value: componentModel.get('repositoryName')
        },
        {
          property: 'componentModel',
          value: JSON.stringify(componentModel.getData())
        }
      ]);
    }
  },

  /**
   * Update asset shown in asset container.
   *
   * @public
   */
  updateAssetContainer: function (gridView, td, cellIndex, assetModel) {
    var me = this;

    me.getAssetContainer().refreshInfo(assetModel);

    me.getRepository(assetModel.get('repositoryName'), function (repository) {
      me.updateDeleteAssetButton(repository, assetModel);
    });
  },

  /**
   * Get the current repository
   *
   * @param repositoryName name of repository
   * @param callback function which will get passed the repository
   *
   * @private
   */
  getRepository: function(repositoryName, callback) {
    var repositoryStore = this.repositoryStore,
        repository = repositoryStore.getAt(repositoryStore.find('name', repositoryName));

    if (repository) {
      callback(repository);
    }
    else {
      repositoryStore.load(function() {
        callback(repositoryStore.getAt(repositoryStore.find('name', repositoryName)));
      });
    }
  },

  /**
   * Remove selected component.
   *
   * @private
   */
  deleteComponent: function() {
    var me = this,
        componentDetails = me.getComponentDetails(),
        componentModel, componentId, componentVersion;

    if (componentDetails) {
      componentModel = componentDetails.componentModel;
      componentVersion = componentModel.get('version');
      componentId = componentModel.get('name') + '/' + componentVersion;
      NX.Dialogs.askConfirmation(NX.I18n.get('ComponentDetails_Delete_Title'), Ext.htmlEncode(componentId), function() {
        NX.direct.coreui_Component.deleteComponent(JSON.stringify(componentModel.getData()), function(response) {
          if (Ext.isObject(response) && response.success) {
            me.refreshComponentList();
            NX.Bookmarks.navigateBackSegments(NX.Bookmarks.getBookmark(), 1);
            NX.Messages.success(NX.I18n.format('ComponentDetails_Delete_Success', componentId));
          }
        });
      });
    }
  },

  /**
   * Enable 'Delete' when user has 'delete' permission. Button will be hidden for group repositories.
   *
   * @private
   */
  updateDeleteButtonVisibility: function() {
    var me = this,
        componentModel = me.fetchComponentModelFromView(),
        formatSpecificActionHandler = me.getFormatSpecificActionHandler(componentModel),
        button = me.getDeleteComponentButton();

    if (componentModel &&
        (!formatSpecificActionHandler ||
            !formatSpecificActionHandler.updateDeleteButtonVisibility(button, componentModel))
    ) {
      me.getRepository(componentModel.get('repositoryName'), function(repository) {
        me.updateDeleteComponentButton(repository, componentModel);
      });
    }
  },

  /**
   * Enable 'Browse' button when format supports it.
   *
   * @private
   */
  updateBrowseButtonVisibility: function() {
    var componentModel = this.fetchComponentModelFromView(),
        formatSpecificActionHandler = this.getFormatSpecificActionHandler(componentModel),
        button = this.getBrowseComponentButton();

    if (!formatSpecificActionHandler || !formatSpecificActionHandler.updateBrowseButtonVisibility(button, componentModel)) {
      button.hide();
    }
  },

  /**
   * Get the format specific component details actions
   */
  getFormatSpecificActionHandler: function(componentModel) {
    var format = componentModel && componentModel.get('format'),
        alias = format && 'nx-coreui-' + format.toLowerCase() + '-component-action-handler';

    return alias && Ext.ClassManager.getByAlias(alias);
  },

  browseComponent: function() {
    var componentModel = this.fetchComponentModelFromView(),
        assetModel = this.fetchAssetModelFromView(),
        formatSpecificActionHandler = this.getFormatSpecificActionHandler(componentModel);

    if (formatSpecificActionHandler && formatSpecificActionHandler.browseComponent) {
      formatSpecificActionHandler.browseComponent(componentModel, assetModel);
    }
  },

  /**
   * @private
   * Remove selected asset.
   */
  deleteAsset: function () {
    var me = this,
        assetList = me.getAssetList(),
        assetInfo = me.getAssetInfo();

    if (assetInfo) {
      var asset = assetInfo.assetModel;
      NX.Dialogs.askConfirmation(NX.I18n.get('AssetInfo_Delete_Title'), Ext.htmlEncode(asset.get('name')), function () {
        NX.direct.coreui_Component.deleteAsset(asset.getId(), asset.get('repositoryName'), function (response) {
          if (Ext.isObject(response) && response.success) {
            assetList.getSelectionModel().deselectAll();
            //Manually managing sync'ing the local AssetStore as AssetStore.load() won't run a callback if the load
            //results in no data being returned.
            var assetStore = assetList.getStore();

            response.data.forEach(function(assetName) {
              assetStore.remove(assetStore.findRecord('name', assetName));
            });

            me.navigateBackOnAssetDelete(asset.get('componentId'), assetStore);
            NX.Messages.success(NX.I18n.format('AssetInfo_Delete_Success', asset.get('name')));
          }
        });
      });
    }
  },

  /**
   * Decide whether or not we should navigate back to a parent Component or to the prior page.
   * @private
   * @param {String} componentId
   * @param {Ext.data.Store} assetStore 
   */
  navigateBackOnAssetDelete: function(componentId, assetStore) {
    var me = this;
    if (!me.getComponentDetails() || !componentId || me.countAssetsInComponent(assetStore, componentId) > 0) {
      // Asset being deleted either does not have an associated component in scope, or is not the last Asset
      //<if debug>
      me.logDebug('Asset deleted with no component in scope or as last remaining asset');
      //</if>
      Ext.util.History.back();
    }
    else {
      //<if debug>
      me.logDebug('Asset deleted with component in scope');
      //</if>
      me.refreshComponentList();
      NX.Bookmarks.navigateBackSegments(NX.Bookmarks.getBookmark(), 2);
    }
  },

  countAssetsInComponent: function(assetStore, componentId) {
    var count = 0;
    assetStore.each(function(asset){
      if (asset.get('componentId') === componentId) {
        count++;
      }
    });

    return count;
  },

  fetchComponentModelFromView: function() {
    return this.getComponentDetails().componentModel;
  },

  fetchAssetModelFromView: function() {
    var assetList = this.getAssetList(),
        assetStore = assetList && assetList.getStore();
    return assetStore && assetStore.getAt(0);
  },

  /**
   * @private
   * Refresh component list.
   */
  refreshComponentList: function () {
    var componentList = this.getComponentList();

    componentList.getSelectionModel().deselectAll();
    // delay refresh of component list because in case of search results it takes a while till removal is
    // propagated to elastic search results. Not 100% but better then still showing
    setTimeout(function() {
      componentList.getStore().load();
    }, 1000);
  }

});

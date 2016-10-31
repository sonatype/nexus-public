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
 * Assets controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Assets', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Bookmarks',
    'NX.Dialogs',
    'NX.I18n',
    'Ext.util.Format'
  ],

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
    {ref: 'componentList', selector: 'grid[componentList=true]'},
    {ref: 'componentDetails', selector: 'nx-coreui-component-details'},
    {ref: 'deleteComponentButton', selector: 'nx-coreui-component-details button[action=deleteComponent]'},
    {ref: 'analyzeApplicationButton', selector: 'nx-coreui-component-details button[action=analyzeApplication]'},
    {ref: 'analyzeApplicationWindow', selector: 'nx-coreui-component-analyze-window'},
    {ref: 'rootContainer', selector: 'nx-main'}
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'asset-type-default': {
        file: 'page_white.png',
        variants: ['x16', 'x32']
      },
      'asset-type-application-java-archive': {
        file: 'file_extension_jar.png',
        variants: ['x16', 'x32']
      },
      'asset-type-text-xml': {
        file: 'page_white_code.png',
        variants: ['x16', 'x32']
      },
      'asset-type-application-xml': {
        file: 'page_white_code.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      component: {
        'nx-coreui-component-assetcontainer': {
          updated: me.showAssetInfo
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
        'nx-coreui-component-details button[action=deleteComponent]': {
          click: me.deleteComponent
        },
        'nx-coreui-component-details button[action=analyzeApplication]': {
          click: me.openAnalyzeApplicationWindow
        },
        'nx-coreui-component-analyze-window button[action=analyze]': {
          click: me.analyzeAsset
        },
        'nx-coreui-component-analyze-window combobox[name="asset"]': {
          select: me.selectedApplicationChanged
        }
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
        panel;

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
            weight: 20,
            autoScroll: true,
            items: [
              {xtype: 'nx-coreui-component-assetattributes'}
            ]
          }
      );

      attributes = container.down('nx-coreui-component-assetattributes');
    }
    attributes.setAssetModel(assetModel);
  },

  showComponentDetails: function (container, componentModel) {
    var repositoryInfo = {},
        componentInfo = {};

    if (componentModel) {
      repositoryInfo[NX.I18n.get('Search_Assets_Repository')] = componentModel.get('repositoryName');
      repositoryInfo[NX.I18n.get('Search_Assets_Format')] = componentModel.get('format');
      componentInfo[NX.I18n.get('Search_Assets_Group')] = componentModel.get('group');
      componentInfo[NX.I18n.get('Search_Assets_Name')] = componentModel.get('name');
      componentInfo[NX.I18n.get('Search_Assets_Version')] = componentModel.get('version');

      container.down('#repositoryInfo').showInfo(repositoryInfo);
      container.down('#componentInfo').showInfo(componentInfo);

      this.bindDeleteComponentButton(this.getDeleteComponentButton());
    }
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
    var assetStore = grid.getStore(),
        filters;

    if (componentModel) {
      assetStore.clearFilter(true);
      filters = [
        {
          property: 'repositoryName',
          value: componentModel.get('repositoryName')
        },
        {
          property: 'componentId',
          value: componentModel.getId()
        },
        {
          property: 'componentName',
          value: componentModel.get('name')
        }
      ];
      if (componentModel.get('group')) {
        filters.push({
          property: 'componentGroup',
          value: componentModel.get('group')
        });
      }
      if (componentModel.get('version')) {
        filters.push({
          property: 'componentVersion',
          value: componentModel.get('version')
        });
      }
      assetStore.addFilter(filters);
    }
  },

  /**
   * Update asset shown in asset container.
   *
   * @public
   */
  updateAssetContainer: function (gridView, td, cellIndex, assetModel) {
    this.getAssetContainer().refreshInfo(assetModel);
    this.bindDeleteAssetButton(this.getDeleteAssetButton());
  },

  /**
   * Enable 'Delete' when user has 'delete' permission. Button will be hidden for group repositories.
   *
   * @private
   */
  bindDeleteComponentButton: function(button) {
    this.bindButton(button, this.getComponentDetails().componentModel.get('repositoryName'));
  },

  /**
   * Enable 'Delete' when user has 'delete' permission. Button will be hidden for group repositories.
   *
   * @private
   */
  bindDeleteAssetButton: function(button) {
    this.bindButton(button, this.getAssetContainer().assetModel.get('repositoryName'));
  },

  /**
   * Bind/Hide delete button.
   *
   * @param button to be shown/hidden
   * @param repositoryName name of repository
   *
   * @private
   */
  bindButton: function(button, repositoryName) {
    var repositoryStore = this.repositoryStore,
        repository,
        showButtonFunction = function(repository) {
          if (repository && repository.get('type') !== 'group') {
            button.show();
            button.enable();
          }
        };

    //check for repositoryName in RepositoryStore and conditionally hide button for groups
    button.hide();
    repository = repositoryStore.getAt(repositoryStore.find('name', repositoryName));
    if (repository) {
      showButtonFunction(repository);
    }
    else {
      repositoryStore.load(function() {
        showButtonFunction(repositoryStore.getAt(repositoryStore.find('name', repositoryName)));
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
        componentList = me.getComponentList(),
        componentDetails = me.getComponentDetails(),
        componentModel, componentId, repositoryName;

    if (componentDetails) {
      componentModel = componentDetails.componentModel;
      componentId = componentModel.get('name') + '/' + componentModel.get('version');
      repositoryName = componentModel.get('repositoryName');
      NX.Dialogs.askConfirmation(NX.I18n.get('ComponentDetails_Delete_Title'), componentId, function() {
        NX.direct.coreui_Component.deleteComponent(componentModel.getId(), repositoryName, function(response) {
          if (Ext.isObject(response) && response.success) {
            componentList.getSelectionModel().deselectAll();
            NX.Bookmarks.navigateBackSegments(NX.Bookmarks.getBookmark(), 1);
            // delay refresh of component list because in case of search results it takes a while till removal is
            // propagated to elastic search results. Not 100% but better then still showing
            setTimeout(function() {
              componentList.getStore().load();
            }, 1000);
            NX.Messages.add({text: NX.I18n.format('ComponentDetails_Delete_Success', componentId), type: 'success'});
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
        componentDetails = me.getComponentDetails(),
        componentId = componentDetails.componentModel.getId(),
        repositoryName = componentDetails.componentModel.get('repositoryName');

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
        componentDetails = me.getComponentDetails(),
        win = button.up('window'),
        form = button.up('form'),
        formValues = form.getForm().getValues(),
        repositoryName = componentDetails.componentModel.get('repositoryName'),
        assetId = form.down('combo[name="asset"]').getValue();

    NX.direct.ahc_Component.analyzeAsset(repositoryName, assetId, formValues.emailAddress, formValues.password,
        formValues.proprietaryPackages, formValues.reportLabel, function(response) {
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({text: NX.I18n.get('ComponentDetails_Analyze_Success'), type: 'success'});
      }
    });
  },

  /**
   * When app changes, update the reportName as well
   */
  selectedApplicationChanged: function(combo) {
    var me = this,
        labelField = me.getAnalyzeApplicationWindow().down('textfield[name="reportLabel"]');

    if (!labelField.isDirty()) {
      //I am setting the original value so it won't be marked dirty unless user touches it
      labelField.originalValue = combo.getRawValue();
      labelField.setValue(combo.getRawValue());
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
      NX.Dialogs.askConfirmation(NX.I18n.get('AssetInfo_Delete_Title'), asset.get('name'), function () {
        NX.direct.coreui_Component.deleteAsset(asset.getId(), asset.get('repositoryName'), function (response) {
          if (Ext.isObject(response) && response.success) {
            assetList.getSelectionModel().deselectAll();
            //Manually managing sync'ing the local AssetStore as AssetStore.load() won't run a callback if the load
            //results in no data being returned.
            var assetStore = assetList.getStore();
            assetStore.remove(assetStore.findRecord('id', asset.getId()));
            me.navigateBackOnAssetDelete(asset.get('componentId'), assetStore);
            NX.Messages.add({text: NX.I18n.format('AssetInfo_Delete_Success', asset.get('name')), type: 'success'});
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
    if (!me.getComponentDetails() || !componentId || assetStore.find('componentId', componentId) > -1) {
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
      NX.Bookmarks.navigateBackSegments(NX.Bookmarks.getBookmark(), 2);
    }
  }

});

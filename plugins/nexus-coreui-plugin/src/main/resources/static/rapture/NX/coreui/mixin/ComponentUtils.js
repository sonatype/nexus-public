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
 * Component helper utils.
 *
 * @since 3.14
 */

Ext.define('NX.coreui.mixin.ComponentUtils', {
  requires: [
    'NX.Bookmarks',
    'NX.Dialogs',
    'NX.I18n',
    'NX.Messages'
  ],

  /**
   * Open the analyze application form window
   *
   */
  openAnalyzeApplicationWindow: function() {
    var me = this,
        componentModel = me.fetchComponentModelFromView();

    var modalWindow = Ext.first('nx-coreui-component-analyze-window');

    modalWindow.component = componentModel.getData();
    modalWindow.show();
  },

  updateAnalyzeButton: function(componentModel) {
    this.user = NX.State.getUser();
    var analyzeApplicationButton = this.getAnalyzeApplicationButton();

    if (!componentModel || !NX.direct.ahc_Component) {
      analyzeApplicationButton.disable();
    }
    else if (this.user && this.user.authenticated) {
      NX.direct.ahc_Component.containsApplication(JSON.stringify(componentModel.getData()), function(response) {
        if (Ext.isObject(response) && response.success) {
          if (response.data) {
            analyzeApplicationButton.enable();
          }
          else {
            analyzeApplicationButton.disableWithTooltip(
                NX.I18n.get('AnalyzeApplicationWindow_No_Assets_Error_Message'));
          }
        }
      });
    }
    else {
      analyzeApplicationButton.disableWithTooltip(NX.I18n.get('AnalyzeApplication_Button_Unauthenticated'));
    }
  },

  viewVulnerabilities: function() {
    var info = this.getComponentInfo || this.getComponentAssetInfo;
    var vulnerabilityPanel = info().getVulnerabilityPanel();
    if (vulnerabilityPanel) {
      window.open(vulnerabilityPanel.referenceLink);
    }
  },

  updateVulnerabilitiesButton: function(panel, vulnerabilityInfo) {
    var viewVulnerabilitiesButton = panel.getViewVulnerabilitiesButton();
    if(vulnerabilityInfo) {
      viewVulnerabilitiesButton.setVisible(true);
      if (vulnerabilityInfo.count > 0) {
        viewVulnerabilitiesButton.setText(
            NX.I18n.format('ComponentDetails_View_Vulnerabilities_Count_Button', vulnerabilityInfo.count));
      }
      else {
        viewVulnerabilitiesButton.setText(NX.I18n.get('ComponentDetails_View_Vulnerabilities_Button'));
      }
    }
    else {
      viewVulnerabilitiesButton.setVisible(false);
    }
  },

  updateDeleteComponentButton: function(currentRepository, componentModel) {
    var me = this,
        deleteComponentButton = me.getDeleteComponentButton();

    if (componentModel) {
      me.updateDeleteButton(deleteComponentButton, currentRepository, function() {
        NX.direct.coreui_Component.canDeleteComponent(JSON.stringify(componentModel.getData()), function(response) {
          if (Ext.isObject(response) && response.success) {
            if (response.data) {
              deleteComponentButton.enable();
            }
            else {
              deleteComponentButton.disableWithTooltip(NX.I18n.get('ComponentUtils_Delete_Component_No_Permissions'));
            }
          }
        });
      });
    }
    else {
      deleteComponentButton.disable();
    }
  },

  updateDeleteAssetButton: function(currentRepository, assetModel, isFolder) {
    var me = this,
        deleteAssetButton = me.getDeleteAssetButton(),
        deleteAssetFolderButton = me.getDeleteAssetFolderButton && me.getDeleteAssetFolderButton();

    if (assetModel) {
      me.updateDeleteButton(deleteAssetButton, currentRepository, function() {
        NX.direct.coreui_Component.canDeleteAsset(assetModel.getId(), assetModel.get('repositoryName'),
            function(response) {
              if (Ext.isObject(response) && response.success) {
                if (response.data) {
                  deleteAssetButton.enable();
                }
                else {
                  deleteAssetButton.disableWithTooltip(NX.I18n.get('ComponentUtils_Delete_Asset_No_Permissions'));
                }
              }
            });
      });
    }
    else {
      deleteAssetButton.disable();
    }

    if (deleteAssetFolderButton) {
      if (isFolder) {
        deleteAssetFolderButton.show();
        me.updateDeleteFolderButton(deleteAssetFolderButton, currentRepository, assetModel.get('name'));
      }
      else {
        deleteAssetFolderButton.disable();
        deleteAssetFolderButton.hide();
      }
    }
  },

  updateDeleteButton: function(deleteButton, currentRepository, isAuthenticatedCallback) {
    this.user = NX.State.getUser();

    if (currentRepository && currentRepository.get('type') !== 'group') {
      deleteButton.disable();
      deleteButton.show();
      if (this.user && this.user.authenticated) {
        isAuthenticatedCallback();
      }
      else {
        deleteButton.disableWithTooltip(NX.I18n.get('ComponentUtils_Delete_Button_Unauthenticated'));
      }
    }
    else {
      deleteButton.hide();
    }
  },

  updateDeleteFolderButton: function (deleteButton, currentRepository, path) {
    var me = this;

    if (path) {
      me.updateDeleteButton(deleteButton, currentRepository, function() {
        NX.direct.coreui_Component.canDeleteFolder(path, currentRepository.get('name'),
            function(response) {
              if (Ext.isObject(response) && response.success) {
                if (response.data) {
                  deleteButton.enable();
                }
                else {
                  deleteButton.disableWithTooltip(NX.I18n.get('ComponentUtils_Delete_Asset_No_Permissions'));
                }
              }
            });
      });
    }
  },

  /**
   * @private
   * @param assetModel
   * @returns string showing either last download date or that no downloads have happened
   */
  getLastDownloadDateForDisplay: function(assetModel) {
    return assetModel.get('lastDownloaded') || NX.I18n.get('Assets_Info_No_Downloads');
  },

  /**
   * @param asset
   * @returns {NX.model.Icon} an icon for a given asset
   */
  getIconForAsset: function (asset) {
    var me = this,
        iconController = NX.getApplication().getIconController();

    switch (asset.get('type')) {
      case 'folder':
        return iconController.findIcon('tree-folder', 'x16');
      case 'component':
        if('OSS' === NX.State.getEdition() && asset.get('vulnerable')) {
          return iconController.findIcon('vulnerability', 'x16');
        }
        return iconController.findIcon('tree-component', 'x16');
      case 'asset':
        if('OSS' === NX.State.getEdition() && asset.get('vulnerable')) {
          return iconController.findIcon('vulnerability', 'x16');
        }
        var assetName = asset.get('text');
        var icon = me.getIconForAssetName(assetName);
        if (icon) {
          return icon;
        }
        return iconController.findIcon((asset.get('leaf') ? 'tree-asset' : 'tree-asset-folder'), 'x16')
    }
  },

  /**
   * @param assetName
   * @returns {NX.model.Icon} an icon for a given asset name
   */
  getIconForAssetName: function (assetName) {
    var index = assetName.lastIndexOf('.');
    if (index != -1) {
      var extension = assetName.substr(index + 1);
      extension = this.getExtensionOverrideMaybe(extension);
      return NX.getApplication().getIconController().findIcon('asset-type-' + extension, 'x16');
    }
    return null;
  },

  /**
   * @private
   * @param extension
   * @returns {string} extension or an extension override
   */
  getExtensionOverrideMaybe: function (extension) {
    switch (extension) {
      case 'gem':
      case 'rb':
        return 'ruby';

      case 'egg':
      case 'nupkg':
      case 'rpm':
      case 'whl':
        return 'zip';

      case 'bz2':
      case 'lzma':
      case 'rz':
      case 'xz':
      case 'Z':
        return 'gz';

      case 'tbz2':
      case 'tlz':
      case 'txz':
        return 'tgz';

      case 'ear':
      case 'war':
        return 'jar';

      case 'sh':
        return 'bat';

      case 'pom':
      case 'xml':
        return 'code';

      case 'deb':
        return 'debian';

      default:
        return extension;
    }
  }

});

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
    NX.direct.ahc_Component.getPredefinedValues(JSON.stringify(componentModel.getData()), function(response) {
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

  updateAnalyzeButton: function(componentModel) {
    var user = NX.State.getUser(),
        analyzeApplicationButton = this.getAnalyzeApplicationButton();

    if (!componentModel) {
      analyzeApplicationButton.disable();
    }
    else if (user && user.authenticated) {
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

  updateDeleteAssetButton: function(currentRepository, assetModel) {
    var me = this,
        deleteAssetButton = me.getDeleteAssetButton();

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
  },

  updateDeleteButton: function(deleteButton, currentRepository, isAuthenticatedCallback) {
    var user = NX.State.getUser();

    if (currentRepository && currentRepository.get('type') !== 'group') {
      deleteButton.disable();
      deleteButton.show();
      if (user && user.authenticated) {
        isAuthenticatedCallback();
      }
      else {
        deleteButton.disableWithTooltip(NX.I18n.get('ComponentUtils_Delete_Button_Unauthenticated'));
      }
    }
  }

});

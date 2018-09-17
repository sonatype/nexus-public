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
 * @since 3.next
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
  }

});

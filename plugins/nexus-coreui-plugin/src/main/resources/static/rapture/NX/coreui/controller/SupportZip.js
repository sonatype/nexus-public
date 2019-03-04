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
 * Support Zip controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SupportZip', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.Security',
    'NX.util.Base64',
    'NX.util.Url',
    'NX.util.DownloadHelper',
    'NX.I18n',
    'NX.Dialogs'
  ],

  views: [
    'support.SupportZip',
    'support.SupportZipCreated',
    'support.SupportZipHaCreated'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'supportzip-zip': {
        file: 'file_extension_zip.png',
        variants: ['x16', 'x32']
      },
      'supportzip-truncated': {
        file: 'warning.png',
        variants: ['x16', 'x32']
      },
      'supportzip-error': {
        file: 'exclamation.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Support ZIP',
      text: NX.I18n.get('SupportZip_Title'),
      description: NX.I18n.get('SupportZip_Description'),
      view: { xtype: 'nx-coreui-support-supportzip' },
      iconConfig: {
        file: 'file_extension_zip.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:atlas:create');
      }
    }, me);

    me.listen({
      component: {
        'nx-coreui-support-supportzip form': {
          submitted: me.showSupportZipCreatedWindow
        },
        'nx-coreui-support-supportzip form button[action=hazips]': {
          click: me.createHaSupportZips
        },
        'nx-coreui-support-supportzipcreated button[action=download]': {
          click: me.download
        },
        'nx-coreui-support-supportziphacreated button[action=download]': {
          click: me.downloadNode
        }
      }
    });
  },

  /**
   * @private
   */
  showSupportZipCreatedWindow: function (form, action) {
    Ext.widget('nx-coreui-support-supportzipcreated').setValues(action.result.data);
  },

  /**
   * @private
   */
  createHaSupportZips: function(button) {
    var form = button.up('form'),
        formData = form.getForm();

    if (formData.isValid()) {
      form.mask(NX.I18n.get('Support_SupportZip_Creating_Message'));

      Ext.Ajax.request({
        url: NX.util.Url.baseUrl + '/service/rest/v1/nodes/supportzips',
        jsonData: formData.getFieldValues(false),
        callback: function(options, success, response) {
          form.unmask();

          if (success) {
            var zipWidget = Ext.widget('nx-coreui-support-supportziphacreated');
            Ext.Array.each(Ext.JSON.decode(response.responseText), function(nodeZip) {
              zipWidget.addNode(nodeZip);
            });
            zipWidget.center();
          }
          else {
            NX.Dialogs.showError(NX.I18n.get('Support_HA_SupportZip_Failed_Title'),
                NX.I18n.get('Support_HA_SupportZip_Failed_Message'));
          }
        }
      });
    }
  },

  /**
   * @private
   * Download support ZIP file.
   */
  download: function (button) {
    var win = button.up('window'),
        fileName = win.down('form').getValues().name;

    this.doDownload(fileName, function() { win.close(); });
  },

  /**
   * @private
   * Download support ZIP file of an HA-C node
   */
  downloadNode: function (button) {
    this.doDownload(button.up('form').getValues().name);
  },

  /**
   * @private
   * Requests the download
   */
  doDownload: function (fileName, onSuccess) {
    NX.Security.doWithAuthenticationToken(
        NX.I18n.get('SupportZip_Authenticate_Text'),
        {
          success: function (authToken) {
            NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf(
                'service/rest/wonderland/download/' + fileName + '?t=' + NX.util.Base64.encode(authToken)
            ));

            if (Ext.isFunction(onSuccess)) {
              onSuccess.call();
            }
          }
        }
    );
  }
});

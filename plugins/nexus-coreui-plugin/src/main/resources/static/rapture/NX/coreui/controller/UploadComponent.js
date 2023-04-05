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
 * Upload controller.
 *
 * @since 3.7
 */
Ext.define('NX.coreui.controller.UploadComponent', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n',
    'NX.controller.ExtDirect'
  ],
  masters: [
    'nx-coreui-uploadcomponentfeature nx-coreui-upload-repository-list',
    'nx-coreui-uploadcomponentfeature nx-coreui-upload-component'
  ],
  stores: [
    'RepositoryReference',
    'UploadComponentDefinition'
  ],
  models: [
    'UploadComponentDefinition',
    'RepositoryReference'
  ],

  views: [
    'upload.UploadComponentFeature',
    'upload.UploadComponent',
    'upload.UploadRepositoryList'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-uploadcomponentfeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-uploadcomponentfeature nx-coreui-upload-repository-list'},
    {ref: 'uploadComponent', selector: 'nx-coreui-uploadcomponentfeature nx-coreui-upload-component'},
    {ref: 'successMessage', selector: '#nx-coreui-upload-success-message'}
  ],

  icons: {
    'upload': {
      file: 'upload.png',
      variants: ['x16', 'x32']
    },
    'tick': {
      file: 'tick.png',
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
      path: '/Upload',
      text: NX.I18n.get('FeatureGroups_Upload_Text'),
      description: NX.I18n.get('FeatureGroups_Upload_Description'),
      view: 'NX.coreui.view.upload.UploadComponentFeature',
      group: false,
      iconCls: 'x-fa fa-upload',
      authenticationRequired: false,
      visible: function() {
          return NX.Permissions.check('nexus:component:create') &&
            !NX.State.getValue('nexus.react.upload', false);
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      component: {
        'nx-coreui-uploadcomponentfeature nx-coreui-upload-repository-list': {
          beforerender: me.onBeforeRender
        },
        'nx-coreui-upload-component button[action=remove_upload_asset]': {
          click: me.removeUploadAsset
        },
        'nx-coreui-upload-component button[action=upload]': {
          click: me.doUpload
        },
        'nx-coreui-upload-component button[action=cancel]': {
          click: me.discardUpload
        },
        'nx-coreui-upload-component button[action=add_asset]': {
          click: me.addAsset
        },
        'nx-coreui-upload-component textfield[name$=extension]': {
          change: me.onExtensionChange
        },
        'nx-coreui-upload-component checkbox[name=generate-pom]' : {
          change: me.onGeneratePomChange
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
    this.loadUploadPage(model);
  },

  loadUploadPage: function(repoModel) {
    var uploadComponentDefinition = this.getStore('UploadComponentDefinition').getById(repoModel.get('format'));
    this.getUploadComponent().loadRecord(uploadComponentDefinition, repoModel);
  },

  /**
   * @override
   */
  onNavigate: function() {
    if (this.getFeature()) {
      this.onBeforeRender();
    }
  },

  loadView: function (index, model) {
    this.callParent(arguments);
    if (model) {
        //redraw the panel after visible, to get around issue where file field can be drawn at invalid size
        this.loadUploadPage(model);
    }
  },

  /**
   * @private
   * Load stores based on the bookmarked URL
   */
  onBeforeRender: function() {
    var me = this,
        uploadComponent = me.getUploadComponent(),
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoStore = me.getRepositoryList().getStore(),
        repoModel;

    // If the list hasn’t loaded, don't do anything
    if (!uploadComponent) {
      return;
    }

    repoStore.removeAll();

    this.getStore('UploadComponentDefinition').load(function (data) {
        var formats = [];

        data.forEach(function(def) {
          formats.push(def.get('format'));
        });

        repoStore.addFilter([{
          property: 'format',
          filterFn: function(item) {
            return formats.indexOf(item.get('format')) !== -1;
          }
        }, {
          property: 'type',
          value: 'hosted'
        }, {
          property: 'versionPolicy',
          filterFn: function(item) {
            return item.get('versionPolicy') == null || item.get('versionPolicy') !== 'SNAPSHOT';
          }
        }, {
          property: 'status',
          filterFn: function(item) {
            return item.get('status') == null || item.get('status').online !== false;
          }
        }]);
        repoStore.load(function () {
            // Load the asset upload page
            if (list_ids[1]) {
                repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
                uploadComponent.getStore().load(function () {
                    me.onModelChanged(0, repoModel);
                    me.onSelection(undefined, repoModel);
                });
            }
            // Load the asset list view or repository list view
            else {
                me.reselect();
            }
        });
    });
  },

  removeUploadAsset: function(fileUploadField) {
    var me = this;

    fileUploadField.up('#nx-coreui-upload-component-assets').remove(fileUploadField.up());
    me.refreshRemoveButtonState();
    me.updatePomFileState();
  },

  doUpload: function(button) {
    var me = this,
        fp = button.up('form');

    if(fp.getForm().isValid()) {
      me.setSuccessMessage();

      fp.getForm().submit({
        waitMsg: NX.I18n.get('FeatureGroups_Upload_Wait_Message'),
        success: function(form, action) {
          var message = NX.I18n.format('FeatureGroups_Upload_Successful_Text', form.getValues().repositoryName);
          if (NX.Permissions.check('nexus:search:read')) {
            message += ", " + NX.util.Url.asLink(
                '#browse/search=' + encodeURIComponent('keyword="' + action.result.data + '"'),
                NX.I18n.get('FeatureGroups_Upload_Successful_Link_Text'), '_self');
          }
          me.setSuccessMessage(message);
          me.resetForm();
        },
        failure: function(form, action) {
          var transaction;

          if (!action.result || !action.result.length) {
            NX.Messages.error('An unknown error occurred uploading components');
            console.error(action);
          }
          else {
            transaction = {
              result: action.result[0]
            };
            transaction.result.success = false;
            NX.getApplication().getExtDirectController().checkResponse(null, transaction);
          }
        }
      });
    }
  },

  setSuccessMessage: function (message) {
      var me = this,
          successMessage = me.getSuccessMessage();

      if (message) {
          successMessage.setTitle(message);
          successMessage.show();
      }
      else {
          successMessage.hide();
      }
  },

  discardUpload: function() {
    var me = this;
    me.resetForm();
    me.loadView(me.BROWSE_INDEX);
  },

  resetForm: function() {
    var me = this,
        form = me.getUploadComponent().down('form');

    form.getForm().reset();

    // remove rows
    form.query('fileuploadfield').forEach(function(fileUploadField) {
      me.removeUploadAsset(fileUploadField);
    });

    // create new row
    me.addAsset();

    // clearOnSubmit prevents normal form reset from working...
    form.down('fileuploadfield').inputEl.dom.value = '';
  },

  addAsset: function() {
    var me = this,
        uploadComponent = me.getUploadComponent(),
        form = uploadComponent.down('form');

    uploadComponent.addAssetRow();
    me.refreshRemoveButtonState();
    me.updatePomFileState();
    form.isValid();
  },

  onExtensionChange: function() {
    var me = this,
        form = me.getUploadComponent().down('form');

    me.updatePomFileState();
    form.isValid();
  },

  updatePomFileState: function() {
    var me = this,
        form = me.getUploadComponent().down('form'),
        componentCoordinatesFieldset = form.down('fieldset[title="Component coordinates"]'),
        isPomFilePresent = form.query('textfield[name$=extension][value=pom]').length !== 0;

    if (componentCoordinatesFieldset === null) {
        return;
    }

    componentCoordinatesFieldset.setDisabled(isPomFilePresent);
    if (isPomFilePresent) {
      componentCoordinatesFieldset.mask(NX.I18n.get('FeatureGroups_Upload_Form_DetailsFromPom_Mask'), 'nx-mask-without-spinner');
    }
    else {
      componentCoordinatesFieldset.unmask();
    }
  },

  /**
   * @private
   * Hide remove buttons if there is only one asset displayed
   */
  refreshRemoveButtonState: function() {
    var me = this,
        buttons = me.getUploadComponent().query('button[action=remove_upload_asset]'),
        hidden = (buttons.length === 1);

    buttons.forEach(function(button) {
      button.setVisible(!hidden);
    });
  },

  /**
   * @private
   * Change disabled state of packaging field based on generate pom checkbox
   */
  onGeneratePomChange: function(element) {
    element.up('form').down('textfield[name=packaging]').setDisabled(!element.getValue());
  }
});

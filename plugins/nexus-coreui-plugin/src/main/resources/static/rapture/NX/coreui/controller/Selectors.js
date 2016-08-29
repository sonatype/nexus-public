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
 * Selectors controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Selectors', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n',
    'NX.coreui.view.selector.SelectorPreviewWindow'
  ],
  masters: [
    'nx-coreui-selector-list'
  ],
  models: [
    'Selector'
  ],
  stores: [
    'Selector',
    'PreviewAsset'
  ],
  views: [
    'selector.SelectorAdd',
    'selector.SelectorFeature',
    'selector.SelectorList',
    'selector.SelectorSettings',
    'selector.SelectorSettingsForm',
    'selector.SelectorPreviewWindow'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-selector-feature'},
    {ref: 'list', selector: 'nx-coreui-selector-list'},
    {ref: 'settings', selector: 'nx-coreui-selector-feature nx-coreui-selector-settings'},
    {ref: 'previewWindow', selector: 'nx-coreui-selector-preview-window'},
    {ref: 'previewAssetList', selector: 'nx-coreui-selector-preview-window nx-coreui-browse-asset-list'},
    {ref: 'previewExpressionTextfield', selector: 'nx-coreui-selector-preview-window textfield[name=jexl]'},
    {ref: 'previewRepositoryComboBox', selector: 'nx-coreui-selector-preview-window combo[name=selectedRepository]'},
    {ref: 'previewRepositoryExpression', selector: 'nx-coreui-selector-preview-window textfield[name=jexl]'},
    {ref: 'addSettingsForm', selector: 'nx-coreui-selector-add nx-coreui-selector-settings-form'},
    {ref: 'editSettingsForm', selector: 'nx-coreui-selector-settings nx-coreui-selector-settings-form'}
  ],
  icons: {
    'selector-default': {
      file: 'content_selector.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Repository/Selectors',
    text: NX.I18n.get('Selectors_Text'),
    description: NX.I18n.get('Selectors_Description'),
    view: {xtype: 'nx-coreui-selector-feature'},
    iconConfig: {
      file: 'content_selector.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('nexus:selectors:read');
    },
    weight: 300
  },
  permission: 'nexus:selectors',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Selector': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-selector-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-selector-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-selector-settings-form': {
          submitted: me.loadStores
        },
        'nx-coreui-selector-settings-form button[action=preview]': {
          click: me.showPreviewWindow
        },
        'nx-coreui-selector-preview-window combo[name=selectedRepository]': {
          select: me.previewRepositorySelected
        },
        'nx-coreui-selector-preview-window textfield[name=jexl]': {
          //note i use blur instead of change, we don't want to requery on every keystroke
          blur: me.previewRepositorySelected
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
   */
  onSelection: function(list, model) {
    var me = this;

    me.addMode = false;

    if (Ext.isDefined(model)) {
      me.getSettings().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    me.addMode = true;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Selectors_Create_Title'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-selector-add'));
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Selector.remove(model.getId(), function(response) {
      me.getSelectorStore().load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Selectors_Delete_Message', description), type: 'success'
        });
      }
    });
  },

  showPreviewWindow: function() {
    var me = this, form;

    if (me.addMode) {
      form = me.getAddSettingsForm().getForm();
    }
    else {
      form = me.getEditSettingsForm().getForm();
    }

    var assetStore = me.getPreviewAssetStore();
    assetStore.addListener('load', me.loadPreviewAssetStore, this);

    Ext.create('NX.coreui.view.selector.SelectorPreviewWindow', {
      jexl: form.findField('expression').getValue(),
      assetStore: assetStore
    });
  },

  previewRepositorySelected: function() {
    var me = this,
        assetList = me.getPreviewAssetList(),
        assetStore = assetList.getStore(),
        repositoryName = me.getPreviewRepositoryComboBox().getValue();

    me.getPreviewExpressionTextfield().clearInvalid();

    if (repositoryName) {
      assetStore.addFilter([
        {
          id: 'repositoryName',
          property: 'repositoryName',
          value: repositoryName
        },
        {
          id: 'jexlExpression',
          property: 'jexlExpression',
          value: me.getPreviewRepositoryExpression().getValue()
        }
      ]);
    }
  },

  loadPreviewAssetStore: function(store, records, successful) {
    var me = this;
    //since we are dealing with a store, there isn't typical api for mapping an error to a form field
    //so we do it manually
    if (!successful &&
        store.getProxy().getReader().jsonData &&
        store.getProxy().getReader().jsonData.errors &&
        store.getProxy().getReader().jsonData.errors.expression) {
      me.getPreviewExpressionTextfield().markInvalid(store.getProxy().getReader().jsonData.errors.expression);
    }
  }
});

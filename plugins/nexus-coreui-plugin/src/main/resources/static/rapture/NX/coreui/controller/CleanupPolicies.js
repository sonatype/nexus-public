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
 * Cleanup Policies controller.
 *
 * @since 3.14
 */
Ext.define('NX.coreui.controller.CleanupPolicies', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-cleanuppolicy-list'
  ],
  stores: [
    'CleanupPolicy',
    'CleanupPolicies',
    'RepositoryFormat',
    'CleanupPreview'
  ],
  models: [
    'CleanupPolicy'
  ],
  views: [
    'cleanuppolicy.CleanupPolicyAdd',
    'cleanuppolicy.CleanupPolicyFeature',
    'cleanuppolicy.CleanupPolicyList',
    'cleanuppolicy.CleanupPolicySettings',
    'cleanuppolicy.CleanupPolicySettingsForm',
    'cleanuppolicy.CleanupPolicyPreviewWindow',
    'formfield.SettingsFieldSet'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-cleanuppolicy-feature'},
    {ref: 'content', selector: 'nx-feature-content'},
    {ref: 'list', selector: 'nx-coreui-cleanuppolicy-list'},
    {ref: 'settings', selector: 'nx-coreui-cleanuppolicy-feature nx-coreui-cleanuppolicy-settings'},
    {ref: 'previewRepositoryComboBox', selector: 'nx-coreui-cleanuppolicy-preview-window combo[name=selectedRepository]'},
    {ref: 'previewWindowPreviewButton', selector: 'nx-coreui-cleanuppolicy-preview-window button[action=preview]'},
    {ref: 'previewComponentList', selector: 'nx-coreui-cleanuppolicy-preview-window gridpanel'},
    {ref: 'addSettingsForm', selector: 'nx-coreui-cleanuppolicy-add nx-coreui-cleanuppolicy-settings-form'},
    {ref: 'editSettingsForm', selector: 'nx-coreui-cleanuppolicy-settings nx-coreui-cleanuppolicy-settings-form'},
    {ref: 'previewWindowCount', selector: 'nx-coreui-cleanuppolicy-preview-window #componentCountPanel'},
    {ref: 'currentComponentCount', selector: 'nx-coreui-cleanuppolicy-preview-window label[name=currentComponentCount]'},
    {ref: 'totalComponentCount', selector: 'nx-coreui-cleanuppolicy-preview-window label[name=totalComponentCount]'}
  ],
  icons: {
    'cleanuppolicy-default': {
      file: 'broom.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:*',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Repository/CleanupPolicies',
      text: NX.I18n.get('CleanupPolicies_Text'),
      description: NX.I18n.get('CleanupPolicies_Description'),
      view: {xtype: 'nx-coreui-cleanuppolicy-feature'},
      iconConfig: {
        file: 'broom.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        // Show feature if the current user has administrator privileges
        return NX.Permissions.checkExistsWithPrefix('nexus:*');
      },
      weight: 400
    };

    me.callParent();

    me.listen({
      store: {
        '#CleanupPolicy': {
          load: me.reselect
        }
      },
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      component: {
        'nx-coreui-cleanuppolicy-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-cleanuppolicy-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-cleanuppolicy-settings-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-cleanuppolicy-feature button[action=delete]': {
          deleteaction: me.deleteCleanupPolicy,
          afterrender: me.bindDeleteButton
        },
        'nx-coreui-cleanuppolicy-settings-form button[action=preview]': {
          click: me.showPreviewWindow
        },
        'nx-coreui-cleanuppolicy-preview-window gridpanel #filter': {
          search: me.filterPreviewCleanupStore,
          searchcleared: me.clearFilterPreviewCleanupStore
        },
        'nx-coreui-cleanuppolicy-preview-window button[action=preview]': {
          click: me.loadStore
        }
      }
    });
  },

  /**
   * @override
   * Returns a description of cleanup policy suitable to be displayed.
   * @param {NX.coreui.model.CleanupPolicy} model selected model
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   * Load Cleanup Policy policy model into detail tabs.
   * @param {NX.coreui.view.cleanuppolicy.CleanupPolicyList} list cleanup policy grid
   * @param {NX.coreui.model.CleanupPolicy} model selected model
   */
  onSelection: function(list, model) {
    var me = this,
        settingsPanel = me.getSettings();

    me.addMode = false;

    Ext.suspendLayouts();

    if (Ext.isDefined(model)) {
      settingsPanel.loadRecord(model);

      // Set immutable fields to readonly
      Ext.Array.each(settingsPanel.query('field[readOnlyOnUpdate=true]'), function(field) {
        field.setReadOnly(true);
        field.addCls('nx-combo-disabled');
      });
    }

    Ext.resumeLayouts();
  },

  /**
   * @private
   * Delete Cleanup Policy after specialized confirmation.
   */
  deleteCleanupPolicy: function() {
    var me = this,
        selection = Ext.ComponentQuery.query('nx-drilldown-master')[0].getSelectionModel().getSelection(),
        description;

    if (Ext.isDefined(selection) && selection.length > 0) {

      NX.direct.cleanup_CleanupPolicy.usage(selection[0].getId(), function(response) {
        if (Ext.isObject(response) && response.success) {

          description = response.data.repositoryCount > 0 ?
              NX.I18n.format('CleanupPolicies_Delete_Description_Multiple', response.data.repositoryCount) :
              NX.I18n.format('CleanupPolicies_Delete_Description');

          NX.Dialogs.askConfirmation(
              NX.I18n.format('CleanupPolicies_Delete_Title'),
              Ext.htmlEncode(description),
              function() {
                me.deleteModel(selection, description);
              },
              {
                scope: me
              }
          );
        }
      });
    }
  },

  /**
   * @private
   * Delete  Policy.
   */
  deleteModel: function(selection, description) {
    var me = this;

    NX.direct.cleanup_CleanupPolicy.remove(selection[0].getId(), function(response) {
      me.loadStores();

      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('CleanupPolicies_Delete_Success', description),
          type: 'success'
        });
      }
    });

    // Reset the bookmark
    NX.Bookmarks.bookmark(NX.Bookmarks.fromToken(NX.Bookmarks.getBookmark().getSegment(0)));
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    me.addMode = true;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('CleanupPolicies_Create_Title'));
    me.loadCreateWizard(1, Ext.create('widget.nx-coreui-cleanuppolicy-add'));
  },

  getSettingsForm: function() {
    return this.addMode
        ? this.getAddSettingsForm().getForm()
        : this.getEditSettingsForm().getForm();
  },

  showPreviewWindow: function() {
    var me = this, form;

    var componentStore = me.getCleanupPreviewStore();
    componentStore.addListener('load', me.loadPreviewCleanupStore, this);

    //make sure to empty the store so we don't see stale data
    componentStore.clearFilter();
    componentStore.removeAll();

    form = me.getSettingsForm();

    Ext.create('NX.coreui.view.cleanuppolicy.CleanupPolicyPreviewWindow', {
      format: form.findField('format').getValue()
    });
  },

  loadStore: function() {
    var me = this,
        form = me.getSettingsForm(),
        componentList = me.getPreviewComponentList(),
        componentStore = componentList.getStore(),
        repositoryName = me.getPreviewRepositoryComboBox().getValue();

    function translateKey(key) {
      return key === 'isPrerelease' ? 'releaseType' : key;
    }

    function getValueFromForm(form, value) {
      return form.findField(value + 'Enabled').getValue() === true ? form.findField(value).getValue() : null;
    }

    function getCriteria(formExtjs) {
      var elements = formExtjs.owner.down('#criteria').items;
      var criteria = {};

      elements.eachKey(function (k, container) {
        var containerId = container.getItemId();

        if (containerId.indexOf('Container') >= 0) {
          var key = translateKey(containerId.substring(0, containerId.indexOf('Container')));
          var value = getValueFromForm(formExtjs, key);
          if (value !== null) {
            criteria[key] = value;
          }
        }
      });

      return criteria;
    }

    var criterias = getCriteria(form);

    if(repositoryName && Object.keys(criterias).length > 0) {
      me.getPreviewWindowPreviewButton().disable();

      var cleanupPolicy = {
        repositoryName: repositoryName,
        criteria: criterias
      };

      componentStore.addFilter([
        {
          id: 'cleanupPolicy',
          property: 'cleanupPolicy',
          value: JSON.stringify(cleanupPolicy)
        }
      ]);
    }
  },

  loadPreviewCleanupStore: function(store, records, successful) {
    var me = this,
        totalComponentCount = me.getTotalComponentCount(),
        currentComponentCount = me.getCurrentComponentCount(),
        previewWindowCount = me.getPreviewWindowCount();

    var previewWindowPreviewButton = me.getPreviewWindowPreviewButton();
    if (previewWindowPreviewButton) {
      previewWindowPreviewButton.enable();
    }

    if(successful && totalComponentCount && currentComponentCount && previewWindowCount) {
      previewWindowCount.setVisible(true);
      totalComponentCount.setText(store.totalCount);
      currentComponentCount.setText(store.data.length);
    }
  },

  filterPreviewCleanupStore: function(filterTextField, value) {
    var grid = filterTextField.up('grid'),
        store = grid.getStore(),
        emptyText = grid.getView().emptyTextFilter;

    if (!grid.emptyText) {
      grid.emptyText = grid.getView().emptyText;
    }
    grid.getView().emptyText = '<div class="x-grid-empty">' + emptyText.replace(/\$filter/, value) + '</div>';
    grid.getSelectionModel().deselectAll();
    store.addFilter([
      {
        id: 'filter',
        property: 'filter',
        value: value
      }
    ]);
  },

  clearFilterPreviewCleanupStore: function(filterTextField) {
    var grid = filterTextField.up('grid'),
        store = grid.getStore();

    if (grid.emptyText) {
      grid.getView().emptyText = grid.emptyText;
    }
    grid.getSelectionModel().deselectAll();
    // we have to remove filter directly as store#removeFilter() does not work when store#remoteFilter = true
    if (store.filters.removeAtKey('filter')) {
      if (store.filters.length) {
        store.filter();
      }
      else {
        store.clearFilter();
      }
    }
  },

  /**
   * @private
   */
  onSettingsSubmitted: function() {
    this.loadStores();
  }
  ,

  /**
   * @private
   */
  bindDeleteButton: function(button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:repository-admin:*:*:delete'),
        {
          satisfied: function() {
            button.enable();
          },
          unsatisfied: function() {
            button.disable();
          }
        }
    );
  }
});

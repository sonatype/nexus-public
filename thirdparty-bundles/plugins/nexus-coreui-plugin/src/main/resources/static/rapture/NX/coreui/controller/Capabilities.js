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
 * Capabilities controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Capabilities', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n',
    'NX.ext.grid.column.Renderers'
  ],
  masters: [
    'nx-coreui-capability-list'
  ],
  stores: [
    'Capability',
    'CapabilityType'
  ],
  models: [
    'Capability'
  ],
  views: [
    'capability.CapabilityAdd',
    'capability.CapabilityFeature',
    'capability.CapabilityList',
    'capability.CapabilitySummary',
    'capability.CapabilitySelectType',
    'capability.CapabilitySettings',
    'capability.CapabilitySettingsForm',
    'capability.CapabilityStatus',
    'capability.CapabilityAbout',
    'formfield.SettingsFieldSet'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-capability-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-capability-list' },
    { ref: 'summaryTab', selector: 'nx-coreui-capability-summary' },
    { ref: 'settingsTab', selector: 'nx-coreui-capability-settings' },
    { ref: 'summaryPanel', selector: '#nx-coreui-capability-summary-subsection' },
    { ref: 'statusPanel', selector: 'nx-coreui-capability-status' },
    { ref: 'aboutPanel', selector: 'nx-coreui-capability-about' },
    { ref: 'notesPanel', selector: '#nx-coreui-capability-notes-subsection' },
    { ref: 'settingsPanel', selector: 'nx-coreui-capability-settings-form' }
  ],
  icons: {
    'capability-default': {
      file: 'brick.png',
      variants: ['x16', 'x32']
    },
    'capability-active': {
      file: 'brick_valid.png',
      variants: ['x16', 'x32']
    },
    'capability-disabled': {
      file: 'brick_grey.png',
      variants: ['x16', 'x32']
    },
    'capability-error': {
      file: 'brick_error.png',
      variants: ['x16', 'x32']
    },
    'capability-passive': {
      file: 'brick_error.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:capabilities',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/System/Capabilities',
      text: NX.I18n.get('Capabilities_Text'),
      description: NX.I18n.get('Capabilities_Description'),
      view: {xtype: 'nx-coreui-capability-feature'},
      iconConfig: {
        file: 'brick.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:capabilities:read');
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Capability': {
          load: me.onCapabilityLoad
        }
      },
      component: {
        'nx-coreui-capability-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-capability-list button[action=new]': {
          click: me.showSelectTypePanel
        },
        'nx-coreui-capability-feature button[action=enable]': {
          runaction: me.enableCapability,
          afterrender: me.bindEnableButton
        },
        'nx-coreui-capability-feature button[action=disable]': {
          runaction: me.disableCapability,
          afterrender: me.bindDisableButton
        },
        'nx-coreui-capability-settings button[action=save]': {
          click: me.updateCapability
        },
        'nx-coreui-capability-add button[action=add]': {
          click: me.createCapability
        },
        'nx-coreui-capability-selecttype': {
          cellclick: me.showAddPanel
        },
        'nx-coreui-capability-summary form': {
          submitted: me.onSettingsSubmitted
        }
      }
    });
  },

  /**
   * @override
   * Returns a description of capability suitable to be displayed.
   * @param {NX.coreui.model.Capability} model selected model
   */
  getDescription: function(model) {
    var description = model.get('typeName');
    if (model.get('description')) {
      description += ' - ' + model.get('description');
    }
    return description;
  },

  /**
   * @override
   * Load capability model into detail tabs.
   * @param {NX.coreui.view.capability.CapabilityList} list capability grid
   * @param {NX.coreui.model.Capability} model selected model
   */
  onSelection: function(list, model) {
    var me = this,
        capabilityTypeModel;

    if (Ext.isDefined(model)) {
      capabilityTypeModel = me.getStore('CapabilityType').getById(model.get('typeId'));

      me.eventuallyShowWarning(model);
      me.showSummary(model);
      me.showSettings(model);
      me.showStatus(model);
      me.showAbout(capabilityTypeModel);
    }
  },

  /**
   * @private
   * Displays a warning message if capability is enabled but is not active.
   * @param {NX.coreui.model.Capability} model capability model
   */
  eventuallyShowWarning: function(model) {
    var me = this;

    if (model.get('enabled') && !model.get('active')) {
      me.showWarning(model.get('stateDescription'));
    }
    else {
      me.clearWarning();
    }
  },

  /**
   * @private
   * Displays capability summary.
   * @param {NX.coreui.model.Capability} model capability model
   */
  showSummary: function(model) {
    var summary = this.getSummaryTab(),
        info = {};

    info[NX.I18n.get('Capabilities_TypeName_Text')] = model.get('typeName');
    info[NX.I18n.get('Capabilities_Description_Text')] = model.get('description');
    info[NX.I18n.get('Capabilities_State_Text')] = Ext.String.capitalize(model.get('state'));

    if (Ext.isDefined(model.get('tags'))) {
      Ext.apply(info, model.get('tags'));
    }

    summary.showInfo(info);
    summary.down('form').loadRecord(model);
  },

  /**
   * @private
   * Displays capability settings.
   * @param {NX.coreui.model.Capability} model capability model
   */
  showSettings: function(model) {
    this.getSettingsTab().loadRecord(model);
  },

  /**
   * @private
   * Displays capability status.
   * @param {NX.coreui.model.Capability} model capability model
   */
  showStatus: function(model) {
    this.getStatusPanel().showStatus(model.get('status'));
  },

  /**
   * @private
   * Displays capability about.
   * @param {NX.coreui.model.CapabilityType} capabilityTypeModel capability type model
   */
  showAbout: function(capabilityTypeModel) {
    this.getAboutPanel().showAbout(capabilityTypeModel ? capabilityTypeModel.get('about') : undefined);
  },

  /**
   * @private
   */
  showSelectTypePanel: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Capabilities_Select_Title'));
    me.loadCreateWizard(1, true, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-capability-selecttype',
          flex: 1
        }
      ]
    }));
  },

  /**
   * @private
   */
  showAddPanel: function(list, td, cellIndex, model) {
    var me = this,
        panel;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(2, NX.I18n.format('Capabilities_Create_Title', model.get('name')));
    me.loadCreateWizard(2, true, panel = Ext.create('widget.nx-coreui-capability-add'));
    var m = me.getCapabilityModel().create({ typeId: model.getId(), enabled: true });
    panel.down('nx-settingsform').loadRecord(m);
  },

  /**
   * @override
   * @protected
   * Enable 'New' button when user has 'create' permission and there is at least one capability type.
   */
  bindNewButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(this.permission + ':create'),
            NX.Conditions.storeHasRecords('CapabilityType')
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Enable 'Enable' button when user has 'update' permission and capability is not enabled.
   */
  bindEnableButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:capabilities:update'),
            NX.Conditions.gridHasSelection('nx-coreui-capability-list', function(model) {
              return !model.get('enabled');
            })
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Enable 'Disable' button when user has 'update' permission and capability is enabled.
   */
  bindDisableButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:capabilities:update'),
            NX.Conditions.gridHasSelection('nx-coreui-capability-list', function(model) {
              return model.get('enabled');
            })
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Creates a capability.
   */
  createCapability: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    NX.direct.capability_Capability.create(values, function(response) {
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('Capabilities_Create_Success',
                me.getDescription(me.getCapabilityModel().create(response.data))),
            type: 'success'
          });
          me.getStore('Capability').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @private
   * Updates capability.
   */
  updateCapability: function(button) {
    var me = this,
        form = button.up('form'),
        values = form.getValues();

    me.getContent().getEl().mask(NX.I18n.get('Capabilities_Update_Mask'));
    NX.direct.capability_Capability.update(values, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('Capabilities_Update_Success',
                me.getDescription(me.getCapabilityModel().create(response.data))),
            type: 'success'
          });
          form.fireEvent('submitted', form);
          me.getStore('Capability').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @override
   * Delete capability.
   * @param {NX.coreui.model.CapabilityType} model capability to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.capability_Capability.remove(model.getId(), function(response) {
      me.getStore('Capability').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Capabilities_Delete_Success', description),
          type: 'success'
        });
      }
    });
  },

  /**
   * @private
   */
  onSettingsSubmitted: function() {
    this.getCapabilityStore().load();
  },

  /**
   * @private
   * Enables selected capability.
   */
  enableCapability: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        model, modelId, description;

    modelId = decodeURIComponent(bookmark.getSegment(1));
    model = me.getList().getStore().getById(modelId);
    description = me.getDescription(model);

    me.getContent().getEl().mask(NX.I18n.get('Capabilities_Enable_Mask'));
    NX.direct.capability_Capability.enable(model.getId(), function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        me.getStore('Capability').load();
        NX.Messages.add({
          text: NX.I18n.format('Capabilities_Enable_Text', description),
          type: 'success'
        });
      }
    });
  },

  /**
   * @private
   * Disables selected capability.
   */
  disableCapability: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        model, modelId, description;

    modelId = decodeURIComponent(bookmark.getSegment(1));
    model = me.getList().getStore().getById(modelId);
    description = me.getDescription(model);

    me.getContent().getEl().mask(NX.I18n.get('Capabilities_Disable_Mask'));
    NX.direct.capability_Capability.disable(model.getId(), function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        me.getStore('Capability').load();
        NX.Messages.add({
          text: NX.I18n.format('Capabilities_Disable_Text', description),
          type: 'success'
        });
      }
    });
  },

  //
  // Dynamic Tags
  //

  /**
   * Cached dynamic tag names.
   *
   * @private
   * @property {String[]}
   */
  dynamicTags: [],

  /**
   * Synchronize tags from loaded capabilities with capability/model and grid columns.
   *
   * @private
   * @param {NX.coreui.store.Capability} store
   */
  onCapabilityLoad: function(store) {
    var me = this,
        list,
        tags;

    // discover dynamic capability tags
    tags = me.discoverDynamicTags(store);

    // mutate model if tags have changed
    if (!Ext.Array.equals(tags, me.dynamicTags)) {
      me.addDynamicTagFieldsToModel(tags);
      // remember the current set of tags since they changed
      me.dynamicTags = tags;
    }

    // apply tag data to data in store
    me.addDynamicTagDataToStoreRecords(store, tags);

    // add columns for tags if the list is ready
    list = me.getList();
    if (list && list.originalColumns) {
      me.addDynamicTagsToGrid(list, tags);
    }
    else {
      //<if debug>
      me.logDebug('List not ready yet to mutate grid columns');
      //</if>
    }

    me.reselect();
  },

  /**
   * Discover the full set of dynamic tags.
   *
   * @private
   * @param {NX.coreui.store.Capability} store
   * @return {String[]} Sorted list of dynamic capability tag names.
   */
  discoverDynamicTags: function(store) {
    var tags = [];

    store.each(function(model) {
      Ext.Object.each(model.get('tags'), function(key) {
        if (tags.indexOf(key) === -1) {
          tags.push(key);
        }
      });
    });
    Ext.Array.sort(tags);

    //<if debug>
    this.logDebug('Discovered dynamic tags:', tags);
    //</if>

    return tags;
  },

  /**
   * Mutates the Capability model adding dynamic 'tag$' fields.
   *
   * @private
   * @param {String[]} tags
   */
  addDynamicTagFieldsToModel: function(tags) {
    var me = this,
        model = me.getCapabilityModel(),
        fields = model.prototype.fields.getRange();

    Ext.Array.each(tags, function(entry) {
      fields.push({
        name: 'tag$' + entry,
        type: 'string'
      });
    });
    model.setFields(fields);

    //<if debug>
    me.logDebug('Dynamic tag fields added to Capability model');
    //</if>
  },

  /**
   * Adds data to Capability store records for dynamic 'tag$' fields.
   *
   * @private
   * @param {NX.coreui.store.Capability} store
   * @param {String[]} tags
   */
  addDynamicTagDataToStoreRecords: function(store, tags) {
    store.each(function(model) {
      // apply dynamic tag data to 'tag$' fields if the record has any tag data
      var data = model.get('tags');
      if (data) {
        Ext.Array.each(tags, function (entry) {
          model.set('tag$' + entry, data[entry]);
        });
      }
    });
    store.commitChanges();

    //<if debug>
    this.logDebug('Dynamic tag data applied Capability store records');
    //</if>
  },

  /**
   * Adds dynamic tag columns to CapabilityList panel.
   *
   * @private
   * @param {NX.coreui.view.capability.CapabilityList} panel
   * @param {String[]} tags
   */
  addDynamicTagsToGrid: function(panel, tags) {
    var columns = Ext.Array.clone(panel.originalColumns),
        tagColumns = [];

    // create new colums for each dynamic tag
    Ext.Array.each(tags, function(entry) {
      tagColumns.push({
        text: entry,
        dataIndex: 'tag$' + entry,
        flex: 1,
        renderer: NX.ext.grid.column.Renderers.optionalData
      });
    });

    // FIXME: insert after icon and category; this may not behave as desired if user rearranges columns
    Ext.Array.insert(columns, 2, tagColumns);
    panel.reconfigure(null, columns);

    //<if debug>
    this.logDebug('Dynamic tag columns added to grid');
    //</if>
  }

});

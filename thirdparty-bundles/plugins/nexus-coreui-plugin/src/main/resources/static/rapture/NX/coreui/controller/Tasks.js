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
 * Tasks controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Tasks', {
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
    'nx-coreui-task-list'
  ],
  stores: [
    'Task',
    'TaskType'
  ],
  models: [
    'Task'
  ],
  views: [
    'task.TaskAdd',
    'task.TaskFeature',
    'task.TaskList',
    'task.TaskSelectType',
    'task.TaskScheduleFieldSet',
    'task.TaskScheduleAdvanced',
    'task.TaskScheduleDaily',
    'task.TaskScheduleHourly',
    'task.TaskScheduleManual',
    'task.TaskScheduleMonthly',
    'task.TaskScheduleOnce',
    'task.TaskScheduleWeekly',
    'task.TaskSummary',
    'task.TaskStatus',
    'task.TaskSettings',
    'task.TaskSettingsForm',
    'formfield.SettingsFieldSet'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-task-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-task-list' },
    { ref: 'summary', selector: 'nx-coreui-task-summary' },
    { ref: 'settings', selector: 'nx-coreui-task-settings' }
  ],
  icons: {
    'task-default': {
      file: 'time.png',
      variants: ['x16', 'x32']
    }
  },

  permission: 'nexus:tasks',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/System/Tasks',
      text: NX.I18n.get('Tasks_Text'),
      description: NX.I18n.get('Tasks_Description'),
      view: {xtype: 'nx-coreui-task-feature'},
      iconConfig: {
        file: 'time.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:tasks:read');
      }
    };

    me.callParent();

    me.listen({
      store: {
        '#Task': {
          load: me.reselect
        }
      },
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      component: {
        'nx-coreui-task-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-task-list button[action=new]': {
          click: me.showSelectTypePanel
        },
        'nx-coreui-task-feature button[action=run]': {
          runaction: me.runTask,
          afterrender: me.bindRunButton
        },
        'nx-coreui-task-feature button[action=stop]': {
          runaction: me.stopTask,
          afterrender: me.bindStopButton
        },
        'nx-coreui-task-settings button[action=save]': {
          click: me.updateTask
        },
        'nx-coreui-task-add button[action=add]': {
          click: me.createTask
        },
        'nx-coreui-task-selecttype': {
          cellclick: me.showAddPanel
        }
      }
    });
  },

  /**
   * @override
   * Returns a description of task suitable to be displayed.
   * @param {NX.coreui.model.Task} model selected model
   */
  getDescription: function(model) {
    return model.get('name') + ' (' + model.get('typeName') + ')';
  },

  /**
   * @override
   * Load task model into detail tabs.
   * @param {NX.coreui.view.task.TaskList} list task grid
   * @param {NX.coreui.model.Task} model selected model
   */
  onSelection: function(list, model) {
    var me = this,
        settings = me.getSettings(),
        taskTypeModel;

    if (Ext.isDefined(model)) {
      me.showSummary(model);
      taskTypeModel = me.getStore('TaskType').getById(model.get('typeId'));
      if (taskTypeModel) {
        if (!settings) {
          me.addTab({ xtype: 'nx-coreui-task-settings', title: NX.I18n.get('Tasks_Settings_Title'), weight: 20 });
        }
        me.showSettings(model);
      }
      else {
        if (settings) {
          me.removeTab(settings);
        }
      }
    }
  },

  /**
   * @private
   * Displays task summary.
   * @param {NX.coreui.model.Task} model task model
   */
  showSummary: function(model) {
    var info = {},
        me = this,
        summary = me.getSummary();

    info[NX.I18n.get('Tasks_ID_Info')] = model.getId();
    info[NX.I18n.get('Tasks_Name_Info')] = model.get('name');
    info[NX.I18n.get('Tasks_Type_Info')] = model.get('typeName');
    info[NX.I18n.get('Tasks_Status_Info')] = model.get('statusDescription');
    info[NX.I18n.get('Tasks_NextRun_Info')] = model.get('nextRun');
    info[NX.I18n.get('Tasks_LastRun_Info')] = model.get('lastRun');
    info[NX.I18n.get('Tasks_LastResult_Info')] = model.get('lastRunResult');

    summary.showInfo(info);
    me.maybeShowSummaryStatuses(model);
  },

  /**
   * @private
   * Displays task status summary if there are clustered task states.
   * @param model the task model to get states from
   */
  maybeShowSummaryStatuses: function(model) {
    var me = this;

    if (!model.clusteredTaskStatesStore) {
      me.getSummary().getStatuses().hide();
    }
    else {
      me.getSummary().getStatuses().show();
      me.getSummary().getStatuses().getGrid().reconfigure(model.clusteredTaskStatesStore);
    }
  },

  /**
   * @private
   * Displays task settings.
   * @param {NX.coreui.model.Task} model task model
   */
  showSettings: function(model) {
    this.getSettings().loadRecord(model);
  },

  /**
   * @private
   */
  showSelectTypePanel: function() {
    var me = this;

    //clear any filters that may previously have been applied
    me.getStore('TaskType').clearFilter();

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Tasks_Select_Title'));
    me.loadCreateWizard(1, true, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch'
      },
      items: {
        xtype: 'nx-coreui-task-selecttype',
        flex: 1
      }
    }));
  },

  /**
   * @private
   */
  showAddPanel: function(list, td, cellIndex, model) {
    var me = this,
      panel;

    // Show the second panel in the create wizard, and set the breadcrumb
    me.setItemName(2, NX.I18n.format('Tasks_Create_Title', model.get('name')));
    me.loadCreateWizard(2, true, panel = Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-task-add',
          flex: 1
        }
      ]
    }));
    panel.down('nx-settingsform').loadRecord(me.getTaskModel().create({typeId: model.getId(), enabled: true}));
  },

  /**
   * @private
   */
  updateTask: function(button) {
    var me = this,
      form = button.up('form'),
      values = form.getValues();

    me.getContent().getEl().mask(NX.I18n.get('Tasks_Update_Mask'));
    NX.direct.coreui_Task.update(values, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('Tasks_Update_Success',
              me.getDescription(me.getTaskModel().create(response.data))),
            type: 'success'
          });
          form.fireEvent('submitted', form);
          me.getStore('Task').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @private
   */
  createTask: function(button) {
    var me = this,
      form = button.up('form'),
      values = form.getValues();

    NX.direct.coreui_Task.create(values, function(response) {
      if (Ext.isObject(response)) {
        if (response.success) {
          NX.Messages.add({
            text: NX.I18n.format('Tasks_Create_Success',
              me.getDescription(me.getTaskModel().create(response.data))),
            type: 'success'
          });
          me.getStore('Task').load();
        }
        else if (Ext.isDefined(response.errors)) {
          form.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @override
   * @protected
   * Enable 'New' when user has 'create' permission and there is at least one task type.
   */
  bindNewButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:tasks:create'),
            NX.Conditions.storeHasRecords('TaskType')
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @override
   * @private
   * Enable 'Run' when user has 'read' permission and task is 'runnable'.
   */
  bindRunButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:tasks:start'),
            NX.Conditions.gridHasSelection('nx-coreui-task-list', function(model) {
              return model.get('runnable');
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
   * @override
   * @private
   * Enable 'Stop' when user has 'delete' permission and task is 'stoppable'.
   */
  bindStopButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:tasks:stop'),
            NX.Conditions.gridHasSelection('nx-coreui-task-list', function(model) {
              return model.get('stoppable');
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
   * @override
   * Delete task.
   * @param model task to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Task.remove(model.getId(), function(response) {
      me.getStore('Task').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Tasks_Delete_Success', description), type: 'success'
        });
      }
    });
  },

  /**
   * @override
   * Run selected task.
   */
  runTask: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        model, modelId, description;

    // Reload the form
    me.getStore('Task').load();

    modelId = decodeURIComponent(bookmark.getSegment(1));
    model = me.getList().getStore().getById(modelId);
    description = me.getDescription(model);

    if (model) {
      description = me.getDescription(model);
      NX.Dialogs.askConfirmation(NX.I18n.get('Tasks_RunConfirm_Title'),
        NX.I18n.format('Tasks_RunConfirm_HelpText', description), function() {
        me.getContent().getEl().mask(NX.I18n.get('Tasks_Run_Mask'));
        NX.direct.coreui_Task.run(model.getId(), function(response) {
          me.getContent().getEl().unmask();
          if (Ext.isObject(response) && response.success) {
            me.getStore('Task').load();
            NX.Messages.add({
              text: NX.I18n.format('Tasks_Run_Success', description), type: 'success'
            });
          }
        });
      }, {scope: me});
    }
  },

  /**
   * @override
   * Stop selected task.
   */
  stopTask: function() {
    var me = this,
      bookmark = NX.Bookmarks.getBookmark(),
      model, modelId, description;

    // Reload the form
    me.getStore('Task').load();

    modelId = decodeURIComponent(bookmark.getSegment(1));
    model = me.getList().getStore().getById(modelId);
    description = me.getDescription(model);

    if (model) {
      description = me.getDescription(model);
      NX.Dialogs.askConfirmation(NX.I18n.get('Tasks_StopConfirm_Title'),
        NX.I18n.format('Tasks_StopConfirm_HelpText', description), function() {
        me.getContent().getEl().mask(NX.I18n.get('Tasks_Stop_Mask'));
        NX.direct.coreui_Task.stop(model.getId(), function(response) {
          me.getContent().getEl().unmask();
          if (Ext.isObject(response) && response.success) {
            me.getStore('Task').load();
            NX.Messages.add({
              text: NX.I18n.format('Tasks_Stop_Success', description), type: 'success'
            });
          }
        });
      }, { scope: me });
    }
  }
});

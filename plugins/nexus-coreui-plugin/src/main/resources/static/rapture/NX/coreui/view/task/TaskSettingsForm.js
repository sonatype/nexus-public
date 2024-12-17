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
 * Task "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.task.TaskSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-task-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'hiddenfield',
        name: 'id'
      },
      {
        xtype: 'hiddenfield',
        name: 'typeId'
      },
      {
        xtype: 'checkbox',
        fieldLabel: NX.I18n.get('Task_TaskSettingsForm_Enabled_FieldLabel'),
        helpText: NX.I18n.get('Task_TaskSettingsForm_Enabled_HelpText'),
        name: 'enabled',
        allowBlank: false,
        checked: true,
        editable: true
      },
      {
        name: 'name',
        fieldLabel: NX.I18n.get('Task_TaskSettingsForm_Name_FieldLabel'),
        helpText: NX.I18n.get('Task_TaskSettingsForm_Name_HelpText'),
        transformRawValue: Ext.htmlDecode
      },
      {
        xtype: 'nx-email',
        name: 'alertEmail',
        fieldLabel: NX.I18n.get('Task_TaskSettingsForm_Email_FieldLabel'),
        helpText: NX.I18n.get('Task_TaskSettingsForm_Email_HelpText'),
        allowBlank: true
      },
      {
        xtype: 'combo',
        name: 'notificationCondition',
        fieldLabel: NX.I18n.get('Task_TaskSettingsForm_NotificationCondition_FieldLabel'),
        helpText: NX.I18n.get('Task_TaskSettingsForm_NotificationCondition_HelpText'),
        editable: false,
        store: [
          ['FAILURE', NX.I18n.get('Task_TaskSettingsForm_NotificationCondition_FailureItem')],
          ['SUCCESS_FAILURE', NX.I18n.get('Task_TaskSettingsForm_NotificationCondition_SuccessFailureItem')]
        ],
        value: 'FAILURE'
      },
      {xtype: 'nx-coreui-formfield-settingsfieldset'},
      {xtype: 'nx-coreui-task-schedulefieldset'}
    ];

    me.editableMarker = NX.I18n.get('Task_TaskSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:tasks:update'),
        NX.Conditions.formHasRecord('nx-coreui-task-settings-form', function(model) {
          return model.get('schedule') !== 'internal';
        })
    );

    me.callParent();
  },

  /**
   * @override
   * Additionally, gets value of properties.
   */
  getValues: function() {
    var me = this,
        values = me.getForm().getFieldValues(),
        task = {
          id: values.id,
          typeId: values.typeId,
          enabled: values.enabled ? true : false,
          name: values.name,
          alertEmail: values.alertEmail,
          notificationCondition: values.notificationCondition,
          schedule: values.schedule
        };

    task.properties = me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values);
    if (this.isPlanReconcileTask()) {
      const scopeFieldsSet = me.down('nx-coreui-task-scopefieldset');
      if (scopeFieldsSet) {
        Object.assign(task.properties, scopeFieldsSet.exportProperties());
      }
    }

    task.recurringDays = me.down('nx-coreui-task-schedulefieldset').getRecurringDays();
    task.startDate = me.down('nx-coreui-task-schedulefieldset').getStartDate();
    task.timeZoneOffset = Ext.Date.format(new Date(), 'P');
    if (task.startDate) {
      task.startDate = task.startDate.toJSON();
    }

    if (task.schedule === 'advanced') {
      task.cronExpression = values.cronExpression;
    }

    return task;
  },

  /**
   * @override
   * Additionally, sets properties values.
   */
  loadRecord: function(model) {
    var me = this,
        taskTypeModel = NX.getApplication().getStore('TaskType').getById(model.get('typeId')),
        settingsFieldSet = me.down('nx-coreui-formfield-settingsfieldset'),
        scheduleFieldSet = me.down('nx-coreui-task-schedulefieldset'),
        formFields;

    this.callParent(arguments);

    this.resetTaskForm();

    if (taskTypeModel) {
      formFields = taskTypeModel.get('formFields');
      if (!taskTypeModel.get('concurrentRun')) {
        var scheduleFieldSetCombo = this.down('combo[name="schedule"]');
        scheduleFieldSetCombo.setStore([
          ['manual', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_ManualItem')],
          ['once', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_OnceItem')]
        ]);
      }

      if (taskTypeModel.get('id') === 'blobstore.planReconciliation') {
        scheduleFieldSet.setHidden(true);
      } else {
        scheduleFieldSet.setHidden(false);
      }

      Ext.each(formFields, function(field) {
        var properties = model.get('properties');
        if (properties && !properties.hasOwnProperty(field.id)) {
          properties[field.id] = null;
          model.set('properties', properties, {dirty: false});
        }
      });

      settingsFieldSet.importProperties(model.get('properties'), formFields);
      scheduleFieldSet.setRecurringDays(model.get('recurringDays'));
      scheduleFieldSet.setStartDate(model.get('startDate'));

      if (this.isPlanReconcileTask()) {
        const scopeFieldsSet = me.down('nx-coreui-task-scopefieldset');
        if (scopeFieldsSet) {
          scopeFieldsSet.importProperties(model.get('properties'));
        }
      }

      this.maybeMakeReadOnly(model);
    }
  },

  /**
   * To Reset Task Form including Days to run checkboxes.
   */
  resetTaskForm: function() {
    var me = this,
        checkboxes = me.query('checkbox[recurringDayValue]');

    Ext.Array.each(checkboxes, function(checkbox) {
      checkbox.originalValue = false;
    });
    me.form.reset();
  },

  /**
   * @override
   * Additionally, marks invalid properties.
   */
  markInvalid: function(errors) {
    this.down('nx-coreui-formfield-settingsfieldset').markInvalid(errors);
  },

  /**
   * Make task setting UI read-only based on the task settings.
   */
  maybeMakeReadOnly: function(model) {
    const readOnly = this.shouldMakeUiReadOnly(model);
    const me = this,
        taskComponent = me.up('nx-coreui-task-feature');

    if (taskComponent) {
      const deleteButton = taskComponent.down('button[action=delete]'),
          runButton = taskComponent.down('button[action=run]');
      if (deleteButton && runButton) {
        Ext.defer(function() {
          if (readOnly) {
            deleteButton.disable();
            runButton.disable();
          }
          else {
            deleteButton.enable();
            // check current task state before enabling run button
            const taskReadyToRun = model.get('runnable');
            if (taskReadyToRun === true) {
              runButton.enable();
            }
          }
        }, 100);
      }

      const editables = me.query('field'),
          itemSelectors = me.query('nx-itemselector');

      Ext.each(editables, function(editable) {
        if (readOnly) {
          editable.disable();
        }
        else {
          editable.enable();
        }
      });
      Ext.each(itemSelectors, function(itemSelector) {
        if (readOnly) {
          itemSelector.disable();
        }
        else {
          itemSelector.enable();
        }
      });
    }
  },

  /**
   * Decide make UI read-only for given task
   */
  shouldMakeUiReadOnly: function(model) {
    return model.get('isReadOnlyUi') === true;
  },

  isPlanReconcileTask: function() {
    const hiddenTypeId = this.down('hiddenfield[name=typeId]');
    if (hiddenTypeId) {
      const typeId = hiddenTypeId.value;
      return typeId === 'blobstore.planReconciliation'
    }
    return false;
  }
});

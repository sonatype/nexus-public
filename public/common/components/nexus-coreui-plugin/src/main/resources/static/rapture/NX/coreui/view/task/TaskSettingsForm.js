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
  xtype: 'settingForm',
  alias: 'widget.nx-coreui-task-settings-form',
  requires: ['NX.Conditions', 'NX.I18n'],

  /**
   * @override
   */
  initComponent: function () {
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
        xtype: 'panel',
        name: 'topPanel',
        ui: 'nx-drilldown-message',
        cls: 'nx-drilldown-info',
        iconCls: NX.Icons.cls('drilldown-info', 'x16'),
        title: NX.I18n.format('Task_TaskSettingsForm_Top_Panel_HelpText'),
        hidden: true
      },
      {
        xtype: 'label',
        name: 'planInformationDisclaimer',
        html: NX.I18n.get('Task_TaskSettingsForm_PlanInformation_Disclaimer'),
        hidden: true
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
      {
        xtype: 'nx-coreui-formfield-settingsfieldset',
        name: 'taskRepositories'
      },
      {
        xtype: 'nx-coreui-task-schedulefieldset',
        name: 'taskScheduleTime'
      },
      {
        xtype: 'panel',
        name: 'bottomPanel',
        ui: 'nx-drilldown-message',
        cls: 'nx-drilldown-info',
        iconCls: NX.Icons.cls('drilldown-info', 'x16'),
        title: NX.I18n.format('Task_TaskSettingsForm_Bottom_Panel_HelpText'),
        hidden: true
      }
    ];

    me.editableMarker = NX.I18n.get('Task_TaskSettingsForm_Update_Error');

    me.editableCondition =
      me.editableCondition ||
      NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:tasks:update'),
        NX.Conditions.formHasRecord('nx-coreui-task-settings-form', function (model) {
          return model.get('schedule') !== 'internal';
        })
      );

    me.callParent();
  },

  /**
   * @override
   * Additionally, gets value of properties.
   */
  getValues: function () {
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

  startPreviousPlanComponent: function (model) {
    var me = this;
    me.data = {
      model: model
    };
    Ext.defer(function () {
      me.down('[name=reconcile-plan-component]').startComponent(model);
    }, 100);
  },

  /**
   * @override
   * Additionally, sets properties values.
   */
  loadRecord: function (model) {
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

      this.initScheduleFieldSet(taskTypeModel, scheduleFieldSet);
      this.initAlertBanners(taskTypeModel);

      Ext.each(formFields, function (field) {
        var properties = model.get('properties');
        if (properties && !properties.hasOwnProperty(field.id)) {
          properties[field.id] = (field.initialValue !== undefined && field.initialValue !== null && field.initialValue !== '')
              ? field.initialValue
              : null;
          model.set('properties', properties, { dirty: false });
        }
        if(!properties && field.id === "name" && field.initialValue) {
          const nameTextField = me.down('[name="name"]');
          if(nameTextField) {
            nameTextField.setValue(field.initialValue);
          }
        }
      });

      settingsFieldSet.importProperties(model.get('properties'), formFields);
      scheduleFieldSet.setRecurringDays(model.get('recurringDays'));
      scheduleFieldSet.setStartDate(model.get('startDate'));

      this.maybeMakeReadOnly(model);

      // Show disclaimer only for Data Repair Task (Plan Reconciliation)
      const disclaimerLabel = me.down('[name="planInformationDisclaimer"]');
      if (disclaimerLabel) {
        disclaimerLabel.setVisible(this.isPlanReconcileTask() || this.isExecutePlanReconcileTask());
      }

      if (this.isPlanReconcileTask()) {
        this.handlePlanReconcileTask(model, me);
      } else if (this.isExecutePlanReconcileTask()) {
        this.renderExecutePlanFields(model, me, settingsFieldSet);
      }
    }
  },
  initScheduleFieldSet: function (taskTypeModel, scheduleFieldSet) {
    if (taskTypeModel.get('id') === 'blobstore.planReconciliation') {
      scheduleFieldSet.setHidden(true);
      var scheduleCombo = this.down('[name="schedule"]');
      if (scheduleCombo) {
        scheduleCombo.setValue('manual');
      }
    } else {
      scheduleFieldSet.setHidden(false);
    }
  },

  /**
   * To Reset Task Form including Days to run checkboxes.
   */
  resetTaskForm: function () {
    var me = this,
      checkboxes = me.query('checkbox[recurringDayValue]');

    Ext.Array.each(checkboxes, function (checkbox) {
      checkbox.originalValue = false;
    });
    me.form.reset();
  },

  /**
   * @override
   * Additionally, marks invalid properties.
   */
  markInvalid: function (errors) {
    this.down('nx-coreui-formfield-settingsfieldset').markInvalid(errors);
  },

  disableComponent: function (component, disabled) {
    if (component != null) {
      component.setDisabled(disabled);
    }
  },

  /**
   * Make task setting UI read-only based on the task settings.
   */
  maybeMakeReadOnly: function (model) {
    const readOnly = this.shouldMakeUiReadOnly(model);
    const me = this,
      taskComponent = me.up('nx-coreui-task-feature');

    if (taskComponent) {
      const deleteButton = taskComponent.down('button[action=delete]'),
        runButton = taskComponent.down('button[action=run]');
      if (deleteButton && runButton) {
        Ext.defer(function () {
          if (readOnly) {
            deleteButton.disable();
            runButton.disable();
          } else {
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

      Ext.each(editables, function (editable) {
        if (readOnly) {
          editable.disable();
        } else {
          editable.enable();
        }
      });
      Ext.each(itemSelectors, function (itemSelector) {
        if (readOnly) {
          itemSelector.disable();
        } else {
          itemSelector.enable();
        }
      });
    }
  },

  /**
   * Decide make UI read-only for given task
   */
  shouldMakeUiReadOnly: function (model) {
    return model.get('isReadOnlyUi') === true;
  },

  isPlanReconcileTask: function () {
    const hiddenTypeId = this.down('hiddenfield[name=typeId]');
    if (hiddenTypeId) {
      const typeId = hiddenTypeId.value;
      return typeId === 'blobstore.planReconciliation';
    }
    return false;
  },

  isExecutePlanReconcileTask: function () {
    const hiddenTypeId = this.down('hiddenfield[name=typeId]');
    if (hiddenTypeId) {
      const typeId = hiddenTypeId.value;
      return typeId === 'blobstore.executeReconciliationPlan';
    }
    return false;
  },
  handlePlanReconcileTask: function (model, me) {
    const scopeFieldsSet = me.down('nx-coreui-task-scopefieldset');
    if (scopeFieldsSet) {
      if (model.get('properties') == null) {
        model.set('properties', {taskScope: 'duration', sinceMinutes: '30'});
      }
      scopeFieldsSet.importProperties(model.get('properties'));
    }
  },
  renderExecutePlanFields: function (model, me, settingsFieldSet) {
    const scopeFieldsSet = me.down('nx-coreui-task-scopefieldset');

    if (!scopeFieldsSet) {
      return;
    }

    scopeFieldsSet.importProperties(model.get('properties'));
    this.disableComponent(scopeFieldsSet.down('[name="taskScope"]'), true);
    const scopeDates = me.down('nx-coreui-task-scope-dates');

    if (scopeDates) {
      scopeDates.importProperties(model.get('properties'));
      const startDateComponent = scopeDates.down('[name="reconcileStartDate"]');
      const endDateComponent = scopeDates.down('[name="reconcileEndDate"]');

      this.disableComponent(startDateComponent, true);
      this.disableComponent(endDateComponent, true);
    }

    const blobStoreFiledSet = settingsFieldSet.down('[name="property_blobstoreName"]');
    const repositoryFieldSet = settingsFieldSet.down('[name="property_repositoryName"]');

    this.disableComponent(blobStoreFiledSet, true);
    this.disableComponent(repositoryFieldSet, true);

    if (blobStoreFiledSet.items) {
      blobStoreFiledSet.items.each(function (childComponent) {
        if (childComponent.title === 'Available' || childComponent.xtype === 'toolbar') {
          childComponent.setHidden(true); // Disable the multiselect field
        }
      });
    }

    if (repositoryFieldSet.items) {
      repositoryFieldSet.items.each(function (childComponent) {
        if (childComponent.title === 'Available' || childComponent.xtype === 'toolbar') {
          childComponent.setHidden(true); // Disable the multiselect field
        }
      });
    }
  },

  initAlertBanners: function(taskTypeModel) {
    const me = this,
        formFields = taskTypeModel.get('formFields'),
        showBanner = function (model, name) {
          if (model) {
            me.down('panel[name="' + name + '"]').setVisible(true);
            me.down('panel[name="' + name + '"]').setTitle(model.helpText);
          } else {
            me.down('panel[name="' + name + '"]').setVisible(false);
          }
        };

    const top = Ext.Array.findBy(formFields, function(field) {
      return field.type === 'templateOnly' && field.id === 'topAlertBanner';
    });
    const bottom = Ext.Array.findBy(formFields, function(field) {
      return field.type === 'templateOnly' && field.id === 'bottomAlertBanner';
    });

    showBanner(top, 'topPanel');
    showBanner(bottom, 'bottomPanel');
  },
});

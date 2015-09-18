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
 * Task Schedule FieldSet.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.task.TaskScheduleFieldSet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-task-schedulefieldset',
  requires: [
    'NX.I18n'
  ],

  autoHeight: false,
  autoScroll: true,
  collapsed: false,
  defaults: {
    allowBlank: false
  },
  items: [
    {
      xtype: 'combo',
      name: 'schedule',
      itemId: 'schedule',
      fieldLabel: NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_FieldLabel'),
      emptyText: NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_EmptyText'),
      editable: false,
      store: [
        ['manual', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_ManualItem')],
        ['once', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_OnceItem')],
        ['hourly', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_HourlyItem')],
        ['daily', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_DailyItem')],
        ['weekly', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_WeeklyItem')],
        ['monthly', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_MonthlyItem')],
        ['advanced', NX.I18n.get('Task_TaskScheduleFieldSet_Recurrence_AdvancedItem')]
      ],
      queryMode: 'local',
      listeners: {
        change: function (combo, newValue, oldValue) {
          var form = combo.up('form'),
              oldCmp;

          if (oldValue) {
            oldCmp = combo.ownerCt.down('nx-coreui-task-schedule-' + oldValue);
            if (oldCmp) {
              combo.ownerCt.remove(oldCmp);
            }
          }

          if (newValue) {
            combo.ownerCt.add({ xtype: 'nx-coreui-task-schedule-' + newValue });
            form.getForm().checkValidity();
            form.isValid();
          }
        }
      }
    }
  ],

  /**
   * @public
   * Exports recurring days.
   * @returns {Array} recurring
   */
  getRecurringDays: function () {
    var me = this,
        days = me.query('checkbox[recurringDayValue]'),
        recurringDays = [];

    Ext.Array.each(days, function (day) {
      if (day.value) {
        recurringDays.push(day.recurringDayValue);
      }
    });

    return recurringDays;
  },

  /**
   * @public
   * Returns start date out of start date/time.
   * @returns {Date} start date
   */
  getStartDate: function () {
    var me = this,
        startDate = me.down('#startDate'),
        startTime = me.down('#startTime');

    if (startDate && startTime) {
      startDate = startDate.getValue();
      startTime = startTime.getValue();
      if (startDate && startTime) {
        startDate.setHours(startTime.getHours(), startTime.getMinutes(), 0, 0);
      }
    }
    return startDate;
  },

  /**
   * @public
   * Set values of start date and time
   */
  setStartDate: function(date) {
    var me = this,
      startDate = me.down('#startDate'),
      startTime = me.down('#startTime');

    if (startDate && startTime) {
      startDate.setValue(date);
      startTime.setValue(date);
    }
  },

  /**
   * @public
   * Set values of recurring days checkboxes
   */
  setRecurringDays: function(recurringDays) {
    var me = this;

    Ext.Array.each(recurringDays, function(day) {
      var checkbox = me.down('checkbox[name=recurringDay-' + day + ']');
      if (checkbox) {
        checkbox.setValue(true);
      }
    });
  }
});

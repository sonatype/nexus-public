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
 * Task Schedule Monthly field set.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.task.TaskScheduleMonthly', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-task-schedule-monthly',
  requires: [
    'NX.util.DateFormat',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        columns = [];

    for (var c = 0; c < 7; c++) {
      columns.push({
        xtype: 'panel',
        items: [],
        width: 50
      });
    }

    for (var day = 1; day <= 32; day++) {
      var columnNumber = day % 7 - 1,
          recurringDayValue = day === 32 ? '999' : day;

      if (columnNumber === -1) {
        columnNumber = 6;
      }
      columns[columnNumber].items.push({
        xtype: 'checkbox',
        name: 'recurringDay-' + recurringDayValue,
        boxLabel: day === 32 ? 'Last' : day,
        submitValue: false,
        recurringDayValue: recurringDayValue
      });
    }

    me.items = [
      {
        xtype: 'datefield',
        name: 'startDate',
        itemId: 'startDate',
        fieldLabel: NX.I18n.get('Task_TaskScheduleDaily_StartDate_FieldLabel'),
        allowBlank: false,
        format: 'm/d/Y',
        value: new Date(),
        submitValue: false
      },
      {
        xtype: 'timefield',
        name: 'startTime',
        itemId: 'startTime',
        fieldLabel: NX.I18n.get('Task_TaskScheduleDaily_Recurring_FieldLabel'),
        helpText: 'The time this task should start on days it will run in your time zone ' +
            NX.util.DateFormat.getTimeZone() + '.',
        allowBlank: false,
        format: 'H:i',
        submitValue: false
      },
      {
        xtype: 'checkboxgroup',
        fieldLabel: NX.I18n.get('Task_TaskScheduleMonthly_Days_FieldLabel'),
        allowBlank: false,
        blankText: NX.I18n.get('Task_TaskScheduleMonthly_Days_BlankText'),
        items: {
          xtype: 'panel',
          layout: 'column',
          items: columns
        }
      }
    ];

    me.callParent(arguments);
  }

});

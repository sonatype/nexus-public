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
 * Task scope Start/End dates fields set.
 */
Ext.define('NX.coreui.view.task.TaskScopeDates', {
  extend: 'NX.coreui.view.task.TaskScopeFields',
  alias: 'widget.nx-coreui-task-scope-dates',

  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    const me = this;

    me.items = [
      {
        xtype: 'component',
        html: '<b>' + NX.I18n.get('Task_TaskScope_StartEndDates_FieldLabel') + '</b>'
      },
      {
        xtype: 'label',
        text: NX.I18n.get('Task_TaskScope_StartEndDates_FieldHelpText')
      },
      {
        xtype: 'datefield',
        name: 'reconcileStartDate',
        fieldLabel: NX.I18n.get('Task_TaskScopeDates_StartDate_FieldLabel'),
        allowBlank: true,
        submitValue: false,
        format: 'm/d/Y',
        value: new Date(),
        required: false
      },
      {
        xtype: 'datefield',
        name: 'reconcileEndDate',
        fieldLabel: NX.I18n.get('Task_TaskScopeDates_EndDate_FieldLabel'),
        allowBlank: true,
        required: false,
        format: 'm/d/Y',
        value: new Date(),
        submitValue: false
      }
    ];

    me.callParent();
  },

  validate: function () {},

  importProperties: function (properties) {
    this.setDateValueFor('reconcileStartDate', properties['reconcileStartDate']);
    this.setDateValueFor('reconcileEndDate', properties['reconcileEndDate']);
  },

  exportProperties: function () {
    return {
      'reconcileStartDate' : this.getDateValueFor('reconcileStartDate'),
      'reconcileEndDate' : this.getDateValueFor('reconcileEndDate')
    };
  },

  getDateValueFor: function (fieldName) {
    const field = this.down('datefield[name="' + fieldName + '"]');
    if (field) {
      const val = field.getValue();
      const formatted = Ext.Date.format(val, 'm/d/Y');
      field.resetOriginalValue();
      return formatted;
    }
    return null;
  },

  setDateValueFor: function (fieldName, value) {
    const field = this.down('datefield[name="' + fieldName + '"]');
    if (field) {
      if (value) {
        console.log("Set field " + fieldName + " to value = " + value);
        field.setValue(value);
      } else {
        field.setValue(new Date());
      }
      field.resetOriginalValue();
    }
  }
});

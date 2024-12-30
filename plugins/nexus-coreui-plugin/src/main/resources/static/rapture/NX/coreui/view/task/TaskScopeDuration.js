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
 * Task scope Duration fields set.
 */
Ext.define('NX.coreui.view.task.TaskScopeDuration', {
  extend: 'NX.coreui.view.task.TaskScopeFields',
  alias: 'widget.nx-coreui-task-scope-duration',

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
        html: '<b>' + NX.I18n.get('Task_TaskScope_Duration_FieldLabel') + '</b>'
      },
      {
        xtype: 'label',
        text: NX.I18n.get('Task_TaskScope_Duration_FieldHelpText')
      },
      {
        xtype: 'numberfield',
        name: 'sinceDays',
        allowBlank: true,
        required: false,
        submitValue: false,
        fieldLabel: NX.I18n.get('Task_TaskScope_Duration_SinceDays_FieldLabel'),
      },
      {
        xtype: 'numberfield',
        name: 'sinceHours',
        itemId: 'sinceHours',
        allowBlank: true,
        required: false,
        submitValue: false,
        fieldLabel: NX.I18n.get('Task_TaskScope_Duration_SinceHours_FieldLabel'),
      },
      {
        xtype: 'numberfield',
        name: 'sinceMinutes',
        itemId: 'sinceMinutes',
        allowBlank: true,
        required: false,
        submitValue: false,
        fieldLabel: NX.I18n.get('Task_TaskScope_Duration_SinceMinutes_FieldLabel'),
      }
    ];

    me.callParent();
  },

  importProperties: function (properties) {
    this.setSinceValueFor('sinceDays', properties['sinceDays']);
    this.setSinceValueFor('sinceHours', properties['sinceHours']);
    this.setSinceValueFor('sinceMinutes', properties['sinceMinutes']);
  },

  exportProperties: function () {
    return {
      'sinceDays' : this.getSinceValueFor('sinceDays'),
      'sinceHours' : this.getSinceValueFor('sinceHours'),
      'sinceMinutes' : this.getSinceValueFor('sinceMinutes')
    };
  },

  getSinceValueFor: function (fieldName) {
    const field = this.down('numberfield[name="' + fieldName + '"]');
    if (field) {
      field.resetOriginalValue();
      return String(field.value);
    }
    return null;
  },

  setSinceValueFor: function (fieldName, value) {
    const field = this.down('numberfield[name="' + fieldName + '"]');
    if (field) {
      field.setValue(value);
      field.resetOriginalValue();
    }
  }
});

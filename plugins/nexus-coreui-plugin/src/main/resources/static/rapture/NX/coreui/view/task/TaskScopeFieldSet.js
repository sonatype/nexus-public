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
Ext.define('NX.coreui.view.task.TaskScopeFieldSet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-task-scopefieldset',

  config: {
    properties: {}
  },

  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    const me = this;

    me.items = [
      {
        xtype: 'component',
        html: '<b>' + NX.I18n.get('Task_TaskScope_Label') + '</b>'
      },
      {
        xtype: 'label',
        text: NX.I18n.get('Task_TaskScope_HelpText')
      },
      {
        xtype: 'combo',
        name: 'taskScope',
        editable: false,
        required: false,
        queryMode: 'local',

        store: [
          ['dates', NX.I18n.get('Task_TaskScope_StartEndDates_Item')],
          ['duration', NX.I18n.get('Task_TaskScope_Duration_Item')]
        ],

        listeners: {
          change: function(combo, newValue, oldValue) {
            const taskScopeFields = me.down('nx-coreui-task-scope-fields');
            if (taskScopeFields) {
              taskScopeFields.up().remove(taskScopeFields);
            }

            if (newValue && newValue !== oldValue) {
              me.config.properties['taskScope'] = newValue;

              const comboOwner = combo.ownerCt;
              if (comboOwner) {
                const fieldsSetType = 'nx-coreui-task-scope-' + newValue
                comboOwner.add({xtype: fieldsSetType});
                // pass properties through to the internal fields set
                const fieldsSet = me.down(fieldsSetType);
                if (fieldsSet) {
                  fieldsSet.importProperties(me.config.properties);
                }
              }
            }
          },
        }
      }
    ];

    me.callParent();
  },

  importProperties: function (properties) {
    const me = this;
    me.config.properties = properties;

    const taskScope = me.config.properties['taskScope'];
    const scopeCombo = me.down('combo[name="taskScope"]');
    if (taskScope) {
      scopeCombo.setValue(taskScope);
    } else {
      scopeCombo.setValue('duration');
    }
    scopeCombo.resetOriginalValue();
  },

  exportProperties: function() {
    const me = this,
        properties = {};

    if (me.config.properties) {
      const taskScope = me.config.properties['taskScope'];
      if (taskScope) {
        properties['taskScope'] = taskScope;
        const fieldsSet = me.down('nx-coreui-task-scope-' + taskScope);
        if (fieldsSet) {
          Object.assign(properties, fieldsSet.exportProperties());
        }
      }
    }
    return properties;
  },
});

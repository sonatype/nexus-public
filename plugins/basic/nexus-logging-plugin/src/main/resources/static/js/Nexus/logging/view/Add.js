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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Add new logger window.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.view.Add', {
  extend: 'Ext.Window',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.logging.Icons'
  ],

  xtype: 'nx-logging-view-add',
  cls: 'nx-logging-view-add',

  title: 'Add logger',

  autoShow: true,
  constrain: true,
  resizable: false,
  width: 500,
  border: false,
  modal: true,

  /**
   * @override
   */
  initComponent: function() {
    var me = this,
        icons = Nexus.logging.Icons,
        loggerNamePattern = /^[a-zA-Z0-9_.]+$/;

    Ext.apply(me, {
      items: [
        {
          xtype: 'panel',
          border: false,
          cls: 'nx-logging-view-add-description',
          html: icons.get('loggers_add').variant('x32').img + '<div>Add a logger with a specific level.</div>'
        },
        {
          xtype: 'form',
          itemId: 'form',
          border: false,
          monitorValid: true,
          layoutConfig: {
            labelSeparator: ''
          },
          items: [
            {
              xtype: 'textfield',
              fieldLabel: 'Logger',
              itemCls: 'required-field',
              helpText: 'Enter a logger name',
              name: 'name',
              allowBlank: false,
              validateOnBlur: false, // allow cancel to be clicked w/o validating this to be non-blank
              anchor: '96%',
              validator: function(value) {
                return loggerNamePattern.test(value);
              },
              invalidText: 'This field should only contain letters, numbers, _ and .'
            },
            {
              xtype: 'nx-logging-combo-logger-level',
              fieldLabel: 'Level',
              itemCls: 'required-field',
              //helpText: 'Select logger level',
              name: 'level',
              value: 'INFO',
              width: 80
            }
          ],

          buttonAlign: 'right',
          buttons: [
            { text: 'Cancel', xtype: 'link-button', handler: me.close, scope: me },
            { text: 'Save', formBind: true, id: 'nx-logging-button-add-save' }
          ]
        }
      ],

      keys: [
        {
          // Save on ENTER
          key: Ext.EventObject.ENTER,
          scope: me,
          fn: function() {
            // fire event only if form is valid
            if (me.getComponent('form').getForm().isValid()) {
              var btn = Ext.getCmp('nx-logging-button-add-save');
              btn.fireEvent('click', btn);
            }
          }
        },
        {
          // Close on ESC
          key: Ext.EventObject.ESC,
          scope: me,
          fn: me.close
        }
      ],

      listeners: {
        show: function(component) {
          component.find('name', 'name')[0].focus(false, 100);
        }
      }
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  }
});
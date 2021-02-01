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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Mark log window.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.view.Mark', {
  extend: 'Ext.Window',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.logging.Icons'
  ],

  xtype: 'nx-logging-view-mark',
  cls: 'nx-logging-view-mark',

  title: 'Mark log',

  autoShow: true,
  constrain: true,
  resizable: false,
  width: 500,
  border: false,
  modal: true,

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.logging.Icons;

    Ext.apply(me, {
      items: [
        {
          xtype: 'panel',
          border: false,
          cls: 'nx-logging-view-mark-description',
          html: icons.get('log_mark').variant('x32').img + '<div>Mark the log with a unique message for reference.</div>'
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
              fieldLabel: 'Message',
              itemCls: 'required-field',
              helpText: 'Message to be included in the log',
              name: 'message',
              allowBlank: false,
              validateOnBlur: false, // allow cancel to be clicked w/o validating this to be non-blank
              anchor: '96%'
            }
          ],

          buttonAlign: 'right',
          buttons: [
            { text: 'Cancel', xtype: 'link-button', formBind: false, handler: me.close, scope: me },
            { text: 'Save', formBind: true, id: 'nx-logging-button-mark-save' }
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
              var btn = Ext.getCmp('nx-logging-button-mark-save');
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
        show: function (component) {
          component.find('name', 'message')[0].focus(false, 100);
        }
      }
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  }
});
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
 * Form styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Forms', {
  extend: 'NX.view.dev.styles.StyleSection',

  title: 'Forms',
  layout: {
    type: 'hbox',
    defaultMargins: {top: 0, right: 4, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    me.items = [
      // basic form layout
      {
        xtype: 'form',
        items: [
          { xtype: 'textfield', value: 'Text Input', allowBlank: false, fieldLabel: '[Label]', helpText: '[Optional description text]', width: 200 },
          { xtype: 'textarea', value: 'Text Input', allowBlank: false, fieldLabel: '[Label]', helpText: '[Optional description text]', width: 200 },
          { xtype: 'checkbox', boxLabel: 'Checkbox', checked: true, fieldLabel: null, helpText: null },
          { xtype: 'radio', boxLabel: 'Radio Button', checked: true, fieldLabel: null, helpText: null }
        ],
        buttons: [
          { text: 'Submit', ui: 'nx-primary' },
          { text: 'Discard' }
        ]
      },

      // form example from extjs example/themes
      {
        xtype: 'form',
        frame: true,
        collapsible: true,

        tools: [
          {type:'toggle'},
          {type:'close'},
          {type:'minimize'},
          {type:'maximize'},
          {type:'restore'},
          {type:'gear'},
          {type:'pin'},
          {type:'unpin'},
          {type:'right'},
          {type:'left'},
          {type:'down'},
          {type:'refresh'},
          {type:'minus'},
          {type:'plus'},
          {type:'help'},
          {type:'search'},
          {type:'save'},
          {type:'print'}
        ],

        bodyPadding: '10 20',

        defaults: {
          anchor    : '98%',
          msgTarget : 'side',
          allowBlank: false
        },

        items: [
          {
            xtype: 'label',
            text: 'Plain Label'
          },
          {
            fieldLabel: 'TextField',
            xtype: 'textfield',
            name: 'someField',
            emptyText: 'Enter a value'
          },
          {
            fieldLabel: 'ComboBox',
            xtype: 'combo',
            store: ['Foo', 'Bar']
          },
          {
            fieldLabel: 'DateField',
            xtype: 'datefield',
            name: 'date'
          },
          {
            fieldLabel: 'TimeField',
            name: 'time',
            xtype: 'timefield'
          },
          {
            fieldLabel: 'NumberField',
            xtype: 'numberfield',
            name: 'number',
            emptyText: '(This field is optional)',
            allowBlank: true
          },
          {
            fieldLabel: 'TextArea',
            xtype: 'textareafield',
            name: 'message',
            cls: 'x-form-valid',
            value: 'This field is hard-coded to have the "valid" style (it will require some code changes to add/remove this style dynamically)'
          },
          {
            fieldLabel: 'Checkboxes',
            xtype: 'checkboxgroup',
            columns: [100, 100],
            items: [
              {boxLabel: 'Foo', checked: true, inputId: 'fooChkInput'},
              {boxLabel: 'Bar'}
            ]
          },
          {
            fieldLabel: 'Radios',
            xtype: 'radiogroup',
            columns: [100, 100],
            items: [
              {boxLabel: 'Foo', checked: true, name: 'radios'},
              {boxLabel: 'Bar', name: 'radios'}
            ]
          },
          {
            hideLabel: true,
            xtype: 'htmleditor',
            name: 'html',
            enableColors: false,
            value: 'Mouse over toolbar for tooltips.<br /><br />The HTMLEditor IFrame requires a refresh between a stylesheet switch to get accurate colors.',
            height: 110
          },
          {
            xtype: 'fieldset',
            title: 'Plain Fieldset',
            items: [
              {
                hideLabel: true,
                xtype: 'radiogroup',
                items: [
                  {boxLabel: 'Radio A', checked: true, name: 'radiogrp2'},
                  {boxLabel: 'Radio B', name: 'radiogrp2'}
                ]
              }
            ]
          },
          {
            xtype: 'fieldset',
            title: 'Collapsible Fieldset',
            collapsible: true,
            items: [
              { xtype: 'checkbox', boxLabel: 'Checkbox 1' },
              { xtype: 'checkbox', boxLabel: 'Checkbox 2' }
            ]
          },
          {
            xtype: 'fieldset',
            title: 'Checkbox Fieldset',
            checkboxToggle: true,
            items: [
              { xtype: 'radio', boxLabel: 'Radio 1', name: 'radiongrp1' },
              { xtype: 'radio', boxLabel: 'Radio 2', name: 'radiongrp1' }
            ]
          }
        ],

        buttons: [
          {
            text: 'Toggle Enabled',
            handler: function () {
              this.up('form').items.each(function (item) {
                item.setDisabled(!item.disabled);
              });
            }
          },
          {
            text: 'Reset Form',
            handler: function () {
              this.up('form').getForm().reset();
            }
          },
          {
            text: 'Validate',
            handler: function () {
              this.up('form').getForm().isValid();
            }
          }
        ]
      }
    ];

    me.callParent();
  }
});

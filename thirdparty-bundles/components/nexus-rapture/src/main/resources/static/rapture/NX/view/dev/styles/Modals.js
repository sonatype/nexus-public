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
 * Modal dialogs styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Modals', {
  extend: 'NX.view.dev.styles.StyleSection',
  requires: [
    'NX.I18n'
  ],

  title: 'Modals',

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
      {
        xtype: 'window',

        title: NX.I18n.get('SignIn_Title'),

        hidden: false,
        collapsible: false,
        floating: false,
        closable: false,
        draggable: false,
        resizable: false,
        width: 320,
        cls: 'fixed-modal',

        items: {
          xtype: 'form',
          ui: 'nx-inset',
          defaultType: 'textfield',
          defaults: {
            anchor: '100%'
          },
          items: [
            {
              name: 'username',
              itemId: 'username',
              emptyText: NX.I18n.get('SignIn_Username_Empty'),
              allowBlank: false,
              // allow cancel to be clicked w/o validating this to be non-blank
              validateOnBlur: false
            },
            {
              name: 'password',
              itemId: 'password',
              inputType: 'password',
              emptyText: NX.I18n.get('SignIn_Password_Empty'),
              allowBlank: false,
              // allow cancel to be clicked w/o validating this to be non-blank
              validateOnBlur: false
            }
          ],

          buttonAlign: 'left',
          buttons: [
            { text: NX.I18n.get('SignIn_Submit_Button'), formBind: true, bindToEnter: true, ui: 'nx-primary' },
            { text: NX.I18n.get('SignIn_Cancel_Button') }
          ]
        }
      },
      {
        xtype: 'window',

        title: 'Session',

        hidden: false,
        collapsible: false,
        floating: false,
        closable: false,
        draggable: false,
        resizable: false,
        width: 320,
        cls: 'fixed-modal',

        items: [
          {
            xtype: 'label',
            text: 'Session is about to expire',
            style: {
              'color': 'red',
              'font-size': '20px',
              'margin': '10px'
            }
          }
        ],
        buttons: [
          { text: 'Cancel' }
        ]
      }
    ];

    me.callParent();
  }
});

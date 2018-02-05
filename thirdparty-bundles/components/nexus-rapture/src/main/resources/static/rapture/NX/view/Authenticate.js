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
 * Authenticate window.
 *
 * @since 3.0
 */
Ext.define('NX.view.Authenticate', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-authenticate',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  cls: 'nx-authenticate',

  /**
   * @cfg message Message to be shown
   */
  message: undefined,

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    me.ui = 'nx-inset';
    me.title = NX.I18n.get('Authenticate_Title');
    me.defaultFocus = 'password';

    me.setWidth(NX.view.ModalDialog.SMALL_MODAL);

    if (!me.message) {
      me.message = NX.I18n.get('Authenticate_Help_Text');
    }

    Ext.apply(this, {
      items: {
        xtype: 'form',
        defaultType: 'textfield',
        defaults: {
          anchor: '100%'
        },
        items: [
          {
            xtype: 'container',
            layout: 'hbox',
            cls: 'message',
            items: [
              { xtype: 'component', html: NX.Icons.img('authenticate', 'x32') },
              { xtype: 'component', html: '<div>' + me.message + '</div>' }
            ]
          },
          {
            name: 'username',
            itemId: 'username',
            emptyText: NX.I18n.get('SignIn_Username_Empty'),
            allowBlank: false,
            readOnly: true
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
          { text: NX.I18n.get('User_View_Authenticate_Submit_Button'), action: 'authenticate', formBind: true, bindToEnter: true, ui: 'nx-primary' },
          { text: NX.I18n.get('Authenticate_Cancel_Button'), handler: me.close, scope: me }
        ]
      }
    });

    me.callParent();
  }

});

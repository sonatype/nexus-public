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
/*global Ext*/

/**
 * Sign-in window.
 *
 * @since 3.0
 */
Ext.define('NX.view.SignIn', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-signin',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.ui = 'nx-inset';
    me.title = NX.I18n.get('SignIn_Title');

    me.setWidth(NX.view.ModalDialog.SMALL_MODAL);

    Ext.apply(me, {
      items: {
        xtype: 'form',
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
          { text: NX.I18n.get('SignIn_Submit_Button'), action: 'signin', formBind: true, bindToEnter: true, ui: 'nx-primary' },
          { text: NX.I18n.get('SignIn_Cancel_Button'), handler: me.close, scope: me }
        ]
      }
    });

    me.on({
      resize: function() {
        me.down('#username').focus();
      },
      single: true
    });

    me.callParent();
  },

  addMessage: function(message) {
    var me = this,
        htmlMessage = '<div id="signin-message">' + message + '</div><br>',
        messageCmp = me.down('#signinMessage');

    if (messageCmp) {
      messageCmp.html(htmlMessage);
    }
    else {
      me.down('form').insert(0, {
        xtype: 'component',
        itemId: 'signinMessage',
        html: htmlMessage
      });
    }
  },

  clearMessage: function() {
    var me = this,
        messageCmp = me.down('#signinMessage');

    if (messageCmp) {
      me.down('form').remove(messageCmp);
    }
  }

});

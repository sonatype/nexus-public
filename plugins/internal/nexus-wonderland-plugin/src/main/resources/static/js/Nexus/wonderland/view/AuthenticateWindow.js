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
/**
 * Window to authenticate current user.
 */
NX.define('Nexus.wonderland.view.AuthenticateWindow', {
  extend: 'Ext.Window',

  requires: [
    'Nexus.wonderland.Icons'
  ],

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  xtype: 'nx-wonderland-view-authenticate',

  title: 'Authenticate',
  id: 'nx-wonderland-view-authenticate',
  cls: 'nx-wonderland-view-authenticate',

  border: false,
  closable: false,
  modal: true,
  height: 190,
  width: 350,
  resizable: false,

  /**
   * @cfg message
   * @type String
   */
  message: 'Authentication required',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.wonderland.Icons;

    me.passwordField = NX.create('Ext.form.TextField', {
      fieldLabel: 'Password',
      inputType: 'password',
      name: 'password',
      width: 170,
      allowBlank: false,
      validateOnBlur: false // allow cancel to be clicked w/o validating this to be non-blank
    });

    Ext.apply(me, {
      items: [
        {
          xtype: 'form',
          height: 160,
          monitorValid: true,
          layoutConfig: {
            labelSeparator: ''
          },
          labelAlign: 'right',

          items: [
            {
              xtype: 'component',
              cls: 'nx-wonderland-view-authenticate-description',
              html: icons.get('lock').variant('x32').img + '<div>' + me.message +
                  '</div><br style="clear:both;"/>' // clear to get remaining form elements aligned correctly
            },
            {
              xtype: 'textfield',
              fieldLabel: 'Username',
              name: 'username',
              width: 170,
              readOnly: true,
              preventMark: true
            },
            me.passwordField
          ],

          buttons: [
            {
              text: 'Confirm',
              formBind: true,
              scope: me,
              handler: me.fireValidate
            },
            {
              xtype: 'link-button',
              text: 'Cancel',
              formBind: false,
              scope: me,
              handler: me.close
            }
          ],

          keys: [
            {
              // Validate on ENTER
              key: Ext.EventObject.ENTER,
              scope: me,
              fn: me.fireValidate
            },
            {
              // Close dialog on ESC
              key: Ext.EventObject.ESC,
              scope: me,
              fn: me.close
            }
          ]
        }
      ],

      listeners: {
        /**
         * Set username to current user and auto-focus password field.
         */
        show: function (component) {
          component.find('name', 'username')[0].setValue(Nexus.currentUser().username);
          component.find('name', 'password')[0].focus(false, 100);
          me.passwordField.markInvalid('This field is required');
        }
      }
    });

    me.constructor.superclass.initComponent.apply(me, arguments);

    // register events
    me.addEvents(
        /**
         * @event validate-credentials
         * @param {Window} self
         * @param {String} username
         * @param {String} password
         */
        'validate-credentials',

        /**
         * @event authenticated
         * @param {Window} self
         * @param {String} authentication ticket
         */
        'authenticated'
    );
  },

  /**
   * @private
   */
  fireValidate: function () {
    this.fireEvent(
        'validate-credentials',
        this,
        Nexus.currentUser().username,
        this.passwordField.getValue()
    );
  },

  /**
   * @public
   */
  markInvalid: function() {
    this.passwordField.markInvalid('Invalid password');
  }
});
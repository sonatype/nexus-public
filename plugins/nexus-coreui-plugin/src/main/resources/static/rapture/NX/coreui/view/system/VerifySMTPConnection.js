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
 * Verify SMTP connection window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.VerifySMTPConnection', {
  extend: 'Ext.window.Window',
  alias: 'widget.nx-coreui-system-verifysmtpconnection',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],
  ui: 'nx-inset',

  title: NX.I18n.get('System_VerifySmtpConnection_VerifyServer_Title'),
  defaultFocus: 'email',

  layout: 'fit',
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
    var me = this;

    me.items = [
      {
        xtype: 'form',
        defaults: {
          labelSeparator: '',
          labelWidth: 40,
          labelAlign: 'right',
          anchor: '100%'
        },
        items: [
          {
            xtype: 'panel',
            layout: 'hbox',
            style: {
              marginBottom: '10px'
            },
            items: [
              { xtype: 'component', html: NX.Icons.img('verifysmtpconnection', 'x32') },
              {
                xtype: 'label',
                html: NX.I18n.get('System_VerifySmtpConnection_HelpText'),
                margin: '0 0 0 5'
              }
            ]
          },
          {
            xtype: 'nx-email',
            name: 'email',
            itemId: 'email',
            fieldLabel: 'E-mail',
            allowBlank: false,
            validateOnBlur: false // allow cancel to be clicked w/o validating this to be non-blank
          }
        ],

        buttonAlign: 'left',
        buttons: [
          {
            text: 'Verify',
            action: 'verify',
            formBind: true,
            bindToEnter: true,
            ui: 'nx-primary',
            glyph: 'xf003@FontAwesome' /* fa-envelope-o */
          },
          {
            text: 'Cancel',
            handler: function () {
              this.up('window').close();
            }
          }
        ]
      }
    ];

    me.callParent();
  }
});

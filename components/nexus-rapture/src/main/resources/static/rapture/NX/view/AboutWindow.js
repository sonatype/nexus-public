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
 * About window.
 *
 * @since 3.0
 */
Ext.define('NX.view.AboutWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-aboutwindow',
  requires: [
    'NX.I18n',
    'NX.Icons',
    'NX.State',
    'NX.util.Url'
  ],

  cls: 'nx-aboutwindow',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.layout = {
      type: 'vbox',
      align: 'stretch'
    };

    me.height = 465;
    me.width = NX.view.ModalDialog.LARGE_MODAL;

    me.title = NX.I18n.get('AboutWindow_Title');

    me.items = [
      {
        xtype: 'container',
        cls: 'summary',
        layout: {
          type: 'hbox',
          align: 'stretch'
        },
        items: [
          {
            xtype: 'component',
            cls: 'logo',
            html: NX.Icons.img('new-nexus-black', 'x100')
          },
          {
            xtype: 'nx-info',
            itemId: 'aboutInfo',
            flex: 1
          }
        ]
      },
      {
        xtype: 'tabpanel',
        ui: 'nx-light',
        flex: 1,
        items: [
          {
            title: NX.I18n.get('AboutWindow_About_Title'),
            xtype: 'uxiframe',
            src: NX.util.Url.urlOf('/COPYRIGHT.html')
          }
        ]
      }
    ];

    me.buttons = [
      { text: NX.I18n.get('Button_Close'), action: 'close', ui: 'nx-primary', handler: function () { me.close(); }}
    ];
    me.buttonAlign = 'left';

    me.callParent();

    // populate initial details
    me.down('#aboutInfo').showInfo({
      'Version': NX.State.getVersion(),
      'Edition': NX.State.getEdition(),
      'Build Revision': NX.State.getBuildRevision(),
      'Build Timestamp': NX.State.getBuildTimestamp()
    });
  }
});

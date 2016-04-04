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
 * Dialog to display a NuGet API Key details.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.nuget.NuGetApiKeyDetails', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-nuget-apikeydetails',
  requires: [
    'NX.Icons',
    'NX.Messages',
    'NX.I18n',
    'NX.util.Url'
  ],
  ui: 'nx-inset',

  title: 'NuGet API Key',

  /**
   * @cfg {String} NuGet API Key
   */
  apiKey: undefined,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.setWidth(NX.view.ModalDialog.LARGE_MODAL);

    me.items = {
      xtype: 'form',
      items: [
        {
          xtype: 'panel',
          layout: 'hbox',
          items: [
            { xtype: 'component', html: NX.Icons.img('nuget-default', 'x32') },
            { xtype: 'label', margin: '0 0 0 5',
              html: NX.I18n.get('Nuget_NuGetApiKeyDetails_Html')
            }
          ]
        },

        {
          xtype: 'panel',
          layout: 'vbox',
          margin: '0 0 10 0',
          items: [
            {
              xtype: 'label',
              margin: '5 0 0 0',
              text: NX.I18n.get('Nuget_NuGetApiKeyDetails_ApiKey_Text')
            },
            {
              xtype: 'textfield',
              value: me.apiKey,
              readOnly: true,
              selectOnFocus: true,
              fieldStyle: {
                padding: '2px',
                'font-family': 'monospace'
              }
            },
            {
              xtype: 'label',
              margin: '5 0 0 0',
              text: NX.I18n.get('Nuget_NuGetApiKeyDetails_Register_Text')
            },
            {
              xtype: 'textfield',
              value: NX.I18n.format('Nuget_NuGetApiKeyDetails_Register_Value', me.apiKey,
                  NX.util.Url.urlOf('repository/{repository name}/')),
              readOnly: true,
              selectOnFocus: true,
              fieldStyle: {
                padding: '2px',
                'font-family': 'monospace'
              }
            }
          ]
        },
        {
          xtype: 'label',
          style: 'font-style: italic;',
          html: NX.I18n.get('Nuget_NuGetApiKeyDetails_AutoClose_Html')
        }
      ],
      buttonAlign: 'left',
      buttons: [
        { text: NX.I18n.get('Button_Close'), handler: function() {
          this.up('window').close();
        }}
      ]
    };

    me.callParent();

    // Automatically close the window
    Ext.defer(function() {
      if (me.isVisible()) { // ignore if already closed
        NX.Messages.add({ text: NX.I18n.get('Nuget_NuGetApiKeyDetails_AutoClose_Message') });
        me.close();
      }
    }, 1 * 60 * 1000); // 1 minute
  }

});

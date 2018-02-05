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
 * NuGet Api Key.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.nuget.NuGetApiKey', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-nuget-apikey',
  requires: [
    'NX.I18n'
  ],

  config: {
    active: false
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',

        items: [
          {
            xtype: 'label',
            margin: '5 0 5 0',
            text: NX.I18n.get('Nuget_NuGetApiKey_Instructions_Text')
          }
        ],

        buttonAlign: 'left',
        buttons: [
          { text: NX.I18n.get('Nuget_NuGetApiKey_Access_Button'), action: 'access', ui: 'nx-primary', glyph: 'xf023@FontAwesome' /* fa-lock */, disabled: true },
          { text: NX.I18n.get('Nuget_NuGetApiKey_Reset_Button'), action: 'reset', glyph: 'xf023@FontAwesome' /* fa-lock */, disabled: true }
        ]
      }
    ];

    me.callParent();
  }

});

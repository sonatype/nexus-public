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
 * Header panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.header.Panel', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-header-panel',
  requires: [
    'NX.I18n',
    'NX.State'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      { xtype: 'nx-header-branding', hidden: true },
      {
        xtype: 'toolbar',

        // set height to ensure we have uniform size and not depend on what is in the toolbar
        height: 40,

        style: {
          backgroundColor: '#000000'
        },
        anchor: '100%',
        padding: "0 0 0 16px",

        defaults: {
          scale: 'medium'
        },

        items: [
          { xtype: 'nx-header-logo' },
          {
            xtype: 'container',
            items: [
              {
                xtype: 'label',
                text: NX.I18n.get('Header_Panel_Logo_Text'),
                cls: 'nx-header-productname'
              },
              {
                xtype: 'label',
                text: NX.State.getEdition() + ' ' + NX.State.getVersion(),
                cls: 'nx-header-productversion',
                style: {
                  'padding-left': '8px'
                }
              }
            ]
          }
        ]
      }
    ];

    me.callParent();
  }
});

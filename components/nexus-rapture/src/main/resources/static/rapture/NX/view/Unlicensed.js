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
/*global Ext*/

// TODO: This is a placeholder view for what to display to the user when a license is required and missing or invalid

/**
 * Unlicensed uber mode panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.Unlicensed', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-unlicensed',

  cls: 'nx-unlicensed',
  layout: 'border',

  items: [
    {
      xtype: 'nx-header-panel',
      region: 'north',
      collapsible: false
    },
    {
      xtype: 'panel',
      region: 'center',
      layout: {
        type: 'vbox',
        align: 'center',
        pack: 'center'
      },
      items: [
        {
          xtype: 'label',
          cls: 'title',
          html: 'Product License Required'
        },
        {
          xtype: 'label',
          cls: 'description',
          text: 'A license is required to use this product.'
        }
      ]
    },
    {
      xtype: 'nx-footer',
      region: 'south',
      hidden: false
    },
    {
      xtype: 'nx-dev-panel',
      region: 'south',
      collapsible: true,
      collapsed: true,
      resizable: true,
      resizeHandles: 'n',

      // keep initial constraints to prevent huge panels
      height: 300,

      // default to hidden, only show if debug enabled
      hidden: true
    }
  ]

});
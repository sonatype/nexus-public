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
 * Footer panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.footer.Panel', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-footer',
  requires: [
    'NX.I18n'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  items: [
    {
      xtype: 'container',
      html: NX.I18n.get('Footer_Panel_HTML'),
      style: {
        'background-color': '#444444',
        'color': '#C6C6C6',
        'font-size': '8px',
        'text-align': 'right',
        'padding': '1px 2px 0px 0px'
      }
    },
    { xtype: 'nx-footer-branding', hidden: true }
  ]
});

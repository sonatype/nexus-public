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
 * Panel styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Panels', {
  extend: 'NX.view.dev.styles.StyleSection',

  title: 'Panels',

  layout: {
    type: 'vbox',
    defaultMargins: {top: 4, right: 0, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    function panelStyle(ui) {
      return {
        layout: {
          type: 'hbox',
          defaultMargins: {top: 0, right: 4, bottom: 0, left: 0}
        },

        items: [
          {
            xtype: 'panel',
            title: ui,
            ui: ui,
            height: 100,
            width: 200,
            items: [
              {
                xtype: 'container',
                html: 'ui: ' + ui
              }
            ]
          },
          {
            xtype: 'panel',
            title: ui + ' framed',
            ui: ui,
            frame: true,
            height: 100,
            width: 200,
            items: [
              {
                xtype: 'container',
                html: 'ui: ' + ui + '; frame: true'
              }
            ]
          }
        ]
      }
    }

    me.items = [
      panelStyle('default'),
      panelStyle('light'),

      // TODO: Consider adding flag to disable 'frame: true' example for subsection
      panelStyle('nx-subsection')
    ];

    me.callParent();
  }
});
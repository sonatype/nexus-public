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
 * Menu styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Menus', {
  extend: 'NX.view.dev.styles.StyleSection',

  title: 'Menus',
  layout: {
    type: 'hbox',
    defaultMargins: {top: 0, right: 4, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    function menu(text, iconCls, tooltip, action) {
      return {
        text: text,
        iconCls: iconCls,
        tooltip: tooltip,
        action: action
      }
    }

    me.items = [
      {
        xtype: 'menu',
        floating: false,
        items: [
          menu('Help for [Feature]', 'nx-icon-search-default-x16', 'Help for the current feature', 'feature'),
          '-',
          menu('About', 'nx-icon-nexus-x16', 'About Sonatype Nexus', 'about'),
          menu('Documentation', 'nx-icon-help-manual-x16', 'Sonatype Nexus product documentation', 'docs'),
          menu('Knowledge Base', 'nx-icon-help-kb-x16', 'Sonatype Nexus knowledge base', 'kb'),
          menu('Community', 'nx-icon-help-community-x16', 'Sonatype Nexus community information', 'community'),
          menu('Issue Tracker', 'nx-icon-help-issues-x16', 'Sonatype Nexus issue and bug tracker', 'issues'),
          '-',
          menu('Support', 'nx-icon-help-support-x16', 'Sonatype Nexus product support', 'support')
        ]
      }
    ];

    me.callParent();
  }
});
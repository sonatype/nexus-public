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
 * Help button.
 *
 * @since 3.0
 */
Ext.define('NX.view.header.Help', {
  extend: 'Ext.button.Button',
  alias: 'widget.nx-header-help',
  requires: [
    'NX.I18n'
  ],

  tooltip: NX.I18n.get('Header_Help_Tooltip'),
  glyph: 'xf059@FontAwesome', // fa-question-circle

  // hide the menu button arrow
  arrowCls: '',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.menu = [
      {
        // text and iconCls is dynamic
        tooltip: NX.I18n.get('Header_Help_Feature_Tooltip'),
        action: 'feature'
      },
      '-',
      {
        text: NX.I18n.get('Header_Help_About_Text'),
        iconCls: 'nx-icon-nexus-x16',
        tooltip: NX.I18n.get('Header_Help_About_Tooltip'),
        action: 'about'
      },
      {
        text: NX.I18n.get('Header_Help_Documentation_Text'),
        iconCls: 'nx-icon-help-manual-x16',
        tooltip: NX.I18n.get('Header_Help_Documentation_Tooltip'),
        action: 'docs'
      },
      {
        text: NX.I18n.get('Header_Help_KB_Text'),
        iconCls: 'nx-icon-help-kb-x16',
        tooltip: NX.I18n.get('Header_Help_KB_Tooltip'),
        action: 'kb'
      },
      {
        text: NX.I18n.get('Header_Help_Community_Text'),
        iconCls: 'nx-icon-help-community-x16',
        tooltip: NX.I18n.get('Header_Help_Community_Tooltip'),
        action: 'community'
      },
      {
        text: NX.I18n.get('Header_Help_Issues_Text'),
        iconCls: 'nx-icon-help-issues-x16',
        tooltip: NX.I18n.get('Header_Help_Issues_Tooltip'),
        action: 'issues'
      },
      '-',
      {
        text: NX.I18n.get('Header_Help_Support_Text'),
        iconCls: 'nx-icon-help-support-x16',
        tooltip: NX.I18n.get('Header_Help_Support_Tooltip'),
        action: 'support'
      }
    ];

    me.callParent();
  }

});

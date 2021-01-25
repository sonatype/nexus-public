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

/**
 * Panel shown in case a bookmarked feature is not found (404 like).
 *
 * @since 3.0
 */
Ext.define('NX.view.feature.NotFound', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-feature-notfound',
  requires: [
    'NX.I18n'
  ],

  cls: [
    'nx-feature-notfound',
    'nx-hr'
  ],

  layout: {
    type: 'vbox',
    align: 'center',
    pack: 'center'
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {
        xtype: 'label',
        cls: 'title',
        text: me.path ? NX.I18n.format('Feature_NotFoundPath_Text', me.path) : NX.I18n.get('Feature_NotFound_Text')
      },
      {
        xtype: 'label',
        cls: 'description',
        // TODO: i18n
        text: 'Sorry the feature you have selected does not exist.  Please make another selection.'
      }
    ];

    me.callParent();
  }

});

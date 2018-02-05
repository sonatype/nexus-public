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
 * Visual style sheet for the application.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Styles', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-dev-styles',
  requires: [
    'NX.view.dev.styles.Colors',
    'NX.view.dev.styles.Fonts',
    'NX.view.dev.styles.Buttons',
    'NX.view.dev.styles.Forms',
    'NX.view.dev.styles.Messages',
    'NX.view.dev.styles.Modals',
    'NX.view.dev.styles.Menus',
    'NX.view.dev.styles.Tabs',
    'NX.view.dev.styles.Pickers',
    'NX.view.dev.styles.Tooltips',
    'NX.view.dev.styles.Panels',
    'NX.view.dev.styles.Toolbars',
    'NX.view.dev.styles.Grids',
    'NX.view.dev.styles.Other'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  title: 'Styles',

  layout: {
    type: 'vbox',
    defaultMargins: {top: 0, right: 4, bottom: 10, left: 0}
  },

  defaults: {
    width: '100%'
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    // build guide components on activate as this is a heavy view
    me.on('activate', function () {
      var sections = [
        'Colors',
        'Fonts',
        'Buttons',
        'Forms',
        'Messages',
        'Modals',
        'Menus',
        'Tooltips',
        'Tabs',
        'Pickers',
        'Panels',
        'Toolbars',
        'Grids',
        'Other'
      ];

      me.logDebug('Creating style guide');

      // TODO: See if suspending layouts here actually helps anything?
      //Ext.AbstractComponent.suspendLayouts();
      //try {
      Ext.Array.each(sections, function (section) {
        me.add(Ext.create('NX.view.dev.styles.' + section));
      });
      //}
      //finally {
      //  Ext.AbstractComponent.resumeLayouts(true);
      //}

      me.logDebug('Style guide ready');
    });

    // and destroy on deactivate to save memory
    me.on('deactivate', function () {
      me.removeAll(true);

      me.logDebug('Destroyed style guide');
    });

    me.callParent();
  }
});
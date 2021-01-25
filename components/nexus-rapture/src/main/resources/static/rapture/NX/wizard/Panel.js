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
/*global Ext, NX*/

/**
 * Wizard panel.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.wizard.Panel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-wizard-panel',
  requires: [
    'NX.I18n'
  ],

  cls: 'nx-wizard-panel',

  autoScroll: true,
  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  items: {
    xtype: 'container',
    itemId: 'container',
    cls: 'screencontainer',
    frame: true,
    layout: 'card'
  },

  // screen header (title + progress)
  dockedItems: {
    xtype: 'toolbar',
    itemId: 'header',
    cls: 'screenheader',
    dock: 'top',
    items: [
      {
        xtype: 'label',
        itemId: 'title',
        cls: 'title'
      },
      '->',
      {
        xtype: 'label',
        itemId: 'progress',
        cls: 'progress'
      }
    ]
  },

  /**
   * @returns {Ext.container.Container}
   */
  getScreenHeader: function () {
    return this.down('#header');
  },

  /**
   * @param {String} title
   */
  setTitle: function (title) {
    this.getScreenHeader().down('#title').setText(title);
  },

  /**
   * @param {number} current  Current screen number.
   * @param {number} total    Total number of screens.
   */
  setProgress: function (current, total) {
    this.getScreenHeader().down('#progress').setText(NX.I18n.format('Wizard_Screen_Progress', current, total)
    );
  },

  /**
   * @returns {Ext.panel.Panel}
   */
  getScreenContainer: function () {
    return this.down('#container');
  }
});
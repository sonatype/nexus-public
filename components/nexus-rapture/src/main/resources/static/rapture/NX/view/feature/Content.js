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
 * Content panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.feature.Content', {
  extend: 'Ext.panel.Panel',
  requires: [ 'NX.view.feature.BreadcrumbPanel', 'NX.view.MaliciousRiskOnDisk' ],
  alias: 'widget.nx-feature-content',
  ariaRole: 'main',
  itemId: 'feature-content',
  ui: 'nx-feature-content',
  cls: 'nx-feature-content',
  layout: 'fit',

  /**
   * @private
   * If false, show a warning modal when you’re about to discard unsaved changes by navigating away
   */
  discardUnsavedChanges: false,

  dockedItems: [
    {
      xtype: 'nx-component-malicious-risk-on-disk',
      dock: 'top',
      hidden: true
    },
    {
      xtype: 'nx-breadcrumb',
      dock: 'top'
    }
  ],

  listeners: {
    afterrender: function(obj) {
      obj.rendered = true;
      obj.showRoot();
    }
  },

  maybeShowMaliciousRiskOnDisk: function() {
    var me = this;
    var maliciousRiskOnDisk = me.down('nx-component-malicious-risk-on-disk');
    const titles = ['Browse', 'Search', 'Welcome'];

    if (titles.includes(me.currentTitle)) {
      maliciousRiskOnDisk.show();
    } else {
      maliciousRiskOnDisk.hide();
    }
  },

  /**
   * Show the feature root (hide the breadcrumb)
   */
  showRoot: function() {
    var me = this;
    var breadcrumb = me.down('#breadcrumb');

    if (!me.rendered) {
      return;
    }

    if (breadcrumb.items.length !== 3) {
      breadcrumb.removeAll();
      breadcrumb.add(
          {
            xtype: 'container',
            itemId: 'nx-feature-icon',
            width: 32,
            height: 32,
            userCls: me.currentIcon,
            ariaRole: 'presentation'
          },
          {
            xtype: 'label',
            cls: 'nx-feature-name',
            text: me.currentTitle
          },
          {
            xtype: 'label',
            cls: 'nx-feature-description',
            text: me.currentDescription
          }
      );
    }
    else {
      breadcrumb.items.getAt(0).setUserCls(me.currentIcon);
      breadcrumb.items.getAt(1).setText(me.currentTitle);
      breadcrumb.items.getAt(2).setText(me.currentDescription);

      if (breadcrumb.items.length > 3) {
        Ext.each(breadcrumb.items.getRange(3), function(item) {
          breadcrumb.remove(item);
        });
      }
    }
  },

  /**
   * The currently set title and icon, so subpanels can access it
   */
  currentIcon: undefined,
  currentTitle: undefined,

  /**
   * Custom handling for icon
   *
   * @override
   * @param iconCls
   */
  setIconCls: function(iconCls) {
    this.currentIcon = iconCls;
  },

  /**
   * Custom handling for title
   *
   * @override
   * @param text
   */
  setTitle: function(text) {
    this.currentTitle = text;
  },

  /**
   * Set description text.
   *
   * @public
   * @param text
   */
  setDescription: function(text) {
    this.currentDescription = text;
  },

  /**
   * The currently set iconCls, so we can remove it when changed.
   *
   * @private
   */
  currentIconCls: undefined,

  /**
   * @public
   * Reset the discardUnsavedChanges flag (false by default)
   */
  resetUnsavedChangesFlag: function(enable) {
    var me = this;

    if (enable) {
      me.discardUnsavedChanges = true;
    }
    else {
      me.discardUnsavedChanges = false;
    }
  }
});

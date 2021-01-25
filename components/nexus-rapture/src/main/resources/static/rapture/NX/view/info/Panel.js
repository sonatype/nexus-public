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
 * Info panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.info.Panel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-info-panel',

  // FIXME: What is this for?
  titled: null,

  framed: true,
  autoScroll: true,
  header: false,

  // FIXME: Reduce use of nested panels

  /**
   * @private
   */
  initComponent: function() {
    var me = this,
      inset,
      subsection;

    subsection = {
      xtype: 'panel',
      ui: 'nx-subsection',
      //if framed the title will come from the inset section
      title: me.framed ? undefined : me.titled,
      frame: me.framed,
      items: { xtype: 'nx-info' }
    };

    inset = {
      xtype: 'panel',
      ui: 'nx-inset',
      title: me.titled,
      collapsible: me.collapsible,

      items: subsection
    };

    if (me.framed) {
      me.items = inset;
    }
    else {
      me.items = subsection;
    }

    me.callParent();
  },

  /**
   * @public
   */
  setTitle: function(title) {
    this.titled = title;
  },

  /**
   * @public
   */
  showInfo: function (info) {
    this.down('nx-info').showInfo(info);
  },

  /**
   * @public
   * Add an additional component to enhance the info.
   */
  addSection: function(component) {
    this.down('nx-info').up('panel').add(component);
  }
});

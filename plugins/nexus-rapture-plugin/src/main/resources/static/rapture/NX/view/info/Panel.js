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
 * Info panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.info.Panel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-info-panel',

  titled: null,
  framed: true,
  autoScroll: true,
  header: false,

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
      title: me.titled,
      frame: me.framed,
      items: { xtype: 'nx-info' }
    };

    inset = {
      xtype: 'panel',
      ui: 'nx-inset',

      items: subsection
    };

    if (me.framed) {
      me.items = inset;
    }
    else {
      me.items = subsection;
    }

    me.callParent(arguments);
  },

  /**
   * @public
   */
  setTitle: function(title) {
    var me = this;
    me.titled = title;
    me.down('panel').down('panel').setTitle(title);
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

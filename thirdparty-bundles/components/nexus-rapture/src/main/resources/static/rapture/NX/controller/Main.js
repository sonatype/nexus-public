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
 * Main uber mode controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Main', {
  extend: 'NX.app.Controller',

  views: [
    'Main',
    'header.Panel',
    'header.Branding',
    'header.Logo',
    'footer.Panel',
    'footer.Branding'
  ],

  refs: [
    {
      ref: 'viewport',
      selector: 'viewport'
    },
    {
      ref: 'main',
      selector: 'nx-main'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'nexus': {
        file: 'nexus.png',
        variants: ['x16', 'x24', 'x32', 'x48', 'x100']
      },
      'sonatype': {
        file: 'sonatype.png',
        variants: ['x16', 'x24', 'x32', 'x48', 'x100']
      }
    });

    me.listen({
      component: {
        'viewport': {
          afterrender: me.onLaunch
        }
      }
    });
  },

  /**
   * Show {@link NX.view.Main} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onLaunch: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Showing main view');
      //</if>

      viewport.add({ xtype: 'nx-main' });
    }
  },

  /**
   * Removes {@link NX.view.Main} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onDestroy: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Removing main view');
      //</if>

      viewport.remove(me.getMain());
    }
  }

});

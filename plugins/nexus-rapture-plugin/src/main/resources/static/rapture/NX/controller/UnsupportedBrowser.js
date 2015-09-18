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
 * Unsupported browser uber mode controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.UnsupportedBrowser', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State'
  ],

  views: [
    'UnsupportedBrowser',
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
      ref: 'unsupportedBrowser',
      selector: 'nx-unsupported-browser'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'viewport': {
          afterrender: me.onLaunch
        },
        'nx-unsupported-browser button[action=continue]': {
          click: me.onContinue
        }
      }
    });
  },

  /**
   * Show {@link NX.view.UnsupportedBrowser} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onLaunch: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Showing unsupported browser view');
      //</if>

      viewport.add({ xtype: 'nx-unsupported-browser' });
    }
  },

  /**
   * Removes {@link NX.view.UnsupportedBrowser} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onDestroy: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Removing unsupported browser view');
      //</if>

      viewport.remove(me.getUnsupportedBrowser());
    }
  },

  onContinue: function () {
    NX.State.setBrowserSupported(true);
  }

});

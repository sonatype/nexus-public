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
 * Refresh controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Refresh', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Messages',
    'NX.I18n'
  ],

  views: [
    'header.Refresh'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'nx-header-refresh': {
          click: me.refresh
        }
      }
    });
  },

  /**
   * Fire refresh event.
   *
   * @public
   */
  refresh: function () {
    var me = this;

    if (me.fireEvent('beforerefresh')) {
      me.fireEvent('refresh');

      // Show a message here, so that if the current view doesn't actually support
      // request that users don't think the feature is broken and spam-click the refresh button
      NX.Messages.info(NX.I18n.get('Refresh_Message'));
    }
  }

});

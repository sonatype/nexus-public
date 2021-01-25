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
 * Unlicensed uber mode controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Unlicensed', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Bookmarks',
    'NX.Messages',
    'NX.I18n'
  ],
  
  
  /**
   * Show {@link NX.view.Unlicensed} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onLaunch: function () {
    var me = this;
    //<if debug>
    me.logDebug('Adding unlicensed listeners');
    //</if>
    Ext.History.on('change', me.forceLicensing);
    me.forceLicensing();
  },

  /**
   * Removes {@link NX.view.Unlicensed} view from {@link Ext.container.Viewport}.
   *
   * @override
   */
  onDestroy: function () {
    var me = this;
    //<if debug>
    me.logDebug('Removing unlicensed listeners');
    //</if>
    Ext.History.un('change', me.forceLicensing);
  },

  /**
   * Show a message and force navigation to the Licensing page, preventing all other navigation in the UI.
   */
  forceLicensing: function () {
    NX.Messages.error(NX.I18n.get('State_License_Invalid_Message'));
    NX.Bookmarks.navigateTo(NX.Bookmarks.fromToken('admin/system/licensing'));
  }

});

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
 * Migration content-options step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.ContentStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.State',
    'NX.coreui.migration.ContentScreen'
  ],

  screen: 'NX.coreui.migration.ContentScreen',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=back]': {
        click: me.moveBack
      },
      'button[action=next]': {
        click: me.doNext
      },
      'button[action=cancel]': {
        click: me.cancel
      }
    });
  },

  /**
   * @override
   */
  reset: function() {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.getForm().reset();
    }

    me.unset('content-options');
    me.callParent();
  },

  /**
   * @private
   */
  doNext: function() {
    var values = this.getScreenCmp().getForm().getFieldValues();
    this.set('content-options', values);

    // when options indicate repositories, move to next
    if (values['repositories']) {
      this.moveNext();
    }
    else {
      // otherwise there is no more configuration, prepare plan
      this.controller.configure();
    }
  }
});

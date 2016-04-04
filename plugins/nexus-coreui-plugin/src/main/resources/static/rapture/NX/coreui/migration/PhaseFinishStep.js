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
 * Migration FINISH phase step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PhaseFinishStep', {
  extend: 'NX.coreui.migration.ProgressStepSupport',
  requires: [
    'NX.coreui.migration.PhaseFinishScreen'
  ],

  screen: 'NX.coreui.migration.PhaseFinishScreen',
  phase: 'FINISH',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=done]': {
        click: me.doDone
      }
    });

    me.callParent();
  },

  /**
   * @override
   */
  reset: function() {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.down('button[action=done]').disable();
    }
    me.callParent();
  },

  /**
   * @override
   */
  doComplete: function() {
    this.getScreenCmp().down('button[action=done]').enable();
  },

  /**
   * @private
   */
  doDone: function() {
    var me = this;

    me.mask(NX.I18n.render(me, 'Done_Mask'));

    NX.direct.migration_Assistant.done(function (response, event) {
      me.unmask();

      if (event.status && response.success) {
        me.finish();

        NX.Messages.success(NX.I18n.render(me, 'Done_Message'));
      }
    });
  }
});

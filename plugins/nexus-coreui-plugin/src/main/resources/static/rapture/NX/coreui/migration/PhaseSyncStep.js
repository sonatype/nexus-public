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
 * Migration SYNC phase step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PhaseSyncStep', {
  extend: 'NX.coreui.migration.ProgressStepSupport',
  requires: [
    'NX.coreui.migration.PhaseSyncScreen',
    'NX.I18n'
  ],

  config: {
    screen: 'NX.coreui.migration.PhaseSyncScreen',
    enabled: true
  },

  phase: 'SYNC',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=abort]': {
        click: me.doAbort
      },
      'button[action=continue]': {
        click: me.doContinue
      }
    });

    me.callParent();
  },

  /**
   * @private
   * @type {boolean}
   */
  checkSyncStatus: true,

  /**
   * @private
   * @type {boolean}
   */
  waitingToFinish: false,

  /**
   * @override
   */
  prepare: function () {
    var me = this,
        selectedRepos = me.controller.getContext().get('selected-repositories');

    me.checkSyncStatus = selectedRepos && selectedRepos.length;
    me.waitingToFinish = false;
    me.callParent();
  },

  /**
   * @override
   */
  reset: function() {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.down('button[action=continue]').setVisible(true);
      screen.down('button[action=continue]').disable();
      screen.down('button[action=abort]').enable();
    }
    me.callParent();
  },

  /**
   * @override
   */
  refresh: function() {
    var me = this,
        screen = me.getScreenCmp();

    me.callParent();

    if (screen && (me.checkSyncStatus || me.controller.getContext().get('checkSyncStatus'))) {
      NX.direct.migration_Assistant.syncStatus(function (response, event) {
        var isComplete = response.success && response.data.waitingForChanges && response.data.scanComplete;
        if (!me.waitingToFinish && event.status && isComplete) {
          screen.down('button[action=continue]').enable().setText(NX.I18n.render(screen, 'Continue_Button'));
        }
      });
    }
  },

  /**
   * @override
   */
  doComplete: function() {
    var me = this;

    me.mask(NX.I18n.render(me, 'Finish_Mask'));

    NX.direct.migration_Assistant.finish(function (response, event) {
      me.unmask();

      if (event.status && response.success) {
        me.moveNext();

        NX.Messages.success(NX.I18n.render(me, 'Finish_Message'));
      }
    });
  },

  /**
   * @private
   */
  doAbort: function() {
    var me = this;

    NX.Dialogs.askConfirmation(
        NX.I18n.render(me, 'Abort_Confirm_Title'),
        NX.I18n.render(me, 'Abort_Confirm_Text'),
        function () {
          me.mask(NX.I18n.render(me, 'Abort_Mask'));

          me.autoRefresh(false);

          NX.direct.migration_Assistant.abort(function (response, event) {
            me.unmask();

            if (event.status && response.success) {
              me.controller.reset();

              NX.Messages.warning(NX.I18n.render(me, 'Abort_Message'));
            }
          });
        }
    );
  },

  /**
   * @private
   */
  doContinue: function() {
    var me = this;

    NX.Dialogs.askConfirmation(
      NX.I18n.render(me, 'Stop_Waiting_Confirm_Title'),
      NX.I18n.render(me, 'Stop_Waiting_Confirm_Text'),
      function () {
        var screen = me.getScreenCmp();
        screen.down('button[action=continue]').disable()
            .setText(NX.I18n.render(screen, 'Continue_Button_Pending'));
        NX.direct.migration_Assistant.stopWaiting(function (response, event) {
          if (event.status && response.success && response.data) {
            me.waitingToFinish = true;
          }
        });
      }
    );
  }

});

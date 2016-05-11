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
 * Migration SYNC phase step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PhaseSyncStep', {
  extend: 'NX.coreui.migration.ProgressStepSupport',
  requires: [
    'NX.coreui.migration.PhaseSyncScreen'
  ],

  screen: 'NX.coreui.migration.PhaseSyncScreen',
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
      },
      'button[action=finish]': {
        click: me.doFinish
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
   * @override
   */
  prepare: function () {
    var me = this,
        selectedRepos = me.controller.getContext().get('selected-repositories');

    me.checkSyncStatus = selectedRepos && selectedRepos.length;
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
      screen.down('button[action=finish]').setVisible(false);
      screen.down('button[action=finish]').disable();
      screen.down('button[action=abort]').enable();
    }
    me.callParent();
  },

  /**
   * @override
   */
  refresh: function() {
    var me = this;

    me.callParent();

    if (me.checkSyncStatus) {
      NX.direct.migration_Assistant.syncStatus(function (response, event) {
        if (event.status && response.success && response.data.waitingForChanges) {
          me.getScreenCmp().down('button[action=continue]').enable();
        }
      });
    }
  },

  /**
   * @override
   */
  doComplete: function() {
    var me = this,
        screen = me.getScreenCmp();

    // if there are no repositories configured for migration, hide the 'Stop Monitoring' button
    if (!me.checkSyncStatus) {
      screen.down('button[action=continue]').setVisible(false);
      screen.down('button[action=finish]').setVisible(true);
    }

    screen.down('button[action=finish]').enable();
    screen.down('button[action=abort]').disable();
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
      'Stop waiting for changes',
      'Any future changes to repositories will not be migrated. Proceed?',
      function () {
        me.mask('Finalizing changes');

        me.autoRefresh(false);
        me.getScreenCmp().down('button[action=continue]').disable();

        NX.direct.migration_Assistant.stopWaiting(function (response, event) {
          me.unmask();

          if (event.status && response.success && response.data) {
            me.getScreenCmp().down('button[action=continue]').setVisible(false);
            me.getScreenCmp().down('button[action=finish]').setVisible(true);
            NX.Messages.success('Changes finalized');
          }
          else {
            me.getScreenCmp().down('button[action=continue]').enable();
          }

          me.autoRefresh(true);
        });
      }
    );
  },

  /**
   * @private
   */
  doFinish: function() {
    var me = this;

    NX.Dialogs.askConfirmation(
        NX.I18n.render(me, 'Finish_Confirm_Title'),
        NX.I18n.render(me, 'Finish_Confirm_Text'),
        function () {
          me.mask(NX.I18n.render(me, 'Finish_Mask'));

          NX.direct.migration_Assistant.finish(function (response, event) {
            me.unmask();

            if (event.status && response.success) {
              me.moveNext();

              NX.Messages.success(NX.I18n.render(me, 'Finish_Message'));
            }
          });
        }
    );
  }

});

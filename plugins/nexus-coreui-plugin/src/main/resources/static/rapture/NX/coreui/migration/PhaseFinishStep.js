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
 * Migration FINISH phase step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PhaseFinishStep', {
  extend: 'NX.coreui.migration.ProgressStepSupport',
  requires: [
    'NX.coreui.migration.PhaseFinishScreen',
    'NX.Messages',
    'NX.Dialogs',
    'NX.I18n'
  ],

  config: {
    screen: 'NX.coreui.migration.PhaseFinishScreen',
    enabled: true
  },

  phase: 'FINISH',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=abort]': {
        click: me.doAbort
      },
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
      screen.down('button[action=abort]').enable();
      screen.down('button[action=done]').disable();
    }
    me.callParent();
  },

  /**
   * @override
   */
  doComplete: function() {
    this.getScreenCmp().down('button[action=abort]').disable();
    this.getScreenCmp().down('button[action=done]').enable();
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

  successfulFinish: function() {
    var me = this;
    me.finish();
    NX.Messages.success(NX.I18n.render(me, 'Done_Message'));
  },

  maybeOpenDoneModal: function() {
    var me = this,
        buildBrowse = NX.State.getValue('migration', {})['buildBrowse'],
        buildSearch = NX.State.getValue('migration', {})['buildSearch'];

    if (buildBrowse && buildSearch) {
      me.successfulFinish();
    }
    else {
      var dialogMsg = NX.I18n.render(me, 'Done_Dialog_prefix');
      if (!buildSearch) {
        dialogMsg += NX.I18n.render(me, 'Done_Dialog_with_search');
      }
      if (!buildBrowse) {
        dialogMsg += NX.I18n.render(me, 'Done_Dialog_with_browse');
      }
      dialogMsg += NX.I18n.render(me, 'Done_Dialog_suffix');
      NX.Dialogs.showInfo(
          NX.I18n.render(me, 'Done_Dialog_title'),
          dialogMsg,
          {
            fn: me.successfulFinish,
            scope: me
          },
          true
      );
    }
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
        me.maybeOpenDoneModal();
      }
    });
  }
});

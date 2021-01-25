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
 * Migration PREPARE phase step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PhasePrepareStep', {
  extend: 'NX.coreui.migration.ProgressStepSupport',
  requires: [
    'NX.coreui.migration.PhasePrepareScreen'
  ],

  config: {
    screen: 'NX.coreui.migration.PhasePrepareScreen',
    enabled: true
  },

  phase: 'PREPARE',

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
   * @override
   */
  reset: function() {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.down('button[action=continue]').disable();
      screen.down('button[action=abort]').enable();
    }
    me.callParent();
  },

  /**
   * @override
   */
  doComplete: function() {
    this.getScreenCmp().down('button[action=continue]').enable();
    this.getScreenCmp().down('button[action=abort]').disable();
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
    me.mask(NX.I18n.render(me, 'Continue_Mask'));

    NX.direct.migration_Assistant.sync(function (response, event) {
      me.unmask();

      if (event.status && response.success) {
        // need to set forms so they won't be queried for dirtiness
        var forms = Ext.ComponentQuery.query('form[settingsForm=true]');
        if (forms.length !== 0) {
          Ext.Array.each(forms, function (form) {
            form.settingsForm = false;
          });
        }
        // now ready to move ahead
        me.moveNext();

        NX.Messages.success(NX.I18n.render(me, 'Continue_Message'));
      }
    });
  }

});

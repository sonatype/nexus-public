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
 * Migration preview step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PreviewStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.coreui.migration.PreviewScreen'
  ],

  config: {
    screen: 'NX.coreui.migration.PreviewScreen',
    enabled: true
  },

  resetOnBack: true,

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=back]': {
        click: me.moveBack
      },
      'button[action=begin]': {
        click: me.doBegin
      },
      'button[action=cancel]': {
        click: me.cancel
      },
      'gridpanel': {
        cellclick: me.doDisplayDetail
      }
    });
  },

  /**
   * @override
   */
  prepare: function () {
    var preview = this.get('plan-preview'),
        store = this.getStore('NX.coreui.migration.PreviewStore');

    store.loadData(preview.steps, false);

    // if plan-preview is valid, then enable begin button
    if (preview.valid) {
      this.getScreenCmp().down('button[action=begin]').enable();
    }
  },

  /**
   * @override
   */
  reset: function () {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.down('button[action=begin]').disable();
    }

    me.getStore('NX.coreui.migration.PreviewStore').removeAll(true);
    me.callParent();
  },

  /**
   * @private
   */
  doBegin: function () {
    var me = this;

    NX.Dialogs.askConfirmation(
        NX.I18n.render(me, 'Begin_Confirm_Title'),
        NX.I18n.render(me, 'Begin_Confirm_Text'),
        function () {
          me.mask(NX.I18n.render(me, 'Begin_Mask'));

          NX.direct.migration_Assistant.prepare(function (response, event) {
            me.unmask();

            if (event.status && response.success) {
              me.moveNext();

              NX.Messages.success(NX.I18n.render(me, 'Begin_Message'));
            }
          });
        }
    );
  },

  /**
   *@private
   */
  doDisplayDetail: function (grid, td, cellIndex, record) {
    this.controller.displayPlanStepDetail(record.get('id'));
    return false;
  }
});

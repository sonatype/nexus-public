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
 * Support for progress steps.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.coreui.migration.ProgressStepSupport', {
  extend: 'NX.wizard.Step',
  requires: [
    'Ext.util.TaskManager',
    'NX.I18n'
  ],

  config: {
    /**
     * @cfg {String}
     */
    phase: null
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'gridpanel': {
        cellclick: me.doDisplayDetail
      }
    });
  },

  /**
   * @override
   */
  prepare: function () {
    var me = this;

    // TODO: Likely have to listen for session timeout events to stop auto-refreshing

    // add loading mask, remove once store data changes first time
    me.mask(NX.I18n.render(me, 'Loading_Mask'));
    me.getStore('NX.coreui.migration.ProgressStore').on({
      single: true,
      datachanged: function() {
        me.unmask();
      }
    });

    me.autoRefresh(true);
  },

  /**
   * @override
   */
  reset: function() {
    this.autoRefresh(false);
    this.getStore('NX.coreui.migration.ProgressStore').removeAll(true);
    this.callParent();
  },

  //
  // Auto-refresh
  //

  /**
   * The auto-refresh task, or undefined if not running.
   *
   * @private
   * @type {Ext.util.TaskRunner.Task}
   */
  refreshTask: undefined,

  /**
   * Toggle auto-refresh.
   *
   * @protected
   * @param {boolean} enable
   */
  autoRefresh: function(enable) {
    var me = this;

    if (enable) {
      //<if assert>
      NX.Assert.assert(me.refreshTask === undefined, 'Auto-refresh task already exists');
      //</if>

      me.refreshTask = Ext.util.TaskManager.start({
        interval: 1000,
        fireOnStart: true,
        run: function() {
          me.refresh();
        }
      });

      //<if debug>
      me.logDebug('Auto-refresh enabled');
      //</if>
    }
    else if (me.refreshTask) {
      Ext.util.TaskManager.stop(me.refreshTask);
      delete me.refreshTask;

      //<if debug>
      me.logDebug('Auto-refresh disabled');
      //</if>
    }
  },

  /**
   * @override
   */
  refresh: function() {
    var me = this;

    NX.direct.migration_Progress.read(me.getPhase(), function (response, event) {
      if (event.status && response.success) {
        var payload = response.data,
            store = me.getStore('NX.coreui.migration.ProgressStore');

        me.getScreenCmp().getProgressBar().updateProgress(payload.complete);

        // replace records
        store.loadData(payload.steps, false);

        // if completed, stop auto-refresh and inform
        if (payload.complete / 1 === 1) {
          me.autoRefresh(false);
          me.doComplete();
        }
      }
    });
  },

  /**
   * Extension-point to inform when progress has indicated completion.
   *
   * @protected
   * @template
   */
  doComplete: Ext.emptyFn,

  /**
   * @private
   */
  doDisplayDetail: function(grid, td, cellIndex, record) {
    this.controller.displayPlanStepDetail(record.get('id'));
    return false;
  }
});

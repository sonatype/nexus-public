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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Logging controller.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.controller.Logging', {
  extend: 'Nexus.controller.Controller',

  requires: [
    'Nexus.siesta',
    'Nexus.logging.Icons',
    'Nexus.logging.view.Panel',
    'Nexus.logging.view.Add',
    'Nexus.logging.view.Mark',
    'Nexus.util.DownloadHelper'
  ],

  init: function () {
    var me = this;

    me.control({
      '#logging': {
        afterrender: me.bindToLoggersGrid,
        activate: me.onLoggingActivate,
        deactivate: me.onLoggingDeactivate,
        beforedestroy: me.onLoggingDeactivate
      },
      '#nx-logging-button-refresh-loggers': {
        click: me.loadLoggers
      },
      '#nx-logging-button-add-logger': {
        click: me.showAddLogger
      },
      '#nx-logging-button-remove-loggers': {
        click: me.removeLogger
      },
      '#nx-logging-button-reset-loggers': {
        click: me.resetLoggers
      },
      '#nx-logging-button-mark': {
        click: me.showMarkLog
      },
      '#nx-logging-button-add-save': {
        click: me.addLogger
      },
      '#nx-logging-button-mark-save': {
        click: me.markLog
      },
      '#nx-logging-view-log': {
        activate: me.onLogTabActivate,
        deactivate: me.onLogTabDeactivate
      },
      '#nx-logging-button-refresh-log': {
        click: me.refreshLog
      },
      '#nx-logging-combo-refresh-period': {
        select: me.changeRefreshPeriod
      },
      '#nx-logging-combo-refresh-size': {
        select: me.changeRefreshSize
      },
      '#nx-logging-button-download-log': {
        click: me.downloadLog
      }
    });

    me.addNavigationMenu();
  },

  /**
   * Adds a menu entry in left navigation bar under config section.
   * @private
   */
  addNavigationMenu: function () {
    // install panel into main NX navigation
    Sonatype.Events.on('nexusNavigationInit', function (panel) {
      var sp = Sonatype.lib.Permissions;

      if (sp.checkPermission('nexus:logconfig', sp.READ) || sp.checkPermission('nexus:logs', sp.READ)) {
        panel.add({
          enabled: true,
          sectionId: 'st-nexus-config',
          title: 'Logging',
          tabId: 'logging',
          tabCode: function () {
            return Ext.create({
              xtype: 'nx-logging-view-panel',
              id: 'logging'
            });
          }
        });
      }
    });
  },

  /**
   * Binds to grid after it has been rendered.
   * @param {Nexus.logging.view.Panel} loggingPanel logging panel to be controlled
   * @private
   */
  bindToLoggersGrid: function (loggingPanel) {
    var me = this,
        grid = loggingPanel.down('nx-logging-view-loggers'),
        store = grid.getStore();

    me.controlSelection(grid);
    store.on('write', me.onSuccessfulWrite, me);
    store.load();
  },

  /**
   * Reloads logger store.
   * @param {Ext.Button} button refresh button that triggered the action (from loggers grid toolbar)
   */
  loadLoggers: function (button) {
    var loggersGrid = button.up('nx-logging-view-loggers'),
        store = loggersGrid.getStore();

    store.load();
  },

  /**
   * Configures handling of grid selection events.
   * @param {Nexus.logging.view.Loggers} loggersGrid logging panel to be controlled
   * @private
   */
  controlSelection: function (loggersGrid) {
    var sp = Sonatype.lib.Permissions,
        removeBtn;

    if (sp.checkPermission('nexus:logconfig', sp.EDIT)) {
      removeBtn = loggersGrid.getTopToolbar().down('#nx-logging-button-remove-loggers');

      loggersGrid.getSelectionModel().on('selectionchange', function (sm, selection) {
        if (sm.hasSelection() && selection.record.get('name') !== 'ROOT') {
          removeBtn.enable();
        }
        else {
          removeBtn.disable();
        }
      });
    }
  },

  /**
   * Shows success messages after records has been successfully written.
   * @private
   */
  onSuccessfulWrite: function (store, action, result, res, rs) {
    if (Ext.isDefined(rs)) {
      Ext.each(rs, function (record) {
        var name = record.get('name');

        if (action === Ext.data.Api.actions.create) {
          Nexus.messages.show('Logging', 'Logger added: ' + name);
        }
        else if (action === Ext.data.Api.actions.update) {
          Nexus.messages.show('Logging', 'Logger updated: ' + name);
        }
        else if (action === Ext.data.Api.actions.destroy) {
          Nexus.messages.show('Logging', 'Logger removed: ' + name);
        }
      });
      store.load();
    }
  },

  /**
   * Opens add logger window.
   * @param {Ext.Button} button add button that triggered the action (from loggers grid toolbar)
   * @private
   */
  showAddLogger: function (button) {
    var win = Ext.create({xtype: 'nx-logging-view-add'});

    win.grid = button.up('nx-logging-view-loggers');
    win.show();
  },

  /**
   * Adds a logger to logger store.
   * @param {Ext.Button} button save button that triggered the action (from add window)
   * @private
   */
  addLogger: function (button) {
    var me = this,
        win = button.up('nx-logging-view-add'),
        store = win.grid.getStore(),
        form = win.down('form').getForm(),
        values = form.getFieldValues(),
        record = store.getById(values.name);

    if (Ext.isDefined(record)) {
      Ext.Msg.show({
        title: 'Confirm',
        msg: 'Logger "' + values.name
            + '" is already configured. Would you like to update its level to "' + values.level + '"?',
        buttons: Ext.Msg.YESNO,
        icon: Ext.MessageBox.QUESTION,
        closeable: false,
        scope: me,
        fn: function (buttonName) {
          if (buttonName === 'yes') {
            record.set('level', values['level']);
            store.save();
            win.grid.getSelectionModel().select(store.indexOf(record), 1);
            win.close();
          }
        }
      });
    }
    else {
      record = new store.recordType(values);
      store.addSorted(record);
      win.grid.getSelectionModel().select(store.indexOf(record), 1);
      win.close();
    }
  },

  /**
   * Remove selected logger (if any)
   * @param {Ext.Button} button delete button that triggered the action (from loggers grid toolbar)
   * @private
   */
  removeLogger: function (button) {
    var loggersGrid = button.up('nx-logging-view-loggers'),
        sm = loggersGrid.getSelectionModel(),
        store = loggersGrid.getStore(),
        icons = Nexus.logging.Icons,
        name;

    // if we have a logger selected, confirm before removal
    if (sm.hasSelection()) {
      name = sm.selection.record.get('name');
      Ext.Msg.show({
        title: 'Remove logger',
        msg: 'Remove "' + name + '" logger ?',
        buttons: Ext.Msg.OKCANCEL,
        icon: icons.get('loggers_remove').variant('x32').cls,
        fn: function (btn) {
          if (btn === 'ok') {
            store.remove(sm.selection.record);
          }
        }
      });
    }
  },

  /**
   * Resets all loggers to their default levels.
   *
   * @param {Ext.Button} button reset button that triggered the action (from loggers grid toolbar)
   * @private
   */
  resetLoggers: function (button) {
    var loggersGrid = button.up('nx-logging-view-loggers'),
        store = loggersGrid.getStore(),
        icons = Nexus.logging.Icons;

    Ext.Msg.show({
      title: 'Reset loggers',
      msg: 'Reset loggers to their default levels?',
      buttons: Ext.Msg.OKCANCEL,
      icon: icons.get('loggers_reset').variant('x32').cls,
      fn: function (btn) {
        if (btn === 'ok') {
          Ext.Ajax.request({
            url: Nexus.siesta.basePath + '/logging/loggers',
            method: 'DELETE',
            suppressStatus: true,
            callback: function () {
              store.load();
            },
            success: function () {
              Nexus.messages.show('Logging', 'Loggers had been reset');
            },
            failure: function (response) {
              Nexus.messages.show('Logging', 'Failed to reset loggers: ' + me.parseExceptionMessage(response));
            }
          });
        }
      }
    });
  },

  /**
   * Opens mark log window.
   * @private
   */
  showMarkLog: function () {
    Ext.create({xtype: 'nx-logging-view-mark'}).show();
  },

  /**
   * Adds a marker to the log by calling PUT on /service/siesta/logging/log/mark REST resource.
   * @param {Ext.Button} button save button that triggered the action (from mark log window)
   * @private
   */
  markLog: function (button) {
    var me = this,
        win = button.up('nx-logging-view-mark'),
        form = win.down('form').getForm(),
        values = form.getFieldValues();

    win.close();

    Ext.Ajax.request({
      url: Nexus.siesta.basePath + '/logging/log/mark',
      method: 'PUT',
      suppressStatus: true,
      jsonData: values,
      success: function () {
        Nexus.messages.show('Logging', 'Log has been marked with: ' + values.message);
        // refresh the log view
        me.retrieveLog(Ext.getCmp('nx-logging-view-log'));
      },
      failure: function (response) {
        Nexus.messages.show('Logging', 'Failed to mark log file: ' + me.parseExceptionMessage(response));
      }
    });
  },

  /**
   * Retrieves the log.
   * @param {Ext.Button} button the refresh button that triggered the action (from log panel toolbar)
   * @private
   */
  refreshLog: function (button) {
    this.retrieveLog(button.up('nx-logging-view-log'));
  },

  /**
   * Opens a new browser window pointing to GET /service/siesta/logging/log.
   * @private
   */
  downloadLog: function () {
    Nexus.util.DownloadHelper.downloadUrl(Nexus.siesta.basePath + '/logging/log');
  },

  /**
   * Starts log retrieving task on log tab activation.
   * @param {Ext.Panel} logPanel that was activated
   * @private
   */
  onLogTabActivate: function (logPanel) {
    var me = this,
        task = logPanel.retrieveLogTask;

    task.run = function () {
      me.retrieveLog(logPanel);
    };
    task.start();
  },

  /**
   * Stops log retrieving task on log tab deactivation.
   * @param {Ext.Panel} logPanel that was deactivated
   * @private
   */
  onLogTabDeactivate: function (logPanel) {
    var task = logPanel.retrieveLogTask;

    task.stop();
    delete task.run;
  },

  /**
   * Starts log retrieving task on logging panel activation if log tab is active.
   * @param {Ext.Panel} panel logging panel that was activated
   * @private
   */
  onLoggingActivate: function (panel) {
    var logPanel = panel.down('nx-logging-view-log');

    if (Ext.isDefined(logPanel)) {
      logPanel.retrieveLogTask.start();
    }
  },

  /**
   * Stops log retrieving task on this panel activation if log tab is active.
   * @param {Ext.Panel} panel logging panel that was deactivated
   * @private
   */
  onLoggingDeactivate: function (panel) {
    var logPanel = panel.down('nx-logging-view-log');

    if (Ext.isDefined(logPanel)) {
      logPanel.retrieveLogTask.stop();
    }
  },

  /**
   * Changes retrieving task recurrence interval when value is changed via refresh period combobox from log panel toolbar.
   * @param {Ext.form.ComboBox} combo refresh period combobox
   * @param {Ext.data.Record} record selected record in combobox
   * @private
   */
  changeRefreshPeriod: function (combo, record) {
    var millis = record.get('seconds') * 1000,
        logPanel = combo.up('nx-logging-view-log'),
        task = logPanel.retrieveLogTask;

    task.changeInterval(millis);
  },

  /**
   * Triggers log retrieval when value is changed via refresh size combobox from log panel toolbar.
   * @param {Ext.form.ComboBox} combo refresh size combobox
   * @private
   */
  changeRefreshSize: function (combo) {
    this.retrieveLog(combo.up('nx-logging-view-log'));
  },

  /**
   * Retrieves log from /service/internal/logs/nexus.log and shows it in log panel.
   * @param {Nexus.logging.view.Log} logPanel where teh log should be shown
   * @private
   */
  retrieveLog: function (logPanel) {
    var me = this,
        size = logPanel.getTopToolbar().down('#nx-logging-combo-refresh-size').getValue(),
        mask;

    mask = NX.create('Ext.LoadMask', logPanel.body, {
      msg: 'Loading...'
    });

    mask.show();

    me.logDebug('Retrieving last ' + size + 'kb from log');

    Ext.Ajax.request({
      url: Nexus.siesta.basePath + '/logging/log',
      method: 'GET',
      headers: {
        'accept': 'text/plain'
      },
      params: {
        bytesCount: -1024 * size
      },
      scope: me,
      suppressStatus: true,
      success: function (response) {
        mask.hide();
        logPanel.showLog(response.responseText);
      },
      failure: function (response) {
        mask.hide();
        logPanel.showLog(
            'Failed to retrieve log due to "' + me.parseExceptionMessage(response) + '".'
        );
      }
    });
  }

});
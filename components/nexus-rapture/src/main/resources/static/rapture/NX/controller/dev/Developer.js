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
/*global Ext, NX, console*/

/**
 * Developer controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.dev.Developer', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.state.Manager',
    'NX.State',
    'NX.Messages'
  ],

  views: [
    'dev.Panel',
    'dev.Tests',
    'dev.Styles',
    'dev.Icons',
    'dev.Features',
    'dev.State',
    'dev.Stores',
    'dev.Logging'
  ],

  refs: [
    {
      ref: 'branding',
      selector: 'nx-header-branding'
    },
    {
      ref: 'developer',
      selector: 'nx-dev-panel'
    }
  ],

  /**
   * @protected
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#State': {
          debugchanged: me.manageDeveloperPanel
        }
      },
      component: {
        'nx-dev-panel': {
          afterrender: me.manageDeveloperPanel
        },
        'nx-dev-panel tool[type=maximize]': {
          click: me.onMaximize
        },

        // Tests actions
        'nx-dev-tests button[action=testError]': {
          click: me.testError
        },
        'nx-dev-tests button[action=testExtError]': {
          click: me.testExtError
        },
        'nx-dev-tests button[action=testMessages]': {
          click: me.testMessages
        },
        'nx-dev-tests button[action=toggleUnsupportedBrowser]': {
          click: me.toggleUnsupportedBrowser
        },
        'nx-dev-tests button[action=showQuorumWarning]': {
          click: me.showQuorumWarning
         },
        'nx-dev-tests button[action=clearLocalState]': {
          click: me.clearLocalState
        }
      }
    });
  },

  /**
   * @override
   */
  onLaunch: function () {
    var me = this;
    Ext.each(Ext.ComponentQuery.query('nx-dev-panel'), function (panel) {
      me.manageDeveloperPanel(panel);
    });
  },

  /**
   * Show/Hide developer panel based on debug state.
   *
   * @private
   * @param {Ext.Panel} developerPanel
   */
  manageDeveloperPanel: function (developerPanel) {
    var debug = NX.State.getValue('debug');

    developerPanel = developerPanel || this.getDeveloper();

    if (developerPanel) {
      if (debug) {
        developerPanel.show();
      }
      else {
        developerPanel.hide();
      }
    }
  },

  /**
   * Maximimze developer panel.
   *
   * @private
   */
  onMaximize: function(tool) {
    var panel = tool.up('nx-dev-panel'),
        tabs = panel.down('tabpanel'),
        win;

    panel.remove(tabs, false);

    win = Ext.create('Ext.window.Window', {
      title: panel.title,
      iconCls: panel.iconCls,
      glyph: panel.glyph,

      maximized: true,
      autoScroll: true,
      closable: false,

      layout: 'fit',
      items: tabs,
      tools: [
        {
          type: 'close',
          handler: function () {
            win.hide(panel, function () {
              win.remove(tabs, false);
              panel.add(tabs);
              win.destroy();
            });
          }
        }
      ]
    });

    win.show(panel);
  },

  /**
   * Attempts to call a object's method that doesn't exist to produce a low-level javascript error.
   *
   * @private
   */
  testError: function () {
    console.log_no_such_method();
  },

  /**
   * Raises an Ext.Error so we can see how that behaves.
   *
   * @private
   */
  testExtError: function () {
    Ext.Error.raise('simulated error');
  },

  /**
   * Adds messages for each of the major types to view styling, etc.
   *
   * @private
   */
  testMessages: function () {
    Ext.each(['default', 'primary', 'danger', 'warning', 'success'], function (type) {
      NX.Messages.add({
        type: type,
        text: 'test of ' + type
      });
    });
  },

  /**
   * Toggle the unsupported browser application state.
   *
   * @private
   */
  toggleUnsupportedBrowser: function() {
    NX.State.setBrowserSupported(!NX.State.isBrowserSupported());
  },

  /**
   * Modify state so that the database quorum warning is shown in the UI.
   *
   * @private
   */
  showQuorumWarning: function () {
    NX.State.setValue('quorum', { 'quorumPresent': false});
  },

  /**
   * Clear local browser state.
   *
   * @private
   */
  clearLocalState: function() {
    var provider = Ext.state.Manager.getProvider();
    // HACK: provider.state is private
    Ext.Object.each(provider.state, function (key, value) {
      provider.clear(key);
    });
  }

});

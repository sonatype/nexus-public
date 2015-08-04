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
 * Atlas controller.
 *
 * @since 2.7
 */
NX.define('Nexus.atlas.controller.Atlas', {
  extend: 'Nexus.controller.Controller',

  requires: [
    'Nexus.siesta',
    'Nexus.atlas.view.Panel',
    'Nexus.atlas.view.SupportZipCreated',
    'Nexus.util.DownloadHelper'
  ],

  init: function() {
    var me = this;

    me.control({
      '#nx-atlas-view-sysinfo': {
        'activate': me.loadSysInfo
      },
      '#nx-atlas-view-sysinfo-button-refresh': {
        'click': me.refreshSysInfo
      },
      '#nx-atlas-view-sysinfo-button-download': {
        'click': me.downloadSysInfo
      },
      '#nx-atlas-view-sysinfo-button-print': {
        'click': me.printSysInfo
      },
      '#nx-atlas-button-supportzip-create': {
        'click': me.createSupportZip
      },
      '#nx-atlas-button-supportzip-download': {
        'authenticated': me.downloadSupportZip
      },
      '#nx-atlas-button-supportrequest-makerequest': {
        'click': me.makeSupportRequest
      }
    });

    me.addNavigationMenu();
  },

  /**
   * Install panel into main navigation menu.
   *
   * @private
   */
  addNavigationMenu: function() {
    Sonatype.Events.on('nexusNavigationInit', function(panel) {
      var sp = Sonatype.lib.Permissions;

      panel.add({
        enabled: sp.checkPermission('nexus:atlas', sp.READ),
        sectionId: 'st-nexus-config',
        title: 'Support Tools',
        tabId: 'supporttools',
        tabCode: function() {
          return Ext.create({ xtype: 'nx-atlas-view-panel', id: 'supporttools' });
        }
      });
    });
  },

  /**
   * Load system information panel.
   *
   * @private
   */
  loadSysInfo: function(panel) {
    var me = this,
        mask = NX.create('Ext.LoadMask', panel.getEl(), { msg: 'Loading...' });

    me.logDebug('Refreshing sysinfo');

    mask.show();
    Ext.Ajax.request({
      url: Nexus.siesta.basePath + '/atlas/system-information',

      scope: me,
      callback: function() {
        mask.hide()
      },
      success: function(response, opts) {
        var obj = Ext.decode(response.responseText);
        panel.setInfo(obj);
      }
      // TODO: handle failure
    });
  },

  /**
   * Refresh the system information panel.
   *
   * @private
   */
  refreshSysInfo: function(button) {
    this.loadSysInfo(Ext.getCmp('nx-atlas-view-sysinfo'));
  },

  /**
   * Download system information report.
   *
   * @private
   */
  downloadSysInfo: function(button) {
    Nexus.util.DownloadHelper.downloadUrl(Nexus.siesta.basePath + '/atlas/system-information');
  },

  /**
   * Print system information panel contents.
   *
   * @private
   */
  printSysInfo: function(button) {
    var me = this,
        panel = Ext.getCmp('nx-atlas-view-sysinfo'),
        win;

    win = window.open('', '', 'width=640,height=480');
    if (win == null) {
      alert('Print window pop-up was blocked!');
      return;
    }

    win.document.write('<html><head>');
    win.document.write('<title>System Information</title>');

    // FIXME: Ideally want some of the style in here
    // FIXME: ... but unsure how to resolve that URL (since it could change for debug, etc)
    // FIXME: See for more details http://stackoverflow.com/questions/5939456/how-to-print-extjs-component

    win.document.write('</head><body>');
    win.document.write(panel.body.dom.innerHTML);
    win.document.write('</body></html>');
    win.print();
  },

  /**
   * Create support ZIP file.
   *
   * @private
   */
  createSupportZip: function(button) {
    var me = this,
        viewport = button.up('viewport'), // mask entire viewport while creating
        mask = NX.create('Ext.LoadMask', viewport.getEl(), { msg: 'Creating support ZIP file...' }),
        values = Ext.getCmp('nx-atlas-view-supportzip').getValues(),
        request = {};

    me.logDebug('Creating support ZIP file');

    mask.show();

    // translate 'on' -> true for boolean conversion
    Ext.iterate(values, function(key, value) {
      request[key] = value === 'on' ? true : value;
    });

    Ext.Ajax.request({
      url: Nexus.siesta.basePath + '/atlas/support-zip',
      method: 'POST',
      jsonData: request,

      scope: me,
      callback: function() {
        mask.hide()
      },
      success: function(response, opts) {
        var obj = Ext.decode(response.responseText),
            win = NX.create('Nexus.atlas.view.SupportZipCreated');

        win.setValues(obj);
        win.show();
      }
    });
  },

  /**
   * Download support ZIP file.
   *
   * @private
   */
  downloadSupportZip: function(button, authTicket) {
    var me = this,
        win = button.up('nx-atlas-view-supportzip-created'),
        fileName = win.getValues().name;

    // encode ticket for query-parameter
    authTicket = Sonatype.utils.base64.encode(authTicket);

    if (Nexus.util.DownloadHelper.downloadUrl(
        Nexus.siesta.basePath + '/wonderland/download/' + fileName + '?t=' + authTicket))
    {
      // if download was initated close the window
      win.close();
    }
  },

  /**
   * Open sonatype support in a new browser window/tab.
   *
   * @private
   */
  makeSupportRequest: function() {
    var win = NX.global.open('http://links.sonatype.com/products/nexus/pro/support-request');
    if (win == null) {
      alert('Window pop-up was blocked!');
    }
  }
});
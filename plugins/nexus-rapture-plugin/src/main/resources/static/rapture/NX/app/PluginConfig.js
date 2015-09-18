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
 * Core plugin configuration.
 *
 * @since 3.0
 */
Ext.define('NX.app.PluginConfig', {
  '@aggregate_priority': 100,

  requires: [
    'NX.app.PluginStrings'
  ],

  controllers: [
    'Content',
    'Dashboard',
    'Help',
    'Main',
    'Menu',
    'MenuGroup',
    'Refresh',
    'SettingsForm',
    'UiSessionTimeout',
    'User',

    {
      id: 'Branding',
      active: true // branding is active in also when we are unlicensed or browser is not supported
    },
    {
      id: 'Unlicensed',
      active: function () {
        return NX.app.Application.supportedBrowser() && NX.app.Application.unlicensed();
      }
    },
    {
      id: 'UnsupportedBrowser',
      active: function () {
        return NX.app.Application.unsupportedBrowser();
      }
    },

    // dev controllers (visible when ?debug and rapture capability debugAllowed = true)
    {
      id: 'dev.Conditions',
      active: function () {
        return NX.app.Application.debugMode;
      }
    },
    {
      id: 'dev.Developer',
      active: function () {
        return NX.app.Application.debugMode;
      }
    },
    {
      id: 'dev.Permissions',
      active: function () {
        return NX.app.Application.debugMode;
      }
    },
    {
      id: 'dev.Stores',
      active: function () {
        return NX.app.Application.debugMode;
      }
    },
    {
      id: 'dev.Logging',
      active: function () {
        return NX.app.Application.debugMode;
      }
    }
  ]
});

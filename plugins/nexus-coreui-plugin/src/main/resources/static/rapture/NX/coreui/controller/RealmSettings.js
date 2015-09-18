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
 * Security realms controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.RealmSettings', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n'
  ],

  views: [
    'security.RealmSettings'
  ],

  stores: [
    'RealmType'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-coreui-security-realm-settings'
    },
    {
      ref: 'form',
      selector: 'nx-coreui-security-realm-settings nx-settingsform'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Security/Realms',
      view: { xtype: 'nx-coreui-security-realm-settings' },
      text: NX.I18n.get('RealmSettings_Text'),
      description: NX.I18n.get('RealmSettings_Description'),
      iconConfig: {
        file: 'shield.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadRealmTypes
        }
      },
      component: {
        'nx-coreui-security-realm-settings': {
          beforerender: me.loadRealmTypes
        }
      }
    });
  },

  /**
   * @private
   */
  loadRealmTypes: function () {
    var me = this,
        panel = me.getPanel();

    if (panel) {
      me.getStore('RealmType').load(function() {
        // The form depends on this store, so load it after the store has loaded
        me.getForm().load();
      });
    }
  }

});

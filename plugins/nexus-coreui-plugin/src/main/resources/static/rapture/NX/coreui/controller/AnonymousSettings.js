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
 * Anonymous Security Settings controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.AnonymousSettings', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n'
  ],

  views: [
    'security.AnonymousSettings'
  ],

  stores: [
    'RealmType'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-coreui-security-anonymous-settings'
    },
    {
      ref: 'form',
      selector: 'nx-coreui-security-anonymous-settings nx-settingsform'
    }
  ],

  /**
   * @override
   */
  init: function() {
    this.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Security/Anonymous',
      text: NX.I18n.get('AnonymousSettings_Text'),
      description: NX.I18n.get('AnonymousSettings_Description'),
      view: {xtype: 'nx-coreui-security-anonymous-settings'},
      iconConfig: {
        file: 'user_silhouette.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, this);

    this.listen({
      controller: {
        '#Refresh': {
          refresh: this.loadRealmTypes
        }
      },
      component: {
        'nx-coreui-security-anonymous-settings': {
          beforerender: this.loadRealmTypes
        }
      }
    });
  },

  /**
   * @private
   */
  loadRealmTypes: function() {
    var panel = this.getPanel();

    if (panel) {
      this.getRealmTypeStore().load({
        scope: this,
        callback: function() {
          // The form depends on this store, so load it after the store has loaded
          var form = this.getForm();
          if (form) {
            form.load();
          }
        }
      });
    }
  }

});

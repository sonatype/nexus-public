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
 * Proprietary Repositories controller.
 *
 * @since 3.30
 */
Ext.define('NX.coreui.controller.ProprietaryRepositories', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n'
  ],

  views: [
    'repository.ProprietaryRepositories'
  ],

  stores: [
    'ProprietaryRepositories'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-coreui-repository-proprietary-repositories'
    },
    {
      ref: 'form',
      selector: 'nx-coreui-repository-proprietary-repositories nx-settingsform'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Repository/Proprietary',
      view: { xtype: 'nx-coreui-repository-proprietary-repositories' },
      text: NX.I18n.get('ProprietaryRepositories_Text'),
      description: NX.I18n.get('ProprietaryRepositories_Description'),
      iconConfig: {
        file: 'database_access.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadProprietaryRepositories
        }
      },
      component: {
        'nx-coreui-repository-proprietary-repositories': {
          beforerender: me.loadProprietaryRepositories
        }
      }
    });
  },

  /**
   * @private
   */
  loadProprietaryRepositories: function () {
    var me = this,
        panel = me.getPanel();

    if (panel) {
      me.getStore('ProprietaryRepositories').load(function() {
        // The form depends on this store, so load it after the store has loaded
        var form = me.getForm();
        if (form) {
          form.load();
        }
      });
    }
  }

});

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
 * Migration repository defaults step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.RepositoryDefaultsStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.coreui.migration.RepositoryDefaultsScreen',
    'NX.I18n'
  ],

  config: {
    screen: 'NX.coreui.migration.RepositoryDefaultsScreen',
    enabled: true
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=back]': {
        click: me.moveBack
      },
      'button[action=next]': {
        click: me.doNext
      },
      'button[action=cancel]': {
        click: me.cancel
      }
    });

    // toggle step enabled when content-options change
    me.getContext().on('add', function(index, value, key, opts) {
      if (key === 'content-options') {
        me.setEnabled(value['repositories']);
      }
    });
  },

  /**
   * Prepare the defaults form.
   *
   * @private
   */
  prepare: function () {
    var me = this;

    me.mask(NX.I18n.render(me, 'Loading_Mask'));

    me.getStore('Blobstore').load();

    // load defaults from server
    NX.direct.migration_Repository.defaults(function (response, event) {
      if (event.status && response.success) {
        me.getScreenCmp().getForm().setValues(response.data);
      }
      me.unmask();
    });
  },

  /**
   * @override
   */
  reset: function () {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.getForm().reset();
    }
    me.unset('repository-defaults');
    me.callParent();
  },

  /**
   * @private
   */
  doNext: function () {
    var me = this,
        values = me.getScreenCmp().getForm().getFieldValues();

    me.set('repository-defaults', {
      dataStore: values.dataStore,
      blobStore: values.blobStore,
      ingestMethod: values.ingestMethod
    });

    me.moveNext();
  }
});

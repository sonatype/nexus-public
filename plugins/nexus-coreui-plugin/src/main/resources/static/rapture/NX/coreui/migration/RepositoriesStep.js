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
 * Migration repository selection step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.RepositoriesStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.coreui.migration.RepositoriesScreen',
    'NX.coreui.migration.RepositoryCustomizeWindow'
  ],

  config: {
    screen: 'NX.coreui.migration.RepositoriesScreen',
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
      },
      'gridpanel nx-actioncolumn': {
        actionclick: me.doCustomize
      }
    });

    me.listen({
      component: {
        'nx-coreui-migration-repositorycustomize button[action=save]': {
          click: me.doCustomizeSave
        }
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
   * Prepare repository store and apply defaults to records.
   *
   * @override
   */
  prepare: function () {
    var me = this,
        uiSettings = NX.State.getValue('uiSettings', {});

    me.mask(NX.I18n.render(me, 'Loading_Mask'));

    // ensure blobstore is loaded, for customize window
    me.getStore('Blobstore').load();

    NX.direct.migration_Repository.read(function (response, event) {
      if (event.status && response.success) {
        //<if debug>
        me.logDebug('Loading', response.data.length, 'records');
        //</if>

        //<if assert>
        NX.Assert.assert(response.data.length !== 0, 'Missing records');
        //</if>

        var store = me.getStore('NX.coreui.migration.RepositoryStore'),
            defaults = me.get('repository-defaults');

        // apply configuration defaults if supported
        Ext.Array.each(response.data, function (data) {
          if (data.supported) {
            Ext.apply(data, defaults);
          }
        });

        store.loadData(response.data);
      }

      me.unmask();
    }, me, {
      timeout: uiSettings['longRequestTimeout'] * 1000
    });
  },

  /**
   * Clear the repository store on reset.
   *
   * @override
   */
  reset: function () {
    this.getStore('NX.coreui.migration.RepositoryStore').removeAll();
    this.unset('selected-repositories');
    this.callParent();
  },

  /**
   * @private
   */
  doNext: function () {
    var me = this,
        selections = me.getScreenCmp().getSelectionModel(),
        repositories = [];

    //<if assert>
    NX.Assert.assert(selections.getCount() !== 0, 'Missing selection');
    //</if>

    // collect selected repositories
    Ext.Array.each(selections.getSelection(), function (record) {
      repositories.push({
        repository: record.get('repository'),
        dataStore: record.get('dataStore'),
        blobStore: record.get('blobStore'),
        ingestMethod: record.get('ingestMethod')
      });
    });
    me.set('selected-repositories', repositories);

    // inform controller to initialize plan configuration
    me.controller.configure();
  },

  /**
   * Open repository customize window.
   *
   * @private
   */
  doCustomize: function (column, grid, ri, ci, item, record, row) {
    if (item.action === 'customize') {
      Ext.create('NX.coreui.migration.RepositoryCustomizeWindow', {
        recordId: record.getId(),
        repository: record.get('repository'),
        dataStore: record.get('dataStore'),
        blobStore: record.get('blobStore'),
        ingestMethod: record.get('ingestMethod')
      });
    }
  },

  /**
   * Apply changes from repository customize window.
   *
   * @private
   */
  doCustomizeSave: function (button) {
    var window = button.up('window'),
        grid = this.getScreenCmp().getGrid(),
        values = window.getForm().getFieldValues(),
        record = grid.getStore().getById(values.id);

    record.set('dataStore', values.dataStore);
    record.set('blobStore', values.blobStore);
    record.set('ingestMethod', values.ingestMethod);
    record.commit();

    window.close();
  }
});

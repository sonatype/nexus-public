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
 * Migration repository defaults screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.RepositoryDefaultsScreen', {
  extend: 'NX.wizard.FormScreen',
  alias: 'widget.nx-coreui-migration-repositorydefaults',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      description: NX.I18n.render(me, 'Description'),

      fields: [
        {
          xtype: 'combo',
          name: 'blobStore',
          fieldLabel: NX.I18n.render(me, 'BlobStore_FieldLabel'),
          helpText: NX.I18n.render(me, 'BlobStore_HelpText'),
          emptyText: NX.I18n.render(me, 'BlobStore_EmptyText'),
          editable: false,
          store: 'Blobstore',
          queryMode: 'local',
          displayField: 'name',
          valueField: 'name',
          readOnlyOnUpdate: true,
          allowBlank: false
        },
        {
          xtype: 'combo',
          name: 'ingestMethod',
          fieldLabel: NX.I18n.render(me, 'IngestMethod_FieldLabel'),
          helpText: NX.I18n.render(me, 'IngestMethod_HelpText'),
          emptyText: NX.I18n.render(me, 'IngestMethod_EmptyText'),
          editable: false,
          allowBlank: false,
          store: [
            ['FS_LINK', NX.I18n.render(me, 'IngestMethod_Link')],
            ['FS_COPY', NX.I18n.render(me, 'IngestMethod_Copy')],
            ['DOWNLOAD', NX.I18n.render(me, 'IngestMethod_Download')]
          ]
        }
      ],

      buttons: ['back', 'next', 'cancel']
    });

    if (NX.State.getValue('datastores')) {
      me.fields.unshift(
        {
          xtype: 'hiddenfield',
          name: 'dataStore',
          editable: false,
          readOnlyOnUpdate: true,
          allowBlank: false,
          hidden: true
        });
    }

    me.callParent();
    me.down('form').settingsForm = true;
  },

  /**
   * Returns the state of the screen form
   *
   * @return {boolean}
   */
  isDirty: function() {
    return this.down('form').isDirty();
  }
});

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
/*global Ext*/

/**
 * Migration repository customization window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.RepositoryCustomizeWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-migration-repositorycustomize',
  requires: [
    'NX.I18n'
  ],

  config: {
    /**
     * @cfg {String}
     */
    repository: undefined,

    /**
     * Id of the record being modified.
     *
     * @cfg {*}
     */
    recordId: undefined,

    /**
     * The default data store to display.
     *
     * @cfg {String}
     */
    dataStore: 'nexus',

    /**
     * The default blob store to display.
     *
     * @cfg {String}
     */
    blobStore: undefined,

    /**
     * The default method to display.
     *
     * @cfg {String}
     */
    ingestMethod: undefined
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      ui: 'nx-inset',
      closable: true,
      width: NX.view.ModalDialog.SMALL_MODAL,

      title: NX.I18n.render(me, 'Title', me.getRepository()),

      items: {
        xtype: 'form',
        defaultType: 'textfield',
        defaults: {
          anchor: '100%'
        },
        items: [
          {
            xtype: 'hiddenfield',
            name: 'id',
            value: me.getRecordId()
          },
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
            value: me.getBlobStore()
          },
          {
            xtype: 'combo',
            name: 'ingestMethod',
            fieldLabel: NX.I18n.render(me, 'IngestMethod_FieldLabel'),
            helpText: NX.I18n.render(me, 'IngestMethod_HelpText'),
            emptyText: NX.I18n.render(me, 'IngestMethod_EmptyText'),
            editable: false,
            store: [
              ['FS_LINK', NX.I18n.render(me, 'IngestMethod_Link')],
              ['FS_COPY', NX.I18n.render(me, 'IngestMethod_Copy')],
              ['DOWNLOAD', NX.I18n.render(me, 'IngestMethod_Download')]
            ],
            value: me.getIngestMethod()
          }
        ],

        buttonAlign: 'left',
        buttons: [
          { text: NX.I18n.get('Button_Save'), action: 'save', scope: me, formBind: true, bindToEnter: true, ui: 'nx-primary' },
          { text: NX.I18n.get('Button_Cancel'), handler: me.close, scope: me }
        ]
      }
    });

    if (NX.State.getValue('datastores')) {
      me.items.items.splice(1, 0,
          {
            xtype: 'hiddenfield',
            name: 'dataStore',
            editable: false,
            readOnlyOnUpdate: true,
            value: 'nexus',
            hidden: true
          });
    }

    me.callParent();
  },

  /**
   * @return {Ext.form.Basic}
   */
  getForm: function() {
    return this.down('form').getForm();
  }
});

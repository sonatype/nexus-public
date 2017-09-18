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
 * Asset info panel.
 *
 * @since 3.6
 */
Ext.define('NX.coreui.view.component.ComponentAssetInfo', {
  extend: 'Ext.Panel',
  alias: 'widget.nx-coreui-component-componentassetinfo',
  requires: [
    'NX.I18n',
    'NX.coreui.util.RepositoryUrls'
  ],

  autoScroll: true,
  cls: 'nx-coreui-component-componentassetinfo',

  dockedItems: {
    xtype: 'nx-actions',
    items: [
      {
        xtype: 'button',
        text: NX.I18n.get('AssetInfo_Delete_Button'),
        glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
        action: 'deleteAsset',
        hidden: true
      }
    ]
  },

  items: [{
    xtype: 'nx-info-panel',
    itemId: 'summaryPanel',
    titled: 'Summary',
    collapsible: true
  }, {
    xtype: 'panel',
    ui: 'nx-inset',
    title: 'Attributes',
    collapsible: true,
    manageHeight: false,
    items: [{
      xtype: 'nx-coreui-component-assetattributes',
      itemId: 'attributesPanel'
    }]
  }],

  summary: {},

  setModel: function(asset, component) {
    var me = this,
        summary = me.summary,
        contentType = asset.get('contentType'),
        size = asset.get('size');

    me.assetModel = asset;
    me.componentModel = component;

    summary[NX.I18n.get('Assets_Info_Repository')] = asset.get('repositoryName');
    summary[NX.I18n.get('Assets_Info_Format')] = asset.get('format');
    summary[NX.I18n.get('Assets_Info_Group')] = component.get('group');
    summary[NX.I18n.get('Assets_Info_Name')] = component.get('name');
    summary[NX.I18n.get('Assets_Info_Version')] = component.get('version');
    summary[NX.I18n.get('Assets_Info_Path')] = NX.coreui.util.RepositoryUrls.asRepositoryLink(asset, asset.get('format'));
    summary[NX.I18n.get('Assets_Info_ContentType')] = contentType;
    summary[NX.I18n.get('Assets_Info_FileSize')] = Ext.util.Format.fileSize(size);
    summary[NX.I18n.get('Assets_Info_Blob_Created')] = asset.get('blobCreated');
    summary[NX.I18n.get('Assets_Info_Blob_Updated')] = asset.get('blobUpdated');
    summary[NX.I18n.get('Assets_Info_Downloaded_Count')] = asset.get('downloadCount') + ' '
            + NX.I18n.get('Assets_Info_Downloaded_Unit');
    summary[NX.I18n.get('Assets_Info_Last_Downloaded')] = asset.get('lastDownloaded');
    summary[NX.I18n.get('Assets_Info_Locally_Cached')] = contentType !== 'unknown' && size > 0;
    summary[NX.I18n.get('Assets_Info_BlobRef')] = asset.get('blobRef');

    this.down('#summaryPanel').showInfo(summary);
    this.down('#attributesPanel').setAssetModel(asset);

    this.setTitle(asset.get('name'));

    this.fireEvent('update', this, asset, component);
  },

  setInfo: function(section, key, value) {
    this.summary[key] = value;
  },

  showInfo: function() {
    if(this.down('#summaryPanel')) {
      this.down('#summaryPanel').showInfo(this.summary);
    }
  }

});

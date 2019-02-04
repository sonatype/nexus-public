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
    'NX.coreui.util.RepositoryUrls',
    'NX.ext.button.Button',
    'NX.view.info.DependencySnippetPanel'
  ],

  mixins: {
    componentUtils: 'NX.coreui.mixin.ComponentUtils'
  },

  autoScroll: true,
  cls: 'nx-coreui-component-componentassetinfo',

  dockedItems: {
    xtype: 'nx-actions',
    items: [
      {
        xtype: 'nx-button',
        text: NX.I18n.get('FolderInfo_Delete_Button'),
        glyph: 'xf1f8@FontAwesome' /* fa-trash */,
        action: 'deleteFolder',
        hidden: true
      }, {
        xtype: 'nx-button',
        text: NX.I18n.get('AssetInfo_Delete_Button'),
        glyph: 'xf1f8@FontAwesome' /* fa-trash */,
        action: 'deleteAsset',
        hidden: true
      }
    ]
  },

  referenceHolder: true,

  items: [{
    xtype: 'nx-info-panel',
    reference: 'summaryPanel',
    titled: 'Summary',
    collapsible: true
  }, {
    xtype: 'nx-info-dependency-snippet-panel',
    reference: 'dependencySnippetPanel'
  }, {
    xtype: 'panel',
    ui: 'nx-inset',
    title: 'Attributes',
    collapsible: true,
    manageHeight: false,
    items: [{
      xtype: 'nx-coreui-component-assetattributes',
      reference: 'attributesPanel'
    }]
  }],

  savedInfo: {},

  setModel: function(asset, component) {
    var me = this;

    var computedInfo = {},
        contentType = asset.get('contentType'),
        size = asset.get('size'),
        attributesPanel = this.lookup('attributesPanel'),
        titleText;

    this.assetModel = asset;
    this.componentModel = component;

    computedInfo[NX.I18n.get('Assets_Info_Repository')] = Ext.htmlEncode(asset.get('repositoryName'));
    computedInfo[NX.I18n.get('Assets_Info_Format')] = Ext.htmlEncode(asset.get('format'));
    computedInfo[NX.I18n.get('Assets_Info_Group')] = Ext.htmlEncode(component.get('group'));
    computedInfo[NX.I18n.get('Assets_Info_Name')] = Ext.htmlEncode(component.get('name'));
    computedInfo[NX.I18n.get('Assets_Info_Version')] = Ext.htmlEncode(component.get('version'));
    computedInfo[NX.I18n.get('Assets_Info_Path')] = NX.coreui.util.RepositoryUrls.asRepositoryLink(asset, asset.get('format'));
    computedInfo[NX.I18n.get('Assets_Info_ContentType')] = Ext.htmlEncode(contentType);
    computedInfo[NX.I18n.get('Assets_Info_FileSize')] = Ext.util.Format.fileSize(size);
    computedInfo[NX.I18n.get('Assets_Info_Blob_Created')] = Ext.htmlEncode(asset.get('blobCreated'));
    computedInfo[NX.I18n.get('Assets_Info_Blob_Updated')] = Ext.htmlEncode(asset.get('blobUpdated'));

    if (asset.get('downloadCount')) {
      computedInfo[NX.I18n.get('Assets_Info_Downloaded_Count')] = Ext.htmlEncode(asset.get('downloadCount')) + ' '
          + NX.I18n.get('Assets_Info_Downloaded_Unit');
    }

    computedInfo[NX.I18n.get('Assets_Info_Last_Downloaded')] = Ext.htmlEncode(
        me.mixins.componentUtils.getLastDownloadDateForDisplay(asset));
    computedInfo[NX.I18n.get('Assets_Info_Locally_Cached')] = Ext.htmlEncode(contentType !== 'unknown' && size > 0);
    computedInfo[NX.I18n.get('Assets_Info_BlobRef')] = Ext.htmlEncode(asset.get('blobRef'));
    computedInfo[NX.I18n.get('Assets_Info_ContainingRepositoryName')] = Ext.htmlEncode(asset.get('containingRepositoryName'));

    computedInfo[NX.I18n.get('Assets_Info_UploadedBy')] = Ext.htmlEncode(asset.get('createdBy'));
    computedInfo[NX.I18n.get('Assets_Info_UploadedIp')] = Ext.htmlEncode(asset.get('createdByIp'));

    if (attributesPanel) {
      attributesPanel.setAssetModel(asset);
    }

    this.showInfo(Ext.apply({}, this.savedInfo, computedInfo));

    titleText = Ext.htmlEncode(asset.get('name'));
    this.setTitle({
      text: titleText,
      listeners: {
        destroy: function(me) {
          Ext.tip.QuickTipManager.unregister(me.getId());
        }
      }
    });

    // title is already rendered at this point so can't use afterrender event here
    Ext.tip.QuickTipManager.register({
      target: this.down('title').getId(),
      text: titleText
    });

    this.fireEvent('updated', this, asset, component);
  },

  setInfo: function(section, key, value) {
    this.savedInfo[key] = value;
  },

  showInfo: function(info) {
    var summaryPanel = this.lookup('summaryPanel');
    if (summaryPanel) {
      summaryPanel.showInfo(info);
    }
  },

  getDependencySnippetPanel: function() {
    return this.lookup('dependencySnippetPanel');
  }
});

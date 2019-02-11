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
 * Component info panel.
 *
 * @since 3.4
 */
Ext.define('NX.coreui.view.component.ComponentInfo', {
  extend: 'Ext.Panel',
  alias: 'widget.nx-coreui-component-componentinfo',
  cls: 'nx-coreui-component-componentinfo',
  requires: [
    'NX.I18n',
    'NX.coreui.util.RepositoryUrls',
    'NX.ext.button.Button',
    'NX.view.info.DependencySnippetPanel'
  ],
  dockedItems: {
    xtype: 'nx-actions',
    dock: 'top',
    items: [
      {
        xtype: 'nx-button',
        text: NX.I18n.get('ComponentDetails_Delete_Button'),
        glyph: 'xf1f8@FontAwesome' /* fa-trash */,
        action: 'deleteComponent',
        hidden: true
      },
      {
        xtype: 'nx-button',
        text: NX.I18n.get('ComponentDetails_Analyze_Button'),
        glyph: 'xf085@FontAwesome' /* fa-gears */,
        action: 'analyzeApplication'
      }
    ]
  },

  referenceHolder: true,

  items: [
    {
      xtype: 'nx-info-panel',
      reference: 'summaryPanel',
      titled: NX.I18n.get('Component_AssetInfo_Info_Title'),
      collapsible: true
    },
    {
      xtype: 'nx-info-dependency-snippet-panel',
      reference: 'dependencySnippetPanel'
    }
  ],
  autoScroll: true,
  summary: {},

  setModel: function(componentModel) {
    var me = this,
        summary = this.summary,
        componentName = Ext.htmlEncode(componentModel.get('name'));

    this.componentModel = componentModel;

    summary[NX.I18n.get('Search_Assets_Repository')] = Ext.htmlEncode(componentModel.get('repositoryName'));
    summary[NX.I18n.get('Search_Assets_Format')] = Ext.htmlEncode(componentModel.get('format'));
    summary[NX.I18n.get('Search_Assets_Group')] = Ext.htmlEncode(componentModel.get('group'));
    summary[NX.I18n.get('Search_Assets_Name')] = Ext.htmlEncode(componentModel.get('name'));
    summary[NX.I18n.get('Search_Assets_Version')] = Ext.htmlEncode(componentModel.get('version'));

    me.showInfo();

    Ext.tip.QuickTipManager.unregister(me.down('title').getId());
    Ext.tip.QuickTipManager.register({
      target: me.down('title').getId(),
      text: componentName
    });

    me.fireEvent('updated', me, me.componentModel);
  },

  setInfo: function(section, key, value) {
    this.summary[key] = value;
  },

  showInfo: function() {
    var summaryPanel = this.lookup('summaryPanel');
    if (summaryPanel) {
      summaryPanel.showInfo(this.summary);
    }
  },

  getDependencySnippetPanel: function() {
    return this.lookup('dependencySnippetPanel');
  }
});

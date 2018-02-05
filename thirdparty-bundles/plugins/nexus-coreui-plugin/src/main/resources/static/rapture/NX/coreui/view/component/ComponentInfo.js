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
        'NX.coreui.util.RepositoryUrls'
    ],
    dockedItems: {
        xtype: 'nx-actions',
        dock: 'top',
        items: [
            {
                xtype: 'button',
                text: NX.I18n.get('ComponentDetails_Delete_Button'),
                glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
                action: 'deleteComponent',
                hidden: true
            },
            {
                xtype: 'button',
                text: NX.I18n.get('ComponentDetails_Analyze_Button'),
                glyph: 'xf085@FontAwesome' /* fa-gears */,
                action: 'analyzeApplication'
            }
        ]
    },
    items: [{
        xtype: 'nx-info-panel',
        itemId: 'summaryPanel',
        titled: NX.I18n.get('Component_AssetInfo_Info_Title'),
        collapsible: true
    }],
    autoScroll: true,
    summary: {},
    setModel: function(componentModel) {
        var me = this;
        me.componentModel = componentModel;

        me.summary[NX.I18n.get('Search_Assets_Repository')] = me.componentModel.get('repositoryName');
        me.summary[NX.I18n.get('Search_Assets_Format')] = me.componentModel.get('format');
        me.summary[NX.I18n.get('Search_Assets_Group')] = me.componentModel.get('group');
        me.summary[NX.I18n.get('Search_Assets_Name')] = me.componentModel.get('name');
        me.summary[NX.I18n.get('Search_Assets_Version')] = me.componentModel.get('version');

        this.showInfo();

        this.fireEvent('updated', this, me.componentModel);
    },
    setInfo: function(section, key, value) {
        this.summary[key] = value;
    },
    showInfo: function() {
        var me = this;
        var summaryPanel = me.down('#summaryPanel');
        if (summaryPanel) {
            summaryPanel.showInfo(me.summary);
        }
    }
});

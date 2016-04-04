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
 * Selectors controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Selectors', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-selector-list'
  ],
  models: [
    'Selector'
  ],
  stores: [
    'Selector'
  ],
  views: [
    'selector.SelectorAdd',
    'selector.SelectorFeature',
    'selector.SelectorList',
    'selector.SelectorSettings',
    'selector.SelectorSettingsForm'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-selector-feature'},
    {ref: 'list', selector: 'nx-coreui-selector-list'},
    {ref: 'settings', selector: 'nx-coreui-selector-feature nx-coreui-selector-settings'}
  ],
  icons: {
    'selector-default': {
      file: 'content_selector.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Repository/Selectors',
    text: NX.I18n.get('Selectors_Text'),
    description: NX.I18n.get('Selectors_Description'),
    view: {xtype: 'nx-coreui-selector-feature'},
    iconConfig: {
      file: 'content_selector.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('nexus:selectors:read');
    },
    weight: 300
  },
  permission: 'nexus:selectors',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Selector': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-selector-list': {
          beforerender: me.loadStores
        },
        'nx-coreui-selector-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-selector-settings-form': {
          submitted: me.loadStores
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    if (Ext.isDefined(model)) {
      this.getSettings().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Selectors_Create_Title'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-selector-add'));
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Selector.remove(model.getId(), function(response) {
      me.getSelectorStore().load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Selectors_Delete_Message', description), type: 'success'
        });
      }
    });
  }
});

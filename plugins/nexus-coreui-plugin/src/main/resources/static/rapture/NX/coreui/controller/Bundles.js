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
 * Bundles controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Bundles', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.util.Url',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-system-bundlelist'
  ],
  stores: [
    'Bundle'
  ],
  views: [
    'system.Bundles',
    'system.BundleList'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-system-bundles' },
    { ref: 'list', selector: 'nx-coreui-system-bundlelist' },
    { ref: 'info', selector: 'nx-coreui-system-bundles nx-info-panel' }
  ],

  icons: {
    'bundle-default': {
      file: 'plugin.png',
      variants: ['x16', 'x32']
    }
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/System/Bundles',
      text: NX.I18n.get('Bundles_Text'),
      description: NX.I18n.get('Bundles_Description'),
      view: 'NX.coreui.view.system.Bundles',
      iconConfig: {
        file: 'plugin.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:bundles:read');
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Bundle': {
          load: me.reselect
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('name');
  },

  onSelection: function (list, model) {
    var me = this,
        info,
        headers;

    // TODO: Resolve better presentation

    if (Ext.isDefined(model)) {
      info = {};
      info[NX.I18n.get('Bundles_ID_Info')] = model.get('id');
      info[NX.I18n.get('Bundles_Name_Info')] = model.get('name');
      info[NX.I18n.get('Bundles_SymbolicName_Info')] = model.get('symbolicName');
      info[NX.I18n.get('Bundles_Version_Info')] = model.get('version');
      info[NX.I18n.get('Bundles_State_Info')] = model.get('state');
      info[NX.I18n.get('Bundles_Location_Info')] = model.get('location');
      info[NX.I18n.get('Bundles_StartLevel_Info')] = model.get('startLevel');
      info[NX.I18n.get('Bundles_LastModified_Info')] = model.get('lastModified');
      info[NX.I18n.get('Bundles_Fragment_Info')] = model.get('fragment');
      info[NX.I18n.get('Bundles_Fragments_Info')] = model.get('fragments');
      info[NX.I18n.get('Bundles_FragmentHosts_Info')] = model.get('fragmentHosts');

      headers = model.get('headers');
      if (headers) {
        Ext.iterate(headers, function (key, value) {
          info[NX.I18n.format('Bundles_Summary_Info', key)] = value;
        });
      }

      me.getInfo().showInfo(info);
    }
  }
});

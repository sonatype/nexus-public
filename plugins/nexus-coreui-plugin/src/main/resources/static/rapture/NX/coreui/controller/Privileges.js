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
 * Privilege controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Privileges', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-privilege-list'
  ],
  stores: [
    'Privilege'
  ],
  views: [
    'privilege.PrivilegeFeature',
    'privilege.PrivilegeList'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-privilege-feature'},
    {ref: 'list', selector: 'nx-coreui-privilege-list'},
    {ref: 'info', selector: 'nx-coreui-privilege-feature nx-info-panel'}
  ],
  icons: {
    'privilege-default': {
      file: 'medal_gold_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-application': {
      file: 'medal_gold_green.png',
      variants: ['x16', 'x32']
    },
    'privilege-wildcard': {
      file: 'medal_gold_blue.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository': {
      file: 'database.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-admin': {
      file: 'database_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-view': {
      file: 'database.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Security/Privileges',
    text: NX.I18n.get('Privileges_Text'),
    description: NX.I18n.get('Privileges_Description'),
    view: {xtype: 'nx-coreui-privilege-feature'},
    iconConfig: {
      file: 'medal_gold_green.png',
      variants: ['x16', 'x32']
    },
    visible: function () {
      return NX.Permissions.check('nexus:privileges:read');
    },
    weight: 10
  },

  permission: 'nexus:privileges',

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
        '#Privilege': {
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
        info = {};

    if (Ext.isDefined(model)) {
      info[NX.I18n.get('Privileges_Summary_ID')] = model.getId();
      info[NX.I18n.get('Privileges_Summary_Type')] = model.get('type');
      info[NX.I18n.get('Privileges_Summary_Name')] = model.get('name');
      info[NX.I18n.get('Privileges_Summary_Description')] = model.get('description');
      info[NX.I18n.get('Privileges_Summary_Permission')] = model.get('permission');

      Ext.iterate(model.get('properties'), function (key, value) {
        info[NX.I18n.format('Privileges_Summary_Property', key)] = value;
      });

      me.getInfo().showInfo(info);
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and privilege is not read only.
   */
  bindDeleteButton: function (button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.gridHasSelection(me.masters[0], function (model) {
              return !model.get('readOnly');
            })
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * @override
   * Deletes a privilege.
   * @param model privilege to be deleted
   */
  deleteModel: function (model) {
    NX.direct.coreui_Privilege.remove(model.getId(), function (response) {
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('Privileges_Delete_Success', model.get('name')),
          type: 'success'
        });
      }
    });
  }

});

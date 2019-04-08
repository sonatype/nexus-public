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
 * Manages health check information (age and popularity) shown in component details view.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.HealthCheckInfo', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.util.Url',
    'NX.I18n',
    'NX.coreui.util.HealthCheckUtil',
    'NX.State'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      component: {
        'nx-coreui-component-details': {
          afterrender: me.bindHealthCheckInfo,
          updated: me.loadHealthCheckInfo
        },
        'nx-coreui-component-componentinfo': {
          afterrender: me.bindHealthCheckInfo,
          updated: me.loadHealthCheckInfo
        },
        'nx-coreui-component-componentassetinfo': {
          beforeRender: me.bindHealthCheckInfo,
          update: me.onComponentAssetInfoUpdated
        }
      }
    });
  },

  /**
   * Loads health check info and displays it.
   *
   * @private
   * @param {Ext.Panel} panel containing health check info section
   * @param {NX.coreui.model.Component} model component model
   */
  loadHealthCheckInfo: function(panel, model) {
    var me = this,
        components = [];

    if (!NX.State.getUser()) {
      me.renderHealthCheckFields(panel, model);
    }
    else if (me.healthCheckAllowed) {
      if (model && model.get('healthCheckLoading') === undefined) {
        model.beginEdit();
        model.set('healthCheckLoading', true);
        model.endEdit();
        components.push({
          id: model.getId(),
          group: model.get('group'),
          name: model.get('name'),
          version: model.get('version'),
          format: model.get('format')
        });
        NX.direct.healthcheck_Info.read(components, function (response) {
          var success = Ext.isObject(response) && response.success;
          model.beginEdit();
          model.set('healthCheckLoading', false);
          model.set('healthCheckError', !success);
          model.endEdit();
          if (success) {
            Ext.Array.each(response.data, function (entry) {
              model.beginEdit();
              Ext.Object.each(entry['healthCheck'], function (key, value) {
                model.set('healthCheck' + Ext.String.capitalize(key), value);
              });
              model.endEdit();
            });
          }
          me.renderHealthCheckFields(panel, model);
        }, undefined, {enableBuffer: false});
      }
      me.renderHealthCheckFields(panel, model);
    }
  },

  onComponentAssetInfoUpdated: function(panel, asset, component) {
    this.bindHealthCheckInfo(panel);
    this.loadHealthCheckInfo(panel, component);
  },

  /**
   * Add/Remove Health Check info section based on nexus:healthcheck:read permission.
   *
   * @private
   * @param {Ext.Panel} panel to add health check info section to
   */
  bindHealthCheckInfo: function(panel) {
    var me = this;
    panel.mon(
        NX.Conditions.isPermitted("nexus:healthcheck:read"),
        {
          satisfied: me.addHealthCheckInfo,
          unsatisfied: me.removeHealthCheckInfo,
          scope: me
        }
    );
  },

  /**
   * Add Health Check info section to component details panel.
   *
   * @private
   * @param {Ext.Panel} panel to add health check info section to
   */
  addHealthCheckInfo: function() {
    var me = this;
    me.healthCheckAllowed = true;
  },

  /**
   * Remove Health Check info section from component details panel.
   *
   * @private
   * @param {Ext.Panel} panel to remove health check info section from
   */
  removeHealthCheckInfo: function() {
    var me = this;
    me.healthCheckAllowed = false;
  },

  /**
   * Render healthcheck fields.
   *
   * @private
   * @param {Ext.Panel} panel containing health check info section
   * @param {NX.coreui.model.Component} model component model
   */
  renderHealthCheckFields: function(panel, model) {
    var me = this,
        infoPanel,
        info = {};

    if (me.healthCheckAllowed) {
      if (model) {
        if (panel.setInfo) {
          panel.setInfo('healthCheckInfo', NX.I18n.get('HealthCheckInfo_Most_Popular_Version_Label'), me.renderMostPopularVersion(model));
          panel.setInfo('healthCheckInfo', NX.I18n.get('HealthCheckInfo_Age_Label'), me.renderAge(model));
          panel.setInfo('healthCheckInfo', NX.I18n.get('HealthCheckInfo_Popularity_Label'), me.renderPopularity(model));
          panel.showInfo();
        }
        else {
          infoPanel = me.getOrAddInfoPanel(panel);
          info[NX.I18n.get('HealthCheckInfo_Most_Popular_Version_Label')] = me.renderMostPopularVersion(model);
          info[NX.I18n.get('HealthCheckInfo_Age_Label')] = me.renderAge(model);
          info[NX.I18n.get('HealthCheckInfo_Popularity_Label')] = me.renderPopularity(model);
          infoPanel.showInfo(info);
        }
      }
      panel.fireEvent('healthCheckLoaded', panel, model);
    }
  },

  getOrAddInfoPanel: function(panel) {
    var infoPanel = panel.down('#healthCheckInfo');
    if (!infoPanel) {
      infoPanel = panel.add({
        xtype: 'nx-info',
        itemId: 'healthCheckInfo'
      });
    }
    return infoPanel;
  },

  /**
   * Render most popular version field.
   *
   * @private
   * @param {NX.coreui.model.Component} model component model
   * @returns {String} rendered value
   */
  renderMostPopularVersion: function(model) {
    var me = this,
        metadata = {},
        result;

    result = me.renderPreconditions(model, metadata);
    if (!result) {
      result = model.get('healthCheckMostPopularVersion');
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return '<div ' + (metadata.attr || '') + '>' + result + '</div>';
  },

  /**
   * Render age column.
   *
   * @private
   * @param {NX.coreui.model.Component} model component model
   * @returns {String} rendered value
   */
  renderAge: function(model) {
    var me = this,
        metadata = {},
        result, age, dayAge;

    result = me.renderPreconditions(model, metadata);
    if (!result) {
      age = model.get('healthCheckAge');
      if (age === 0 || age > 0) {
        // convert millis to a day count
        dayAge = age / (1000 * 60 * 60 * 24);

        if (dayAge > 364) {
          result = (dayAge / 365).toFixed(1) + ' yrs';
        }
        else {
          result = dayAge.toFixed(0) + ' d';
        }
      }
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return '<div ' + (metadata.attr || '') + '>' + result + '</div>';
  },

  /**
   * Render popularity column.
   *
   * @private
   * @param {NX.coreui.model.Component} model component model
   * @returns {String} rendered value
   */
  renderPopularity: function(model) {
    var me = this,
        metadata = {},
        result, popularity;

    result = me.renderPreconditions(model, metadata);
    if (!result) {
      popularity = model.get('healthCheckPopularity');
      if (popularity === 0 || popularity > 0) {
        if (popularity > 100) {
          popularity = 100;
        }
        result = popularity + ' %';
      }
      if (!result) {
        result = me.renderNotAvailable(metadata);
      }
    }
    return '<div ' + (metadata.attr || '') + '>' + result + '</div>';
  },

  /**
   * Render value based on preconditions.
   *
   * @private
   * @param {NX.coreui.model.Component} model component model
   * @param metadata column metadata
   * @returns {*} rendered value
   */
  renderPreconditions: function(model, metadata) {
    var util = NX.coreui.util.HealthCheckUtil;

    if (!NX.State.getUser()) {
      metadata.attr = 'data-qtip="' + NX.I18n.get('HealthCheckInfo_LoggedInOnly_Tooltip') + '"';
      return util.iconSpan('fa-lock', 'opacity: 0.33;');
    }
    else if (model.get('healthCheckLoading')) {
      return NX.I18n.get('HealthCheckInfo_Loading_Text');
    }
    else if (model.get('healthCheckDisabled')) {
      metadata.attr = 'data-qtip="' + NX.I18n.get('HealthCheckInfo_Disabled_Tooltip') + '"';
      return util.iconSpan('fa-info-circle', 'opacity: 0.33;');
    }
    else if (model.get('healthCheckError')) {
      metadata.attr = 'data-qtip="' + NX.I18n.get('HealthCheckInfo_Error_Tooltip') + '"';
      return util.iconSpan('fa-exclamation-triangle', 'color: red;');
    }
    else if (model.get('healthCheckCapped') || (model && model.get('capped'))) {
      metadata.attr = 'data-qtip="' + NX.I18n.get('HealthCheckInfo_Quota_Tooltip') + '"';
      return util.iconSpan('fa-exclamation-triangle', 'color: yellow;');
    }
    return undefined;
  },

  /**
   * Render a not available value (no data).
   *
   * @private
   * @param metadata column metadata
   * @returns {string} rendered value
   */
  renderNotAvailable: function(metadata) {
    metadata.attr = 'data-qtip="' + NX.I18n.get('HealthCheckInfo_Unavailable_Tooltip') + '"';
    return NX.coreui.util.HealthCheckUtil.iconSpan('fa-ban', 'opacity: 0.33;');
  }

});

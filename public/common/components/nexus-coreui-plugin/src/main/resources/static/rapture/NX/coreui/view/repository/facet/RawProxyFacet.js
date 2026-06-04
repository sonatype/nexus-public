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
 * Configuration for query parameter forwarding and write method support in raw proxy repositories.
 *
 * @since 3.90
 */
Ext.define('NX.coreui.view.repository.facet.RawProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-rawproxy-facet',
  requires: [
    'NX.I18n',
    'NX.Icons'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_Title'),

        items: [
          // Checkbox with description below
          {
            xtype: 'checkbox',
            name: 'attributes.raw.forwardQueryParameters',
            itemId: 'forwardQueryParametersCheckbox',
            fieldLabel: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_Checkbox'),
            boxLabel: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_Description'),
            value: false,
            listeners: {
              change: function(checkbox, newValue) {
                var form = checkbox.up('nx-coreui-repository-settings-form');
                if (form) {
                  var cachingWarning = form.down('#queryParamsCachingWarning');
                  var infoPanel = form.down('#queryParamsInfo');
                  var exclusionField = form.down('#excludedQueryParameters');

                  if (newValue) {
                    // Forwarding ENABLED
                    checkbox.setBoxLabel(NX.I18n.get('Repository_Facet_RawProxy_QueryParams_DescriptionEnabled'));
                    if (cachingWarning) {
                      cachingWarning.show();
                    }
                    if (infoPanel) {
                      infoPanel.show();
                    }
                    if (exclusionField) {
                      exclusionField.show();
                      exclusionField.enable();
                    }
                  } else {
                    // Forwarding DISABLED
                    checkbox.setBoxLabel(NX.I18n.get('Repository_Facet_RawProxy_QueryParams_Description'));
                    if (cachingWarning) {
                      cachingWarning.hide();
                    }
                    if (infoPanel) {
                      infoPanel.hide();
                    }
                    if (exclusionField) {
                      exclusionField.hide();
                      exclusionField.disable();
                    }
                  }
                }
              }
            }
          },

          // Warning panel for caching behavior (shown when enabled)
          {
            xtype: 'panel',
            itemId: 'queryParamsCachingWarning',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-warning',
            iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
            hidden: true,
            title: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_CachingWarningTitle'),
            html: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_CachingWarning')
          },

          // Info panel (shown when enabled)
          {
            xtype: 'panel',
            itemId: 'queryParamsInfo',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-info',
            iconCls: NX.Icons.cls('drilldown-info', 'x16'),
            hidden: true,
            title: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_InfoTitle'),
            html: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_InfoDescription')
          },

          // Exclusion list (textfield with comma-separated values)
          {
            xtype: 'textfield',
            name: 'attributes.raw.excludedQueryParameters',
            itemId: 'excludedQueryParameters',
            fieldLabel: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_ExcludedLabel'),
            helpText: NX.I18n.get('Repository_Facet_RawProxy_QueryParams_ExcludedHelp'),
            hidden: true,
            disabled: true,
            allowBlank: true,
            // Convert comma-separated string to array on submit
            getSubmitValue: function() {
              var value = this.getValue();
              if (!value || value.trim() === '') {
                return [];
              }
              return value.split(',').map(function(param) {
                return param.trim();
              }).filter(function(param) {
                return param.length > 0;
              });
            },
            // Bypass BasicForm toString coercion to preserve array type
            getModelData: function() {
              var data = {};
              data[this.getName()] = this.getSubmitValue();
              return data;
            },
            // Convert array to comma-separated string on load
            setValue: function(value) {
              if (Array.isArray(value)) {
                value = value.join(', ');
              }
              return Ext.form.field.Text.prototype.setValue.call(this, value);
            }
          }
        ]
      }
    ];

    me.callParent();
  }

});

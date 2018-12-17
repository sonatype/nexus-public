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
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.DockerConnectorFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-docker-connector-facet',
  requires: [
    'NX.I18n',
    'NX.State'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        itemId: 'dockerConnectors',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_DockerConnectorFacet_Title'),
        width: 600,

        items: [
          {
            xtype: 'panel',
            layout: {
              type: 'hbox',
              align: 'center',
              pack: 'center'
            },
            items: [
              {
                xtype: 'panel',
                bodypadding: '10px',
                width: '85%',
                html: NX.I18n.get('Repository_Facet_DockerConnectorFacet_Help')
              }
            ]
          },
          {
            xtype: 'fieldcontainer',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpPort_HelpText'),
            layout: 'hbox',
            items: [
              me.createCheckbox('http'),
              me.createPort('http')
            ]
          },
          {
            xtype: 'fieldcontainer',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpsPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpsPort_HelpText'),
            layout: 'hbox',
            items: [
              me.createCheckbox('https'),
              me.createPort('https')
            ]
          },
          {
            xtype: 'checkbox',
            name: 'attributes.docker.forceBasicAuth',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_BasicAuth_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerProxyFacet_BasicAuth_BoxLabel'),
            value: false
          }
        ]
      }
    ];

    Ext.override(me.up('form'), {
      doGetValues: function(values) {
        var processed = { attributes: {} };

        Ext.Object.each(values, function(key, value) {
          if (key === 'attributes.docker.forceBasicAuth') {
            value = !value;
          }

          var segments = key.split('.'),
              parent = processed;

          Ext.each(segments, function(segment, pos) {
            if (pos === segments.length - 1) {
              parent[segment] = value;
            }
            else {
              if (!parent[segment]) {
                parent[segment] = {};
              }
              parent = parent[segment];
            }
          });
        });

        return processed;
      },

      doSetValues: function(values) {
        var process = function(child, prefix) {
          Ext.Object.each(child, function(key, value) {
            var newPrefix = (prefix ? prefix + '.' : '') + key;

            if (newPrefix === 'attributes.docker.forceBasicAuth') {
              value = !value;
            }

            if (Ext.isObject(value)) {
              process(value, newPrefix);
            }
            else {
              values[newPrefix] = value;
            }
          });
        };

        process(values);
      }
    });

    me.callParent();
  },

  createCheckbox: function(type) {
    return {
      xtype: 'checkbox',
      itemId: type + 'Enabled',
      listeners: {
        /**
         * Enable/Disable the port.
         */
        change: function() {
          var form = this.up('form'),
              port = form.down('#' + type + 'Port');
          if (this.getValue()) {
            port.enable();
          }
          else {
            port.disable();
          }
          form.isValid();
        }
      }
    };
  },

  createPort: function(type) {
    return {
      xtype: 'numberfield',
      name: 'attributes.docker.' + type + 'Port',
      itemId: type + 'Port',
      minValue: 1,
      maxValue: 65536,
      allowDecimals: false,
      allowExponential: false,
      allowBlank: false,
      disabled: true,
      width: 565,
      style: {
        marginLeft: '5px'
      },
      listeners: {
        /**
         * Check the checkbox if port has value.
         */
        change: function() {
          var checkbox = this.up('form').down('#' + type + 'Enabled');
          if (this.getValue() && !checkbox.getValue()) {
            checkbox.setValue(true);
            checkbox.resetOriginalValue();
          }
        }
      }
    };
  }

});

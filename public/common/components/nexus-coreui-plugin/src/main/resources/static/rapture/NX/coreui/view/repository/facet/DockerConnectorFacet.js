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
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.DockerConnectorFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-docker-connector-facet',
  requires: ['NX.I18n', 'NX.State'],

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
      subdomainVisibility = NX.State.getEdition() === 'PRO';
    // Remove repositoryName lookup here, as the form/field may not exist yet
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
              pack: 'center',
            },
            items: [
              {
                xtype: 'panel',
                bodypadding: '10px',
                width: '85%',
                html: NX.I18n.get('Repository_Facet_DockerConnectorFacet_Help'),
              },
            ],
          },
          {
            xtype: 'panel',
            itemId: 'warning',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-warning',
            iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
            title:
              '<span style="font-weight:normal;">' +
              NX.I18n.get('Repository_Facet_Docker_PathEnabled_WarningText') +
              '</span>',
            hidden: true, // Hide by default, show only in edit
            margin: '8 0 4 0', // 10px top and bottom margin
          },
          {
            xtype: 'radiogroup',
            itemId: 'dockerRoutingMode',
            fieldLabel: '',
            columns: 1,
            vertical: true,
            items: [
              {
                boxLabel:
                  NX.I18n.get('Repository_Facet_Docker_PathEnabled_FieldLabel') +
                  '<br>' +
                  NX.I18n.get('Repository_Facet_Docker_PathEnabled_HelpText'),
                name: 'attributes.docker.pathEnabled',
                inputValue: 'true',
              },
              {
                boxLabel:
                  NX.I18n.get('Repository_Facet_Docker_OtherConnectors_FieldLabel') +
                  '<br>' +
                  NX.I18n.get('Repository_Facet_Docker_OtherConnectors_HelpText'),
                name: 'attributes.docker.pathEnabled',
                inputValue: 'false',
              },
            ],
            listeners: {
              change: function (group, newValue) {
                var form = group.up('form');
                updateConnectorFields(form, newValue['attributes.docker.pathEnabled']);
              },
              afterRender: function () {
                var group = this;
                var form = group.up('form');
                var nameField = form && form.down('#name');
                var repoName = nameField ? nameField.getValue() : 'repo-name';
                var isCreateMode = !repoName || repoName.trim() === '';
                var items = group.items.items;
                items.forEach(function (item) {
                  if (item.boxLabel && item.boxLabel.indexOf('{{repoName}}') !== -1) {
                    item.setBoxLabel(item.boxLabel.replace('{{repoName}}', repoName || 'repo-name'));
                  }
                });
                var value = group.getValue()['attributes.docker.pathEnabled'];
                // Ensure value is explicitly 'true' or 'false'; default to 'true' otherwise
                if (!['true', 'false'].includes(value)) {
                  // If not explicit, default based on mode:
                  // - Create mode: default to 'true' (path-based routing)
                  // - Edit mode: default to 'false' to preserve existing repos with alternate access methods
                  //   (repos may have no connectors configured if using reverse proxy, subdomain or ports.)
                  value = isCreateMode ? 'true' : 'false';

                  group.setValue({ 'attributes.docker.pathEnabled': value });
                  // Reset the original value of the radiogroup to ensure that the newly set default value
                  // is treated as the initial state. This helps maintain consistency in form validation
                  // and dirty state tracking.
                  if (typeof group.resetOriginalValue === 'function') {
                    group.resetOriginalValue();
                  }
                }
                updateConnectorFields(form, value);
              },
            },
          },
          subdomainVisibility
            ? {
                xtype: 'fieldcontainer',
                style: 'margin-left: 23px;',
                fieldLabel: NX.I18n.get('Repository_Facet_Docker_Subdomain_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_Docker_Subdomain_HelpText'),
                layout: 'hbox',
                items: [me.createCheckbox('subdomain'), me.createSubdomain('subdomain')],
              }
            : undefined,
          {
            xtype: 'fieldcontainer',
            style: 'margin-left: 23px;',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpPort_HelpText'),
            layout: 'hbox',
            items: [me.createCheckbox('http'), me.createPort('http')],
          },
          {
            xtype: 'fieldcontainer',
            style: 'margin-left: 23px;',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpsPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerConnectorFacet_HttpsPort_HelpText'),
            layout: 'hbox',
            items: [me.createCheckbox('https'), me.createPort('https')],
          },
          {
            xtype: 'checkbox',
            name: 'attributes.docker.forceBasicAuth',
            fieldLabel: NX.I18n.get('Repository_Facet_DockerProxyFacet_BasicAuth_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_DockerProxyFacet_BasicAuth_BoxLabel'),
            value: false,
          },
          {
            xtype: 'panel',
            bodypadding: '10px',
            ui: 'nx-inset',
            cls: 'nx-info-panel',
            html: '<span class="x-fa fa-info-circle"></span> ' + NX.I18n.get('Repository_Facet_DockerProxyFacet_BasicAuth_HelpText'),
            style: {
              marginLeft: '23px',
              marginTop: '5px'
            }
          },
        ],
      },
    ];

    // Helper to update connector field states
    function updateConnectorFields(form, isPathEnabled) {
      var subdomainCheckbox = form.down('#subdomainEnabled');
      var subdomainField = form.down('#subdomainPort');
      var httpCheckbox = form.down('#httpEnabled');
      var httpsCheckbox = form.down('#httpsEnabled');
      var httpPortField = form.down('#httpPort');
      var httpsPortField = form.down('#httpsPort');
      if (isPathEnabled === 'true') {
        if (subdomainVisibility) {
          subdomainCheckbox.setDisabled(true);
          subdomainCheckbox.setValue(false);
        }
        httpCheckbox.setDisabled(true);
        httpCheckbox.setValue(false);
        httpsCheckbox.setDisabled(true);
        httpsCheckbox.setValue(false);
        subdomainField && subdomainField.setValue('');
        httpPortField && httpPortField.setValue('');
        httpsPortField && httpsPortField.setValue('');
      } else {
        subdomainVisibility && subdomainCheckbox.setDisabled(false);
        httpCheckbox.setDisabled(false);
        httpsCheckbox.setDisabled(false);
      }
    }

    Ext.override(me.up('form'), {
      doGetValues: function (values) {
        var processed = { attributes: {} };

        Ext.Object.each(values, function (key, value) {
          if (key === 'attributes.docker.forceBasicAuth') {
            value = !value;
          }

          var segments = key.split('.'),
            parent = processed;

          Ext.each(segments, function (segment, pos) {
            if (pos === segments.length - 1) {
              parent[segment] = value;
            } else {
              if (!parent[segment]) {
                parent[segment] = {};
              }
              parent = parent[segment];
            }
          });
        });

        return processed;
      },

      doSetValues: function (values) {
        var process = function (child, prefix) {
          Ext.Object.each(child, function (key, value) {
            var newPrefix = (prefix ? prefix + '.' : '') + key;

            if (newPrefix === 'attributes.docker.forceBasicAuth') {
              value = !value;
            }
            // Coerce pathEnabled to string for radiogroup
            if (newPrefix === 'attributes.docker.pathEnabled') {
              value = String(value);
            }

            if (Ext.isObject(value)) {
              process(value, newPrefix);
            } else {
              values[newPrefix] = value;
            }
          });
        };

        process(values);
        // Show warning panel only in edit mode (when doSetValues is called)
        var form = this;
        var warningPanel = form.down && form.down('#warning');
        if (warningPanel) {
          warningPanel.show();
        }
        // Update connector fields visibility based on the actual pathEnabled value from backend
        // This must happen after doSetValues (not afterRender) because afterRender fires before data is loaded
        var routingGroup = form.down && form.down('#dockerRoutingMode');
        if (routingGroup) {
          var pathEnabledValue = routingGroup.getValue()['attributes.docker.pathEnabled'];
          updateConnectorFields(form, pathEnabledValue);
        }
      },
    });

    me.callParent();
  },

  createCheckbox: function (type) {
    return {
      xtype: 'checkbox',
      itemId: type + 'Enabled',
      name: 'dockercheckbox' + type,
      listeners: {
        /**
         * Enable/Disable the port. When enabled and port is empty, auto-suggest a free port.
         */
        change: function () {
          var form = this.up('form'),
            port = form.down('#' + type + 'Port');
          if (this.getValue()) {
            port.enable();
            if (!port.getValue() && (type === 'http' || type === 'https')) {
              var otherType = type === 'http' ? 'https' : 'http';
              var otherPort = form.down('#' + otherType + 'Port');
              var otherPortValue = otherPort && otherPort.getValue();
              var url = (NX.app.relativePath || '') +
                (otherPortValue
                  ? '/service/rest/internal/ui/docker/suggest-port?exclude=' + otherPortValue
                  : '/service/rest/internal/ui/docker/suggest-port');
              Ext.Ajax.request({
                url: url,
                method: 'GET',
                success: function (response) {
                  var suggested = parseInt(response.responseText, 10);
                  if (!isNaN(suggested)) {
                    port.setValue(suggested);
                  }
                }
              });
            }
          } else {
            port.disable();
          }
          form.isValid();
        },
      },
    };
  },

  createPort: function (type) {
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
      width: 560,
      style: {
        marginLeft: '5px',
      },
      listeners: {
        /**
         * Check the checkbox if port has value.
         */
        change: function () {
          var checkbox = this.up('form').down('#' + type + 'Enabled');
          if (this.getValue() && !checkbox.getValue()) {
            checkbox.setValue(true);
            checkbox.resetOriginalValue();
          }
        },
      },
    };
  },

  createSubdomain: function (type) {
    return {
      xtype: 'textfield',
      name: 'attributes.docker.subdomain',
      itemId: type + 'Port',
      allowBlank: false,
      disabled: true,
      width: 560,
      vtype: 'nx-subdomain',
      style: {
        marginLeft: '5px',
      },
      listeners: {
        /**
         * Check the checkbox if subdomain has value.
         */
        change: function () {
          var checkbox = this.up('form').down('#' + type + 'Enabled');
          if (this.getValue() && !checkbox.getValue()) {
            checkbox.setValue(true);
            checkbox.resetOriginalValue();
          }
        },
        enable: function () {
          if (this.getValue() === '') {
            const repositoryName = this.up('form').down('#name').value;
            this.setValue(repositoryName);
          }
        },
      },
    };
  },
});

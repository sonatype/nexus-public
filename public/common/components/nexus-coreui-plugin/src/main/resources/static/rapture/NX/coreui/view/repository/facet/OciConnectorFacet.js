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
/**
 * OCI Repository Connector configuration facet.
 * This is OCI-specific and independent from Docker infrastructure.
 * Stores attributes under 'attributes.oci.*' namespace.
 *
 * Defined here in nexus-coreui-plugin so OCI recipes bundled with coreui can
 * reference it without a cross-bundle Ext.Loader dependency. The cloud plugin
 * (nexus-cloudui-plugin) intentionally redefines this same FQCN to swap in
 * its path-only variant — that override mechanism is preserved.
 *
 * @since 3.91
 */
Ext.define('NX.oci.view.repository.facet.OciConnectorFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-oci-repository-connector-facet',
  requires: ['NX.I18n', 'NX.State'],

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
      isCloud = NX.State.getValue('isCloud') === true,
      // Subdomain routing is a Pro-only, self-hosted feature — never available on cloud.
      subdomainVisibility = !isCloud && NX.State.getEdition() === 'PRO';

    me.items = [
      {
        xtype: 'fieldset',
        itemId: 'ociConnectors',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_OciConnectorFacet_Title'),
        width: 600,

        items: [
          {
            xtype: 'panel',
            bodyPadding: '10px',
            html: NX.I18n.get('Repository_Facet_OciConnectorFacet_Help'),
            style: {
              textAlign: 'left'
            }
          },
          // The "switching to path-based routing will remove subdomain/port connectors" warning
          // is only meaningful on self-hosted, where subdomain/port connectors actually exist.
          // Cloud is path-only and rejects port/subdomain at config-save time, so the warning
          // would always be misleading there — render it only when not on cloud.
          isCloud ? undefined : {
            xtype: 'panel',
            itemId: 'warning',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-warning',
            iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
            title:
              '<span style="font-weight:normal;">' +
              NX.I18n.get('Repository_Facet_Oci_PathEnabled_WarningText') +
              '</span>',
            hidden: true,
            margin: '8 0 4 0',
          },
          {
            xtype: 'radiogroup',
            itemId: 'ociRoutingMode',
            fieldLabel: '',
            columns: 1,
            vertical: true,
            items: [
              {
                boxLabel:
                  NX.I18n.get('Repository_Facet_Oci_PathEnabled_FieldLabel') +
                  '<br>' +
                  NX.I18n.get('Repository_Facet_Oci_PathEnabled_HelpText'),
                name: 'attributes.oci.pathEnabled',
                inputValue: 'true',
              },
              {
                boxLabel:
                  NX.I18n.get('Repository_Facet_Oci_OtherConnectors_FieldLabel') +
                  '<br>' +
                  NX.I18n.get('Repository_Facet_Oci_OtherConnectors_HelpText'),
                name: 'attributes.oci.pathEnabled',
                inputValue: 'false',
              },
            ],
            listeners: {
              change: function (group, newValue) {
                var form = group.up('form');
                updateConnectorFields(form, newValue['attributes.oci.pathEnabled']);
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
                var value = group.getValue()['attributes.oci.pathEnabled'];
                if (!['true', 'false'].includes(value)) {
                  value = isCreateMode ? 'true' : 'false';
                  group.setValue({ 'attributes.oci.pathEnabled': value });
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
                fieldLabel: NX.I18n.get('Repository_Facet_Oci_Subdomain_FieldLabel'),
                helpText: NX.I18n.get('Repository_Facet_Oci_Subdomain_HelpText'),
                layout: 'hbox',
                items: [me.createCheckbox('subdomain'), me.createSubdomain('subdomain')],
              }
            : undefined,
          {
            xtype: 'fieldcontainer',
            style: 'margin-left: 23px;',
            fieldLabel: NX.I18n.get('Repository_Facet_OciConnectorFacet_HttpPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_OciConnectorFacet_HttpPort_HelpText'),
            layout: 'hbox',
            items: [me.createCheckbox('http'), me.createPort('http')],
          },
          {
            xtype: 'fieldcontainer',
            style: 'margin-left: 23px;',
            fieldLabel: NX.I18n.get('Repository_Facet_OciConnectorFacet_HttpsPort_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_OciConnectorFacet_HttpsPort_HelpText'),
            layout: 'hbox',
            items: [me.createCheckbox('https'), me.createPort('https')],
          },
          {
            xtype: 'checkbox',
            name: 'attributes.oci.forceBasicAuth',
            fieldLabel: NX.I18n.get('Repository_Facet_OciProxyFacet_BasicAuth_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_OciProxyFacet_BasicAuth_BoxLabel'),
            value: false,
          },
          {
            xtype: 'panel',
            bodyPadding: '10px',
            ui: 'nx-inset',
            cls: 'nx-info-panel',
            html: '<span class="x-fa fa-info-circle"></span> ' + NX.I18n.get('Repository_Facet_OciProxyFacet_BasicAuth_HelpText'),
            style: {
              marginLeft: '-20px',
              marginTop: '-15px'
            }
          },
        ],
      },
    ];

    // Helper to update connector field states.
    //
    // On cloud the HTTP/HTTPS connector checkboxes and the subdomain checkbox are intentionally
    // not rendered (port-based and subdomain routing are rejected by
    // OciProxyRepositoryApiRequestToConfigurationConverter on cloud). `form.down('#oci…')` then
    // returns null, and an unguarded `.setDisabled(...)` call broke "Create OCI repository" with
    // "Cannot read properties of null (reading 'setDisabled')". Every field lookup is now
    // null-checked so the helper is safe regardless of which fields the active rendering path
    // actually produced.
    function updateConnectorFields(form, isPathEnabled) {
      var subdomainCheckbox = form.down('#ociSubdomainEnabled');
      var subdomainField = form.down('#ociSubdomainPort');
      var httpCheckbox = form.down('#ociHttpEnabled');
      var httpsCheckbox = form.down('#ociHttpsEnabled');
      var httpPortField = form.down('#ociHttpPort');
      var httpsPortField = form.down('#ociHttpsPort');
      if (isPathEnabled === 'true') {
        if (subdomainVisibility && subdomainCheckbox) {
          subdomainCheckbox.setDisabled(true);
          subdomainCheckbox.setValue(false);
        }
        if (httpCheckbox) {
          httpCheckbox.setDisabled(true);
          httpCheckbox.setValue(false);
        }
        if (httpsCheckbox) {
          httpsCheckbox.setDisabled(true);
          httpsCheckbox.setValue(false);
        }
        subdomainField && subdomainField.setValue('');
        httpPortField && httpPortField.setValue('');
        httpsPortField && httpsPortField.setValue('');
      } else {
        if (subdomainVisibility && subdomainCheckbox) {
          subdomainCheckbox.setDisabled(false);
        }
        httpCheckbox && httpCheckbox.setDisabled(false);
        httpsCheckbox && httpsCheckbox.setDisabled(false);
      }
    }

    Ext.override(me.up('form'), {
      doGetValues: function (values) {
        var processed = { attributes: {} };

        Ext.Object.each(values, function (key, value) {
          if (key === 'attributes.oci.forceBasicAuth') {
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

            if (newPrefix === 'attributes.oci.forceBasicAuth') {
              value = !value;
            }
            if (newPrefix === 'attributes.oci.pathEnabled') {
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
        // Show the routing-mode-change warning only on self-hosted. On cloud the panel isn't
        // even rendered (see the isCloud-gated definition above), and there are no
        // subdomain/port connectors to lose anyway.
        if (!isCloud) {
          var form = this;
          var warningPanel = form.down && form.down('#warning');
          if (warningPanel) {
            warningPanel.show();
          }
        }
      },
    });

    me.callParent();
  },

  createCheckbox: function (type) {
    return {
      xtype: 'checkbox',
      itemId: 'oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Enabled',
      name: 'ocicheckbox' + type,
      hideLabel: true,
      listeners: {
        change: function () {
          var form = this.up('form'),
            port = form.down('#oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Port');
          if (this.getValue()) {
            port.enable();
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
      name: 'attributes.oci.' + type + 'Port',
      itemId: 'oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Port',
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
        change: function () {
          var checkbox = this.up('form').down('#oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Enabled');
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
      name: 'attributes.oci.subdomain',
      itemId: 'oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Port',
      allowBlank: false,
      disabled: true,
      width: 560,
      vtype: 'nx-subdomain',
      style: {
        marginLeft: '5px',
      },
      listeners: {
        change: function () {
          var checkbox = this.up('form').down('#oci' + type.charAt(0).toUpperCase() + type.slice(1) + 'Enabled');
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

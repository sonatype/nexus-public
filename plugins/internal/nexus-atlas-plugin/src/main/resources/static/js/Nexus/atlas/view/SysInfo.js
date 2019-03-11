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
/**
 * System information view.
 *
 * @since 2.7
 */
NX.define('Nexus.atlas.view.SysInfo', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.atlas.Icons'
  ],

  xtype: 'nx-atlas-view-sysinfo',
  title: 'System Information',
  id: 'nx-atlas-view-sysinfo',
  cls: 'nx-atlas-view-sysinfo',
  layout: 'fit',
  autoScroll: true,

  /**
   * @override
   */
  initComponent: function() {
    var me = this,
        icons = Nexus.atlas.Icons;

    // simple named section with list of key-value properties
    me.sectionTpl = NX.create('Ext.XTemplate',
        '<div class="nx-atlas-view-sysinfo-section">',
        '<h2>{name:htmlEncode}</h2>',
        '<table>',
        '<tpl for="props">',
        '<tr>',
        '<td class="property-name">{name:htmlEncode}</td>',
        '<td class="property-value">{value:htmlEncode}</td>',
        '</tr>',
        '</tpl>',
        '</table>',
        '</div>',
        {
          compiled: true
        }
    );

    // nested named section with list of child named sections
    me.nestedSectionTpl = NX.create('Ext.XTemplate',
        '<div class="nx-atlas-view-sysinfo-nestedsection">',
        '<h2>{name:htmlEncode}</h2>',
        '<tpl for="nested">',
        '<h3>{name:htmlEncode}</h3>',
        '<table>',
        '<tpl for="props">',
        '<tr>',
        '<td class="property-name">{name:htmlEncode}</td>',
        '<td class="property-value">{value:htmlEncode}</td>',
        '</tr>',
        '</tpl>',
        '</table>',
        '</tpl>',
        '</div>',
        {
          compiled: true
        }
    );

    // Helper to convert an object into an array of {name,value} properties
    function objectToProperties(obj) {
      var props = [];
      Ext.iterate(obj, function(key, value) {
        props.push({
          name: key,
          value: value
        })
      });
      return props;
    }

    // Main template renders all sections
    me.mainTpl = NX.create('Ext.XTemplate',
        '<div class="nx-atlas-view-sysinfo-body">',
        icons.get('sysinfo').variant('x32').img,
        // nexus details
        '{[ this.section("nexus-status", values) ]}',
        '{[ this.section("nexus-configuration", values) ]}',
        '{[ this.section("nexus-properties", values) ]}',
        '{[ this.section("nexus-license", values) ]}',
        '{[ this.nestedSection("nexus-plugins", values) ]}',
        // system details
        '{[ this.section("system-time", values) ]}',
        '{[ this.section("system-properties", values) ]}',
        '{[ this.section("system-environment", values) ]}',
        '{[ this.section("system-runtime", values) ]}',
        '{[ this.nestedSection("system-network", values) ]}',
        '{[ this.nestedSection("system-filestores", values) ]}',
        '</div>',
        {
          compiled: true,

          /**
           * Render a section.
           */
          section: function(name, values) {
            var data = values[name];

            return me.sectionTpl.apply({
              name: name,
              props: objectToProperties(data)
            });
          },

          /**
           * Render a nested section.
           */
          nestedSection: function(name, values) {
            var data = values[name],
                nested = [];

            Ext.iterate(data, function(key, value) {
              nested.push({
                name: key,
                props: objectToProperties(value)
              })
            });

            return me.nestedSectionTpl.apply({
              name: name,
              nested: nested
            });
          }
        }
    );

    Ext.apply(me, {
      tbar: [
        {
          xtype: 'button',
          id: 'nx-atlas-view-sysinfo-button-refresh',
          text: 'Refresh',
          tooltip: 'Refresh system information',
          iconCls: icons.get('refresh').cls
        },
        {
          xtype: 'button',
          id: 'nx-atlas-view-sysinfo-button-download',
          text: 'Download',
          tooltip: 'Download system information',
          iconCls: icons.get('download').cls
        },
        '-',
        {
          xtype: 'button',
          id: 'nx-atlas-view-sysinfo-button-print',
          text: 'Print',
          tooltip: 'Print system information',
          iconCls: icons.get('print').cls
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  },

  /**
   * Update the system information display.
   *
   * @public
   */
  setInfo: function(info) {
    this.mainTpl.overwrite(this.body, info);
  }
});
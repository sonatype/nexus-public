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
 * System Information panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.SysInfo', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-support-sysinfo',
  requires: [
    'Ext.XTemplate',
    'NX.Assert',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.layout = 'fit';

    me.dockedItems = [{
      xtype: 'nx-actions',
      items: [
        {
          xtype: 'button',
          text: NX.I18n.get('Support_SysInfo_Download_Button'),
          glyph: 'xf019@FontAwesome' /* fa-download */,
          action: 'download'
        }
      ]
    }];

    // FIXME: clean up style, can reduce lots of complexity here with some better naming + scss nesting

    // simple named section with list of key-value properties
    me.sectionTpl = Ext.create('Ext.XTemplate',
      '<div class="x-panel x-panel-nx-subsection-framed" style="margin: 0 0 20px 0">',
      '<div class="x-panel-header-nx-subsection-framed x-panel-header-nx-subsection-framed-horizontal" style="border: none !important">',
      '<span class="x-panel-header-text-container-nx-subsection-framed">{name}</span>',
      '</div>',
      '<div class="x-panel-body x-panel-body-nx-subsection-framed" style="border: none !important">',
      '<table>',
      '<tpl for="props">',
      '<tr>',
      '<td class="nx-info-entry-name">{name}</td>',
      '<td class="nx-info-entry-value">{value}</td>',
      '</tr>',
      '</tpl>',
      '</table>',
      '</div>',
      '</div>',
      {
        compiled: true
      }
    );

    // nested named section with list of child named sections
    me.nestedSectionTpl = Ext.create('Ext.XTemplate',
      '<div class="x-panel x-panel-nx-subsection-framed" style="margin: 0 0 20px 0">',
      '<div class="x-panel-header-nx-subsection-framed x-panel-header-nx-subsection-framed-horizontal" style="border: none !important">',
      '<span class="x-panel-header-text-container-nx-subsection-framed">{name}</span>',
      '</div>',
      '<div class="x-panel-body x-panel-body-nx-subsection-framed" style="border: none !important">',
      '<tpl for="nested">',
      '<h3>{name}</h3>',
      '<table>',
      '<tpl for="props">',
      '<tr>',
      '<td class="nx-info-entry-name">{name}</td>',
      '<td class="nx-info-entry-value">{value}</td>',
      '</tr>',
      '</tpl>',
      '</table>',
      '</tpl>',
      '</div>',
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
        });
      });
      return props;
    }

    // Main template renders all sections
    me.mainTpl = Ext.create('Ext.XTemplate',
        '<div class="nx-atlas-view-sysinfo-body nx-hr" style="height: 100%; overflow-y: scroll;">',
        '<div class="x-panel x-panel-nx-inset">',
        // nexus details
        '{[ this.section("nexus-status", values) ]}',
        '{[ this.section("nexus-node", values) ]}',
        '{[ this.section("nexus-configuration", values) ]}',
        '{[ this.section("nexus-properties", values) ]}',
        '{[ this.section("nexus-license", values) ]}',
        '{[ this.nestedSection("nexus-bundles", values) ]}',
        // system details
        '{[ this.section("system-time", values) ]}',
        '{[ this.section("system-properties", values) ]}',
        '{[ this.section("system-environment", values) ]}',
        '{[ this.section("system-runtime", values) ]}',
        '{[ this.nestedSection("system-network", values) ]}',
        '{[ this.nestedSection("system-filestores", values) ]}',
        '</div>',
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
              });
            });

            return me.nestedSectionTpl.apply({
              name: name,
              nested: nested
            });
          }
        }
    );

    me.callParent();
  },

  /**
   * Update the system information display.
   *
   * @public
   */
  setInfo: function(info) {
    var me = this;
    //<if assert>
    NX.Assert.assert(me.rendered, "Expected me.rendered to be true");
    //</if>
    if (me.rendered) {
      me.mainTpl.overwrite(me.body, info);
    }
  }

});

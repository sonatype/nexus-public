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

/*global NX*/

/**
 * 'panelMessage' factory.
 */
Ext.define('NX.coreui.view.formfield.factory.FormfieldPanelMessageFactory', {
  singleton: true,
  alias: ['nx.formfield.factory.panelMessage'],

  create: function(formField) {
    var panelType = formField.attributes['panelType'];
    if (!panelType) {
      panelType = 'info';
    }

    const item = {
      xtype: 'container',
      layout: {
        type: 'vbox',
        align: 'stretch'
      },
      items: [
        {
          // Label
          xtype: 'component',
          html: formField.label,
          htmlDecode: true,
          style: {
            fontWeight: 'bold',
          },
          listeners: {
            afterrender: function(component) {
              component.addCls('x-unselectable');
            }
          }
        },
        {
          // Panel
          xtype: 'panel',
          itemId: panelType,
          cls: 'nx-drilldown-' + panelType,
          iconCls: NX.Icons.cls('drilldown-' + panelType, 'x16'),
          ui: 'nx-drilldown-message',
          minHeight: 54,
          title: formField.helpText,
          listeners: {
            afterrender: function(panel) {
              var title = panel.el.down('.x-panel-header-title-nx-drilldown-message');
              if (title) {
                title.el.setStyle('font-weight', 'normal');
              }
            }
          }
        }
      ],
      validate: function() {
      },
    };
    return item;
  }
});

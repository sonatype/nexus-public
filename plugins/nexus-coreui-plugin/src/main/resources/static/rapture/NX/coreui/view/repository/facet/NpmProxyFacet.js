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
 * Configuration specific to npm proxy repos
 *
 * @since 3.29
 */
Ext.define('NX.coreui.view.repository.facet.NpmProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-npm-proxy-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    itemCls: 'required-field'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_Npm_Title'),
        items: [
          {
            xtype: 'checkbox',
            name: 'attributes.npm.removeQuarantinedVersions',
            value: false,
            disabled: true,
            fieldLabel: NX.I18n.get('Repository_Facet_Npm_RemoveQuarantined_Label'),
            helpText: NX.I18n.get('Repository_Facet_Npm_RemoveQuarantined_HelpText')
          },
          {
            xtype: 'panel',
            name: 'npmProxyFirewallWarning',
            itemId: 'warning',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-warning',
            iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
            title: NX.I18n.format('Repository_Facet_Npm_RemoveQuarantined_Warning_Default'),
            hidden: false
          }
        ]
      }
    ];

    me.callParent();
  }

});

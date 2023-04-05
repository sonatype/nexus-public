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
 * Configuration specific to Http connections for repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.NugetProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-nugetproxy-facet',
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
        title: NX.I18n.get('Repository_Facet_NugetProxyFacet_Title'),

        items: [
          {
            xtype: 'radiogroup',
            name: 'nugetVersion',
            fieldLabel: NX.I18n.get('Repository_Facet_NugetProxyFacet_ProtocolVersion'),
            columns: 1,
            vertical: true,
            items: [
              {
                boxLabel: NX.I18n.get('Repository_Facet_NugetProxyFacet_V2'),
                name: 'attributes.nugetProxy.nugetVersion',
                inputValue: 'V2'
              },
              {
                boxLabel: NX.I18n.get('Repository_Facet_NugetProxyFacet_V3'),
                name: 'attributes.nugetProxy.nugetVersion',
                inputValue: 'V3'
              }
            ],
            listeners: {
              afterrender: function(radioGroupForm) {
                if (Ext.Object.isEmpty(radioGroupForm.getValue())) {
                  var defaultValue = {
                    'attributes.nugetProxy.nugetVersion': 'V3'
                  };
                  radioGroupForm.setValue(defaultValue);
                }
              }
            }
          },
          {
            xtype: 'numberfield',
            name: 'attributes.nugetProxy.queryCacheItemMaxAge',
            fieldLabel: NX.I18n.get('Repository_Facet_NugetProxyFacet_ItemMaxAge_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_NugetProxyFacet_ItemMaxAge_HelpText'),
            minValue: 0,
            value: 3600
          }
        ]
      }
    ];

    me.callParent();
  }

});

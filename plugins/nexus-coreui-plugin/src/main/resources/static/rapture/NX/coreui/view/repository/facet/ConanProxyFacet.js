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
 * Configuration for Conan proxy repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.ConanProxyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-conanproxy-facet',
  requires: [
    'NX.I18n',
    'NX.State',
    'NX.Icons'
  ],

  /**
   * @override
   */
  initComponent: function() {
    const me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_ConanProxyFacet_Title'),
        itemId: 'conan-radio-group-section',
        hidden: !NX.State.isConanV2Enabled(),
        items: [
          {
            xtype: 'label',
            cls: 'description',
            html: NX.I18n.get('Repository_Facet_ConanProxyFacet_HelpLink'),
            style: {
              fontSize: '12px',
            }
          },
          {
            xtype: 'radiogroup',
            name: 'conanVersion',
            fieldLabel: NX.I18n.get('Repository_Facet_ConanProxyFacet_ProtocolVersion'),
            columns: 1,
            vertical: true,
            items: [
              {
                boxLabel: NX.I18n.get('Repository_Facet_ConanProxyFacet_V1'),
                name: 'attributes.conan.conanVersion',
                inputValue: 'V1'
              },
              {
                boxLabel: NX.I18n.get('Repository_Facet_ConanProxyFacet_V2'),
                name: 'attributes.conan.conanVersion',
                inputValue: 'V2'
              }
            ],
            listeners: {
              afterrender: function(radioGroupForm) {
                if (Ext.Object.isEmpty(radioGroupForm.getValue())) {
                  const defaultValue = {
                    'attributes.conan.conanVersion': 'V1'
                  };
                  radioGroupForm.setValue(defaultValue);
                } else {
                  radioGroupForm.up('fieldset').hide();
                }
              }
            }
          },
          {
            xtype: 'panel',
            itemId: 'info',
            ui: 'nx-drilldown-message',
            cls: 'nx-drilldown-info',
            iconCls: NX.Icons.cls('drilldown-info', 'x16'),
            title: NX.I18n.get('Repository_Facet_ConanProxyFacet_HelpText')
          }
        ]
      },
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_ConanProxyFacet_Title'),
        hidden: !NX.State.isConanV2Enabled(),
        items: [
          {
            xtype: 'textfield',
            name: 'version',
            itemId: 'version',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_Facet_ConanProxyFacet_Version'),
            readOnly: true,
            listeners: {
              afterrender: function() {
                const me = this;
                const form = me.up('form');
                const radioGroupSection = form.down('#conan-radio-group-section');

                if (radioGroupSection.isHidden()) {
                  const conanVersion = Ext.Object.getValues(radioGroupSection.down('radiogroup').getValue())[0];
                  me.setValue(conanVersion);
                  me.resetOriginalValue();
                } else {
                  me.up('fieldset').hide();
                }
              }
            }
          }
        ]
      }
    ];

    me.callParent();
  }
});

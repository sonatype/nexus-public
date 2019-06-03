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
 * Configuration specific to apt repositories.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.repository.facet.AptSigningFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-aptui-repository-aptsigning-facet',
  requires: [
    'NX.I18n'
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
        title: NX.I18n.get('Repository_Facet_AptFacet_Title'),
        items: [
          {
            xtype:'textareafield',
            name: 'attributes.aptSigning.keypair',
            fieldLabel: NX.I18n.get('Repository_Facet_AptSigningFacet_Keypair_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_AptSigningFacet_Keypair_HelpText'),
            allowBlank: false,
            grow: true
          },
          {
            xtype:'nx-password',
            name: 'attributes.aptSigning.passphrase',
            fieldLabel: NX.I18n.get('Repository_Facet_AptSigningFacet_Passphrase_FieldLabel'),
            allowBlank: true
          }
        ]
      }
    ];

    me.callParent();
  }

});

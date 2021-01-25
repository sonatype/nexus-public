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
 * Configuration for repository cleanup policy facet.
 *
 * @since 3.14
 */
Ext.define('NX.coreui.view.repository.facet.CleanupPolicyFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-cleanup-policy-facet',
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
        title: NX.I18n.get('Repository_Facet_CleanupPolicyFacet_Title'),

        items: {
          xtype: 'nx-itemselector',
          name: 'attributes.cleanup.policyName',
          itemId: 'cleanupPolicyName',
          fieldLabel: NX.I18n.get('Repository_Facet_CleanupPolicyFacet_Policy_FieldLabel'),
          helpText: NX.I18n.get('Repository_Facet_CleanupPolicyFacet_Policy_HelpText'),
          buttons: ['add', 'remove'],
          fromTitle: NX.I18n.get('Repository_Facet_CleanupPolicyFacet_Policy_FromTitle'),
          toTitle: NX.I18n.get('Repository_Facet_CleanupPolicyFacet_Policy_ToTitle'),
          editable: false,
          store: 'CleanupPolicy',
          valueField: 'name',
          displayField: 'name',
          allowBlank: true,
          delimiter: null,
          forceSelection: true,
          queryMode: 'local',
          triggerAction: 'all',
          selectOnFocus: false
        }
      }
    ];

    me.callParent();
  }
});

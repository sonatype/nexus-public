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
 * HuggingFace XET protocol toggle facet.
 * Hidden when the global feature flag nexus.format.huggingface.xet.enabled is false.
 */
Ext.define('NX.coreui.view.repository.facet.HuggingFaceXetFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-huggingface-xet-facet',
  requires: [
    'NX.I18n',
    'NX.constants.FeatureFlags'
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
        title: 'Transfer Protocol',
        hidden: !NX.State.getValue(NX.constants.FeatureFlags.HUGGING_FACE_XET_ENABLED),
        items: [
          {
            xtype: 'checkbox',
            name: 'attributes.xet.enabled',
            itemId: 'xetEnabled',
            fieldLabel: 'Enable XET',
            helpText: 'When enabled, the XET protocol is used for file transfers. When disabled, the legacy LFS path is used.',
            // NEXUS-48685: default UNCHECKED so pre-feature repositories (no
            // persisted xet block) preserve legacy LFS behavior on upgrade.
            // Create mode explicitly seeds this to checked via the recipe's
            // afterRender hook. This matches the super UI's semantics where
            // formData.xet?.enabled ?? false is the render fallback and
            // buildDefaultFormData seeds xet.enabled=true for new repos.
            value: false
          }
        ]
      }
    ];

    me.callParent();
  }
});

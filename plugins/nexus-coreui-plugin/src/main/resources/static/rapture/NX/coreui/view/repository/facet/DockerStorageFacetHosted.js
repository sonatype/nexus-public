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
 * Storage configuration for docker hosted repositories.
 *
 * @since 3.21
 */
Ext.define('NX.coreui.view.repository.facet.DockerStorageFacetHosted', {
  extend: 'NX.coreui.view.repository.facet.StorageFacetHosted',
  alias: 'widget.nx-coreui-repository-docker-storage-hosted-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this, writePolicyComponent, latestCheckbox, writePolicyFieldSet;

    me.callParent();
    writePolicyFieldSet = me.down('#writePolicyFieldset');
    writePolicyFieldSet.add({
      xtype: 'checkboxfield',
      name: 'attributes.storage.latestPolicy',
      fieldLabel: NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_DisableLatestItem'),
      helpText: NX.I18n.get('Repository_Facet_StorageFacetHosted_Deployment_DisableLatestItemHelpText'),
      checked: false
    });

    writePolicyComponent = me.down('[name=attributes.storage.writePolicy]');
    latestCheckbox = me.down('[name=attributes.storage.latestPolicy]');

    writePolicyComponent.on({
      select: function() {
        setLatestTagRedeployCheckboxVisibility(this.getValue(), latestCheckbox);
      },
      beforeRender: function() {
        setLatestTagRedeployCheckboxVisibility(this.getValue(), latestCheckbox);
      }
    });
  }

});

function setLatestTagRedeployCheckboxVisibility(writePolicyComponentValue, latestCheckbox) {
  latestCheckbox.setVisible(writePolicyComponentValue === 'ALLOW_ONCE');
}

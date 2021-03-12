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
 * Configuration specific to HTTP connections for Maven2 Proxy repositories including preemptive authentication support.
 *
 * @since 3.30
 */
Ext.define('NX.coreui.view.repository.facet.HttpClientFacetWithPreemptiveAuth', {
  extend: 'NX.coreui.view.repository.facet.HttpClientFacet',
  alias: 'widget.nx-coreui-repository-httpclient-facet-with-preemptive-auth',
  requires: [
    'NX.I18n'
  ],

  authFields: function() {
    var me = this,
        parentAuthFields = me.callParent(arguments);

    parentAuthFields.push({
      xtype: 'checkbox',
      itemId: 'attributes_httpclient_authentication_preemptive',
      name: 'attributes.httpclient.authentication.preemptive',
      fieldLabel: NX.I18n.get('System_AuthenticationSettings_Preemptive_FieldLabel'),
      helpText: NX.I18n.get('System_AuthenticationSettings_Preemptive_HelpText'),
      value: false,
      disabled: true
    });

    return parentAuthFields;
  }
});

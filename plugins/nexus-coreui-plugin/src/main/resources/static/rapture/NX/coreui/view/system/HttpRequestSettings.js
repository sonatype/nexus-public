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
 * Http request settings fields.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.HttpRequestSettings', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-system-httprequestsettings',
  requires: [
    'NX.I18n'
  ],

  items: [
    {
      xtype: 'textfield',
      name: 'userAgentSuffix',
      fieldLabel: NX.I18n.get('System_HttpRequestSettings_UserAgentCustomization_FieldLabel'),
      helpText: NX.I18n.get('System_HttpRequestSettings_UserAgentCustomization_HelpText')
    },
    {
      xtype: 'numberfield',
      name: 'timeout',
      fieldLabel: NX.I18n.get('System_HttpRequestSettings_Timeout_FieldLabel'),
      helpText: NX.I18n.get('System_HttpRequestSettings_Timeout_HelpText'),
      allowDecimals: false,
      allowExponential: false,
      minValue: 1,
      maxValue: 3600,
      emptyText: '20'
    },
    {
      xtype: 'numberfield',
      name: 'retries',
      fieldLabel: NX.I18n.get('System_HttpRequestSettings_Attempts_FieldLabel'),
      helpText: NX.I18n.get('System_HttpRequestSettings_Attempts_HelpText'),
      allowDecimals: false,
      allowExponential: false,
      minValue: 0,
      maxValue: 10,
      emptyText: '2'
    }
  ]

});

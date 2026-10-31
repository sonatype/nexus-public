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
/*global Ext*/

/**
 * Validation helpers.
 *
 * @since 3.0
 */
Ext.define('NX.util.Validator', {
  singleton: true,
  requires: [
      'Ext.form.field.VTypes',
      'NX.I18n'
  ],

  /**
   * Changes to this property should be sync'd in:
   * components/nexus-validation/src/main/java/org/sonatype/nexus/validation/constraint/NamePatternConstants.java
   * @private
   */
  nxNameRegex : /^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$/,

  /**
   * Removes the constraint for a maximum of 6 characters in the last element of the domain name, otherwise
   * is the same as default ExtJS email vtype.
   * @private
   */
  nxEmailRegex : /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.?){1,5}([A-Za-z]){2,60}$/,

  /**
   * A regular expression to detect a valid hostname according to RFC 1123.
   * See also http-headers-patterns.properties and HostnameValidator.java for other uses of this regex.
   * @private
   */
  nxRfc1123HostRegex: new RegExp(
    "^(((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|" +
     "(\\[(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\])|" +
     "(\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)\\])|" +
     "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|" +
     "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))(:([0-9]+))?$"
  ),

  /**
   * A regular expression to detect a possibly valid URL
   * @private
   */
  nxUrlRegex: /^https?:\/\/[^"<>^`{|}]+$/i,

  /**
   * A regular expression to detect whether we have leading and trailing white space
   *
   * @private
   */
  nxLeadingAndTrailingWhiteSpaceRegex : /^[ \s]+|[ \s]+$/,

  /**
   * Regular expression to validate docker subdomain
   * @private
   */
  nxSubdomainRegex : /^[a-zA-Z](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?$/,

  /**
   * @public
   * @param vtype {object}
   */
  registerVtype: function(vtype) {
    Ext.apply(Ext.form.field.VTypes, vtype);
  },

  constructor: function () {
    var me = this;

    me.vtypes = [
      {
        'nx-name': function(val) {
          return NX.util.Validator.nxNameRegex.test(val);
        },
        'nx-nameText': NX.I18n.get('Util_Validator_Text'),
        'nx-email': function(val) {
          return NX.util.Validator.nxEmailRegex.test(val);
        },
        'nx-emailText': Ext.form.field.VTypes.emailText,
        'nx-hostname': function(val) {
          return NX.util.Validator.nxRfc1123HostRegex.test(val);
        },
        'nx-hostnameText': NX.I18n.get('Util_Validator_Hostname'),
        'nx-trim': function(val) {
          return !NX.util.Validator.nxLeadingAndTrailingWhiteSpaceRegex.test(val);
        },
        'nx-trimText': NX.I18n.get('Util_Validator_Trim'),
        'nx-url': function(val) {
          return NX.util.Validator.nxUrlRegex.test(val);
        },
        'nx-urlText': NX.I18n.get('Util_Validator_Url'),
        'nx-subdomain': function(val) {
          return NX.util.Validator.nxSubdomainRegex.test(val);
        },
        'nx-subdomainText': NX.I18n.get('Util_Validator_Subdomain_Text'),
      }
    ];

    Ext.each(me.vtypes, function(vtype) {
      me.registerVtype(vtype);
    });
  }

});

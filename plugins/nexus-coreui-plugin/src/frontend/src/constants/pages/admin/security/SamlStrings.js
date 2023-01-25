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
export default {
  SAML_CONFIGURATION: {
    MENU: {
      text: 'SAML',
      description: 'SAML Identity Provider Configuration'
    },
    FIELDS: {
      idpMetadataLabel: 'SAML Identity Provider Metadata XML',

      entityIdUriLabel: 'Entity ID URI',
      entityIdUriDescription: 'Unique identifier for this Service Provider.  Normally the default Entity ID can be used unless NXRM does not have a fixed URL and or port.',
      entityIdUriValidationError: 'Entity ID must be a URI',

      validateResponseSignatureLabel: 'Validate Response Signature',
      validateResponseSignatureDescription: 'By default, if a signing key is found in the IdP metadata, then NXRM will attempt to validate signatures on the response.',

      validateAssertionSignatureLabel: 'Validate Assertion Signature',
      validateAssertionSignatureDescription: 'By default, if a signing key is found in the IdP metadata, then NXRM will attempt to validate signatures on the assertions.',

      defaultRoleLabel: 'Default Role Mapping',
      defaultRoleDescription: 'The role a user should be assigned if no other roles were able to be mapped to the user',

      usernameAttrLabel: 'Username',

      firstNameAttrLabel: 'First Name',

      lastNameAttrLabel: 'Last Name',

      emailAttrLabel: 'Email',

      roleAttrLabel: 'Roles/Groups',
      roleFieldDescription: 'Map Identity Provider roles or groups to Nexus Repository Manager roles'
    },
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while attempting to load the SAML Identity Provider configuration',
      SAVE_ERROR: 'An error occurred while attempting to save the SAML Identity Provider configuration',
      SAVE_SUCCESS: 'SAML Identity Provider configuration updated'
    },
    LABELS: {
      FIELDS: 'IDP Field Mappings'
    }
  }
};
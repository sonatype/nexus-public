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

export const ROLE_FORM = {
  LOADING: 'Loading form...',

  BUTTON_CANCEL: 'Cancel',
  BUTTON_CREATE: 'Create',
  BUTTON_SAVE: 'Save',
  BUTTON_DELETE: 'Delete Role',
  BUTTON_EDIT_PRIVILEGES: 'Edit Privileges',
  BUTTON_EDIT_CONTAINED_ROLES: 'Edit Contained Roles',

  SECTION_TYPE: {
    TITLE: 'Role Type',
    HELP: 'Choose the type of role to create.',
    FIELD_LABEL: 'Type',
    FIELD_HELP:
      'Select whether this is a standard Nexus role or maps to an external authentication source',
    OPTION_NEXUS_LABEL: 'Nexus Role',
    OPTION_NEXUS_DESCRIPTION: 'Standard role managed by Nexus',
    OPTION_EXTERNAL_LABEL: 'External Role Mapping',
    OPTION_EXTERNAL_DESCRIPTION: 'Map to roles from LDAP, SAML, or Crowd',
  },

  SECTION_EXTERNAL: {
    TITLE: 'External Source',
    FIELD_LABEL: 'External Source',
    FIELD_HELP: 'Select the external authentication source (LDAP, SAML, or Crowd)',
    MAPPED_ROLE_LABEL: 'Mapped Role',
    LDAP_PLACEHOLDER: 'Search LDAP roles...',
    LDAP_HELP: 'Enter or search for a role from LDAP. Type 3+ characters to search.',
    LDAP_MIN_CHARS: 'Enter 3 or more characters to search.',
    LDAP_SEARCH_FAILED: 'Failed to search LDAP roles.',
    OTHER_PLACEHOLDER: (source: string) => `Enter ${source} role name`,
    OTHER_HELP: (source: string) => `Enter the exact role name from ${source}.`,
  },

  SECTION_SETUP: {
    TITLE: 'Role Setup',
    ID_LABEL: 'ID',
    ID_PLACEHOLDER: 'nx-custom-role',
    ID_HELP: 'Unique identifier for this role',
    NAME_LABEL: 'Name',
    NAME_PLACEHOLDER: 'My Custom Role',
    NAME_HELP: 'Human-readable name for this role',
    DESCRIPTION_LABEL: 'Description',
    DESCRIPTION_PLACEHOLDER: 'Grants read access to all Maven repositories',
    DESCRIPTION_HELP: "Optional description of this role's purpose",
  },

  SECTION_PRIVILEGES: {
    TITLE: 'Privileges',
    HELP: 'Privileges granted directly to this role.',
    COUNT: (count: number) => `${count} privilege${count === 1 ? '' : 's'} selected`,
  },

  SECTION_CONTAINED_ROLES: {
    TITLE: 'Contained Roles',
    HELP: 'Other roles included by this role. Their privileges are inherited.',
    COUNT: (count: number) => `${count} role${count === 1 ? '' : 's'} selected`,
  },

  PRIVILEGE_WARNING: 'Select at least one privilege or contained role',

  PICKERS: {
    PRIVILEGES_TITLE: 'Privileges Selection',
    PRIVILEGES_EMPTY: 'No privileges available',
    CONTAINED_ROLES_TITLE: 'Contained Roles Selection',
    CONTAINED_ROLES_EMPTY: 'No roles available',
  },
} as const;

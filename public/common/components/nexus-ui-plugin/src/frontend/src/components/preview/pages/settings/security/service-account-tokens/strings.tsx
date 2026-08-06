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

import React from 'react';

export const SERVICE_ACCOUNT_TOKENS_STRINGS = {
  PAGE: {
    TITLE: 'Service Account Tokens',
    DESCRIPTION: 'Manage tokens for automated service access',
    BREADCRUMB_SETTINGS: 'Settings',
    FILTER_PLACEHOLDER: 'Filter by name, role, creator',
    FILTER_LABEL: 'Filter service account tokens',
    CREATE_BUTTON: 'Create Service Account Token',
    TABLE_ARIA_LABEL: 'Service account tokens',
    TABLE_REGION_LABEL: 'Service account tokens table',
    ACTIONS_BUTTON_LABEL: 'Actions',
    REVOKE_ACTION: 'Revoke',
    LAST_USED_NEVER: 'Never used',
    LOADING: 'Loading tokens…',
    COLUMNS: {
      NAME: 'Name',
      ROLE: 'Role',
      CREATED_BY: 'Created By',
      EXPIRES: 'Expires',
      LAST_USED: 'Last Used',
    },
    EMPTY: {
      TITLE_NO_TOKENS: 'No service account tokens',
      TITLE_NO_MATCH: 'No matching tokens',
      DESCRIPTION_NO_TOKENS:
        'Create a service account token to give automation, applications, or other services secure, role-scoped access.',
      DESCRIPTION_NO_MATCH: 'Try adjusting your filter criteria.',
    },
    ABOUT: {
      TITLE: 'About service account tokens',
      TEXT_ADMIN:
        'View, create, and revoke tokens used by automation, applications, and other services that need to authenticate to this Nexus Repository instance. Each token inherits a Nexus role and can be revoked at any time without affecting user accounts.',
      TEXT_VIEWER:
        'View tokens used by automation, applications, and other services that need to authenticate to this Nexus Repository instance. Each token inherits a Nexus role.',
      LINK: 'Learn more about service account tokens',
      LINK_HREF: 'https://links.sonatype.com/products/nxrm3/docs/service-account-tokens',
    },
  },
  EXPIRES_CELL: {
    NEVER: 'Never',
    EXPIRED: 'Expired',
    EXPIRED_ON: (date: string) => `Expired on ${date}`,
  },
  CREATE_MODAL: {
    TITLE: 'Create Service Account Token',
    NAME_LABEL: 'Name',
    NAME_PLACEHOLDER: 'e.g. jenkins-prod-pusher',
    NAME_HELP: 'A unique, human-readable identifier for this service account',
    NAME_INVALID_CHARS_ERROR: 'Use only letters, numbers, -, or _.',
    NAME_DUPLICATE_ERROR: (name: string) => `A token named "${name}" already exists`,
    ROLE_LABEL: 'Role',
    ROLE_PLACEHOLDER: 'Select a role...',
    ROLE_HELP: 'The role determines what this service account can access',
    ROLES_LOAD_ERROR_FORBIDDEN: (
      <>
        Missing privilege: <code>nexus:roles:read</code>. Contact your administrator.
      </>
    ),
    ROLES_LOAD_ERROR_GENERIC: 'Failed to load roles. Please try again later.',
    EXPIRATION_LABEL: 'Token Expiration',
    EXPIRATION_HELP: 'The token will automatically expire after this period',
    DESCRIPTION_LABEL: 'Description',
    DESCRIPTION_HELP: 'Optional description of the token purpose',
    NEVER_EXPIRES_WARNING:
      'Tokens with no expiration are less secure. Consider using a limited expiration period.',
    CANCEL_BUTTON: 'Cancel',
    CREATE_BUTTON: 'Create Token',
    EXPIRY_OPTIONS: [
      {value: '30', label: '30 days'},
      {value: '60', label: '60 days'},
      {value: '90', label: '90 days'},
      {value: '365', label: '1 year'},
      {value: '-1', label: 'Never'},
    ],
  },
  REVEAL_MODAL: {
    TITLE: 'Service Account Token Created',
    DIALOG_DESCRIPTION: 'Copy this token now. It will not be shown again.',
    WARNING: 'Copy this token now — you will not be able to see it again.',
    TOKEN_LABEL: 'Token',
    COPY_BUTTON: 'Copy',
    COPIED_BUTTON: 'Copied!',
    COPY_ANNOUNCEMENT: 'Token copied to clipboard.',
    COPY_FAILED: 'Copy failed. Select the token above and copy it manually.',
    AUTO_CLOSE_NOTICE: (seconds: number) => `This dialog closes automatically in ${seconds}s.`,
    DONE_BUTTON: 'Done',
  },
  REVOKE_MODAL: {
    TITLE: 'Revoke Token',
    WARNING: (name: string) => (
      <>
        Revoking '<strong>{name}</strong>' cannot be undone. Any systems using this token will
        immediately lose access.
      </>
    ),
    INPUT_LABEL: (name: string) => (
      <>
        Type '<strong>{name}</strong>' to confirm this action and proceed
      </>
    ),
    CANCEL_BUTTON: 'Cancel',
    REVOKE_BUTTON: 'Revoke Token',
  },
  MESSAGES: {
    CREATE_SUCCESS: (name: string) => `Service account token "${name}" was created successfully.`,
    CREATE_ERROR_INVALID:
      'The service account token could not be created. Please check the name and selected role.',
    CREATE_ERROR_FORBIDDEN: 'You do not have permission to create this service account token.',
    CREATE_ERROR_GENERIC: 'Failed to create the service account token.',
    REVOKE_SUCCESS: (name: string) => `Service account token "${name}" was revoked.`,
    REVOKE_ERROR_NOT_FOUND: 'The service account token no longer exists.',
    REVOKE_ERROR_FORBIDDEN: 'You do not have permission to revoke this service account token.',
    REVOKE_ERROR_GENERIC: 'Failed to revoke the service account token.',
    LOAD_ERROR: 'Failed to load service account tokens.',
    OPERATION_IN_PROGRESS:
      'Another token operation is already in progress. Please wait for it to complete before starting another.',
  },
};

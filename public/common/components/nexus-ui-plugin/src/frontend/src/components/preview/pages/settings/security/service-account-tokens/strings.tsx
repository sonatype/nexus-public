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
    FILTER_PLACEHOLDER: 'Filter by name, role, or creator',
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
        'Create a service account token to give applications and other automated services role-based access to Nexus Repository.',
      DESCRIPTION_NO_MATCH: 'Try adjusting your filter.',
    },
    ABOUT: {
      TITLE: 'About service account tokens',
      TEXT_ADMIN:
        'Service account tokens let applications and other automated services authenticate to Nexus Repository without using a user account. Each token is assigned a Nexus Repository role that determines what it can access. You can revoke a token at any time without affecting user accounts.',
      TEXT_VIEWER:
        'Service account tokens let applications and other automated services authenticate to Nexus Repository without using a user account. Each token is assigned a Nexus Repository role that determines what it can access.',
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
    NAME_HELP: 'Enter a unique name for the service account token.',
    NAME_INVALID_CHARS_ERROR: 'Use only letters, numbers, hyphens (-), and underscores (_).',
    NAME_DUPLICATE_ERROR: (name: string) => `A token named "${name}" already exists.`,
    ROLE_LABEL: 'Role',
    ROLE_PLACEHOLDER: 'Select a role',
    ROLE_HELP: 'Select the role that determines what this token can access.',
    ROLES_LOAD_ERROR_FORBIDDEN: (
      <>
        Missing privilege: <code>nexus:roles:read</code>. Contact your administrator.
      </>
    ),
    ROLES_LOAD_ERROR_GENERIC: 'Unable to load roles. Try again later.',
    EXPIRATION_LABEL: 'Token expiration',
    EXPIRATION_HELP: 'Select when this token expires. Use the shortest period that meets your needs.',
    DESCRIPTION_LABEL: 'Description',
    DESCRIPTION_HELP: 'Describe what this token is used for.',
    NEVER_EXPIRES_WARNING:
      'Tokens that never expire are less secure. Consider setting an expiration period.',
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
    WARNING: "Copy this token now. You won't be able to view it again.",
    TOKEN_LABEL: 'Token',
    COPY_BUTTON: 'Copy',
    COPIED_BUTTON: 'Copied!',
    COPY_ANNOUNCEMENT: 'Token copied to clipboard.',
    COPY_USERNAME_ARIA: 'Copy username',
    COPY_PASSWORD_ARIA: 'Copy password',
    COPY_TOKEN_ARIA: 'Copy token',
    COPY_ENCODED_ARIA: 'Copy encoded Authorization header',
    COPY_FAILED: "Couldn't copy the value. Select it and copy it manually.",
    DONE_BUTTON: 'Done',
    BASIC_SECTION_LABEL: 'Basic authentication',
    USERNAME_LABEL: 'Username',
    PASSWORD_LABEL: 'Password',
    BEARER_SECTION_LABEL: 'Bearer token',
    USERNAME_COPY_ANNOUNCEMENT: 'Username copied to clipboard.',
    PASSWORD_COPY_ANNOUNCEMENT: 'Password copied to clipboard.',
    ENCODED_COPY_ANNOUNCEMENT: 'Encoded Authorization header copied to clipboard.',
    ENCODED_REVEAL_BUTTON: 'Show encoded header',
    ENCODED_LABEL: 'Encoded Authorization header',
    ENCODED_VALUE: (encoded: string) => `Authorization: Basic ${encoded}`,
    ENCODED_NOTE: (
      <>
        This value is already Base64 encoded. Use it in an <code>Authorization: Basic</code>{' '}
        header or as the npm <code>_auth</code> value. Don't use it with <code>curl -u</code> or
        other tools that encode credentials.
      </>
    ),
    DOCS_LINK: 'Authenticating with service accounts',
    DOCS_LINK_HREF: 'https://links.sonatype.com/products/nxrm3/docs/authenticating-service-accounts',
  },
  REVOKE_MODAL: {
    TITLE: 'Revoke Service Account Token',
    WARNING: (name: string) => (
      <>
        Revoking "<strong>{name}</strong>" cannot be undone. Applications and services using
        this token will immediately lose access.
      </>
    ),
    INPUT_LABEL: (name: string) => (
      <>
        Type "<strong>{name}</strong>" to confirm
      </>
    ),
    CANCEL_BUTTON: 'Cancel',
    REVOKE_BUTTON: 'Revoke Token',
  },
  MESSAGES: {
    CREATE_SUCCESS: (name: string) => `Service account token "${name}" created.`,
    CREATE_ERROR_INVALID: 'Unable to create the token. Check the name and selected role.',
    CREATE_ERROR_FORBIDDEN: "You don't have permission to create service account tokens.",
    CREATE_ERROR_GENERIC: 'Unable to create the service account token. Try again.',
    REVOKE_SUCCESS: (name: string) => `Service account token "${name}" revoked.`,
    REVOKE_ERROR_NOT_FOUND: (name: string) =>
      name
        ? `Service account token "${name}" no longer exists.`
        : 'That service account token no longer exists.',
    REVOKE_ERROR_FORBIDDEN: "You don't have permission to revoke this service account token.",
    REVOKE_ERROR_GENERIC: 'Unable to revoke the service account token. Try again.',
    LOAD_ERROR: 'Unable to load service account tokens. Try again.',
    OPERATION_IN_PROGRESS:
      'Another token operation is in progress. Wait for it to finish before starting another.',
  },
};

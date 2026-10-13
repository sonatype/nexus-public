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
import {faRobot} from '@fortawesome/free-solid-svg-icons';

export default {
  SERVICE_ACCOUNT_TOKENS: {
    MENU: {
      text: 'Service Account Tokens',
      description: 'Manage tokens for automated service access',
      icon: faRobot,
    },
    LIST: {
      CREATE_BUTTON: 'Create Service Account Token',
      EMPTY_LIST: 'No service account tokens',
      EMPTY_LIST_FILTERED: 'No matching tokens',
      FILTER_PLACEHOLDER: 'Filter by name, role, or creator',
      COLUMNS: {
        NAME: 'Name',
        ROLE: 'Role',
        CREATED_BY: 'Created By',
        EXPIRES: 'Expires',
        LAST_USED: 'Last Used',
        LAST_USED_NEVER: 'Never used',
        EXPIRED_BADGE: 'Expired',
        NEVER_EXPIRES: 'Never',
        ACTIONS: '',
      },
      ABOUT: {
        TITLE: 'About service account tokens',
        TEXT_ADMIN: 'Service account tokens let applications and other automated services authenticate to Nexus Repository without using a user account. Each token is assigned a Nexus Repository role that determines what it can access. You can revoke a token at any time without affecting user accounts.',
        TEXT_VIEWER: 'Service account tokens let applications and other automated services authenticate to Nexus Repository without using a user account. Each token is assigned a Nexus Repository role that determines what it can access.',
        LINK: 'Learn more about service account tokens',
      },
      ACTIONS: {
        REVOKE: 'Revoke',
      },
    },
    CREATE_MODAL: {
      TITLE: 'Create Service Account Token',
      NAME_LABEL: 'Name',
      NAME_DESCRIPTION: 'Enter a unique name for the service account token.',
      NAME_INVALID_CHARS_ERROR: 'Use only letters, numbers, hyphens (-), and underscores (_).',
      NAME_DUPLICATE_ERROR: (name) => `A token named "${name}" already exists.`,
      ROLE_LABEL: 'Role',
      ROLE_DESCRIPTION: 'Select the role that determines what this token can access.',
      ROLE_PLACEHOLDER: 'Select a role',
      ROLES_LOAD_ERROR_FORBIDDEN: <>Missing privilege: <code>nexus:roles:read</code>. Contact your administrator.</>,
      ROLES_LOAD_ERROR_GENERIC: 'Unable to load roles. Try again later.',
      EXPIRATION_LABEL: 'Token expiration',
      EXPIRATION_DESCRIPTION: 'Select when this token expires. Use the shortest period that meets your needs.',
      EXPIRATION_OPTIONS: {
        THIRTY_DAYS: '30 days',
        SIXTY_DAYS: '60 days',
        NINETY_DAYS: '90 days',
        ONE_YEAR: '1 year',
        NEVER: 'Never',
      },
      NEVER_EXPIRES_WARNING: 'Tokens that never expire are less secure. Consider setting an expiration period.',
      DESCRIPTION_LABEL: 'Description',
      DESCRIPTION_DESCRIPTION: 'Describe what this token is used for.',
      CREATE_BUTTON: 'Create Token',
      CREATING_MASK: 'Creating…',
      CANCEL_BUTTON: 'Cancel',
    },
    TOKEN_MODAL: {
      TITLE: 'Service Account Token Created',
      WARNING: "Copy this token now. You won't be able to view it again.",
      AUTO_CLOSE_NOTICE: (seconds) => `This dialog closes in ${seconds} seconds.`,
      BASIC_SECTION_LABEL: 'Basic authentication',
      BEARER_SECTION_LABEL: 'Bearer token',
      USERNAME_LABEL: 'Username',
      PASSWORD_LABEL: 'Password',
      TOKEN_LABEL: 'Token',
      COPY_BUTTON: 'Copy',
      COPIED_BUTTON: 'Copied!',
      COPY_ANNOUNCEMENT: 'Token copied to clipboard.',
      USERNAME_COPY_ANNOUNCEMENT: 'Username copied to clipboard.',
      PASSWORD_COPY_ANNOUNCEMENT: 'Password copied to clipboard.',
      ENCODED_COPY_ANNOUNCEMENT: 'Encoded Authorization header copied to clipboard.',
      COPY_USERNAME_ARIA: 'Copy username',
      COPY_PASSWORD_ARIA: 'Copy password',
      COPY_TOKEN_ARIA: 'Copy token',
      COPY_ENCODED_ARIA: 'Copy encoded Authorization header',
      COPY_FAILED: "Couldn't copy the value. Select it and copy it manually.",
      ENCODED_REVEAL_BUTTON: 'Show encoded header',
      ENCODED_LABEL: 'Encoded Authorization header',
      ENCODED_VALUE: (encoded) => `Authorization: Basic ${encoded}`,
      ENCODED_NOTE: <>This value is already Base64 encoded. Use it in an <code>Authorization: Basic</code> header or as the npm <code>_auth</code> value. Don't use it with <code>curl -u</code> or other tools that encode credentials.</>,
      DOCS_LINK: 'Authenticating with service accounts',
      DOCS_LINK_HREF: 'https://links.sonatype.com/products/nxrm3/docs/authenticating-service-accounts',
      DONE_BUTTON: 'Done',
    },
    REVOKE_MODAL: {
      TITLE: 'Revoke Service Account Token',
      WARNING: (name) => <>Revoking "<strong>{name}</strong>" cannot be undone. Applications and services using this token will immediately lose access.</>,
      LABEL: (name) => <>Type "<strong>{name}</strong>" to confirm</>,
      VALIDATION_ERROR: (name) => `Enter "${name}" to confirm.`,
      REVOKE_BUTTON: 'Revoke Token',
      REVOKING_MASK: 'Revoking…',
      CANCEL_BUTTON: 'Cancel',
    },
    MESSAGES: {
      LOAD_ERROR: 'Unable to load service account tokens. Try again.',
      CREATE_SUCCESS: (name) => `Service account token "${name}" created.`,
      CREATE_ERROR_INVALID: 'Unable to create the token. Check the name and selected role.',
      CREATE_ERROR_FORBIDDEN: "You don't have permission to create service account tokens.",
      CREATE_ERROR_GENERIC: 'Unable to create the service account token. Try again.',
      REVOKE_SUCCESS: (name) => `Service account token "${name}" revoked.`,
      REVOKE_ERROR_NOT_FOUND: (name) => `Service account token "${name}" no longer exists.`,
      REVOKE_ERROR_FORBIDDEN: "You don't have permission to revoke this service account token.",
      REVOKE_ERROR_GENERIC: 'Unable to revoke the service account token. Try again.',
    },
  },
};

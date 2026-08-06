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
      CREATE_BUTTON: 'Create Token',
      EMPTY_LIST: 'No service account tokens were found.',
      EMPTY_LIST_FILTERED: 'No tokens match the current filter.',
      FILTER_PLACEHOLDER: 'Filter by name, role, creator',
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
        TITLE: 'How service account tokens work',
        TEXT_ADMIN: 'View, create, and revoke tokens used by automation, applications, and other services that need to authenticate to this Nexus Repository instance. Each token inherits a Nexus role and can be revoked at any time without affecting user accounts.',
        TEXT_VIEWER: 'View tokens used by automation, applications, and other services that need to authenticate to this Nexus Repository instance. Each token inherits a Nexus role.',
        LINK: 'Learn more about service account tokens',
      },
      ACTIONS: {
        REVOKE: 'Revoke',
      },
    },
    CREATE_MODAL: {
      TITLE: 'Create Service Account Token',
      NAME_LABEL: 'Name',
      NAME_DESCRIPTION: 'A unique, human-readable name for this service account',
      NAME_INVALID_CHARS_ERROR: 'Use only letters, numbers, -, or _.',
      NAME_DUPLICATE_ERROR: (name) => `'${name}' already exists`,
      ROLE_LABEL: 'Role',
      ROLE_DESCRIPTION: 'The role that determines what this token can access',
      ROLE_PLACEHOLDER: 'Select a role...',
      ROLES_LOAD_ERROR_FORBIDDEN: <>Missing privilege: <code>nexus:roles:read</code>. Contact your administrator.</>,
      ROLES_LOAD_ERROR_GENERIC: 'Failed to load roles. Please try again later.',
      EXPIRATION_LABEL: 'Token expiration',
      EXPIRATION_DESCRIPTION: 'When this token will stop working. Choose the shortest period that fits your workflow.',
      EXPIRATION_OPTIONS: {
        THIRTY_DAYS: '30 days',
        SIXTY_DAYS: '60 days',
        NINETY_DAYS: '90 days',
        ONE_YEAR: '1 year',
        NEVER: 'Never',
      },
      NEVER_EXPIRES_WARNING: 'Tokens with no expiration are less secure. Consider using a limited expiration period.',
      DESCRIPTION_LABEL: 'Description',
      DESCRIPTION_DESCRIPTION: 'Optional description of the token purpose',
      CREATE_BUTTON: 'Create',
      CREATING_MASK: 'Creating…',
      CANCEL_BUTTON: 'Cancel',
    },
    TOKEN_MODAL: {
      TITLE: 'Token Created Successfully',
      WARNING: 'Copy this token now — you will not be able to see it again.',
      AUTO_CLOSE_NOTICE: (seconds) => `This dialog closes automatically in ${seconds}s.`,
      COPY_BUTTON: 'Copy',
      COPIED_BUTTON: 'Copied!',
      COPY_ANNOUNCEMENT: 'Token copied to clipboard.',
      COPY_FAILED: 'Copy failed. Select the token above and copy it manually.',
      DONE_BUTTON: 'Done',
    },
    REVOKE_MODAL: {
      TITLE: 'Revoke Token',
      WARNING: (name) => <>Revoking ‘<strong>{name}</strong>’ cannot be undone. Any systems using this token will immediately lose access.</>,
      LABEL: 'Verify Revoke',
      SUBLABEL: (name) => <>Type ‘<strong>{name}</strong>’ to confirm this action and proceed</>,
      VALIDATION_ERROR: 'The confirmation string provided is incorrect',
      REVOKE_BUTTON: 'Revoke Token',
      REVOKING_MASK: 'Revoking…',
      CANCEL_BUTTON: 'Cancel',
    },
    MESSAGES: {
      LOAD_ERROR: 'Failed to load service account tokens.',
      CREATE_SUCCESS: (name) => `Service account token "${name}" was created successfully.`,
      CREATE_ERROR_INVALID: 'The service account token could not be created. Please check the name and selected role.',
      CREATE_ERROR_FORBIDDEN: 'You do not have permission to create this service account token.',
      CREATE_ERROR_GENERIC: 'Failed to create the service account token.',
      REVOKE_SUCCESS: (name) => `Service account token "${name}" was revoked.`,
      REVOKE_ERROR_NOT_FOUND: 'The service account token no longer exists.',
      REVOKE_ERROR_FORBIDDEN: 'You do not have permission to revoke this service account token.',
      REVOKE_ERROR_GENERIC: 'Failed to revoke the service account token.',
    },
  },
};

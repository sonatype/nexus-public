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
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  USER_TOKEN_CONFIGURATION: {
    MENU: {
      text: 'User Tokens',
      description: 'Manage user token configuration'
    },
    CAPTION: 'Token Configuration',
    HELP_TEXT: <>
      The user tokens feature allows users to authenticate securely without typical
      user credentials such as those used by LDAP or Crowd. User tokens generated for
      this server are only valid for use on this server. Once enabled, users can access
      their user token from  {' '}
      <NxTextLink href="#user/usertoken">user mode</NxTextLink>.
    </>,
    USER_TOKENS_CHECKBOX: {
      LABEL: 'User Tokens',
      DESCRIPTION: 'Enable'
    },
    REPOSITORY_AUTHENTICATION_CHECKBOX: {
      LABEL: 'Require User Tokens for Repository Authentication',
      DESCRIPTION: 'Enable'
    },
    EXPIRATION_CHECKBOX: {
      LABEL: 'User Token Expiration',
      DESCRIPTION: 'Enable'
    },
    USER_TOKEN_EXPIRY: {
      LABEL: 'User Token Expiry',
      SUBLABEL: 'Specify the number of days for which a user token is valid. This defaults to 30 days when token expiration is enabled. (E.g., 1-999)'
    },
    RESET_ALL_TOKENS_BUTTON: 'Reset all user tokens',
    RESET_ERROR_MSG: 'An error occurred while resetting user tokens',
    RESET_SUCCESS_MSG: 'User tokens successfully reset',
    RESET_CONFIRMATION: {
      CAPTION: 'Reset User Tokens',
      CONFIRMATION_STRING: 'Reset all user tokens',
      VALIDATION_ERROR: 'The confirmation string provided is incorrect',
      LABEL: 'Verify Reset',
      get SUBLABEL() {
        return <>Type ‘<strong>{this.CONFIRMATION_STRING}</strong>’ to confirm this action and  proceed</>
      },
      WARNING: 'Reset will invalidate ALL existing user tokens and force new tokens to be created the next time they are accessed.',
      BUTTON: 'Reset User Tokens',
    },
    USER_TOKEN_EXPIRY_CONFIRMATION: {
      CAPTION: 'User Token Expiry Changes',
      WARNING: (enabled) => {
        return enabled ? 'Changes to user token expiration will apply to all existing user tokens. Any user tokens older than the specified age will now expire.'
            : 'Disabling user token expiration means that all active user tokens will remain active and never expire.'
      },
      CONFIRM_BUTTON: 'Confirm Changes',
      CANCEL_BUTTON: 'Cancel'
    }
  }
};

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
      SUBLABEL: "Allow user tokens for repository access",
      DESCRIPTION: 'Enable'
    },
    REPOSITORY_AUTHENTICATION_CHECKBOX: {
      LABEL: 'Require User Tokens for Repository Authentication',
      SUBLABEL: "When enabled, any format clients must use a user token to access content. REST APIs and the UI will continue to allow normal logins",
      DESCRIPTION: 'Enable'
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
    }
  }
};
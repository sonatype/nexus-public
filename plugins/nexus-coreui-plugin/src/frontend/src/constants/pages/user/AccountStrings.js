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
  USER_ACCOUNT: {
    MENU: {
      text: 'Account',
      description: 'Manage your account',
    },
    ACTIONS: {
      changePassword: 'Change password',
      discardChangePassword: 'Discard password change',
    },
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading User Account, see console for more details',
      UPDATE_SUCCESS: 'User account settings updated',
      UPDATE_ERROR: 'An error occurred while updating user account settings',
      PASSWORD_MUST_DIFFER_ERROR: 'New password must be different',
      PASSWORD_CHANGE_SUCCESS: 'Password changed',
      PASSWORD_CHANGE_ERROR: 'Change password failed',
    },
    ID_FIELD_LABEL: 'Username',
    FIRST_FIELD_LABEL: 'First Name',
    LAST_FIELD_LABEL: 'Last Name',
    EMAIL_FIELD_LABEL: 'Email',
    PASSWORD_CURRENT_FIELD_LABEL: 'Current Password',
    PASSWORD_NEW_FIELD_LABEL: 'New Password',
    PASSWORD_NEW_CONFIRM_FIELD_LABEL: 'New Password (Confirm)',
  }
};
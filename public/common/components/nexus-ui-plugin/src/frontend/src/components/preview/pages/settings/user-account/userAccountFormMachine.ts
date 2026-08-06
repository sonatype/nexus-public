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

import { assign } from 'xstate';
import { ENDPOINTS, restClient } from '../../../../../interface/api';
import {
  createFormMachine,
  type FormContext,
  type ValidationErrors,
} from '../../../../../interface/form';

const PASSWORD_MIN_LENGTH = 8;

export interface UserAccountData {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  external: boolean;
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export const DEFAULT_USER_ACCOUNT_DATA: UserAccountData = {
  userId: '',
  firstName: '',
  lastName: '',
  email: '',
  external: false,
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

interface RestUserAccount {
  userId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  external?: boolean;
}

function restToData(rest: RestUserAccount): UserAccountData {
  return {
    userId: rest.userId ?? '',
    firstName: rest.firstName ?? '',
    lastName: rest.lastName ?? '',
    email: rest.email ?? '',
    external: rest.external ?? false,
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  };
}

/**
 * Validates the password-change portion of the account form.
 *
 * Note: `currentPassword` presence is deliberately NOT enforced here. Emptiness of any
 * single field just means "user hasn't started changing password yet" — surfacing that as
 * a validation error would light up the whole form on load. The "must fill all three
 * fields before submitting" gate lives in `isPasswordFormReady`, which the UI uses to
 * enable/disable the submit button.
 */
export function validateUserAccount(data: UserAccountData): ValidationErrors {
  const errors: ValidationErrors = {};
  const { currentPassword, newPassword, confirmPassword } = data;
  const anyPasswordFieldFilled =
    Boolean(currentPassword) || Boolean(newPassword) || Boolean(confirmPassword);

  if (!anyPasswordFieldFilled) {
    return errors;
  }

  if (newPassword && newPassword.length < PASSWORD_MIN_LENGTH) {
    errors.newPassword = `Password must be at least ${PASSWORD_MIN_LENGTH} characters`;
  }

  if (newPassword !== confirmPassword) {
    errors.confirmPassword = 'Passwords do not match';
  }

  return errors;
}

/**
 * True when every password field is filled and there are no validation errors,
 * meaning the change-password form is ready to submit.
 */
export function isPasswordFormReady(data: UserAccountData): boolean {
  const { currentPassword, newPassword, confirmPassword } = data;
  if (!currentPassword || !newPassword || !confirmPassword) {
    return false;
  }
  return Object.keys(validateUserAccount(data)).length === 0;
}

/**
 * User account settings form machine.
 *
 * Loads the current user's profile (userId, firstName, lastName, email, external) from
 * the internal UI endpoint. The profile fields are display-only; the editable surface is
 * the change-password form (currentPassword, newPassword, confirmPassword).
 *
 * Save is a PUT to /service/rest/v1/security/users/{userId}/change-password with the new
 * password as a text/plain body. Password validation only fires when the user has begun
 * typing in any password field.
 */
export function createUserAccountFormMachine() {
  return createFormMachine<UserAccountData>({
    id: 'user-account-form',
    resetAfterSave: true,
    context: {
      data: { ...DEFAULT_USER_ACCOUNT_DATA },
    },
    actions: {
      validate: assign((ctx: FormContext<UserAccountData>) => ({
        validationErrors: validateUserAccount(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const rest = await restClient.get<RestUserAccount>(ENDPOINTS.USER_ACCOUNT);
        return { data: restToData(rest) };
      },
      save: async (ctx: FormContext<UserAccountData>) => {
        const { userId, newPassword } = ctx.data;
        if (!userId) {
          throw new Error('User not found');
        }
        await restClient.put(
          `/service/rest/v1/security/users/${encodeURIComponent(userId)}/change-password`,
          newPassword,
          { headers: { 'Content-Type': 'text/plain' } }
        );
      },
    },
  });
}

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

import { useEffect, useMemo, useRef } from 'react';
import { useForm } from '../../../../../interface/form';
import { useToast } from '../../../shared';
import {
  createUserAccountFormMachine,
  isPasswordFormReady,
  type UserAccountData,
} from './userAccountFormMachine';

/**
 * Custom hook for managing the User Account settings form.
 *
 * Wraps createUserAccountFormMachine with toast notifications for load/save success/failure.
 * The account profile fields (userId, firstName, lastName, email, external) are loaded
 * once from the internal UI endpoint; the change-password form is the only editable
 * surface. The machine owns the save PUT request; this hook only wires side-effects
 * (toasts) via state-change effects.
 */
export function useUserAccountForm() {
  const toast = useToast();
  const toastRef = useRef(toast);
  toastRef.current = toast;

  const machine = useMemo(() => createUserAccountFormMachine(), []);
  const form = useForm<UserAccountData>(machine);

  const { hasLoadError, isSaving, saveError } = form;

  useEffect(() => {
    if (hasLoadError) {
      toastRef.current.error('Failed to load account information.');
    }
  }, [hasLoadError]);

  const wasSavingRef = useRef(false);
  useEffect(() => {
    if (wasSavingRef.current && !isSaving) {
      if (saveError) {
        toastRef.current.error(saveError);
      } else {
        toastRef.current.success('Password changed successfully');
      }
    }
    wasSavingRef.current = isSaving;
  }, [isSaving, saveError]);

  return {
    ...form,
    canSubmitPassword: isPasswordFormReady(form.data),
  };
}

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

import { useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { useToast } from '../../../../shared';
import { useUserTokensApi } from './useUserTokensApi';
import { createUserTokensMachine, UserTokensMachineContext } from './userTokensMachine';
import { UserTokenSettings } from './types';

export interface UseUserTokensFormResult {
  data: UserTokenSettings;
  pristineData: UserTokenSettings;
  expirationDaysError?: string;
  isPristine: boolean;
  isLoading: boolean;
  isSaving: boolean;
  isResetting: boolean;
  error: string | null;
  showExpirationWarning: boolean;
  showResetModal: boolean;
  resetConfirmationInput: string;
  resetConfirmationError: string | null;
  handleChange: (field: keyof UserTokenSettings, value: boolean | number) => void;
  handleSubmit: () => void;
  handleDiscard: () => void;
  confirmSave: () => void;
  cancelSave: () => void;
  requestReset: () => void;
  setResetConfirmation: (value: string) => void;
  confirmReset: () => void;
  cancelReset: () => void;
  clearError: () => void;
}

/**
 * Integration hook wiring userTokensMachine to useUserTokensApi and toast
 * notifications. Machine drives state/transitions; this hook only maps
 * context/state to UI-facing flags and dispatches events.
 */
export function useUserTokensForm(): UseUserTokensFormResult {
  const api = useUserTokensApi();
  const toast = useToast();
  const machine = useMemo(() => createUserTokensMachine(), []);
  const [state, send] = useMachine(machine, {
    services: {
      load: () => api.fetchSettings(),
      save: async (ctx: UserTokensMachineContext) => {
        await api.saveSettings(ctx.data);
        toast.success('User token settings saved successfully');
      },
      resetAllTokens: async () => {
        await api.resetAllTokens();
        toast.success('All user tokens have been reset');
      },
    },
  });

  const c = state.context;

  return {
    data: c.data,
    pristineData: c.pristineData,
    expirationDaysError: c.validationErrors.expirationDays ?? undefined,
    isPristine: c.isPristine,
    isLoading: state.matches('loading'),
    isSaving: state.matches('saving'),
    isResetting: state.matches('resettingAllTokens'),
    error: c.loadError ?? c.saveError ?? c.resetError,
    showExpirationWarning: state.matches('confirmingSaveWithExpirationWarning'),
    showResetModal: state.matches('confirmingResetAllTokens') || state.matches('resettingAllTokens'),
    resetConfirmationInput: c.resetConfirmationInput,
    resetConfirmationError: c.resetConfirmationError,
    handleChange: (field, value) => send({ type: 'UPDATE', field, value }),
    handleSubmit: () => send({ type: 'SUBMIT' }),
    handleDiscard: () => send({ type: 'DISCARD' }),
    confirmSave: () => send({ type: 'CONFIRM_SAVE' }),
    cancelSave: () => send({ type: 'CANCEL_SAVE' }),
    requestReset: () => send({ type: 'REQUEST_RESET' }),
    setResetConfirmation: (value) => send({ type: 'UPDATE_RESET_CONFIRMATION', value }),
    confirmReset: () => send({ type: 'CONFIRM_RESET' }),
    cancelReset: () => send({ type: 'CANCEL_RESET' }),
    clearError: () => send({ type: 'CLEAR_ERROR' }),
  };
}

export default useUserTokensForm;

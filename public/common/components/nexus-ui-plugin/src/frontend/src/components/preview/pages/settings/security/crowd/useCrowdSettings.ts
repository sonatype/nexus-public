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
import { clearDirtyState, useToast } from '../../../../shared';
import { createCrowdSettingsMachine, CrowdMachineContext, CrowdValidationErrors } from './crowdSettingsMachine';
import { CrowdConfig } from './types';
import { fetchCrowdConfig, saveCrowdConfig, verifyCrowdConnection, clearCrowdCache } from './crowdApi';

export interface UseCrowdSettingsResult {
  config: CrowdConfig;
  validationErrors: CrowdValidationErrors;
  isDirty: boolean;
  isFormValid: boolean;
  /** Initial data load in progress (drives the full-page spinner). */
  isInitialLoading: boolean;
  /** A save/verify/clear-cache operation is in progress (disables actions). */
  isBusy: boolean;
  error: string | null;
  handleChange: (field: keyof CrowdConfig, value: string | boolean | number | undefined) => void;
  handleSubmit: () => void;
  handleDiscard: () => void;
  handleVerifyConnection: () => void;
  handleClearCache: () => void;
  clearError: () => void;
}

/**
 * Integration hook wiring the Crowd settings machine to React. Injects the API
 * services and success toasts; the machine owns config/validation/busy state.
 *
 * Note: the legacy 'crowd-form' dirty-tracking key is retained (cleared on save)
 * to preserve existing navigation-warning behavior.
 */
export function useCrowdSettings(): UseCrowdSettingsResult {
  const toast = useToast();
  const machine = useMemo(() => createCrowdSettingsMachine(), []);

  const [state, send] = useMachine(machine, {
    services: {
      load: () => fetchCrowdConfig(),
      save: async (ctx: CrowdMachineContext) => {
        await saveCrowdConfig(ctx.data);
        clearDirtyState('crowd-form');
        toast.success('Atlassian Crowd settings updated');
      },
      verifyConnection: async (ctx: CrowdMachineContext) => {
        await verifyCrowdConnection(ctx.data);
        toast.success('Connection to Crowd server verified');
      },
      clearCache: async () => {
        await clearCrowdCache();
        toast.success('Crowd cache has been cleared');
      },
    },
  });

  const c = state.context;
  const isBusy =
    state.matches('saving') || state.matches('verifyingConnection') || state.matches('clearingCache');

  return {
    config: c.data,
    validationErrors: c.validationErrors,
    isDirty: !c.isPristine,
    isFormValid: Object.keys(c.validationErrors).length === 0,
    isInitialLoading: state.matches('loading'),
    isBusy,
    error: c.error,
    handleChange: (field, value) => send({ type: 'UPDATE', field, value }),
    handleSubmit: () => send({ type: 'SUBMIT' }),
    handleDiscard: () => send({ type: 'DISCARD' }),
    handleVerifyConnection: () => send({ type: 'VERIFY_CONNECTION' }),
    handleClearCache: () => send({ type: 'CLEAR_CACHE' }),
    clearError: () => send({ type: 'CLEAR_ERROR' }),
  };
}

export default useCrowdSettings;

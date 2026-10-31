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

import { useState, useCallback } from 'react';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient, parseApiError } from '../../../../../../interface/api';

import { RecoveryModeData } from './types';

const RECOVERY_MODE_UI_URL = APIConstants.REST.INTERNAL.RECOVERY_MODE;
const RECOVERY_MODE_PUBLIC_URL = APIConstants.REST.PUBLIC.RECOVERY_MODE;

/**
 * Hook for Recovery Mode API operations.
 *
 * - fetchRecoveryMode: GET internal/ui/recovery-mode (state + tasks)
 * - enableRecoveryMode: POST v1/recovery-mode
 * - disableRecoveryMode: DELETE v1/recovery-mode
 */
export function useRecoveryModeApi() {
  const [error, setError] = useState<string | null>(null);

  const fetchRecoveryMode = useCallback(async (): Promise<RecoveryModeData> => {
    setError(null);
    try {
      return await restClient.get<RecoveryModeData>(RECOVERY_MODE_UI_URL);
    } catch (err: unknown) {
      const message = parseApiError(err).message || 'Failed to load recovery mode settings';
      setError(message);
      throw err;
    }
  }, []);

  const enableRecoveryMode = useCallback(async (): Promise<void> => {
    setError(null);
    try {
      await restClient.post(RECOVERY_MODE_PUBLIC_URL);
    } catch (err: unknown) {
      const message = parseApiError(err).message || 'Failed to enable recovery mode';
      setError(message);
      throw err;
    }
  }, []);

  const disableRecoveryMode = useCallback(async (): Promise<void> => {
    setError(null);
    try {
      await restClient.delete(RECOVERY_MODE_PUBLIC_URL);
    } catch (err: unknown) {
      const message = parseApiError(err).message || 'Failed to disable recovery mode';
      setError(message);
      throw err;
    }
  }, []);

  return {
    error,
    setError,
    fetchRecoveryMode,
    enableRecoveryMode,
    disableRecoveryMode,
  };
}

export default useRecoveryModeApi;

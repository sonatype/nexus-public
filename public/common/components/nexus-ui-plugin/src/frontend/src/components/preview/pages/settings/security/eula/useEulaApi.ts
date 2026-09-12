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

import { useCallback } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';

/**
 * EULA status response from the backend.
 */
export interface EulaStatus {
  accepted: boolean;
  disclaimer: string;
}

/**
 * Request body for accepting the EULA.
 */
export interface AcceptEulaRequest {
  accepted: boolean;
  disclaimer: string;
}

const EULA_API = '/service/rest/v1/system/eula';

/**
 * Thin wrapper around the EULA REST endpoints. Consumers own loading/error
 * state; this hook only exposes the two request functions so the same call
 * sites don't trigger extra re-renders on internal loading toggles.
 *
 * Shared between the preview EULA settings page (its original home) and the
 * onboarding wizard's EulaStep. Moving it out of this directory would break
 * either import path, so the two share it from here until an obvious neutral
 * location (`src/interface/`) is warranted.
 */
export function useEulaApi() {
  const fetchEulaStatus = useCallback(async (): Promise<EulaStatus> => {
    try {
      return await restClient.get<EulaStatus>(EULA_API);
    } catch (err: unknown) {
      throw new Error(parseApiError(err).message);
    }
  }, []);

  const acceptEula = useCallback(async (disclaimer: string): Promise<void> => {
    const requestBody: AcceptEulaRequest = { accepted: true, disclaimer };
    try {
      await restClient.post(EULA_API, requestBody);
    } catch (err: unknown) {
      throw new Error(parseApiError(err).message);
    }
  }, []);

  return { fetchEulaStatus, acceptEula };
}

export default useEulaApi;

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

import { useCallback, useEffect, useRef, useState } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';

const ENDPOINT = '/service/rest/v1/repositories/global-distribution/settings';

export interface CdnDefaultsError {
  status: number;
  message: string;
}

interface CdnDefaultsResponse {
  defaultForNewRepositories: boolean;
}

export interface UseCdnDefaultsResult {
  defaultForNewRepositories: boolean;
  isLoading: boolean;
  hasLoaded: boolean;
  error: CdnDefaultsError | null;
  setDefault: (value: boolean) => Promise<void>;
  setError: (error: CdnDefaultsError | null) => void;
}

export interface UseCdnDefaultsOptions {
  /** False skips the fetch; the endpoint only exists on Enterprise. */
  enabled?: boolean;
}

export function useCdnDefaults({ enabled = true }: UseCdnDefaultsOptions = {}): UseCdnDefaultsResult {
  const [defaultForNewRepositories, setValue] = useState(false);
  const [isLoading, setIsLoading] = useState(enabled);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [error, setError] = useState<CdnDefaultsError | null>(null);

  // Last value the server acknowledged; rollback target on PUT failure. A ref avoids the stale
  // closure when setDefault is invoked twice in quick succession (the second call captured the
  // pre-first-call state via useCallback deps).
  const committedRef = useRef(false);

  useEffect(() => {
    if (!enabled) {
      // Cancels any in-flight GET; clear loading so the page isn't stuck on a spinner.
      setError(null);
      setIsLoading(false);
      return;
    }
    let cancelled = false;
    setIsLoading(true);
    (async () => {
      try {
        const data = await restClient.get<CdnDefaultsResponse>(ENDPOINT);
        if (!cancelled) {
          const loaded = Boolean(data?.defaultForNewRepositories);
          committedRef.current = loaded;
          setValue(loaded);
          setHasLoaded(true);
          setError(null);
          setIsLoading(false);
        }
      } catch (err) {
        if (!cancelled) {
          const apiError = parseApiError(err);
          setError({ status: apiError.status, message: apiError.message });
          setIsLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [enabled]);

  const setDefault = useCallback(async (value: boolean) => {
    const previous = committedRef.current;
    setValue(value);
    setError(null);
    try {
      await restClient.put(ENDPOINT, { defaultForNewRepositories: value });
      committedRef.current = value;
    } catch (err) {
      setValue(previous);
      const apiError = parseApiError(err);
      setError({ status: apiError.status, message: apiError.message });
    }
  }, []);

  return { defaultForNewRepositories, isLoading, hasLoaded, error, setDefault, setError };
}

export default useCdnDefaults;

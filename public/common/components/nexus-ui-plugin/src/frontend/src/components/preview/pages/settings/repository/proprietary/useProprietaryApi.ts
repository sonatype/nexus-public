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

/**
 * Proprietary Repositories API Hook
 *
 * 100% REST - No ExtDirect.
 * Backend: RepositoryProprietaryContentResource.java
 * - GET  /internal/proprietary-content -> list of proprietary repo names
 * - POST /internal/proprietary-content -> update proprietary/nonProprietary lists
 */

import { useState, useCallback } from 'react';
import { restClient, parseApiError, ENDPOINTS } from '../../../../../../interface/api';
import { RepositoryReference, ProprietaryRepositoriesSettings } from './types';

const PROPRIETARY_URL = '/service/rest/internal/proprietary-content';

interface RestRepository {
  name: string;
  format: string;
  type: string;
  url?: string;
  online?: boolean;
}

export function useProprietaryApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSettings = useCallback(async (): Promise<ProprietaryRepositoriesSettings> => {
    try {
      const data = await restClient.get<string[]>(PROPRIETARY_URL);
      return {
        enabledRepositories: Array.isArray(data) ? data : [],
      };
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      throw new Error(apiError.message || 'Failed to load proprietary repositories settings');
    }
  }, []);

  const fetchPossibleRepositories = useCallback(async (): Promise<RepositoryReference[]> => {
    try {
      const repositories = await restClient.get<RestRepository[]>(ENDPOINTS.REPOSITORIES);
      if (!Array.isArray(repositories)) return [];
      return repositories
        .filter((repo) => repo.type === 'hosted')
        .map((repo) => ({ id: repo.name, name: repo.name }));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      throw new Error(apiError.message || 'Failed to load repositories');
    }
  }, []);

  const updateSettings = useCallback(async (
    enabledRepositories: string[],
    allPossibleRepositories?: RepositoryReference[]
  ): Promise<ProprietaryRepositoriesSettings> => {
    setLoading(true);
    setError(null);
    try {
      const allHostedNames = allPossibleRepositories?.map((r) => r.name) || [];
      const enabledSet = new Set(enabledRepositories);
      const nonProprietary = allHostedNames.filter((name) => !enabledSet.has(name));

      await restClient.post(PROPRIETARY_URL, {
        proprietary: enabledRepositories,
        nonProprietary,
      });

      return await fetchSettings();
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to update proprietary repositories settings';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, [fetchSettings]);

  return {
    loading,
    error,
    setError,
    fetchSettings,
    fetchPossibleRepositories,
    updateSettings,
  };
}

export default useProprietaryApi;

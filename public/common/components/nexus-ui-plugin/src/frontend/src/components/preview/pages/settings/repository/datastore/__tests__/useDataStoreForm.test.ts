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

import { renderHook, act, waitFor } from '@testing-library/react';
import { useDataStoreForm } from '../useDataStoreForm';
import * as api from '../../../../../../../interface/api';

jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: { DATASTORE: '/service/rest/internal/ui/datastore' },
  restClient: {
    get: jest.fn().mockResolvedValue({
      jdbcUrl: 'jdbc:postgresql://h/db', username: 'u', schema: 's',
      maximumConnectionPool: 100, advanced: 'socketTimeout=30000',
    }),
    put: jest.fn().mockResolvedValue({ maximumConnectionPool: 200, advanced: 'socketTimeout=30000' }),
  },
  parseApiError: (err: unknown) => ({
    status: 0,
    message: err instanceof Error ? err.message : String(err),
  }),
}));

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('../../../../../shared', () => ({ useToast: () => ({ success: mockToastSuccess, error: mockToastError }) }));

describe('useDataStoreForm', () => {
  beforeEach(() => jest.clearAllMocks());

  it('exposes derived databaseType and effectiveConfig after load', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.databaseType).toBe('PostgreSQL');
    expect(result.current.effectiveConfig[0].name).toBe('maximumConnectionPool');
  });

  it('confirmResetParams removes custom params and closes the confirm', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    act(() => result.current.requestResetParams());
    expect(result.current.showResetConfirm).toBe(true);
    act(() => result.current.confirmResetParams());
    expect(result.current.showResetConfirm).toBe(false);
    expect(result.current.data.jdbcParameters).toHaveLength(0);
  });

  it('humanizes error for unknown JDBC parameters on save failure', async () => {
    const { restClient } = api as jest.Mocked<typeof api>;
    (restClient.put as jest.Mock).mockRejectedValueOnce(new Error('Connection property "unknown" is invalid or contains unknown'));
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.submit());
    await waitFor(() => {
      expect(result.current.saveError).toBe(
        'One or more advanced JDBC parameters are not recognized by the database. Remove unknown parameters and try again.'
      );
    });
  });

  it('sets showAllValidation when submit is called with parameter errors', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    // Add a custom parameter with an error to trigger hasParameterErrors (empty name is invalid)
    act(() => {
      result.current.setParameters([{ name: '', value: 'val', isDefault: false, isCustom: true }]);
    });

    expect(result.current.hasParameterErrors).toBe(true);
    expect(result.current.showAllValidation).toBe(false);

    act(() => result.current.submit());

    expect(result.current.showAllValidation).toBe(true);
  });

  it('keeps maximumConnectionPool source as Default when only jdbcParameters change', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => {
      result.current.setParameters([{ name: 'socketTimeout', value: '60000', isDefault: false, isCustom: true }]);
    });

    expect(result.current.isPristine).toBe(false);
    expect(result.current.effectiveConfig[0].name).toBe('maximumConnectionPool');
    expect(result.current.effectiveConfig[0].source).toBe('Default');
  });

  it('marks maximumConnectionPool source as Custom only when its own value changes', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setMaxPool('50'));

    expect(result.current.effectiveConfig[0].source).toBe('Custom');
  });

  it('reset clears showAllValidation and showResetConfirm local state', async () => {
    const { result } = renderHook(() => useDataStoreForm());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => {
      result.current.setParameters([{ name: '', value: 'val', isDefault: false, isCustom: true }]);
    });
    act(() => result.current.submit());
    expect(result.current.showAllValidation).toBe(true);
    act(() => result.current.requestResetParams());
    expect(result.current.showResetConfirm).toBe(true);

    act(() => result.current.reset());

    expect(result.current.showAllValidation).toBe(false);
    expect(result.current.showResetConfirm).toBe(false);
  });

  describe('databaseType derivation', () => {
    it('returns PostgreSQL for postgresql JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:postgresql://host/db' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('PostgreSQL');
    });

    it('returns H2 for h2 JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:h2:mem:test' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('H2');
    });

    it('returns MySQL for mysql JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:mysql://host/db' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('MySQL');
    });

    it('returns Oracle for oracle JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:oracle:thin:@host:1521:db' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('Oracle');
    });

    it('returns SQL Server for sqlserver JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:sqlserver://host:1433;database=db' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('SQL Server');
    });

    it('returns Unknown for unrecognized JDBC URL', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: 'jdbc:somethingelse://host' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('Unknown');
    });

    it('returns Not configured when jdbcUrl is empty', async () => {
      const { restClient } = api as jest.Mocked<typeof api>;
      (restClient.get as jest.Mock).mockResolvedValueOnce({ jdbcUrl: '' });
      const { result } = renderHook(() => useDataStoreForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.databaseType).toBe('Not configured');
    });
  });
});

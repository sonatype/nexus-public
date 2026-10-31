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

import { renderHook, act } from '@testing-library/react';
import { useRecoveryModeApi } from '../useRecoveryModeApi';
import { RecoveryModeData } from '../types';

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockDelete = jest.fn();

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
  parseApiError: (err: unknown) => ({
    message: err instanceof Error ? err.message : '',
  }),
}));

const INTERNAL_URL = 'service/rest/internal/ui/recovery-mode';
const PUBLIC_URL = 'service/rest/v1/recovery-mode';

const data: RecoveryModeData = {
  enabled: true,
  unexecutedPlans: false,
  blockedTaskNames: [],
  reconcileTasks: [],
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('useRecoveryModeApi', () => {
  it('starts with no error', () => {
    const { result } = renderHook(() => useRecoveryModeApi());
    expect(result.current.error).toBeNull();
  });

  describe('fetchRecoveryMode', () => {
    it('GETs the internal UI endpoint and returns its data', async () => {
      mockGet.mockResolvedValue(data);
      const { result } = renderHook(() => useRecoveryModeApi());

      let returned: RecoveryModeData | undefined;
      await act(async () => {
        returned = await result.current.fetchRecoveryMode();
      });

      expect(mockGet).toHaveBeenCalledWith(INTERNAL_URL);
      expect(returned).toEqual(data);
      expect(result.current.error).toBeNull();
    });

    it('sets the parsed error message and rethrows on failure', async () => {
      mockGet.mockRejectedValue(new Error('boom'));
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.fetchRecoveryMode()).rejects.toThrow('boom');
      });

      expect(result.current.error).toBe('boom');
    });

    it('falls back to a default message when the error has none', async () => {
      mockGet.mockRejectedValue({});
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.fetchRecoveryMode()).rejects.toBeDefined();
      });

      expect(result.current.error).toBe('Failed to load recovery mode settings');
    });
  });

  describe('enableRecoveryMode', () => {
    it('POSTs the public endpoint', async () => {
      mockPost.mockResolvedValue(undefined);
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await result.current.enableRecoveryMode();
      });

      expect(mockPost).toHaveBeenCalledWith(PUBLIC_URL);
      expect(result.current.error).toBeNull();
    });

    it('sets the error and rethrows on failure', async () => {
      mockPost.mockRejectedValue(new Error('nope'));
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.enableRecoveryMode()).rejects.toThrow('nope');
      });

      expect(result.current.error).toBe('nope');
    });

    it('falls back to a default enable error message', async () => {
      mockPost.mockRejectedValue({});
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.enableRecoveryMode()).rejects.toBeDefined();
      });

      expect(result.current.error).toBe('Failed to enable recovery mode');
    });
  });

  describe('disableRecoveryMode', () => {
    it('DELETEs the public endpoint', async () => {
      mockDelete.mockResolvedValue(undefined);
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await result.current.disableRecoveryMode();
      });

      expect(mockDelete).toHaveBeenCalledWith(PUBLIC_URL);
      expect(result.current.error).toBeNull();
    });

    it('sets the error and rethrows on failure', async () => {
      mockDelete.mockRejectedValue(new Error('denied'));
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.disableRecoveryMode()).rejects.toThrow('denied');
      });

      expect(result.current.error).toBe('denied');
    });

    it('falls back to a default disable error message', async () => {
      mockDelete.mockRejectedValue({});
      const { result } = renderHook(() => useRecoveryModeApi());

      await act(async () => {
        await expect(result.current.disableRecoveryMode()).rejects.toBeDefined();
      });

      expect(result.current.error).toBe('Failed to disable recovery mode');
    });
  });
});

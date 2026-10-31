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

import { renderHook } from '@testing-library/react';

import { useEulaApi, type EulaStatus } from '../useEulaApi';

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockParseApiError = jest.fn();

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
  parseApiError: (err: unknown) => mockParseApiError(err),
}));

const EULA_API = '/service/rest/v1/system/eula';

describe('useEulaApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockParseApiError.mockImplementation((err: any) => ({
      message: err?.response?.data?.message || err?.message || 'Unknown error',
    }));
  });

  describe('fetchEulaStatus', () => {
    it('calls the GET endpoint and returns the response body', async () => {
      const response: EulaStatus = { accepted: false, disclaimer: 'EULA text' };
      mockGet.mockResolvedValueOnce(response);

      const { result } = renderHook(() => useEulaApi());
      const status = await result.current.fetchEulaStatus();

      expect(mockGet).toHaveBeenCalledWith(EULA_API);
      expect(status).toEqual(response);
    });

    it('propagates the response body when accepted is true', async () => {
      const response: EulaStatus = { accepted: true, disclaimer: 'Accepted EULA' };
      mockGet.mockResolvedValueOnce(response);

      const { result } = renderHook(() => useEulaApi());
      const status = await result.current.fetchEulaStatus();

      expect(status).toEqual(response);
    });

    it('maps GET errors through parseApiError', async () => {
      const original = {
        response: { data: { message: 'Backend refused' }, status: 500 },
      };
      mockGet.mockRejectedValueOnce(original);

      const { result } = renderHook(() => useEulaApi());

      await expect(result.current.fetchEulaStatus()).rejects.toThrow('Backend refused');
      expect(mockParseApiError).toHaveBeenCalledWith(original);
    });

    it('falls back to the parseApiError default for opaque errors', async () => {
      mockGet.mockRejectedValueOnce({});

      const { result } = renderHook(() => useEulaApi());

      await expect(result.current.fetchEulaStatus()).rejects.toThrow('Unknown error');
    });

    it('surfaces plain Error messages via parseApiError', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network down'));

      const { result } = renderHook(() => useEulaApi());

      await expect(result.current.fetchEulaStatus()).rejects.toThrow('Network down');
    });
  });

  describe('acceptEula', () => {
    it('POSTs { accepted: true, disclaimer } to the EULA endpoint', async () => {
      mockPost.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useEulaApi());
      await result.current.acceptEula('The disclaimer body');

      expect(mockPost).toHaveBeenCalledWith(EULA_API, {
        accepted: true,
        disclaimer: 'The disclaimer body',
      });
    });

    it('resolves with undefined on success (204 No Content contract)', async () => {
      mockPost.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useEulaApi());
      await expect(result.current.acceptEula('text')).resolves.toBeUndefined();
    });

    it('maps POST errors through parseApiError', async () => {
      const original = {
        response: { data: { message: 'Disclaimer mismatch' }, status: 400 },
      };
      mockPost.mockRejectedValueOnce(original);

      const { result } = renderHook(() => useEulaApi());

      await expect(result.current.acceptEula('stale')).rejects.toThrow('Disclaimer mismatch');
      expect(mockParseApiError).toHaveBeenCalledWith(original);
    });

    it('passes an empty disclaimer through unchanged', async () => {
      mockPost.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useEulaApi());
      await result.current.acceptEula('');

      expect(mockPost).toHaveBeenCalledWith(EULA_API, { accepted: true, disclaimer: '' });
    });
  });

  describe('hook identity', () => {
    it('returns stable function references across renders', () => {
      const { result, rerender } = renderHook(() => useEulaApi());
      const first = result.current;
      rerender();
      const second = result.current;

      expect(second.fetchEulaStatus).toBe(first.fetchEulaStatus);
      expect(second.acceptEula).toBe(first.acceptEula);
    });
  });
});

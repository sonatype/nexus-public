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
import { useNodesApi } from '../useNodesApi';

const mockExtAPIRequest = jest.fn();
const mockCheckForError = jest.fn();
const mockRestClientGet = jest.fn();

jest.mock('../../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: (...args: unknown[]) => mockExtAPIRequest(...args),
    checkForError: (...args: unknown[]) => mockCheckForError(...args),
  },
}));

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestClientGet(...args),
  },
  parseApiError: (err: unknown) => ({
    message: err instanceof Error ? err.message : 'Unknown error',
  }),
}));

const extDirectNodes = [
  { name: 'uuid-1234', local: true },
  { name: 'uuid-5678', local: false },
];

const activeNodes = [
  { nodeId: 'uuid-1234', hostname: 'Guillermos-MacBook-Pro.local' },
  { nodeId: 'uuid-5678', hostname: 'remote-host.local' },
];

describe('useNodesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckForError.mockReturnValue(null);
  });

  describe('fetchNodes', () => {
    it('merges hostname from activenodes with local flag from ExtDirect', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: extDirectNodes } },
      });
      mockRestClientGet.mockResolvedValue(activeNodes);

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual([
        { name: 'uuid-1234', displayName: 'Guillermos-MacBook-Pro.local', local: true },
        { name: 'uuid-5678', displayName: 'remote-host.local', local: false },
      ]);
      expect(mockExtAPIRequest).toHaveBeenCalledWith('node_NodeAccess', 'nodes');
      expect(mockRestClientGet).toHaveBeenCalledWith(
        'service/rest/internal/ui/supportzip/activenodes'
      );
    });

    it('falls back to UUID as display name when activenodes is unavailable', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: extDirectNodes } },
      });
      mockRestClientGet.mockRejectedValue(new Error('Not available'));

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual([
        { name: 'uuid-1234', displayName: 'uuid-1234', local: true },
        { name: 'uuid-5678', displayName: 'uuid-5678', local: false },
      ]);
    });

    it('falls back to UUID when activenodes returns no match for a node', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: extDirectNodes } },
      });
      mockRestClientGet.mockResolvedValue([{ nodeId: 'uuid-1234', hostname: 'Guillermos-MacBook-Pro.local' }]);

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual([
        { name: 'uuid-1234', displayName: 'Guillermos-MacBook-Pro.local', local: true },
        { name: 'uuid-5678', displayName: 'uuid-5678', local: false },
      ]);
    });

    it('handles error when ExtDirect fetch fails', async () => {
      mockExtAPIRequest.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useNodesApi());

      await act(async () => {
        await expect(result.current.fetchNodes()).rejects.toThrow('Network error');
      });
    });

    it('returns empty array when ExtDirect data is not an array', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: null } },
      });
      mockRestClientGet.mockResolvedValue([]);

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual([]);
    });
  });
});

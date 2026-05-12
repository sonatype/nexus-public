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

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtAPIUtils: {
    extAPIRequest: (...args: unknown[]) => mockExtAPIRequest(...args),
    checkForError: (...args: unknown[]) => mockCheckForError(...args),
  },
}));

describe('useNodesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckForError.mockReturnValue(null);
  });

  describe('fetchNodes', () => {
    it('fetches nodes successfully', async () => {
      const mockNodes = [
        {
          nodeId: 'node-1',
          socketAddress: '192.168.1.1:8081',
          hostname: 'nexus-node-1',
          local: true,
          friendlyName: 'Primary Node',
        },
        {
          nodeId: 'node-2',
          socketAddress: '192.168.1.2:8081',
          hostname: 'nexus-node-2',
          local: false,
          friendlyName: 'Secondary Node',
        },
      ];

      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: mockNodes } },
      });

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual(mockNodes);
      expect(mockExtAPIRequest).toHaveBeenCalledWith('node_NodeAccess', 'nodes');
    });

    it('handles error when fetching nodes fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      mockExtAPIRequest.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useNodesApi());

      await act(async () => {
        await expect(result.current.fetchNodes()).rejects.toThrow('Network error');
      });

      errorSpy.mockRestore();
    });

    it('returns empty array when data is not an array', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { result: { data: null } },
      });

      const { result } = renderHook(() => useNodesApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchNodes();
      });

      expect(nodes).toEqual([]);
    });
  });
});

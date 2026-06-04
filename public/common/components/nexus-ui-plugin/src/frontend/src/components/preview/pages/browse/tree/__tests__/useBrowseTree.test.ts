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
import { useBrowseTree } from '../useBrowseTree';
import {
  mockRootNodes, mockFolder1Children, mockSubfolder1Children,
  getMockChildren, mockMixedWithErrorNodes, mockErrorNodes,
  mockGoRootNodes, mockGoGithubChildren, mockGoDatadogChildren,
  mockGoGostackparseChildren, mockGoAtVChildren,
  mockPlusRootNodes, mockPlusChildren,
} from './mockData';
import type { BrowseNode } from '../browse-tree.types';

// Mock the relative paths that the source imports from
jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response) => response.data.result.data),
  },
}));

jest.mock('../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    EXT: {
      BROWSE: {
        ACTION: 'coreui_Browse',
        METHODS: {
          READ: 'read',
        },
      },
    },
  },
}));

import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';

/**
 * Helper to create a mock API response.
 */
function mockApiResponse(data: BrowseNode[]) {
  return {
    data: {
      result: {
        success: true,
        data,
      },
    },
  };
}

/**
 * Helper to setup the ExtAPIUtils mock.
 */
function setupApiMock() {
  ExtAPIUtils.extAPIRequest.mockImplementation(
    async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
      const requestedNodeId = data[0].node;
      const mockChildren = getMockChildren(requestedNodeId);
      return mockApiResponse(mockChildren);
    }
  );
}

describe('useBrowseTree', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupApiMock();
  });

  describe('initialization', () => {
    it('starts in loading state', () => {
      ExtAPIUtils.extAPIRequest.mockImplementation(() => new Promise(() => {})); // Never resolves

      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      expect(result.current.state.loading).toBe(true);
      expect(result.current.state.nodes).toEqual([]);
    });

    it('loads root nodes on mount', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      expect(result.current.state.nodes).toHaveLength(3);
      expect(result.current.state.nodes[0].node.id).toBe('folder1');
    });

    it('sets error state on fetch failure', async () => {
      ExtAPIUtils.extAPIRequest.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      expect(result.current.state.error).toBe('Network error');
    });
  });

  describe('toggle action', () => {
    it('expands a node and loads children', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
      expect(folder1?.expanded).toBe(true);
      expect(folder1?.children).toHaveLength(2);
    });

    it('collapses an expanded node', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Collapse folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
      expect(folder1?.expanded).toBe(false);
      // Children should still be cached
      expect(folder1?.children).toHaveLength(2);
    });

    it('uses cached children when re-expanding', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Clear mock to track calls
      ExtAPIUtils.extAPIRequest.mockClear();

      // Collapse folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Expand folder1 again
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Should not have made another API call (children cached from auto-expand)
      expect(ExtAPIUtils.extAPIRequest).not.toHaveBeenCalled();
    });

    it('sets loading state while fetching children', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Set up controlled mock for subfolder1's children (not yet loaded)
      let resolvePromise: (value: unknown) => void;
      ExtAPIUtils.extAPIRequest.mockImplementation(
        () =>
          new Promise((resolve) => {
            resolvePromise = resolve;
          })
      );

      // Start expanding subfolder1 (children not loaded yet)
      act(() => {
        result.current.actions.toggle('folder1/subfolder1');
      });

      // subfolder1 should be loading
      await waitFor(() => {
        const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
        const subfolder1 = folder1?.children?.find((n) => n.node.id === 'folder1/subfolder1');
        expect(subfolder1?.loading).toBe(true);
      });

      // Resolve the promise
      await act(async () => {
        resolvePromise!(mockApiResponse(mockSubfolder1Children));
      });

      // subfolder1 should no longer be loading
      await waitFor(() => {
        const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
        const subfolder1 = folder1?.children?.find((n) => n.node.id === 'folder1/subfolder1');
        expect(subfolder1?.loading).toBe(false);
      });
    });
  });

  describe('select action', () => {
    it('sets the selected node id', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      act(() => {
        result.current.actions.select('folder1');
      });

      expect(result.current.state.selectedNodeId).toBe('folder1');
      expect(result.current.state.focusedNodeId).toBe('folder1');
    });
  });

  describe('refresh action', () => {
    it('reloads the root nodes', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      ExtAPIUtils.extAPIRequest.mockClear();

      await act(async () => {
        await result.current.actions.refresh();
      });

      expect(ExtAPIUtils.extAPIRequest).toHaveBeenCalled();
    });
  });

  describe('keyboard navigation', () => {
    it('focusNext moves to the next visible node', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Set initial focus
      act(() => {
        result.current.actions.select('folder1');
      });

      // Move to next — folder1 is expanded, so next is its first child
      act(() => {
        result.current.actions.focusNext();
      });

      expect(result.current.state.focusedNodeId).toBe('folder1/subfolder1');
    });

    it('focusPrevious moves to the previous visible node', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Set initial focus on component1
      act(() => {
        result.current.actions.select('component1');
      });

      // Move to previous — folder1 is expanded, so previous is its last child
      act(() => {
        result.current.actions.focusPrevious();
      });

      expect(result.current.state.focusedNodeId).toBe('folder1/component2');
    });

    it('focusNext includes children of expanded nodes', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Set focus on folder1
      act(() => {
        result.current.actions.select('folder1');
      });

      // Move to next - should be first child
      act(() => {
        result.current.actions.focusNext();
      });

      expect(result.current.state.focusedNodeId).toBe('folder1/subfolder1');
    });

    it('focusParent moves to the parent node', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Focus on child
      act(() => {
        result.current.actions.select('folder1/subfolder1');
      });

      // Move to parent
      act(() => {
        result.current.actions.focusParent();
      });

      expect(result.current.state.focusedNodeId).toBe('folder1');
    });

    it('focusFirstChild moves to the first child of expanded node', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Focus on folder1
      act(() => {
        result.current.actions.select('folder1');
      });

      // Move to first child
      act(() => {
        result.current.actions.focusFirstChild();
      });

      expect(result.current.state.focusedNodeId).toBe('folder1/subfolder1');
    });
  });

  describe('expandToPath', () => {
    it('expands the tree to a specific path', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1 via expandToPath
      await act(async () => {
        await result.current.actions.expandToPath('folder1');
      });

      const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
      expect(folder1?.expanded).toBe(true);
      expect(folder1?.children).toBeTruthy();
    });
  });

  describe('error node filtering (bug s0h6)', () => {
    it('filters out nodes with Java exception text', async () => {
      ExtAPIUtils.extAPIRequest.mockResolvedValue(mockApiResponse(mockMixedWithErrorNodes));

      const { result } = renderHook(() => useBrowseTree('cargo-proxy-1'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      expect(result.current.state.nodes).toHaveLength(2);
      expect(result.current.state.nodes[0].node.id).toBe('folder1');
      expect(result.current.state.nodes[1].node.id).toBe('asset1.jar');
    });

    it('returns empty tree when all nodes are errors', async () => {
      ExtAPIUtils.extAPIRequest.mockResolvedValue(mockApiResponse(mockErrorNodes));

      const { result } = renderHook(() => useBrowseTree('cargo-proxy-1'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      expect(result.current.state.nodes).toHaveLength(0);
    });

    it('filters error nodes from children too', async () => {
      ExtAPIUtils.extAPIRequest.mockImplementation(
        async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
          const requestedNodeId = data[0].node;
          if (requestedNodeId === '/') {
            return mockApiResponse(mockRootNodes);
          }
          if (requestedNodeId === 'folder1') {
            return mockApiResponse([...mockFolder1Children, ...mockErrorNodes]);
          }
          return mockApiResponse(getMockChildren(requestedNodeId));
        }
      );

      const { result } = renderHook(() => useBrowseTree('cargo-proxy-1'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1; children should be filtered
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      const folder1 = result.current.state.nodes.find((n) => n.node.id === 'folder1');
      expect(folder1?.children).toHaveLength(2);
    });
  });

  describe('select action with paths', () => {
    it('can select a node after expanding', async () => {
      const { result } = renderHook(() => useBrowseTree('maven-releases'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Manually expand folder1
      await act(async () => {
        await result.current.actions.toggle('folder1');
      });

      // Select the subfolder
      act(() => {
        result.current.actions.select('folder1/subfolder1');
      });

      expect(result.current.state.selectedNodeId).toBe('folder1/subfolder1');
    });
  });

  describe('special characters in paths (Java/JS encoding mismatch)', () => {
    /**
     * Go module paths use `!` as a case-folding marker (e.g., `!data!dog` → `DataDog`).
     * Java's URLEncoder encodes `!` to `%21`, but JS encodeURIComponent does NOT encode `!`.
     * The decoded-comparison fallback must resolve this mismatch.
     */
    function setupGoApiMock() {
      ExtAPIUtils.extAPIRequest.mockImplementation(
        async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
          const requestedNodeId = data[0].node;
          const mockChildren = getMockChildren(requestedNodeId);
          if (mockChildren.length > 0) return mockApiResponse(mockChildren);
          // Fallback for root
          if (requestedNodeId === '/') return mockApiResponse(mockGoRootNodes);
          return mockApiResponse([]);
        }
      );
    }

    it('expandToPath resolves ! chars (decoded path vs %21-encoded tree nodes)', async () => {
      setupGoApiMock();
      const { result } = renderHook(() => useBrowseTree('go-proxy'));

      // Override root load with Go root nodes
      ExtAPIUtils.extAPIRequest.mockImplementation(
        async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
          const requestedNodeId = data[0].node;
          if (requestedNodeId === '/') return mockApiResponse(mockGoRootNodes);
          return mockApiResponse(getMockChildren(requestedNodeId));
        }
      );

      const { unmount } = renderHook(() => useBrowseTree('go-proxy'));

      // Re-render with the Go mock in place
      const { result: goResult } = renderHook(() => useBrowseTree('go-proxy'));

      await waitFor(() => {
        expect(goResult.current.state.loading).toBe(false);
      });

      // The URL path is decoded: github.com/!data!dog/gostackparse/@v/v0.7.0.mod
      // The tree nodes use Java encoding: github.com/%21data%21dog/gostackparse/%40v/v0.7.0.mod
      await act(async () => {
        await goResult.current.actions.expandToPath(
          'github.com/!data!dog/gostackparse/@v/v0.7.0.mod'
        );
      });

      // Should have selected the asset with its Java-encoded ID
      expect(goResult.current.state.selectedNodeId).toBe(
        'github.com/%21data%21dog/gostackparse/%40v/v0.7.0.mod'
      );

      // Verify the node is an asset with proper assetId
      const githubNode = goResult.current.state.nodes.find((n) => n.node.id === 'github.com');
      expect(githubNode?.expanded).toBe(true);

      unmount();
    });

    it('expandToPath resolves + chars (decoded path vs %2B-encoded tree nodes)', async () => {
      ExtAPIUtils.extAPIRequest.mockImplementation(
        async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
          const requestedNodeId = data[0].node;
          if (requestedNodeId === '/') return mockApiResponse(mockPlusRootNodes);
          return mockApiResponse(getMockChildren(requestedNodeId));
        }
      );

      const { result } = renderHook(() => useBrowseTree('test-repo'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // URL decoded path has raw + signs, tree uses %2B
      await act(async () => {
        await result.current.actions.expandToPath('lib++/file+1.txt');
      });

      expect(result.current.state.selectedNodeId).toBe('lib%2B%2B/file%2B1.txt');
    });

    it('toggle works with %21-encoded node IDs', async () => {
      ExtAPIUtils.extAPIRequest.mockImplementation(
        async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
          const requestedNodeId = data[0].node;
          if (requestedNodeId === '/') return mockApiResponse(mockGoRootNodes);
          return mockApiResponse(getMockChildren(requestedNodeId));
        }
      );

      const { result } = renderHook(() => useBrowseTree('go-proxy'));

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Expand github.com
      await act(async () => {
        await result.current.actions.toggle('github.com');
      });

      const githubNode = result.current.state.nodes.find((n) => n.node.id === 'github.com');
      expect(githubNode?.expanded).toBe(true);
      expect(githubNode?.children).toHaveLength(1);
      // Child should have Java-encoded ID with %21
      expect(githubNode?.children?.[0].node.id).toBe('github.com/%21data%21dog');
    });
  });
});


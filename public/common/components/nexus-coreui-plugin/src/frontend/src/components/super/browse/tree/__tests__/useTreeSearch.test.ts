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
import { useTreeSearch } from '../useTreeSearch';
import type { TreeNodeState } from '../browse-tree.types';

/**
 * Create mock tree node state for testing.
 */
function createNodeState(
  id: string,
  text: string,
  children: TreeNodeState[] | null = null,
  expanded = false
): TreeNodeState {
  return {
    node: {
      id,
      text,
      type: 'folder',
      leaf: children === null,
    },
    expanded,
    children,
    loading: false,
  };
}

/**
 * Create a mock tree structure for testing.
 */
function createMockTree(): TreeNodeState[] {
  return [
    createNodeState('com', 'com', [
      createNodeState('com/example', 'example', [
        createNodeState('com/example/lib', 'lib', [
          createNodeState('com/example/lib/1.0.0', '1.0.0', [
            createNodeState('com/example/lib/1.0.0/lib-1.0.0.jar', 'lib-1.0.0.jar'),
            createNodeState('com/example/lib/1.0.0/lib-1.0.0.pom', 'lib-1.0.0.pom'),
          ]),
        ]),
        createNodeState('com/example/utils', 'utils', [
          createNodeState('com/example/utils/2.0.0', '2.0.0'),
        ]),
      ]),
    ]),
    createNodeState('org', 'org', [
      createNodeState('org/apache', 'apache', [
        createNodeState('org/apache/maven', 'maven'),
      ]),
    ]),
    createNodeState('net', 'net'),
  ];
}

describe('useTreeSearch', () => {
  describe('initial state', () => {
    it('starts with empty search term', () => {
      const { result } = renderHook(() => useTreeSearch());

      expect(result.current.searchTerm).toBe('');
      expect(result.current.isSearchActive).toBe(false);
      expect(result.current.matchCount).toBe(0);
    });

    it('starts with valid validation state', () => {
      const { result } = renderHook(() => useTreeSearch());

      expect(result.current.validation.isValid).toBe(true);
      expect(result.current.validation.error).toBeUndefined();
    });
  });

  describe('setSearchTerm', () => {
    it('updates search term', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('example');
      });

      expect(result.current.searchTerm).toBe('example');
      expect(result.current.isSearchActive).toBe(true);
    });

    it('trims leading whitespace', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('   example');
      });

      expect(result.current.searchTerm).toBe('example');
    });

    it('preserves internal whitespace', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('hello world');
      });

      expect(result.current.searchTerm).toBe('hello world');
    });
  });

  describe('clearSearch', () => {
    it('clears the search term', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('example');
      });

      expect(result.current.searchTerm).toBe('example');

      act(() => {
        result.current.clearSearch();
      });

      expect(result.current.searchTerm).toBe('');
      expect(result.current.isSearchActive).toBe(false);
      expect(result.current.matchCount).toBe(0);
    });
  });

  describe('validation', () => {
    it('rejects search terms exceeding max length', () => {
      const { result } = renderHook(() => useTreeSearch());
      const longTerm = 'a'.repeat(101);

      act(() => {
        result.current.setSearchTerm(longTerm);
      });

      expect(result.current.validation.isValid).toBe(false);
      expect(result.current.validation.error).toContain('100 characters');
      expect(result.current.isSearchActive).toBe(false);
    });

    it('accepts search terms at max length', () => {
      const { result } = renderHook(() => useTreeSearch());
      const maxTerm = 'a'.repeat(100);

      act(() => {
        result.current.setSearchTerm(maxTerm);
      });

      expect(result.current.validation.isValid).toBe(true);
      expect(result.current.isSearchActive).toBe(true);
    });
  });

  describe('filterNodes', () => {
    it('returns all nodes when search is inactive', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      const filtered = result.current.filterNodes(mockTree);

      expect(filtered).toBe(mockTree);
    });

    it('filters nodes by text match', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('apache');
      });

      const filtered = result.current.filterNodes(mockTree);

      // Should include 'org' (parent) and 'apache' (match)
      expect(filtered.length).toBe(1);
      expect(filtered[0].node.text).toBe('org');
      expect(filtered[0].children?.[0].node.text).toBe('apache');
    });

    it('filters nodes by path (id) match', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('com/example');
      });

      const filtered = result.current.filterNodes(mockTree);

      expect(filtered.length).toBe(1);
      expect(filtered[0].node.text).toBe('com');
    });

    it('matches case-insensitively', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('APACHE');
      });

      const filtered = result.current.filterNodes(mockTree);

      expect(filtered.length).toBe(1);
      expect(filtered[0].children?.[0].node.text).toBe('apache');
    });

    it('auto-expands parents of matching nodes', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('maven');
      });

      const filtered = result.current.filterNodes(mockTree);

      // org -> apache -> maven path should be expanded
      expect(filtered[0].expanded).toBe(true); // org
      expect(filtered[0].children?.[0].expanded).toBe(true); // apache
    });

    it('returns empty array when no matches', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('nonexistent');
      });

      const filtered = result.current.filterNodes(mockTree);

      expect(filtered.length).toBe(0);
      expect(result.current.matchCount).toBe(0);
    });

    it('updates match count correctly', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('example');
      });

      act(() => {
        result.current.filterNodes(mockTree);
      });

      // 'example' appears in paths: com/example, com/example/lib, com/example/utils, etc.
      expect(result.current.matchCount).toBeGreaterThan(0);
    });

    it('matches partial text', () => {
      const { result } = renderHook(() => useTreeSearch());
      const mockTree = createMockTree();

      act(() => {
        result.current.setSearchTerm('lib');
      });

      let filtered: readonly TreeNodeState[] = [];
      act(() => {
        filtered = result.current.filterNodes(mockTree);
      });

      // Should match 'lib' folder and 'lib-1.0.0.jar', 'lib-1.0.0.pom'
      expect(filtered.length).toBe(1);
      expect(result.current.matchCount).toBeGreaterThan(1);
    });

    it('handles empty tree', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('test');
      });

      const filtered = result.current.filterNodes([]);

      expect(filtered.length).toBe(0);
      expect(result.current.matchCount).toBe(0);
    });
  });

  describe('isSearchActive', () => {
    it('is false for empty string', () => {
      const { result } = renderHook(() => useTreeSearch());

      expect(result.current.isSearchActive).toBe(false);
    });

    it('is true for single character', () => {
      const { result } = renderHook(() => useTreeSearch());

      act(() => {
        result.current.setSearchTerm('a');
      });

      expect(result.current.isSearchActive).toBe(true);
    });

    it('is false when validation fails', () => {
      const { result } = renderHook(() => useTreeSearch());
      const longTerm = 'a'.repeat(101);

      act(() => {
        result.current.setSearchTerm(longTerm);
      });

      expect(result.current.isSearchActive).toBe(false);
    });
  });
});

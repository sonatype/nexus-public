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

import { useState, useCallback, useMemo, useRef } from 'react';
import type { TreeNodeState } from './browse-tree.types';

/**
 * Maximum search term length to prevent performance issues.
 */
const MAX_SEARCH_LENGTH = 100;

/**
 * Minimum search term length to trigger filtering.
 */
const MIN_SEARCH_LENGTH = 1;

/**
 * Validation result for search input.
 */
export interface SearchValidation {
  isValid: boolean;
  error?: string;
}

/**
 * Result of the useTreeSearch hook.
 */
export interface UseTreeSearchResult {
  /** Current search term */
  searchTerm: string;
  /** Update the search term */
  setSearchTerm: (term: string) => void;
  /** Clear the search */
  clearSearch: () => void;
  /** Whether search is active (has valid term) */
  isSearchActive: boolean;
  /** Validation state */
  validation: SearchValidation;
  /** Filter tree nodes based on search term */
  filterNodes: (nodes: readonly TreeNodeState[]) => readonly TreeNodeState[];
  /** Count of matching nodes */
  matchCount: number;
}

/**
 * Validate a search term.
 */
function validateSearchTerm(term: string): SearchValidation {
  if (term.length > MAX_SEARCH_LENGTH) {
    return {
      isValid: false,
      error: `Search term must be ${MAX_SEARCH_LENGTH} characters or less`,
    };
  }
  return { isValid: true };
}

/**
 * Check if a node matches the search term.
 * Matches against both the node text and the full path (id).
 */
function nodeMatchesSearch(node: TreeNodeState, searchLower: string): boolean {
  const textLower = node.node.text.toLowerCase();
  const idLower = node.node.id.toLowerCase();
  return textLower.includes(searchLower) || idLower.includes(searchLower);
}

/**
 * Recursively filter tree nodes based on search term.
 * A node is included if:
 * 1. It matches the search term, OR
 * 2. Any of its descendants match the search term
 *
 * When a parent is included due to a matching descendant, it is auto-expanded.
 */
function filterTreeNodes(
  nodes: readonly TreeNodeState[],
  searchLower: string
): { filtered: readonly TreeNodeState[]; matchCount: number } {
  let totalMatchCount = 0;
  const filtered: TreeNodeState[] = [];

  for (const nodeState of nodes) {
    const selfMatches = nodeMatchesSearch(nodeState, searchLower);

    if (selfMatches) {
      totalMatchCount++;
    }

    // Recursively filter children
    let filteredChildren: readonly TreeNodeState[] | null = null;
    let childMatchCount = 0;

    if (nodeState.children && nodeState.children.length > 0) {
      const childResult = filterTreeNodes(nodeState.children, searchLower);
      filteredChildren = childResult.filtered;
      childMatchCount = childResult.matchCount;
      totalMatchCount += childMatchCount;
    }

    // Include node if it matches or has matching descendants
    if (selfMatches || childMatchCount > 0) {
      filtered.push({
        ...nodeState,
        // Auto-expand nodes with matching descendants
        expanded: childMatchCount > 0 ? true : nodeState.expanded,
        children: filteredChildren,
      });
    }
  }

  return { filtered, matchCount: totalMatchCount };
}

/**
 * Hook for managing tree search/filter functionality.
 *
 * Features:
 * - Client-side filtering of tree nodes by name/path
 * - Input validation (max length)
 * - Auto-expansion of parent nodes with matching children
 * - Match count tracking
 */
export function useTreeSearch(): UseTreeSearchResult {
  const [searchTerm, setSearchTermState] = useState('');
  const [matchCount, setMatchCount] = useState(0);
  // Use a ref to track the latest match count synchronously for immediate access
  const matchCountRef = useRef(0);

  const validation = useMemo(() => validateSearchTerm(searchTerm), [searchTerm]);

  const isSearchActive = useMemo(
    () => searchTerm.length >= MIN_SEARCH_LENGTH && validation.isValid,
    [searchTerm, validation.isValid]
  );

  const setSearchTerm = useCallback((term: string) => {
    // Trim the term but allow spaces within
    const trimmed = term.trimStart();
    setSearchTermState(trimmed);
  }, []);

  const clearSearch = useCallback(() => {
    setSearchTermState('');
    matchCountRef.current = 0;
    setMatchCount(0);
  }, []);

  const filterNodes = useCallback(
    (nodes: readonly TreeNodeState[]): readonly TreeNodeState[] => {
      if (!isSearchActive) {
        matchCountRef.current = 0;
        setMatchCount(0);
        return nodes;
      }

      const searchLower = searchTerm.toLowerCase().trim();
      if (!searchLower) {
        matchCountRef.current = 0;
        setMatchCount(0);
        return nodes;
      }

      const result = filterTreeNodes(nodes, searchLower);
      matchCountRef.current = result.matchCount;
      setMatchCount(result.matchCount);
      return result.filtered;
    },
    [isSearchActive, searchTerm]
  );

  return {
    searchTerm,
    setSearchTerm,
    clearSearch,
    isSearchActive,
    validation,
    filterNodes,
    matchCount,
  };
}

export default useTreeSearch;

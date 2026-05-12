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

import { useState, useEffect, useRef, useCallback } from 'react';
import { restClient } from '@/utils/api';
import type { RepositoryTreeNode } from './useRepositoryTree';

const SEARCH_URL = '/service/rest/v1/search';
const DEBOUNCE_MS = 400;
const MAX_REPOS_FOR_HIGHLIGHT = 100;

interface SearchItem {
  repository?: string;
}

/**
 * Flattens the group membership tree to a set of leaf repo names (hosted + proxy).
 */
export function getAllMemberRepos(tree: RepositoryTreeNode[]): Set<string> {
  const result = new Set<string>();
  const visit = (nodes: RepositoryTreeNode[]) => {
    for (const node of nodes) {
      if (node.type === 'group' && node.children) {
        visit(node.children);
      } else {
        result.add(node.name);
      }
    }
  };
  visit(tree);
  return result;
}

export interface UseStructureComponentSearchResult {
  reposWithMatches: Set<string>;
  loading: boolean;
  error: string | null;
}

/**
 * Debounced component search for Structure tab.
 * Fetches components matching query, extracts repository from results,
 * intersects with group members to return repos that contain the component.
 */
export function useStructureComponentSearch(
  repositoryName: string,
  query: string,
  memberRepos: Set<string>,
  _tree: RepositoryTreeNode[]
): UseStructureComponentSearchResult {
  const [reposWithMatches, setReposWithMatches] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);

  const searchComponents = useCallback(
    async (q: string) => {
      if (!q.trim()) {
        setReposWithMatches(new Set());
        setLoading(false);
        setError(null);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        // Scope to group: backend expands group to leaf members and searches only those
        const response = await restClient.get<{ items?: SearchItem[] }>(SEARCH_URL, {
          params: { q: q.trim(), repository: repositoryName },
        });
        const items = response?.items || [];

        const repos = new Set<string>();
        for (const item of items) {
          if (item.repository && memberRepos.has(item.repository)) {
            repos.add(item.repository);
            if (repos.size >= MAX_REPOS_FOR_HIGHLIGHT) break;
          }
        }

        if (mountedRef.current) {
          setReposWithMatches(repos);
        }
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Search failed';
        if (mountedRef.current) {
          setError(message);
          setReposWithMatches(new Set());
        }
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
      }
    },
    [memberRepos, repositoryName]
  );

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!query.trim()) {
      setReposWithMatches(new Set());
      setLoading(false);
      setError(null);
      return;
    }

    const timer = setTimeout(() => {
      searchComponents(query);
    }, DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [query, searchComponents]);

  return { reposWithMatches, loading, error };
}

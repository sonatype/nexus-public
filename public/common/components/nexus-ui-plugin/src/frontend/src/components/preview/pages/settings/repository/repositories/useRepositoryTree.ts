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

import { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { useRepositoriesApi } from './useRepositoriesApi';
import { Repository, RepositoryType } from './types';

export interface RepositoryTreeNode {
  id: string; // unique identifier (parentPath::name)
  name: string;
  type: RepositoryType;
  format: string;
  status: string;
  online: boolean;
  blobStore?: string;
  remoteUrl?: string;
  children?: RepositoryTreeNode[];
  isCircular?: boolean;
  isLoaded?: boolean;
  isLoading?: boolean;
}

export function useRepositoryTree(repositoryName: string) {
  const { fetchRepositories, fetchRepository } = useRepositoriesApi();
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [groupMembers, setGroupMembers] = useState<Map<string, string[]>>(new Map());
  const [loading, setLoading] = useState(true);
  const [loadingMembers, setLoadingMembers] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | undefined>();
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  // Helper to get all group node IDs recursively from existing data
  const getAllGroupIds = useCallback((nodes: RepositoryTreeNode[], result: string[] = []) => {
    for (const node of nodes) {
      if (node.type === 'group') {
        result.push(node.id);
        if (node.children) {
          getAllGroupIds(node.children, result);
        }
      }
    }
    return result;
  }, []);

  const [expanding, setExpanding] = useState(false);

  // Ref to store group members map to avoid closure issues in callbacks
  const groupMembersRef = useRef<Map<string, string[]>>(new Map());

  const loadInitialData = useCallback(async () => {
    setLoading(true);
    try {
      // 1. Fetch ALL repositories (basic info)
      const allRepos = await fetchRepositories();
      setRepositories(allRepos);

      const rootRepoBasic = allRepos.find(r => r.name === repositoryName);
      const isGroup = rootRepoBasic?.type === 'group';

      if (isGroup) {
        // 2. Fetch detail for the ROOT repository to get its immediate members
        const rootRepo = await fetchRepository(repositoryName);
        const groupConfig = (rootRepo as any)?.group || rootRepo?.attributes?.group;
        
        if (groupConfig?.memberNames) {
          const members = groupConfig.memberNames;
          
          // Enrich members of the root repo immediately
          const results = await Promise.allSettled(
            members.map(memberName => fetchRepository(memberName))
          );

          const memberDetails = results
            .filter((r): r is PromiseFulfilledResult<Repository | null> => r.status === 'fulfilled')
            .map(r => r.value);

          setRepositories(prev => {
            const updated = [...prev];
            if (rootRepo) {
              const rootIndex = updated.findIndex(r => r.name === repositoryName);
              if (rootIndex !== -1) {
                updated[rootIndex] = { ...updated[rootIndex], ...rootRepo };
              }
            }
            memberDetails.forEach(detail => {
              if (detail) {
                const index = updated.findIndex(r => r.name === detail.name);
                if (index !== -1) {
                  updated[index] = { ...updated[index], ...detail };
                } else {
                  updated.push(detail);
                }
              }
            });
            return updated;
          });

          const newMap = new Map(groupMembersRef.current);
          newMap.set(repositoryName, members);
          groupMembersRef.current = newMap;
          setGroupMembers(newMap);
          
          // Auto-expand root
          setExpandedIds(new Set([repositoryName]));
        }
      } else {
        // 3. For non-group repos, we need to know who uses them.
        // Fetch ALL group details in parallel (best effort)
        const groups = allRepos.filter(r => r.type === 'group');
        Promise.allSettled(groups.map(g => fetchRepository(g.name))).then(results => {
          const newMap = new Map(groupMembersRef.current);
          results.forEach(result => {
            if (result.status === 'fulfilled' && result.value) {
              const d = result.value;
              const gConfig = (d as any).group || d.attributes?.group;
              if (gConfig?.memberNames) {
                newMap.set(d.name, gConfig.memberNames);
              }
            }
          });
          groupMembersRef.current = newMap;
          setGroupMembers(newMap);
        });
      }
      
      setError(undefined);
    } catch (err: any) {
      setError(err.message || 'Failed to load repository data');
    } finally {
      setLoading(false);
    }
  }, [fetchRepositories, fetchRepository, repositoryName]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  const repoMap = useMemo(() => {
    const map = new Map<string, Repository>();
    repositories.forEach(repo => map.set(repo.name, repo));
    return map;
  }, [repositories]);

  /**
   * Fetch members for a specific group repository and their metadata.
   */
  const loadMembers = useCallback(async (name: string) => {
    if (groupMembersRef.current.has(name) || loadingMembers.has(name)) return;

    setLoadingMembers(prev => new Set(prev).add(name));
    try {
      // 1. Fetch the group detail to get its member names
      const groupRepo = await fetchRepository(name);
      const groupConfig = (groupRepo as any)?.group || groupRepo?.attributes?.group;
      
      if (groupConfig?.memberNames) {
        const members = groupConfig.memberNames;
        
        // Fetch details for each member to get their attributes (blob store, remote URL)
        const results = await Promise.allSettled(
          members.map(memberName => fetchRepository(memberName))
        );

        const memberDetails = results
          .filter((r): r is PromiseFulfilledResult<Repository | null> => r.status === 'fulfilled')
          .map(r => r.value);

        // 3. Update the repositories state with enriched details
        setRepositories(prev => {
          const updated = [...prev];
          if (groupRepo) {
            const groupIndex = updated.findIndex(r => r.name === name);
            if (groupIndex !== -1) {
              updated[groupIndex] = { ...updated[groupIndex], ...groupRepo };
            }
          }
          memberDetails.forEach(detail => {
            if (detail) {
              const index = updated.findIndex(r => r.name === detail.name);
              if (index !== -1) {
                updated[index] = { ...updated[index], ...detail };
              } else {
                updated.push(detail);
              }
            }
          });
          return updated;
        });

        const newMap = new Map(groupMembersRef.current);
        newMap.set(name, members);
        groupMembersRef.current = newMap;
        setGroupMembers(newMap);
      }
    } catch (err) {
      console.warn(`Failed to load members for group ${name}:`, err);
    } finally {
      setLoadingMembers(prev => {
        const next = new Set(prev);
        next.delete(name);
        return next;
      });
    }
  }, [fetchRepository, loadingMembers]);

  const expandAll = useCallback(async () => {
    setExpanding(true);
    try {
      const knownGroups = new Set<string>();

      // BFS level-order loading: load all groups at each level in parallel (avoids 5s+ hangs on large trees)
      let currentLevel = new Set<string>([repositoryName]);
      const allRepos = new Map(repoMap);
      const isGroup = (name: string) =>
        groupMembersRef.current.has(name) ||
        allRepos.get(name)?.type === 'group' ||
        repositories.find(r => r.name === name)?.type === 'group';

      while (currentLevel.size > 0) {
        const toLoad = [...currentLevel].filter((name) => !groupMembersRef.current.has(name));
        if (toLoad.length > 0) {
          await Promise.all(toLoad.map((name) => loadMembers(name)));
        }

        const nextLevel = new Set<string>();
        for (const name of currentLevel) {
          const members = groupMembersRef.current.get(name);
          if (members) {
            knownGroups.add(name);
            for (const m of members) {
              if (isGroup(m)) {
                nextLevel.add(m);
              }
            }
          }
        }
        currentLevel = nextLevel;
      }

      // Collect all group node IDs using the path-based ID scheme
      const allIds = new Set<string>();
      const collectIds = (name: string, path: string[] = []) => {
        const id = [...path, name].join('::');
        if (knownGroups.has(name)) {
          allIds.add(id);
          const members = groupMembersRef.current.get(name) || [];
          members.forEach((m) => collectIds(m, [...path, name]));
        }
      };
      collectIds(repositoryName);
      setExpandedIds(allIds);
    } catch (err) {
      console.error('Failed to expand all:', err);
    } finally {
      setExpanding(false);
    }
  }, [repositoryName, loadMembers, repoMap, repositories]);

  const collapseAll = useCallback(() => {
    setExpandedIds(new Set());
  }, []);

  const buildTree = useCallback((
    name: string,
    path: string[] = [],
    visited: Set<string> = new Set()
  ): RepositoryTreeNode => {
    const repo = repoMap.get(name);
    const id = [...path, name].join('::');

    if (!repo) {
      return {
        id,
        name,
        type: 'hosted',
        format: 'unknown',
        status: 'unknown',
        online: false,
      };
    }

    // REST API puts these at top level, ExtDirect puts them in attributes.
    const storage = (repo as any).storage || repo.attributes?.storage;
    const proxy = (repo as any).proxy || repo.attributes?.proxy;
    const group = (repo as any).group || repo.attributes?.group;
    const httpClient = (repo as any).httpClient || repo.attributes?.httpClient;

    const type = repo.type?.toLowerCase() as RepositoryType;

    const node: RepositoryTreeNode = {
      id,
      name: repo.name,
      type,
      format: repo.format,
      status: repo.status?.online ? 'online' : 'offline',
      online: repo.online,
      blobStore: storage?.blobStoreName,
      remoteUrl: proxy?.remoteUrl,
    };

    if (type === 'proxy' && httpClient?.blocked) {
      node.status = 'blocked';
    }
    if (!repo.online && repo.status?.reason?.toLowerCase().includes('service')) {
      node.status = 'out-of-service';
    }

    if (type === 'group') {
      // Check for members in map OR in the repo attributes/top-level
      const members = groupMembers.get(name) || group?.memberNames;
      node.isLoaded = !!groupMembers.get(name);
      node.isLoading = loadingMembers.has(name);

      if (visited.has(name)) {
        node.isCircular = true;
        return node;
      }

      if (members) {
        const nextVisited = new Set(visited);
        nextVisited.add(name);
        node.children = members.map(memberName => 
          buildTree(memberName, [...path, name], nextVisited)
        );
      }
    }

    return node;
  }, [repoMap, groupMembers, loadingMembers]);

  const tree = useMemo(() => {
    if (repositories.length === 0) return [];
    return [buildTree(repositoryName)];
  }, [repositories, repositoryName, buildTree]);

  const revealIssues = useCallback(() => {
    const idsToExpand = new Set<string>(expandedIds);

    const findIssuePaths = (nodes: RepositoryTreeNode[]) => {
      let branchHasIssue = false;
      for (const node of nodes) {
        let nodeOrDescendantHasIssue = false;

        if (node.status !== 'online') {
          nodeOrDescendantHasIssue = true;
        }

        if (node.children && node.children.length > 0) {
          const childHasIssue = findIssuePaths(node.children);
          if (childHasIssue) {
            nodeOrDescendantHasIssue = true;
            idsToExpand.add(node.id);
          }
        }

        if (nodeOrDescendantHasIssue) {
          branchHasIssue = true;
        }
      }
      return branchHasIssue;
    };

    findIssuePaths(tree);
    setExpandedIds(idsToExpand);
  }, [tree, expandedIds]);

  const toggleExpand = useCallback((nodeId: string) => {
    const parts = nodeId.split('::');
    const name = parts[parts.length - 1];
    
    setExpandedIds(prev => {
      const next = new Set(prev);
      if (next.has(nodeId)) {
        next.delete(nodeId);
      } else {
        next.add(nodeId);
        // Load members if not already loaded
        const repo = repoMap.get(name);
        if (repo?.type === 'group' && !groupMembersRef.current.has(name)) {
          loadMembers(name);
        }
      }
      return next;
    });
  }, [repoMap, loadMembers]);

  const usages = useMemo(() => {
    return repositories
      .filter(repo => repo.type === 'group')
      .filter(repo => {
        const members = groupMembers.get(repo.name);
        return members?.includes(repositoryName);
      })
      .map(repo => repo.name);
  }, [repositories, groupMembers, repositoryName]);

  return {
    tree,
    loading,
    expanding,
    error,
    expandedIds,
    toggleExpand,
    usages,
    expandAll,
    collapseAll,
    revealIssues,
    setExpandedIds,
    refresh: loadInitialData,
  };
}

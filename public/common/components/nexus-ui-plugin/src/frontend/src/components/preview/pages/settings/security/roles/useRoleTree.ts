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

import { useState, useCallback, useMemo, useEffect } from 'react';
import { useRolesApi } from './useRolesApi';
import { usePrivilegesApi } from '../../security/privileges/usePrivilegesApi';
import { useContentSelectorsApi } from '../../repository/selectors/useContentSelectorsApi';
import { Role } from './types';
import { Privilege } from '../../security/privileges/types';
import { ContentSelector } from '../../repository/selectors/types';

export type NodeType = 'role' | 'privilege' | 'content-selector';

export interface SecurityTreeNode {
  id: string; // unique ID within the tree (could be path-like or roleId:privilegeId)
  entityId: string; // The actual ID of the Role/Privilege/Selector
  name: string;
  type: NodeType;
  inherited: boolean;
  parentRoleName?: string;
  description?: string;
  children?: SecurityTreeNode[];
  expanded?: boolean;
  csel?: string;
  actions?: string;
  repository?: string;
  format?: string;
  source?: string;
  isCircular?: boolean;
  isVisible?: boolean; // For filtering
}

export interface UseRoleTreeResult {
  tree: SecurityTreeNode[];
  effectivePrivileges: Privilege[];
  loading: boolean;
  error: string | null;
  toggleExpand: (nodeId: string) => void;
  expandAll: () => void;
  collapseAll: () => void;
  setExpandedNodes: (nodes: Set<string>) => void;
  expandedNodes: Set<string>;
}

interface UseRoleTreeOptions {
  searchTerm?: string;
  initialExpandedNodes?: Set<string>;
  onExpandedNodesChange?: (nodes: Set<string>) => void;
}

export function useRoleTree(rootRoleId: string, options: UseRoleTreeOptions = {}): UseRoleTreeResult {
  const { searchTerm = '', initialExpandedNodes, onExpandedNodesChange } = options;
  const { fetchRoles } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();
  const { fetchContentSelectors } = useContentSelectorsApi();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [allPrivileges, setAllPrivileges] = useState<Privilege[]>([]);
  const [allSelectors, setAllSelectors] = useState<ContentSelector[]>([]);
  
  const [internalExpandedNodes, setInternalExpandedNodes] = useState<Set<string>>(new Set());
  
  const expandedNodes = initialExpandedNodes ?? internalExpandedNodes;
  const setExpandedNodes = onExpandedNodesChange ?? setInternalExpandedNodes;

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [roles, privilegesData, selectors] = await Promise.all([
        fetchRoles(),
        fetchPrivileges(),
        fetchContentSelectors(),
      ]);
      setAllRoles(roles);
      setAllPrivileges(privilegesData.data);
      setAllSelectors(selectors);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load security data');
    } finally {
      setLoading(false);
    }
  }, [fetchRoles, fetchPrivileges, fetchContentSelectors]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Optimization: use Maps for O(1) lookups during tree construction
  const roleMap = useMemo(() => new Map(allRoles.map((r) => [r.id, r])), [allRoles]);
  const privilegeMap = useMemo(() => new Map(allPrivileges.map((p) => [p.id, p])), [allPrivileges]);
  const selectorMap = useMemo(() => new Map(allSelectors.map((s) => [s.name, s])), [allSelectors]);

  const toggleExpand = useCallback((nodeId: string) => {
    const next = new Set(expandedNodes);
    if (next.has(nodeId)) {
      next.delete(nodeId);
    } else {
      next.add(nodeId);
    }
    setExpandedNodes(next);
  }, [expandedNodes, setExpandedNodes]);

  const buildTree = useCallback(
    (
      roleId: string,
      inherited = false,
      parentRoleId?: string,
      visited = new Set<string>()
    ): SecurityTreeNode | null => {
      const role = roleMap.get(roleId);
      if (!role) return null;

      const roleNodeId = parentRoleId ? `${parentRoleId} > ${role.id}` : role.id;
      const node: SecurityTreeNode = {
        id: roleNodeId,
        entityId: role.id,
        name: role.name,
        type: 'role',
        inherited,
        parentRoleName: inherited ? roleMap.get(parentRoleId!)?.name : undefined,
        description: role.description,
        source: role.source,
        children: [],
      };

      if (visited.has(roleId)) {
        node.isCircular = true;
        return node;
      }

      const newVisited = new Set(visited);
      newVisited.add(roleId);

      // Add nested roles as children
      if (role.roles) {
        role.roles.forEach((nestedRoleId) => {
          const nestedNode = buildTree(nestedRoleId, true, role.id, newVisited);
          if (nestedNode) node.children?.push(nestedNode);
        });
      }

      // Add privileges as children
      if (role.privileges) {
        role.privileges.forEach((privilegeId) => {
          const privilege = privilegeMap.get(privilegeId);
          if (privilege) {
            const privNodeId = `${roleNodeId} : ${privilege.id}`;
            const privNode: SecurityTreeNode = {
              id: privNodeId,
              entityId: privilege.id,
              name: privilege.name,
              type: 'privilege',
              inherited,
              parentRoleName: inherited ? role.name : undefined,
              description: privilege.description,
              actions: privilege.properties.actions,
              repository: privilege.properties.repository,
              format: privilege.properties.format,
              children: [],
            };

            // Add content selector if applicable
            if (privilege.type === 'repository-content-selector' && privilege.properties.contentSelector) {
              const selector = selectorMap.get(privilege.properties.contentSelector);
              if (selector) {
                privNode.children?.push({
                  id: `${privNodeId} -> ${selector.name}`,
                  entityId: selector.name,
                  name: selector.name,
                  type: 'content-selector',
                  inherited,
                  parentRoleName: inherited ? role.name : undefined,
                  description: selector.description,
                  csel: selector.expression,
                });
              }
            }
            node.children?.push(privNode);
          }
        });
      }

      return node;
    },
    [roleMap, privilegeMap, selectorMap]
  );

  const rawTree = useMemo(() => {
    if (loading || !rootRoleId || allRoles.length === 0) return [];
    const rootNode = buildTree(rootRoleId);
    return rootNode ? [rootNode] : [];
  }, [loading, rootRoleId, allRoles.length, buildTree]);

  // Apply search filtering
  const treeWithFiltering = useMemo(() => {
    if (!searchTerm) {
      const applyExpansion = (nodes: SecurityTreeNode[]): SecurityTreeNode[] => {
        return nodes.map(node => ({
          ...node,
          expanded: expandedNodes.has(node.id),
          isVisible: true,
          children: node.children ? applyExpansion(node.children) : undefined,
        }));
      };
      return applyExpansion(rawTree);
    }

    const term = searchTerm.toLowerCase();

    const filterNodes = (nodes: SecurityTreeNode[]): { filtered: SecurityTreeNode[], anyVisible: boolean } => {
      let anyVisibleInBranch = false;
      const filtered: SecurityTreeNode[] = [];

      nodes.forEach(node => {
        const { filtered: filteredChildren, anyVisible: childrenVisible } = node.children 
          ? filterNodes(node.children) 
          : { filtered: [], anyVisible: false };

        const matches = 
          node.name.toLowerCase().includes(term) ||
          node.description?.toLowerCase().includes(term) ||
          node.actions?.toLowerCase().includes(term) ||
          node.repository?.toLowerCase().includes(term) ||
          node.csel?.toLowerCase().includes(term);

        if (matches || childrenVisible) {
          anyVisibleInBranch = true;
          filtered.push({
            ...node,
            expanded: matches ? expandedNodes.has(node.id) : true, // Auto-expand parents of matches
            isVisible: true,
            children: filteredChildren,
          });
        }
      });

      return { filtered, anyVisible: anyVisibleInBranch };
    };

    return filterNodes(rawTree).filtered;
  }, [rawTree, searchTerm, expandedNodes]);

  // Recursively calculate effective privileges
  const calculateEffectivePrivileges = useCallback((roleId: string, visited = new Set<string>()): Privilege[] => {
    if (visited.has(roleId)) return [];
    const role = roleMap.get(roleId);
    if (!role) return [];

    const newVisited = new Set(visited);
    newVisited.add(roleId);

    let privileges: Privilege[] = (role.privileges || [])
      .map(pId => privilegeMap.get(pId))
      .filter((p): p is Privilege => !!p);

    if (role.roles) {
      role.roles.forEach(nestedRoleId => {
        privileges = [...privileges, ...calculateEffectivePrivileges(nestedRoleId, newVisited)];
      });
    }

    return privileges;
  }, [roleMap, privilegeMap]);

  const effectivePrivileges = useMemo(() => {
    if (loading || !rootRoleId || allRoles.length === 0) return [];
    const privs = calculateEffectivePrivileges(rootRoleId);
    // Unique by ID
    const uniquePrivs = Array.from(new Map(privs.map(p => [p.id, p])).values());
    return uniquePrivs;
  }, [loading, rootRoleId, allRoles.length, calculateEffectivePrivileges]);

  const expandAll = useCallback(() => {
    const allNodeIds = new Set<string>();
    const traverse = (nodes: SecurityTreeNode[]) => {
      nodes.forEach(node => {
        allNodeIds.add(node.id);
        if (node.children) traverse(node.children);
      });
    };
    traverse(rawTree);
    setExpandedNodes(allNodeIds);
  }, [rawTree, setExpandedNodes]);

  const collapseAll = useCallback(() => {
    setExpandedNodes(new Set());
  }, [setExpandedNodes]);

  return {
    tree: treeWithFiltering,
    effectivePrivileges,
    loading,
    error,
    toggleExpand,
    expandAll,
    collapseAll,
    setExpandedNodes,
    expandedNodes,
  };
}

export interface UseCombinedRoleTreeOptions {
  searchTerm?: string;
  initialExpandedNodes?: Set<string>;
  onExpandedNodesChange?: (nodes: Set<string>) => void;
}

/**
 * Builds a merged tree from multiple roles. Deduplicates privilege nodes by entityId
 * (when same privilege appears via multiple roles, show only once under first role).
 */
function mergeRoleTreesWithDedupe(
  roots: SecurityTreeNode[],
  seenPrivilegeIds: Set<string>
): SecurityTreeNode[] {
  const result: SecurityTreeNode[] = [];

  for (const root of roots) {
    const filtered = filterDuplicatePrivileges(root, seenPrivilegeIds);
    if (filtered) result.push(filtered);
  }

  return result;
}

function filterDuplicatePrivileges(
  node: SecurityTreeNode,
  seenPrivilegeIds: Set<string>
): SecurityTreeNode | null {
  if (node.type === 'privilege') {
    if (seenPrivilegeIds.has(node.entityId)) return null;
    seenPrivilegeIds.add(node.entityId);
  }

  const filteredChildren = (node.children || [])
    .map((child) => filterDuplicatePrivileges(child, seenPrivilegeIds))
    .filter((n): n is SecurityTreeNode => n !== null);

  return { ...node, children: filteredChildren };
}

/**
 * useCombinedRoleTree - Merges effective permissions from multiple roles into one tree.
 * Used by User Wizard Step 2 to show a unified view of all granted roles' permissions.
 */
export function useCombinedRoleTree(
  roleIds: string[],
  options: UseCombinedRoleTreeOptions = {}
): UseRoleTreeResult {
  const { searchTerm = '', initialExpandedNodes, onExpandedNodesChange } = options;
  const { fetchRoles } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();
  const { fetchContentSelectors } = useContentSelectorsApi();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [allPrivileges, setAllPrivileges] = useState<Privilege[]>([]);
  const [allSelectors, setAllSelectors] = useState<ContentSelector[]>([]);

  const [internalExpandedNodes, setInternalExpandedNodes] = useState<Set<string>>(new Set());
  const expandedNodes = initialExpandedNodes ?? internalExpandedNodes;
  const setExpandedNodes = onExpandedNodesChange ?? setInternalExpandedNodes;

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [roles, privilegesData, selectors] = await Promise.all([
        fetchRoles(),
        fetchPrivileges(),
        fetchContentSelectors(),
      ]);
      setAllRoles(roles);
      setAllPrivileges(privilegesData.data);
      setAllSelectors(selectors);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load security data');
    } finally {
      setLoading(false);
    }
  }, [fetchRoles, fetchPrivileges, fetchContentSelectors]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const roleMap = useMemo(() => new Map(allRoles.map((r) => [r.id, r])), [allRoles]);
  const privilegeMap = useMemo(() => new Map(allPrivileges.map((p) => [p.id, p])), [allPrivileges]);
  const selectorMap = useMemo(() => new Map(allSelectors.map((s) => [s.name, s])), [allSelectors]);

  const toggleExpand = useCallback(
    (nodeId: string) => {
      const next = new Set(expandedNodes);
      if (next.has(nodeId)) next.delete(nodeId);
      else next.add(nodeId);
      setExpandedNodes(next);
    },
    [expandedNodes, setExpandedNodes]
  );

  const buildTree = useCallback(
    (
      roleId: string,
      inherited = false,
      parentRoleId?: string,
      visited = new Set<string>()
    ): SecurityTreeNode | null => {
      const role = roleMap.get(roleId);
      if (!role) return null;

      const roleNodeId = parentRoleId ? `${parentRoleId} > ${role.id}` : role.id;
      const node: SecurityTreeNode = {
        id: roleNodeId,
        entityId: role.id,
        name: role.name,
        type: 'role',
        inherited,
        parentRoleName: inherited ? roleMap.get(parentRoleId!)?.name : undefined,
        description: role.description,
        source: role.source,
        children: [],
      };

      if (visited.has(roleId)) {
        node.isCircular = true;
        return node;
      }

      const newVisited = new Set(visited);
      newVisited.add(roleId);

      if (role.roles) {
        role.roles.forEach((nestedRoleId) => {
          const nestedNode = buildTree(nestedRoleId, true, role.id, newVisited);
          if (nestedNode) node.children?.push(nestedNode);
        });
      }

      if (role.privileges) {
        role.privileges.forEach((privilegeId) => {
          const privilege = privilegeMap.get(privilegeId);
          if (privilege) {
            const privNodeId = `${roleNodeId} : ${privilege.id}`;
            const privNode: SecurityTreeNode = {
              id: privNodeId,
              entityId: privilege.id,
              name: privilege.name,
              type: 'privilege',
              inherited,
              parentRoleName: inherited ? role.name : undefined,
              description: privilege.description,
              actions: privilege.properties.actions,
              repository: privilege.properties.repository,
              format: privilege.properties.format,
              children: [],
            };

            if (
              privilege.type === 'repository-content-selector' &&
              privilege.properties.contentSelector
            ) {
              const selector = selectorMap.get(privilege.properties.contentSelector);
              if (selector) {
                privNode.children?.push({
                  id: `${privNodeId} -> ${selector.name}`,
                  entityId: selector.name,
                  name: selector.name,
                  type: 'content-selector',
                  inherited,
                  parentRoleName: inherited ? role.name : undefined,
                  description: selector.description,
                  csel: selector.expression,
                });
              }
            }
            node.children?.push(privNode);
          }
        });
      }

      return node;
    },
    [roleMap, privilegeMap, selectorMap]
  );

  const rawTree = useMemo(() => {
    if (loading || roleIds.length === 0 || allRoles.length === 0) return [];
    const roots: SecurityTreeNode[] = [];
    for (const roleId of roleIds) {
      const rootNode = buildTree(roleId);
      if (rootNode) roots.push(rootNode);
    }
    return mergeRoleTreesWithDedupe(roots, new Set<string>());
  }, [loading, roleIds.join(','), allRoles.length, buildTree]);

  const filterNodes = useCallback(
    (nodes: SecurityTreeNode[]): { filtered: SecurityTreeNode[]; anyVisible: boolean } => {
      const term = searchTerm.toLowerCase();
      let anyVisibleInBranch = false;
      const filtered: SecurityTreeNode[] = [];

      nodes.forEach((node) => {
        const { filtered: filteredChildren, anyVisible: childrenVisible } = node.children
          ? filterNodes(node.children)
          : { filtered: [], anyVisible: false };

        const matches =
          node.name.toLowerCase().includes(term) ||
          node.description?.toLowerCase().includes(term) ||
          node.actions?.toLowerCase().includes(term) ||
          node.repository?.toLowerCase().includes(term) ||
          node.csel?.toLowerCase().includes(term);

        if (matches || childrenVisible) {
          anyVisibleInBranch = true;
          filtered.push({
            ...node,
            expanded: matches ? expandedNodes.has(node.id) : true,
            isVisible: true,
            children: filteredChildren,
          });
        }
      });

      return { filtered, anyVisible: anyVisibleInBranch };
    },
    [searchTerm, expandedNodes]
  );

  const treeWithFiltering = useMemo(() => {
    if (!searchTerm) {
      const applyExpansion = (nodes: SecurityTreeNode[]): SecurityTreeNode[] => {
        return nodes.map((node) => ({
          ...node,
          expanded: expandedNodes.has(node.id),
          isVisible: true,
          children: node.children ? applyExpansion(node.children) : undefined,
        }));
      };
      return applyExpansion(rawTree);
    }
    return filterNodes(rawTree).filtered;
  }, [rawTree, searchTerm, expandedNodes, filterNodes]);

  const effectivePrivileges = useMemo(() => {
    if (loading || roleIds.length === 0 || allRoles.length === 0) return [];
    const allPrivs: Privilege[] = [];
    const seen = new Set<string>();
    for (const roleId of roleIds) {
      const role = roleMap.get(roleId);
      if (!role) continue;
      const collect = (rId: string, v = new Set<string>()) => {
        if (v.has(rId)) return;
        const r = roleMap.get(rId);
        if (!r) return;
        v.add(rId);
        (r.privileges || []).forEach((pId) => {
          const p = privilegeMap.get(pId);
          if (p && !seen.has(p.id)) {
            seen.add(p.id);
            allPrivs.push(p);
          }
        });
        (r.roles || []).forEach((nId) => collect(nId, v));
      };
      collect(roleId);
    }
    return allPrivs;
  }, [loading, roleIds.join(','), allRoles.length, roleMap, privilegeMap]);

  const expandAll = useCallback(() => {
    const allNodeIds = new Set<string>();
    const traverse = (nodes: SecurityTreeNode[]) => {
      nodes.forEach((node) => {
        allNodeIds.add(node.id);
        if (node.children) traverse(node.children);
      });
    };
    traverse(rawTree);
    setExpandedNodes(allNodeIds);
  }, [rawTree, setExpandedNodes]);

  const collapseAll = useCallback(() => {
    setExpandedNodes(new Set());
  }, [setExpandedNodes]);

  return {
    tree: treeWithFiltering,
    effectivePrivileges,
    loading,
    error,
    toggleExpand,
    expandAll,
    collapseAll,
    setExpandedNodes,
    expandedNodes,
  };
}

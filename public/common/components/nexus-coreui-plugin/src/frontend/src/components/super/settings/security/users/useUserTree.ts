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

import { useState, useMemo } from 'react';
import { useCombinedRoleTree } from '../roles/useRoleTree';
import type { SecurityTreeNode } from '../roles/useRoleTree';
import { User } from './types';
import { DEFAULT_SOURCE, getSourceLabel } from './types';

export interface UseUserTreeResult {
  tree: SecurityTreeNode[];
  loading: boolean;
  error: string | null;
  toggleExpand: (nodeId: string) => void;
  expandAll: () => void;
  collapseAll: () => void;
  setSearchTerm: (term: string) => void;
}

/**
 * Hook: Build a user-rooted security tree (User → Roles → Privileges → Content selectors).
 * Wraps useCombinedRoleTree with a synthetic user root node for the User Profile Security Tree tab.
 */
export function useUserTree(user: User | null): UseUserTreeResult {
  const [searchTerm, setSearchTerm] = useState('');

  const roleIds = user?.roles || [];
  const {
    tree: roleTrees,
    loading,
    error,
    toggleExpand,
    expandAll,
    collapseAll,
  } = useCombinedRoleTree(roleIds, { searchTerm });

  const userNodeId = user ? `user:${user.userId}:${user.source || DEFAULT_SOURCE}` : '';

  const tree = useMemo(() => {
    if (!user || roleTrees.length === 0) return [];

    const sourceLabel = getSourceLabel(user.source || DEFAULT_SOURCE);
    const userRoot: SecurityTreeNode = {
      id: userNodeId,
      entityId: user.userId,
      name: `User: ${user.userId} (${sourceLabel})`,
      type: 'role',
      inherited: false,
      expanded: true,
      isVisible: true,
      children: roleTrees,
    };
    return [userRoot];
  }, [user, userNodeId, roleTrees]);

  return {
    tree,
    loading,
    error,
    toggleExpand,
    expandAll,
    collapseAll,
    setSearchTerm,
  };
}

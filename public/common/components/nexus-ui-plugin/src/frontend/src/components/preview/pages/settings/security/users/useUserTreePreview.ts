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

import { useMemo, useState } from 'react';
import { useCombinedRoleTree, type SecurityTreeNode } from '../roles/useRoleTree';
import { DEFAULT_SOURCE, User, getSourceLabel } from './types';

export interface UseUserTreeResult {
  tree: SecurityTreeNode[];
  loading: boolean;
  error: string | null;
  toggleExpand: (nodeId: string) => void;
  expandAll: () => void;
  collapseAll: () => void;
  setSearchTerm: (term: string) => void;
}

/** Builds a user-rooted security tree from an explicit set of role IDs. Consumed
 *  by useUserTree (persisted roles) and by the Edit User inline preview (pending
 *  roles) so both paths render identical node ids and labels. */
export function useUserTreePreview(
  pendingRoleIds: string[],
  user: User | null = null,
): UseUserTreeResult {
  const [searchTerm, setSearchTerm] = useState('');

  const {
    tree: roleTrees,
    loading,
    error,
    toggleExpand,
    expandAll,
    collapseAll,
  } = useCombinedRoleTree(pendingRoleIds, { searchTerm });

  const tree = useMemo<SecurityTreeNode[]>(() => {
    if (roleTrees.length === 0) return [];
    if (!user) return roleTrees;
    const source = user.source || DEFAULT_SOURCE;
    const userRoot: SecurityTreeNode = {
      id: `user:${user.userId}:${source}`,
      entityId: user.userId,
      name: `User: ${user.userId} (${getSourceLabel(source)})`,
      type: 'role',
      inherited: false,
      expanded: true,
      isVisible: true,
      children: roleTrees,
    };
    return [userRoot];
  }, [user, roleTrees]);

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

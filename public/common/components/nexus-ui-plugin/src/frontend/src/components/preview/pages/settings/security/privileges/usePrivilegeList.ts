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

import { useMemo, useCallback } from 'react';
import { useList } from '../../../../../../interface/list';
import { createPrivilegeListMachine, type Privilege, type PrivilegeFilters } from './privilegeListMachine';

export interface UsePrivilegeListOptions {
  onRowClick: (privilegeId: string) => void;
  onCreate: () => void;
}

/**
 * Hook to manage privileges list state with XState
 *
 * Provides loading, filtering, sorting, and error handling for privileges list
 * Also provides derived UI state like type options and filter sections
 * Also provides navigation callbacks for routing
 */
export function usePrivilegeList({ onRowClick, onCreate }: UsePrivilegeListOptions) {
  // Create machine once (memoized)
  const machine = useMemo(() => createPrivilegeListMachine(), []);

  // Use the list hook
  const listState = useList<Privilege, PrivilegeFilters>(machine);

  // Get unique types with counts (raw data - no UI labels)
  const typeCounts = useMemo(() => {
    const counts = new Map<string, number>();

    listState.pristineData.forEach((priv) => {
      const type = priv.type || 'unknown';
      counts.set(type, (counts.get(type) || 0) + 1);
    });

    // Return sorted array of [type, count] tuples
    return Array.from(counts.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [listState.pristineData]);

  // Locked (read-only) vs Unlocked (editable) counts for filter
  const readOnlyCounts = useMemo(() => {
    let locked = 0;
    let unlocked = 0;
    listState.pristineData.forEach((priv) => {
      if (priv.readOnly === true) locked++;
      else unlocked++;
    });
    return { locked, unlocked };
  }, [listState.pristineData]);

  // Handle filter changes from FilterSidebar
  const handleFilterChange = useCallback(
    (sectionId: string, value: string | string[]) => {
      if (sectionId === 'type') {
        listState.setFilters({ typeFilter: value as string[] });
      } else if (sectionId === 'readOnly') {
        listState.setFilters({ readOnlyFilter: value as string[] });
      }
    },
    [listState.setFilters]
  );

  // Navigation callbacks
  const handleRowClick = useCallback(
    (privilege: Privilege) => {
      // Use name as the identifier since the API uses name for lookups
      onRowClick(privilege.name);
    },
    [onRowClick]
  );

  const handleCreate = useCallback(() => {
    onCreate();
  }, [onCreate]);

  return {
    ...listState,
    typeCounts,
    readOnlyCounts,
    handleFilterChange,
    handleRowClick,
    handleCreate,
  };
}

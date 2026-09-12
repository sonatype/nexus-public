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

import { useCallback, useMemo, useState } from 'react';

import type { SortDirection } from '../../../shared';
import type { RepoRow } from './gaRepositoriesMachine';

type SortKey = 'repositoryName' | 'type' | 'versionCount';

interface Options {
  rows: readonly RepoRow[];
}

export interface UseGARepositoriesTabResult {
  // Filter / sort / paging state
  readonly sortKey: SortKey;
  readonly sortDirection: SortDirection;
  readonly searchQuery: string;
  readonly filterTypes: readonly string[];
  readonly showMobileFilters: boolean;
  readonly currentPage: number;
  readonly itemsPerPage: number;

  // Derived data
  readonly filteredRows: readonly RepoRow[];
  readonly sortedRows: readonly RepoRow[];
  readonly paginatedRows: readonly RepoRow[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly hasActiveFilters: boolean;

  // Commands
  changeSort(key: string, direction: SortDirection): void;
  changeSortKey(key: string): void;
  changeSortDirection(direction: SortDirection): void;
  changeSearchQuery(value: string): void;
  toggleTypeFilter(value: string, checked: boolean): void;
  changePage(page: number): void;
  changeItemsPerPage(limit: number): void;
  openMobileFilters(): void;
  closeMobileFilters(): void;
  clearAllFilters(): void;
}

export function useGARepositoriesTab({ rows }: Options): UseGARepositoriesTabResult {
  const [sortKey, setSortKey] = useState<SortKey>('repositoryName');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTypes, setFilterTypes] = useState<string[]>([]);
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(20);

  const filteredRows = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return rows.filter((r) => {
      if (q) {
        const hay = `${r.repositoryName} ${r.type}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      if (filterTypes.length > 0 && !filterTypes.includes(r.type)) return false;
      return true;
    });
  }, [rows, searchQuery, filterTypes]);

  const sortedRows = useMemo(() => {
    const arr = [...filteredRows];
    arr.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'repositoryName':
          cmp = a.repositoryName.localeCompare(b.repositoryName);
          break;
        case 'type':
          cmp = a.type.localeCompare(b.type);
          break;
        case 'versionCount':
          cmp = a.versionCount - b.versionCount;
          break;
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return arr;
  }, [filteredRows, sortKey, sortDirection]);

  const totalItems = sortedRows.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const offset = (currentPage - 1) * itemsPerPage;
  const paginatedRows = useMemo(
    () => sortedRows.slice(offset, offset + itemsPerPage),
    [sortedRows, offset, itemsPerPage],
  );

  const hasActiveFilters = searchQuery.length > 0 || filterTypes.length > 0;

  const changeSort = useCallback((key: string, direction: SortDirection) => {
    setSortKey(key as SortKey);
    setSortDirection(direction);
    setCurrentPage(1);
  }, []);

  const changeSortKey = useCallback((key: string) => {
    setSortKey(key as SortKey);
    setCurrentPage(1);
  }, []);

  const changeSortDirection = useCallback((direction: SortDirection) => {
    setSortDirection(direction);
    setCurrentPage(1);
  }, []);

  const changeSearchQuery = useCallback((value: string) => {
    setSearchQuery(value);
    setCurrentPage(1);
  }, []);

  const toggleTypeFilter = useCallback((value: string, checked: boolean) => {
    setFilterTypes((prev) =>
      checked ? [...prev, value] : prev.filter((x) => x !== value),
    );
    setCurrentPage(1);
  }, []);

  const changePage = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  const changeItemsPerPage = useCallback((limit: number) => {
    setItemsPerPage(limit);
    setCurrentPage(1);
  }, []);

  const openMobileFilters = useCallback(() => setShowMobileFilters(true), []);
  const closeMobileFilters = useCallback(() => setShowMobileFilters(false), []);

  const clearAllFilters = useCallback(() => {
    setSearchQuery('');
    setFilterTypes([]);
    setCurrentPage(1);
    setShowMobileFilters(false);
  }, []);

  return {
    sortKey,
    sortDirection,
    searchQuery,
    filterTypes,
    showMobileFilters,
    currentPage,
    itemsPerPage,

    filteredRows,
    sortedRows,
    paginatedRows,
    totalItems,
    totalPages,
    hasActiveFilters,

    changeSort,
    changeSortKey,
    changeSortDirection,
    changeSearchQuery,
    toggleTypeFilter,
    changePage,
    changeItemsPerPage,
    openMobileFilters,
    closeMobileFilters,
    clearAllFilters,
  };
}

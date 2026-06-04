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

import React, { useState, useMemo, useCallback } from 'react';
import { Box, Flex, Text, TextField } from '@radix-ui/themes';
import { Search, Key } from 'lucide-react';
import { Privilege } from '../../security/privileges/types';
import { EntityTable, TableColumn, EmptyState } from '../../../../shared';

const PRIVILEGES_BASE_PATH = 'preview/admin/security/privileges';

interface CalculatedPermissionsProps {
  privileges: Privilege[];
  loading: boolean;
  /** When true, privilege links render as plain text — used in full-screen modal */
  linksDisabled?: boolean;
}

export function CalculatedPermissions({ privileges, loading, linksDisabled = false }: CalculatedPermissionsProps) {
  const [filter, setFilter] = useState('');
  const [sortField, setSortField] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const filteredPrivileges = useMemo(() => {
    let result = privileges;
    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter(p => 
        p.name.toLowerCase().includes(lowerFilter) ||
        p.description.toLowerCase().includes(lowerFilter) ||
        p.permission.toLowerCase().includes(lowerFilter)
      );
    }

    return [...result].sort((a, b) => {
      const aVal = (a as any)[sortField] || '';
      const bVal = (b as any)[sortField] || '';
      const comparison = String(aVal).localeCompare(String(bVal));
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [privileges, filter, sortField, sortDirection]);

  const handleSort = useCallback((columnId: string) => {
    if (sortField === columnId) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(columnId);
      setSortDirection('asc');
    }
  }, [sortField, sortDirection]);

  const emptyState = useMemo(() => (
    <EmptyState
      title="No permissions found"
      description={filter ? `No permissions matching "${filter}"` : "This role has no granted permissions."}
      icon={Key}
    />
  ), [filter]);

  const navigateToPrivilege = useCallback((privilegeId: string) => {
    window.location.hash = `${PRIVILEGES_BASE_PATH}/${encodeURIComponent(privilegeId)}/profile`;
  }, []);

  const columns: TableColumn<Privilege>[] = useMemo(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: (p) => (
        <Flex align="center" gap="2">
          <Key size={14} color="var(--amber-9)" />
          {linksDisabled ? (
            <Text size="2" weight="medium" as="span">{p.name}</Text>
          ) : (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                navigateToPrivilege(p.id);
              }}
              style={{
                background: 'none',
                border: 'none',
                padding: 0,
                font: 'inherit',
                color: 'var(--blue-11)',
                textDecoration: 'underline',
                cursor: 'pointer',
              }}
              aria-label={`View privilege: ${p.name}`}
            >
              <Text size="2" weight="medium" as="span">{p.name}</Text>
            </button>
          )}
        </Flex>
      ),
      sortable: true,
      width: '40%',
    },
    {
      id: 'permission',
      header: 'Permission',
      accessor: (p) => (
        <Text size="1" color="gray" style={{ fontFamily: 'var(--font-mono)' }}>
          {p.permission}
        </Text>
      ),
      sortable: true,
      width: '60%',
    }
  ], [linksDisabled, navigateToPrivilege]);

  return (
    <Flex direction="column" gap="3" className="calculated-permissions" style={{ height: '100%' }}>
      <Box className="calculated-permissions__header" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <Text size="3" weight="bold">Calculated Permissions</Text>
        <Text size="1" color="gray">
          Flattened list of all privileges granted by this role and its descendants.
        </Text>
      </Box>

      <TextField.Root 
        placeholder="Search permissions..." 
        value={filter} 
        onChange={(e) => setFilter(e.target.value)}
      >
        <TextField.Slot>
          <Search size={14} />
        </TextField.Slot>
      </TextField.Root>

      <Box style={{ flex: 1, minHeight: 0 }}>
        <EntityTable<Privilege>
          data={filteredPrivileges}
          columns={columns}
          getRowKey={(p) => p.id}
          loading={loading}
          loadingMessage="Calculating effective permissions..."
          ariaLabel="Calculated permissions list"
          sortBy={sortField}
          sortDirection={sortDirection}
          onSort={handleSort}
          emptyState={emptyState}
          showRowArrow={false}
          clickable={false}
          className="calculated-permissions__table"
        />
      </Box>
    </Flex>
  );
}

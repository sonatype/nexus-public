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

import React, { useMemo, useState } from 'react';
import { Badge, Box, Checkbox, Flex, ScrollArea, Select, Text, TextField } from '@radix-ui/themes';

import type { EndpointAccessDot } from './utils/endpointAccess';
import type { MergedApiEndpoint } from './utils/mergeSwaggerPermissions';

export function endpointRowId(row: MergedApiEndpoint): string {
  return `${row.httpMethod}|${row.fullPath}`;
}

export interface EndpointListProps {
  endpoints: MergedApiEndpoint[];
  accessById: Record<string, EndpointAccessDot>;
  selectedId: string | null;
  onSelect: (row: MergedApiEndpoint) => void;
  loading?: boolean;
  /** Role lens deep link: blue granted dots instead of green/red session view */
  accessDotPalette?: 'session' | 'roleLens';
}

const METHODS = ['ALL', 'GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'] as const;

function AccessDot({
  state,
  palette,
}: {
  state: EndpointAccessDot;
  palette: 'session' | 'roleLens';
}) {
  const color =
    palette === 'roleLens'
      ? state === 'granted'
        ? 'var(--blue-9)'
        : state === 'denied'
          ? 'var(--gray-8)'
          : 'var(--gray-8)'
      : state === 'granted'
        ? 'var(--green-9)'
        : state === 'denied'
          ? 'var(--red-9)'
          : 'var(--gray-8)';
  return (
    <Box
      aria-hidden
      className="api-endpoint-list__dot"
      style={{
        width: 8,
        height: 8,
        borderRadius: '50%',
        background: color,
        flexShrink: 0,
      }}
    />
  );
}

export function EndpointList({
  endpoints,
  accessById,
  selectedId,
  onSelect,
  loading,
  accessDotPalette = 'session',
}: EndpointListProps) {
  const [search, setSearch] = useState('');
  const [methodFilter, setMethodFilter] = useState<string>('ALL');
  const [tagFilter, setTagFilter] = useState<string>('ALL');
  const [onlyDenied, setOnlyDenied] = useState(false);

  const tags = useMemo(() => {
    const s = new Set<string>();
    for (const e of endpoints) {
      s.add(e.tag);
    }
    return ['ALL', ...[...s].sort((a, b) => a.localeCompare(b))];
  }, [endpoints]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return endpoints.filter((e) => {
      if (methodFilter !== 'ALL' && e.httpMethod !== methodFilter) {
        return false;
      }
      if (tagFilter !== 'ALL' && e.tag !== tagFilter) {
        return false;
      }
      if (onlyDenied) {
        const a = accessById[endpointRowId(e)];
        if (a === 'granted' || a === 'unknown') {
          return false;
        }
      }
      if (!q) {
        return true;
      }
      const hay = `${e.httpMethod} ${e.fullPath} ${e.summary || ''} ${e.operationId || ''}`.toLowerCase();
      return hay.includes(q);
    });
  }, [endpoints, search, methodFilter, tagFilter, onlyDenied, accessById]);

  const grouped = useMemo(() => {
    const m = new Map<string, MergedApiEndpoint[]>();
    for (const e of filtered) {
      const list = m.get(e.tag) ?? [];
      list.push(e);
      m.set(e.tag, list);
    }
    return [...m.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  }, [filtered]);

  return (
    <Box className="api-endpoint-list">
      <Flex direction="column" gap="3" className="api-endpoint-list__filters">
        <TextField.Root
          placeholder="Search path or summary…"
          value={search}
          onChange={(ev) => setSearch(ev.target.value)}
          aria-label="Search endpoints"
        />
        <Flex gap="2" wrap="wrap">
          <Box style={{ minWidth: 120 }}>
            <Text size="1" weight="medium" mb="1" as="div">
              Method
            </Text>
            <Select.Root value={methodFilter} onValueChange={setMethodFilter}>
              <Select.Trigger placeholder="Method" />
              <Select.Content>
                {METHODS.map((m) => (
                  <Select.Item key={m} value={m}>
                    {m}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>
          <Box style={{ minWidth: 140 }}>
            <Text size="1" weight="medium" mb="1" as="div">
              Tag
            </Text>
            <Select.Root value={tagFilter} onValueChange={setTagFilter}>
              <Select.Trigger placeholder="Tag" />
              <Select.Content>
                {tags.map((t) => (
                  <Select.Item key={t} value={t}>
                    {t}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>
        </Flex>
        <Text as="label" size="2">
          <Flex align="center" gap="2">
            <Checkbox checked={onlyDenied} onCheckedChange={(v) => setOnlyDenied(v === true)} />
            {accessDotPalette === 'roleLens'
              ? 'Only endpoints this role cannot access'
              : "Only endpoints I can't access"}
          </Flex>
        </Text>
      </Flex>

      <ScrollArea
        type="hover"
        scrollbars="vertical"
        className="api-endpoint-list__scroll"
        style={{ maxHeight: 'min(70vh, 640px)' }}
      >
        {loading ? (
          <Flex direction="column" gap="2" p="2" aria-busy="true">
            {Array.from({ length: 8 }).map((_, i) => (
              <Box
                key={i}
                className="api-endpoint-list__skeleton"
                style={{ height: 36, borderRadius: 6, background: 'var(--gray-4)' }}
              />
            ))}
          </Flex>
        ) : filtered.length === 0 ? (
          <Text size="2" color="gray" p="3">
            No endpoints match your search. Try adjusting your filters.
          </Text>
        ) : (
          <Box
            role="listbox"
            aria-label="API endpoints"
            className="api-endpoint-list__list"
          >
            {grouped.map(([tag, rows]) => (
              <Box key={tag} mb="3">
                <Text size="1" weight="bold" color="gray" mb="2" as="div">
                  {tag}
                </Text>
                <Flex direction="column" gap="1">
                  {rows.map((row) => {
                    const id = endpointRowId(row);
                    const selected = id === selectedId;
                    const access = accessById[id] ?? 'unknown';
                    return (
                      <button
                        key={id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`api-endpoint-list__row${selected ? ' api-endpoint-list__row--selected' : ''}`}
                        onClick={() => onSelect(row)}
                      >
                        <Flex align="center" gap="2" width="100%">
                          <AccessDot state={access} palette={accessDotPalette} />
                          <Badge size="1" variant="soft" color="blue">
                            {row.httpMethod}
                          </Badge>
                          <Text size="2" className="api-endpoint-list__path" style={{ textAlign: 'left' }}>
                            {row.fullPath}
                          </Text>
                        </Flex>
                      </button>
                    );
                  })}
                </Flex>
              </Box>
            ))}
          </Box>
        )}
      </ScrollArea>
    </Box>
  );
}

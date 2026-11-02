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
import type { BadgeProps } from '@radix-ui/themes';
import { CircleCheck, CircleMinus, CircleQuestionMark, CircleX } from 'lucide-react';

import ApiStrings from '../../../../constants/pages/settings/system/api/ApiStrings';
import type { EndpointAccessDot } from './utils/endpointAccess';
import type { MergedApiEndpoint } from './utils/mergeSwaggerPermissions';

const STRINGS = ApiStrings.API_ENDPOINT_LIST;

export function endpointRowId(row: MergedApiEndpoint): string {
  return `${row.httpMethod}|${row.fullPath}`;
}

/** Per-method badge color (design mockup): GET blue, POST green, PUT amber, DELETE red. */
const METHOD_BADGE_COLOR: Record<string, BadgeProps['color']> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'amber',
  DELETE: 'red',
  PATCH: 'orange',
};

function methodBadgeColor(method: string): BadgeProps['color'] {
  return METHOD_BADGE_COLOR[method] ?? 'gray';
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

/**
 * HTTP methods the search box matches exactly rather than as a substring, so
 * typing "PUT" doesn't match a summary containing "output" (NEXUS-54907).
 */
const HTTP_METHOD_TOKENS = new Set([
  'get',
  'post',
  'put',
  'delete',
  'patch',
  'head',
  'options',
  'trace',
]);

const ACCESS_ICON_SIZE = 16;

interface AccessProps {
  state: EndpointAccessDot;
  palette: 'session' | 'roleLens';
}

/**
 * Access indicator icon (design mockup): check (full), cross (none),
 * half/minus (partial), question (unknown). In the roleLens palette, a granted
 * state reads as the role satisfying the required permissions.
 */
function AccessIcon({ state, palette }: AccessProps) {
  const common = { size: ACCESS_ICON_SIZE, 'aria-hidden': true as const };
  if (state === 'granted') {
    const color = palette === 'roleLens' ? 'var(--blue-9)' : 'var(--green-9)';
    return <CircleCheck {...common} color={color} />;
  }
  if (state === 'denied') {
    return <CircleX {...common} color="var(--red-9)" />;
  }
  if (state === 'partial') {
    return <CircleMinus {...common} color="var(--amber-9)" />;
  }
  return <CircleQuestionMark {...common} color="var(--gray-8)" />;
}

function AccessIndicator(props: AccessProps) {
  return (
    <Box
      className="api-endpoint-list__access"
      role="img"
      aria-label={STRINGS.access[props.state]}
    >
      <AccessIcon {...props} />
    </Box>
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
  const [tagFilter, setTagFilter] = useState<string>('ALL');
  const [onlyDenied, setOnlyDenied] = useState(false);

  const tags = useMemo(() => {
    const s = new Set<string>();
    for (const e of endpoints) {
      s.add(e.tag);
    }
    return ['ALL', ...[...s].sort((a, b) => a.localeCompare(b))];
  }, [endpoints]);

  // Whitespace-separated tokens, all of which must match (NEXUS-54907). A single
  // substring match would reject "GET /v1/assets", because the full path puts
  // "/service/rest" between the method and "/v1/assets".
  const searchTokens = useMemo(
    () => search.trim().toLowerCase().split(/\s+/).filter(Boolean),
    [search]
  );

  const filtered = useMemo(() => {
    return endpoints.filter((e) => {
      if (tagFilter !== 'ALL' && e.tag !== tagFilter) {
        return false;
      }
      if (onlyDenied) {
        const a = accessById[endpointRowId(e)];
        if (a === 'granted' || a === 'unknown') {
          return false;
        }
      }
      if (searchTokens.length === 0) {
        return true;
      }
      const method = e.httpMethod.toLowerCase();
      // The method is matched exactly from its own field, so it is deliberately
      // absent from the haystack; a method token must not match path text.
      const hay = `${e.fullPath} ${e.summary || ''} ${e.operationId || ''}`.toLowerCase();
      return searchTokens.every((token) =>
        HTTP_METHOD_TOKENS.has(token) ? method === token : hay.includes(token)
      );
    });
  }, [endpoints, searchTokens, tagFilter, onlyDenied, accessById]);

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
          placeholder={STRINGS.searchPlaceholder}
          value={search}
          onChange={(ev) => setSearch(ev.target.value)}
          aria-label={STRINGS.searchLabel}
        />
        <Flex gap="2" wrap="wrap">
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
        <Text
          size="1"
          color="gray"
          as="div"
          className="api-endpoint-list__count"
          aria-live="polite"
        >
          {STRINGS.resultCount(filtered.length, endpoints.length)}
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
                <Text
                  size="1"
                  weight="bold"
                  color="gray"
                  mb="2"
                  as="div"
                  className="api-endpoint-list__group-title"
                >
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
                        <Flex align="start" gap="2" width="100%">
                          <AccessIndicator state={access} palette={accessDotPalette} />
                          <Badge
                            size="1"
                            variant="soft"
                            radius="full"
                            color={methodBadgeColor(row.httpMethod)}
                            className="api-endpoint-list__method"
                          >
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

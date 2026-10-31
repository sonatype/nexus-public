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

/**
 * Sort options for the search results header dropdown — the only user-facing
 * sorting control.
 *
 * The dropdown renders this list and reports selections as a `field:direction`
 * composite value, and the same list is the allowlist that URL params and
 * stored sort state are validated against, so nothing can select a sort the
 * dropdown cannot display.
 *
 * Sorting itself is performed server-side: the field/direction pair is sent to
 * `/service/rest/v1/search` as the `sort` and `direction` query params (see
 * `useUnifiedSearch.addSortParams`).
 */

import React, { useCallback } from 'react';
import { ArrowDown, ArrowUp } from 'lucide-react';

import type { SearchResult, SortDirection, SortField, SortOption } from './unified.types';

/** Separator between field and direction in a composite sort value. */
const SORT_VALUE_SEPARATOR = ':';

/**
 * Separator between field and direction in a full sort label, e.g.
 * "Last updated — Newest first".
 */
const SORT_LABEL_SEPARATOR = ' — ';

/** Per-field sort metadata. Option order below follows this array. */
interface SortFieldMeta {
  /** Internal sort field, also the URL `sort` value */
  field: SortField;
  /** Field label, used as the dropdown group heading */
  label: string;
  /**
   * The field's most useful direction. Its option is listed first within the
   * field's group.
   */
  defaultDirection: SortDirection;
  /** Direction-only labels, e.g. "Newest first" / "A-Z" */
  directionLabels: Record<SortDirection, string>;
}

/**
 * Fields offered by the sort dropdown.
 *
 * Deliberately excludes `version`: the server orders versions by a normalised
 * column (`cs.normalised_version`), which `compareBySort` cannot reproduce from
 * the raw version string in mock mode ('10.0' would sort before '9.0'). Rather
 * than ship an option whose mock ordering disagrees with the server's, the
 * option is not offered at all.
 */
const SORT_FIELDS: readonly SortFieldMeta[] = [
  {
    field: 'lastUpdated',
    label: 'Last updated',
    defaultDirection: 'desc',
    directionLabels: { desc: 'Newest first', asc: 'Oldest first' },
  },
  {
    field: 'name',
    label: 'Name',
    defaultDirection: 'asc',
    directionLabels: { asc: 'A-Z', desc: 'Z-A' },
  },
  {
    field: 'repository',
    label: 'Repository',
    defaultDirection: 'asc',
    directionLabels: { asc: 'A-Z', desc: 'Z-A' },
  },
];

/** Build the composite `field:direction` value used by the sort dropdown. */
export function toSortValue(field: SortField, direction: SortDirection): string {
  return `${field}${SORT_VALUE_SEPARATOR}${direction}`;
}

/**
 * Direction indicator, kept here so the direction-to-arrow mapping lives with
 * the labels and values rather than in the control.
 *
 * Decorative: it repeats what the option label already says in words, so
 * direction is never conveyed by icon or colour alone.
 */
export function SortDirectionIcon({
  direction,
  size,
}: {
  direction: SortDirection;
  size: number;
}): JSX.Element {
  return direction === 'asc' ? (
    <ArrowUp size={size} aria-hidden />
  ) : (
    <ArrowDown size={size} aria-hidden />
  );
}

/**
 * Every selectable sort option, ordered by field and — within a field — with
 * that field's default direction first.
 *
 * `label` composes the field and direction labels rather than restating them,
 * so the dropdown's closed state names the complete active selection ("Last
 * updated — Newest first") in text, independently of the direction arrow.
 */
export const SORT_OPTIONS: readonly SortOption[] = SORT_FIELDS.flatMap((meta) => {
  const directions: SortDirection[] =
    meta.defaultDirection === 'desc' ? ['desc', 'asc'] : ['asc', 'desc'];
  return directions.map((direction) => ({
    value: toSortValue(meta.field, direction),
    label: `${meta.label}${SORT_LABEL_SEPARATOR}${meta.directionLabels[direction]}`,
    field: meta.field,
    direction,
    directionLabel: meta.directionLabels[direction],
  }));
});

/**
 * Sort options grouped by field, for rendering the dropdown's grouped sections.
 */
export const SORT_OPTION_GROUPS: readonly { field: SortField; label: string; options: SortOption[] }[] =
  SORT_FIELDS.map((meta) => ({
    field: meta.field,
    label: meta.label,
    options: SORT_OPTIONS.filter((option) => option.field === meta.field),
  }));

/**
 * Look up the option matching a field/direction pair. Returns undefined for a
 * combination the dropdown does not offer.
 */
export function findSortOption(
  field: SortField | undefined,
  direction: SortDirection | undefined,
): SortOption | undefined {
  return SORT_OPTIONS.find((option) => option.field === field && option.direction === direction);
}

/**
 * Parse a composite `field:direction` value emitted by the sort dropdown.
 * Returns undefined for any value not present in SORT_OPTIONS, so a malformed
 * value can never be forwarded to the machine or the API.
 */
export function parseSortValue(
  value: string,
): { field: SortField; direction: SortDirection } | undefined {
  const option = SORT_OPTIONS.find((candidate) => candidate.value === value);
  return option ? { field: option.field, direction: option.direction } : undefined;
}

/**
 * Adapt the dropdown's composite `field:direction` value to the page's
 * `(field, direction)` callback, so translating and guarding the value lives
 * here rather than in the control.
 */
export function useSortValueChange(
  onSortChange?: (field: SortField, direction: SortDirection) => void,
): (value: string) => void {
  return useCallback(
    (value: string) => {
      const parsed = parseSortValue(value);
      // parseSortValue only resolves values present in SORT_OPTIONS, so an
      // unexpected value is dropped rather than forwarded to the machine.
      if (parsed && onSortChange) {
        onSortChange(parsed.field, parsed.direction);
      }
    },
    [onSortChange],
  );
}

/**
 * Compare two results by a sort field/direction pair.
 *
 * This exists ONLY to give mock mode (`isMockMode()`, which never reaches the
 * REST API) the same ordering the server would apply. It is never used as a
 * fallback for real searches — those are always sorted server-side.
 *
 * String fields compare lexically, matching the server's column ordering;
 * `lastUpdated` holds ISO-8601 timestamps, so lexical comparison is also
 * chronological. Results missing a value sort last in both directions.
 */
export function compareBySort(
  a: SearchResult,
  b: SearchResult,
  field: SortField,
  direction: SortDirection,
): number {
  const left = a[field] ?? '';
  const right = b[field] ?? '';
  if (left === right) return 0;
  // Missing values sort last regardless of direction.
  if (!left) return 1;
  if (!right) return -1;
  const comparison = left.localeCompare(right);
  return direction === 'desc' ? -comparison : comparison;
}

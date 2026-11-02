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

import { useCallback, useState } from 'react';

import { useDebounced } from './useDebounced';

/**
 * A debounced value together with the generation it belongs to.
 *
 * Treat it as opaque: read the settled value with {@link debouncedValueOf}, and pass the token
 * itself — never the unpacked value — as the effect dependency. That is the whole point of the
 * type, so it is worth being explicit about why.
 *
 * A trailing debounce emits only the value its input settles on, and React bails out of the update
 * when that value equals the one already published. A component that debounces a query but reacts
 * to the raw one elsewhere — collapsing a dropdown at once rather than waiting out the interval —
 * therefore splits one decision across two effects, and a round trip away from a query and back to
 * it inside a single interval is swallowed: the collapse discards the results, the debounced query
 * settles on the text that was already searched, and the effect that would refetch sees no change
 * in its dependency. The result is a valid query above an empty list.
 *
 * Pairing the value with a generation that only ever increases removes that case, because
 * returning to a discarded value is always a new token.
 */
export type DebouncedToken = string;

/**
 * Reads the settled value out of a {@link DebouncedToken}.
 *
 * The generation is always digits, so the first ':' separates it from the value, which may
 * contain ':' of its own.
 */
export function debouncedValueOf(token: DebouncedToken): string {
  return token.slice(token.indexOf(':') + 1);
}

/**
 * Debounces `value` like {@link useDebounced}, and additionally lets the caller mark the results
 * derived from it as discarded, so that returning to the same value issues fresh work.
 *
 * Returns the debounced token and a stable `discard` callback. Call `discard` from wherever the
 * derived results are thrown away — a collapse below a minimum query length, a cleared field, a
 * selection that empties the input — and key the effect that produces those results on the token.
 *
 * @example
 * const [searchToken, discardResults] = useDebouncedWithDiscard(query, APIConstants.DEBOUNCE_DELAY);
 *
 * useEffect(() => {
 *   if (query.length < MIN_QUERY_LENGTH) {
 *     setResults([]);
 *     discardResults();
 *   }
 * }, [query, discardResults]);
 *
 * useEffect(() => {
 *   const debouncedQuery = debouncedValueOf(searchToken);
 *   if (debouncedQuery.length < MIN_QUERY_LENGTH) return;
 *   void fetchResults(debouncedQuery).then(setResults);
 * }, [searchToken]);
 */
export function useDebouncedWithDiscard(
  value: string,
  delayMs: number
): [DebouncedToken, () => void] {
  const [generation, setGeneration] = useState(0);

  const discard = useCallback(() => {
    setGeneration((current) => current + 1);
  }, []);

  return [useDebounced(`${generation}:${value}`, delayMs), discard];
}

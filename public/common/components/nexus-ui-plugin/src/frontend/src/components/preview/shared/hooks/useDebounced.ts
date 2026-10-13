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

import { useState, useEffect } from 'react';

/**
 * Returns `value` after it has stopped changing for `delayMs` (trailing debounce). Use this for
 * search and filter inputs: bind the input to immediate state so typing stays responsive, and
 * feed the debounced value to whatever issues the request.
 *
 * Pass `APIConstants.DEBOUNCE_DELAY` as the delay unless a screen has a reason to differ — it is
 * the app-wide interval already used by the list machines and the combobox field factory.
 *
 * @example
 * const [search, setSearch] = useState('');
 * const debouncedSearch = useDebounced(search, APIConstants.DEBOUNCE_DELAY);
 * useEffect(() => { fetchResults(debouncedSearch); }, [debouncedSearch]);
 */
export function useDebounced<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}

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

import { useCallback, useEffect, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { firewallCountsMachine } from './firewallCountsMachine';
import type { SecurityStatusData } from './security.types';

export interface FirewallCountsViewModel {
  /** Counts fetched so far, keyed by repository name */
  counts: Record<string, SecurityStatusData>;
  /** Targeted repositories still waiting for their count (requested or queued) */
  loadingNames: ReadonlySet<string>;
  /** Targeted repositories whose count is not available: their request failed, or fetching halted */
  unavailableNames: ReadonlySet<string>;
  /** Re-run the fetch for the current targets, clearing earlier failures */
  refresh: () => void;
}

/**
 * ViewModel for inline Firewall counts. Pass the names of the enabled repositories on the
 * current page; the machine owns the batched fetching.
 */
export function useFirewallCounts(enabledRepoNames: readonly string[]): FirewallCountsViewModel {
  const [state, send] = useMachine(firewallCountsMachine);

  const key = useMemo(() => [...enabledRepoNames].sort().join('\u0000'), [enabledRepoNames]);
  useEffect(() => {
    send({ type: 'SET_TARGETS', names: key ? key.split('\u0000') : [] });
  }, [key, send]);

  const refresh = useCallback(() => send({ type: 'REFRESH' }), [send]);

  const { counts, failures } = state.context;
  const fetching = state.matches('fetching');
  const halted = state.matches('halted');

  const { loadingNames, unavailableNames } = useMemo(() => {
    const loading = new Set<string>();
    const unavailable = new Set<string>();
    for (const name of enabledRepoNames) {
      if (counts[name]) continue;
      if (failures.includes(name) || halted) unavailable.add(name);
      else if (fetching) loading.add(name);
    }
    return { loadingNames: loading, unavailableNames: unavailable };
  }, [enabledRepoNames, counts, failures, fetching, halted]);

  return { counts, loadingNames, unavailableNames, refresh };
}

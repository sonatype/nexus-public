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

import { useEffect, useState } from 'react';
import { ENDPOINTS, restClient } from '../../../interface/api';
import { ExtJS } from '../../../interface/ExtJS';
import { useHasUser } from './hooks/useHasUser';
import { useFirewallSecurityPagesTier } from './useFirewallSecurityPagesTier';

const QUARANTINE_READ_PERMISSION = 'nexus:firewall-quarantine:read';
const POLL_INTERVAL_MS = 60_000;

interface QuarantineSummary {
  quarantinedComponentCount?: number;
}

/*
 * Module-level store: one poller and one count shared by every subscriber (the Security nav item and the
 * Quarantine tab trigger). The poller runs only while at least one enabled hook instance is mounted, never
 * overlaps requests (the next poll is scheduled after the previous one settles) and pauses while the document
 * is hidden.
 */
let count: number | null = null;
const listeners = new Set<() => void>();
let timer: ReturnType<typeof setTimeout> | null = null;
let running = false;
let generation = 0;
let inFlightGeneration: number | null = null;
// Start time of the last request, so a tab that keeps flipping visible cannot poll more than once per interval.
let lastFetchAt: number | null = null;
// Set on a 404 (summary endpoint not shipped): stops polling until the store is reset or the poller restarts.
let endpointMissing = false;

const isDocumentVisible = () => typeof document === 'undefined' || document.visibilityState !== 'hidden';

function setCount(next: number | null) {
  if (next === count) {
    return;
  }
  count = next;
  listeners.forEach((listener) => {
    listener();
  });
}

function clearTimer() {
  if (timer !== null) {
    clearTimeout(timer);
    timer = null;
  }
}

function schedule(gen: number, delayMs = POLL_INTERVAL_MS) {
  clearTimer();
  if (!running || gen !== generation || !isDocumentVisible()) {
    return;
  }
  timer = setTimeout(() => {
    timer = null;
    void fetchOnce(gen);
  }, delayMs);
}

async function fetchOnce(gen: number): Promise<void> {
  if (inFlightGeneration === gen) {
    return;
  }
  inFlightGeneration = gen;
  lastFetchAt = Date.now();
  let next: number | null = null;
  try {
    const summary = await restClient.get<QuarantineSummary>(ENDPOINTS.FIREWALL_QUARANTINE_SUMMARY);
    next = typeof summary?.quarantinedComponentCount === 'number' ? summary.quarantinedComponentCount : null;
  } catch (error) {
    // 403, 404 (endpoint not shipped yet) and network errors all mean "no badge"
    next = null;
    const failure = error as { status?: number; response?: { status?: number } } | null;
    if (failure?.status === 404 || failure?.response?.status === 404) {
      endpointMissing = true;
    }
  }
  if (inFlightGeneration === gen) {
    inFlightGeneration = null;
  }
  if (gen !== generation) {
    return;
  }
  setCount(next);
  if (!endpointMissing) {
    schedule(gen);
  }
}

function onVisibilityChange() {
  if (!running) {
    return;
  }
  if (!isDocumentVisible()) {
    clearTimer();
  } else if (!endpointMissing && timer === null && inFlightGeneration !== generation) {
    const elapsed = lastFetchAt === null ? POLL_INTERVAL_MS : Date.now() - lastFetchAt;
    if (elapsed >= POLL_INTERVAL_MS) {
      void fetchOnce(generation);
    } else {
      schedule(generation, POLL_INTERVAL_MS - elapsed);
    }
  }
}

function start() {
  running = true;
  endpointMissing = false;
  lastFetchAt = null;
  generation += 1;
  document.addEventListener('visibilitychange', onVisibilityChange);
  if (isDocumentVisible()) {
    void fetchOnce(generation);
  }
}

function stop() {
  running = false;
  generation += 1;
  clearTimer();
  document.removeEventListener('visibilitychange', onVisibilityChange);
  count = null;
  lastFetchAt = null;
}

/** Test helper: drops all store state and listeners. */
export function __resetQuarantineBadgeStoreForTests(): void {
  stop();
  listeners.clear();
  inFlightGeneration = null;
}

/**
 * Number of quarantined components for the Security nav badge and the Quarantine tab badge, or null when not
 * applicable or unknown. Enabled only when active (callers pass false where the nav item is not shown, e.g. Classic UI), on the live tier,
 * for users holding nexus:firewall-quarantine:read.
 */
export function useQuarantineBadgeCount(active = true): number | null {
  const tier = useFirewallSecurityPagesTier();
  const hasUser = useHasUser();
  const canRead = ExtJS.usePermission(() => ExtJS.checkPermission(QUARANTINE_READ_PERMISSION), [hasUser]);
  const enabled = active && tier === 'live' && !!canRead;
  const [value, setValue] = useState<number | null>(count);

  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    const listener = () => setValue(count);
    listeners.add(listener);
    setValue(count);
    if (listeners.size === 1) {
      start();
    }
    return () => {
      listeners.delete(listener);
      if (listeners.size === 0) {
        stop();
      }
    };
  }, [enabled]);

  return enabled ? value : null;
}

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

import type { IqServerConfiguration } from './types';

/**
 * Shared module-level caches for IQ Server connect/disconnect navigation.
 *
 * After a PUT, the backend processes IQ state asynchronously, so an immediate GET may return
 * stale data. Two mechanisms cover the two flows:
 *
 *   freshIqConfigCache  — set after a successful Save (connect). The newly mounted
 *     IqServerConnectedPage reads it on first load to seed url / licensedSolutions from the
 *     PUT response and, when the cache is authoritative (non-empty licensedSolutions),
 *     suppress the follow-up GET. Verify-connection still runs. A timestamp bound guarantees
 *     a stranded entry (e.g. a superseded go()) cannot suppress the GET on a later mount.
 *
 *   pendingDisconnect   — set before navigating away after Disconnect. IqServerOverviewPage
 *     checks it on mount and skips its own GET entirely, showing the disconnected card
 *     without racing the server.
 */

// Handoff window: bounded above by "aborted-navigation staleness" (a superseded go() must not
// feed a later mount), bounded below by "must comfortably survive a UIRouter transition + React
// commit" — observed 100–600 ms in practice.
const MAX_HANDOFF_AGE_MS = 5000;

let freshIqConfig: IqServerConfiguration | null = null;
let freshIqConfigAt = 0;

export function setFreshIqConfigCache(config: IqServerConfiguration): void {
  freshIqConfig = config;
  freshIqConfigAt = Date.now();
}

export function clearFreshIqConfigCache(): void {
  freshIqConfig = null;
  freshIqConfigAt = 0;
}

function freshIqConfigIsFresh(): boolean {
  return freshIqConfig !== null && Date.now() - freshIqConfigAt <= MAX_HANDOFF_AGE_MS;
}

// Read-only accessor rather than a mutable binding so callers cannot bypass the TTL by importing
// the raw slot. Returns null once the entry ages out, matching the "no handoff available" shape.
export function getFreshIqConfigCache(): IqServerConfiguration | null {
  if (!freshIqConfigIsFresh()) {
    // Drop the stranded entry so subsequent peeks don't keep paying the Date.now() comparison.
    freshIqConfig = null;
    freshIqConfigAt = 0;
    return null;
  }
  return freshIqConfig;
}

export let pendingDisconnect = false;
export function setPendingDisconnect(): void { pendingDisconnect = true; }
export function clearPendingDisconnect(): void { pendingDisconnect = false; }

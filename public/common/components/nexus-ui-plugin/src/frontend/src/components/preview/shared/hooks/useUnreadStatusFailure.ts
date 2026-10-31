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

import { useCallback, useEffect, useRef, useState } from 'react';

export const STATUS_BELL_ACK_STORAGE_KEY = 'nxrm.statusBell.acknowledgedFailure';

function readAcknowledged(): boolean {
  try {
    return localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function writeAcknowledged(value: boolean): void {
  try {
    if (value) {
      localStorage.setItem(STATUS_BELL_ACK_STORAGE_KEY, 'true');
    } else {
      localStorage.removeItem(STATUS_BELL_ACK_STORAGE_KEY);
    }
  } catch {
    // Storage disabled (Safari Private Mode, quota exceeded, etc.) — degrade to
    // showing the dot whenever the backend reports a failure. Same behavior we
    // had before this hook existed.
  }
}

export interface UseUnreadStatusFailureReturn {
  showDot: boolean;
  markAcknowledged: () => void;
}

/**
 * Give the status bell "unread notification" semantics.
 *
 * Instead of the dot mirroring `healthChecksFailed` directly, this hook layers
 * a per-browser acknowledgement on top:
 * - Dot appears when a *new* failure lands (healthy → unhealthy transition, or
 *   the app mounts unhealthy with no prior acknowledgement).
 * - `markAcknowledged()` (called on bell click) hides the dot for the current
 *   failure. It stays hidden across page refreshes as long as the state
 *   remains unhealthy.
 * - When the backend clears the failure (unhealthy → healthy), the
 *   acknowledgement is reset so the next failure re-alerts.
 * - If the tab is closed mid-recovery (the transition is never observed), the
 *   ack is instead cleared on the next healthy mount so a stale flag can never
 *   silently suppress a subsequent, unrelated failure.
 */
export function useUnreadStatusFailure(
  healthChecksFailed: boolean,
): UseUnreadStatusFailureReturn {
  const [acknowledged, setAcknowledged] = useState<boolean>(() => {
    // Only trust a persisted ack when we mount while still unhealthy. Mounting
    // healthy means any leftover ack is stale from a previous failure — drop
    // it now so a fresh failure later cannot be silently suppressed.
    if (!healthChecksFailed) {
      writeAcknowledged(false);
      return false;
    }
    return readAcknowledged();
  });
  const prevFailedRef = useRef<boolean | null>(null);

  useEffect(() => {
    const prev = prevFailedRef.current;
    prevFailedRef.current = healthChecksFailed;

    if (prev === null) {
      return;
    }

    if (prev === false && healthChecksFailed === true) {
      writeAcknowledged(false);
      setAcknowledged(false);
      return;
    }

    if (prev === true && healthChecksFailed === false) {
      writeAcknowledged(false);
      setAcknowledged(false);
    }
  }, [healthChecksFailed]);

  const markAcknowledged = useCallback(() => {
    writeAcknowledged(true);
    setAcknowledged(true);
  }, []);

  return {
    showDot: healthChecksFailed && !acknowledged,
    markAcknowledged,
  };
}

/** Clears the persisted acknowledgement. Intended for tests and manual reset. */
export function resetUnreadStatusFailure(): void {
  writeAcknowledged(false);
}

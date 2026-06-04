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

import { useEffect, useMemo, useState } from 'react';
import { ExtJS } from '../../../../../../../interface/ExtJS';

import {
  DEEP_LINK_ID_PATTERN,
  parseApiModuleHashParams,
} from '../utils/apiModuleDeepLinkParams';

export interface ApiModuleDeepLinkState {
  /** Raw parsed params from the hash query string */
  raw: ReturnType<typeof parseApiModuleHashParams>;
  /** Human-readable issues (invalid param, missing admin for role lens, etc.) */
  warnings: string[];
  /** User id for view-as mode (server access-check), or null */
  viewAsUserId: string | null;
  /** Role id for role lens, or null if gated / invalid */
  roleLensId: string | null;
  permissionFilter: string | null;
  endpointParam: string | null;
}

function isAdmin(): boolean {
  try {
    return ExtJS.checkPermission('nexus:*');
  } catch {
    return false;
  }
}

function buildDeepLinkState(hash: string): ApiModuleDeepLinkState {
  const raw = parseApiModuleHashParams(hash);
  const warnings: string[] = [];
  let viewAsUserId: string | null = null;
  let roleLensId: string | null = null;

  if (raw.user) {
    if (!DEEP_LINK_ID_PATTERN.test(raw.user)) {
      warnings.push('Ignoring invalid user query parameter (allowed: letters, digits, . _ -).');
    } else {
      viewAsUserId = raw.user;
    }
  }

  if (raw.role) {
    if (!DEEP_LINK_ID_PATTERN.test(raw.role)) {
      warnings.push('Ignoring invalid role query parameter (allowed: letters, digits, . _ -).');
    } else if (!isAdmin()) {
      warnings.push('Role lens (?role=) requires administrator privileges. Showing your own access instead.');
    } else {
      roleLensId = raw.role;
    }
  }

  return {
    raw,
    warnings,
    viewAsUserId,
    roleLensId,
    permissionFilter: raw.permission,
    endpointParam: raw.endpoint,
  };
}

export function useApiModuleDeepLink(): ApiModuleDeepLinkState {
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const onHash = () => setTick((t) => t + 1);
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  return useMemo(() => buildDeepLinkState(window.location.hash || ''), [tick]);
}

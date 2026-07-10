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
import { parseApiError, restClient } from '../../../../interface/api';

/**
 * The four firewall modes persisted on a proxy repository's configuration post-migration
 * (STL-381). The mode lives on the top-level `firewall` field of the typed repository REST
 * representation (e.g. `GET /v1/repositories/maven/proxy/<name>`).
 *
 * PCCS is only valid for npm and pypi proxies; check via {@link fetchPccsSupportedFormats}
 * before offering it in UI.
 */
export type FirewallMode = 'DISABLED' | 'AUDIT' | 'QUARANTINE' | 'PCCS';

/**
 * Status of the firewall configuration for a single repository.
 *
 * The boolean fields are kept for backward compatibility with components that pre-date the
 * 4-mode model (Protect overview, FirewallCard, MalwareRisk pages). Components that need to
 * distinguish QUARANTINE from PCCS should read {@link IqAuditStatus.mode} directly — both
 * collapse to `enabledQuarantine: true` under the boolean view.
 */
export interface IqAuditStatus {
  repositoryName: string;
  enabled: boolean;
  enabledQuarantine: boolean;
  /**
   * Full firewall mode. Always populated by {@link fetchIqAuditStatus} post-migration; older
   * clients that synthesise this object directly may omit it.
   */
  mode?: FirewallMode;
}

interface FirewallFormatCapability {
  format: string;
  pccsModeSupported?: boolean;
}

const FORMAT_CAPABILITIES_URL = '/service/rest/v1/repositories/firewall/format-capabilities';

/**
 * Map a UI format identifier to the path segment used in the typed repository REST endpoint
 * (`/v1/repositories/<format>/<type>/<name>`). Maven is the only format whose REST path
 * differs from its UI label (`maven2` → `maven`).
 */
function toApiFormat(format: string): string {
  return format === 'maven2' ? 'maven' : format;
}

/**
 * Fetch the list of formats that support PCCS mode, using the same backend endpoint the legacy
 * React-in-classic UI ({@code FirewallConfiguration.jsx}) consults.
 *
 * Cached at module scope: PCCS support is a server-side capability question that does not
 * change at runtime, so a single fetch per page session is enough. Falls back to a conservative
 * `['npm', 'pypi']` if the call fails — matches legacy behaviour.
 */
let cachedPccsFormats: string[] | null = null;
let pccsFormatsPromise: Promise<string[]> | null = null;

export async function fetchPccsSupportedFormats(): Promise<string[]> {
  if (cachedPccsFormats !== null) {
    return cachedPccsFormats;
  }
  if (pccsFormatsPromise === null) {
    pccsFormatsPromise = (async () => {
      try {
        const data = await restClient.get<FirewallFormatCapability[]>(FORMAT_CAPABILITIES_URL);
        const formats = Array.isArray(data)
          ? data.filter((entry) => entry?.pccsModeSupported).map((entry) => entry.format)
          : ['npm', 'pypi'];
        cachedPccsFormats = formats;
        return formats;
      } catch {
        cachedPccsFormats = ['npm', 'pypi'];
        return cachedPccsFormats;
      } finally {
        pccsFormatsPromise = null;
      }
    })();
  }
  return pccsFormatsPromise;
}

/**
 * Test-only: clear the PCCS-formats cache between tests so each exercises the fetch path.
 * @internal
 */
export function __resetPccsFormatsCacheForTests(): void {
  cachedPccsFormats = null;
  pccsFormatsPromise = null;
}

/**
 * Read the current firewall configuration for a repository.
 *
 * Performs a basic GET to discover the repository's format/type, then a typed GET to read
 * the `firewall.mode` field. Returns `null` if the repository does not exist or the request
 * fails — callers should treat that as "no information" and degrade gracefully (matching the
 * pre-existing contract).
 */
export async function fetchIqAuditStatus(
  repositoryName: string,
): Promise<IqAuditStatus | null> {
  try {
    const basic = await restClient.get<{ format?: string; type?: string }>(
      `/service/rest/v1/repositories/${encodeURIComponent(repositoryName)}`,
    );
    if (!basic?.format || !basic?.type) {
      return null;
    }
    const typedUrl = `/service/rest/v1/repositories/${toApiFormat(basic.format)}/${encodeURIComponent(basic.type)}/${encodeURIComponent(repositoryName)}`;
    const typed = await restClient.get<{ firewall?: { mode?: string } | null }>(typedUrl);
    const mode = normaliseMode(typed?.firewall?.mode);
    return {
      repositoryName,
      enabled: mode !== 'DISABLED',
      enabledQuarantine: mode === 'QUARANTINE' || mode === 'PCCS',
      mode,
    };
  } catch {
    return null;
  }
}

function normaliseMode(value: unknown): FirewallMode {
  return value === 'AUDIT' || value === 'QUARANTINE' || value === 'PCCS'
    ? value
    : 'DISABLED';
}

/**
 * Set the firewall mode on a proxy repository.
 *
 * Uses a read-modify-write through the standard typed repository REST endpoint
 * (`PUT /v1/repositories/<format>/<type>/<name>`) so all four modes — including PCCS — go
 * through one uniform code path. The legacy boolean `iq/audit` endpoint cannot represent PCCS
 * (it collapses QUARANTINE and PCCS into the same `enabledQuarantine: true` state), so any UI
 * that needs the full 4-state model must use this function.
 *
 * Concurrency note: the GET-then-PUT is not atomic. Last writer wins under contention. This
 * matches the existing semantics of every other repo-config update path in NXRM (the
 * server-side `iq/audit` endpoint had the same characteristic — see
 * {@code RepositoryAttributeStorageSupport.setFirewallConfiguration}). Firewall mode changes
 * are infrequent admin operations, so the race window is narrow in practice.
 */
export async function setFirewallMode(
  repositoryName: string,
  mode: FirewallMode,
): Promise<void> {
  const basic = await restClient.get<{ format?: string; type?: string }>(
    `/service/rest/v1/repositories/${encodeURIComponent(repositoryName)}`,
  );
  if (!basic?.format || !basic?.type) {
    throw new Error(`Repository '${repositoryName}' not found`);
  }
  const typedUrl = `/service/rest/v1/repositories/${toApiFormat(basic.format)}/${encodeURIComponent(basic.type)}/${encodeURIComponent(repositoryName)}`;
  const typed = await restClient.get<Record<string, unknown>>(typedUrl);
  // Strip server-only fields the PUT contract does not accept.
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { format: _f, type: _t, url: _u, ...writable } = typed ?? {};
  writable.firewall = { mode };
  await restClient.put(typedUrl, writable);
}

/**
 * Enable Firewall in Audit-only mode (monitors, no blocking).
 * Backed by the uniform {@link setFirewallMode} so all modes share one wire format.
 */
export async function enableFirewallAudit(repositoryName: string): Promise<void> {
  await setFirewallMode(repositoryName, 'AUDIT');
}

/**
 * Enable Firewall in Quarantine mode (monitors and blocks policy violations).
 */
export async function enableFirewallQuarantine(repositoryName: string): Promise<void> {
  await setFirewallMode(repositoryName, 'QUARANTINE');
}

/**
 * Enable Firewall in PCCS mode (Quarantine plus metadata filtering). Only valid for formats
 * returned by {@link fetchPccsSupportedFormats} — currently npm and pypi.
 */
export async function enableFirewallPccs(repositoryName: string): Promise<void> {
  await setFirewallMode(repositoryName, 'PCCS');
}

/**
 * Disable Firewall (no protection).
 */
export async function disableFirewall(repositoryName: string): Promise<void> {
  await setFirewallMode(repositoryName, 'DISABLED');
}

/**
 * Hook for setting Firewall mode on a repository with loading/error state.
 *
 * The `enableAudit`, `enableQuarantine`, and `disable` methods are kept for backward
 * compatibility with pre-PCCS callers. New code should prefer {@link useFirewallEnable.setMode}
 * which accepts the full {@link FirewallMode} union (including PCCS).
 */
export function useFirewallEnable(repositoryName: string) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(
    async (op: () => Promise<void>, onSuccess?: () => void) => {
      setLoading(true);
      setError(null);
      try {
        await op();
        onSuccess?.();
      }
      catch (err) {
        const apiError = parseApiError(err);
        setError(apiError.message);
        throw err;
      }
      finally {
        setLoading(false);
      }
    },
    [],
  );

  const setMode = useCallback(
    (mode: FirewallMode, onSuccess?: () => void) =>
      run(() => setFirewallMode(repositoryName, mode), onSuccess),
    [repositoryName, run],
  );

  const enableAudit = useCallback(
    (onSuccess?: () => void) => setMode('AUDIT', onSuccess),
    [setMode],
  );

  const enableQuarantine = useCallback(
    (onSuccess?: () => void) => setMode('QUARANTINE', onSuccess),
    [setMode],
  );

  const enablePccs = useCallback(
    (onSuccess?: () => void) => setMode('PCCS', onSuccess),
    [setMode],
  );

  const disable = useCallback(
    (onSuccess?: () => void) => setMode('DISABLED', onSuccess),
    [setMode],
  );

  return { setMode, enableAudit, enableQuarantine, enablePccs, disable, loading, error };
}

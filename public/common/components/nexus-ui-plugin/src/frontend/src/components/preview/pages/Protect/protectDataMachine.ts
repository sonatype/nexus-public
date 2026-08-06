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

import { assign, createMachine } from 'xstate';

import { restClient, ENDPOINTS } from '../../../../interface/api';

/** Response from GET /service/rest/v1/iq/capabilities (license + connection metadata). */
export interface ProtectIqCapabilities {
  connected: boolean;
  hasFirewall: boolean;
  hasLifecycle?: boolean;
  url?: string;
  deploymentId?: string;
}

export interface ProtectDataContext {
  iqCapabilities: ProtectIqCapabilities | null;
  hcInstanceEnabled: boolean;
}

type ProtectDataEvent =
  | { type: 'done.invoke.fetchIqCapabilities'; data: ProtectIqCapabilities | null }
  | { type: 'done.invoke.fetchHcInstanceEnabled'; data: boolean };

/**
 * Owns `useProtectData`'s two direct fetches (IQ capabilities + healthcheck instance flag).
 *
 * Two intentional design choices, both preserving the pre-migration hook contract:
 *  - Fetch-once-on-mount, no REFRESH: the previous hook fetched both facts in `useEffect(…, [])`
 *    and never re-fetched them (`refetch` only ever re-loaded repo data). These two facts are
 *    session-stable (IQ connection / healthcheck capability don't change without a page reload),
 *    so each parallel region runs `loading → loaded` once and then idles.
 *  - No error state: `onError` targets `loaded` with no error captured. The services already
 *    catch failures and return safe defaults (`null` / `true`), matching the old "fail silently,
 *    use defaults" behavior, so `onError` is only a belt-and-suspenders fallback and there is
 *    deliberately nothing for the caller to observe.
 */
export const protectDataMachine = createMachine<ProtectDataContext, ProtectDataEvent>(
  {
    id: 'protectData',
    type: 'parallel',
    context: {
      iqCapabilities: null,
      hcInstanceEnabled: true,
    },
    states: {
      capabilities: {
        initial: 'loading',
        states: {
          loading: {
            invoke: {
              id: 'fetchIqCapabilities',
              src: 'fetchIqCapabilities',
              onDone: { target: 'loaded', actions: 'setIqCapabilities' },
              onError: { target: 'loaded' },
            },
          },
          loaded: {},
        },
      },
      instance: {
        initial: 'loading',
        states: {
          loading: {
            invoke: {
              id: 'fetchHcInstanceEnabled',
              src: 'fetchHcInstanceEnabled',
              onDone: { target: 'loaded', actions: 'setHcInstanceEnabled' },
              onError: { target: 'loaded' },
            },
          },
          loaded: {},
        },
      },
    },
  },
  {
    actions: {
      setIqCapabilities: assign((_ctx, event) => ({
        iqCapabilities: (event as { data: ProtectIqCapabilities | null }).data,
      })),
      setHcInstanceEnabled: assign((_ctx, event) => ({
        hcInstanceEnabled: (event as { data: boolean }).data,
      })),
    },
    services: {
      // GET /iq/capabilities — return null on invalid shape or failure (matches previous hook).
      // Note: IQ_CAPABILITIES is not declared on nexus-ui-plugin's ENDPOINTS (it lives in
      // nexus-coreui-plugin), so it resolves to undefined at runtime and this fetch falls back to
      // null — unchanged from pre-migration behavior. Declaring the key is a separate follow-up.
      fetchIqCapabilities: async (): Promise<ProtectIqCapabilities | null> => {
        try {
          const cap = await restClient.get<ProtectIqCapabilities>(ENDPOINTS.IQ_CAPABILITIES);
          const valid =
            cap &&
            typeof cap === 'object' &&
            !Array.isArray(cap) &&
            typeof (cap as ProtectIqCapabilities).hasFirewall === 'boolean';
          return valid ? (cap as ProtectIqCapabilities) : null;
        } catch {
          return null;
        }
      },
      // GET /capabilities — healthcheck capability enabled? Missing row/failure => assume true.
      fetchHcInstanceEnabled: async (): Promise<boolean> => {
        try {
          const caps = await restClient.get<Array<{ type?: string; typeId?: string; enabled?: boolean }>>(
            ENDPOINTS.CAPABILITIES,
          );
          if (!Array.isArray(caps)) return true;
          const hc = caps.find((c) => (c.type ?? c.typeId) === 'healthcheck');
          return hc ? !!hc.enabled : true;
        } catch {
          return true;
        }
      },
    },
  },
);

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
import { restClient } from '../../../../../../interface/api';
import { LicenseData, LICENSE_API } from './types';

export interface LicensingContext {
  license: LicenseData;
  loadError: string | null;
  activeTab: string;
}

type LicensingEvent =
  | { type: 'SET_TAB'; tab: string }
  | { type: 'LICENSE_INSTALLED'; license: LicenseData }
  | { type: 'DISMISS_ERROR' }
  | { type: 'done.invoke.loadLicense'; data: LicenseData }
  | { type: 'error.platform.loadLicense'; data: Error };

const LICENSE_LOAD_ERROR = 'Failed to load license information';

/**
 * Fetch the current license. A 402 (Payment Required) means no license is
 * installed and is a normal empty state, not an error. Any other failure is
 * surfaced as a load error.
 */
async function loadLicense(): Promise<LicenseData> {
  try {
    const data = await restClient.get<LicenseData>(LICENSE_API.BASE_URL);
    return data || {};
  } catch (err) {
    if ((err as any)?.response?.status === 402) {
      return {};
    }
    throw new Error(LICENSE_LOAD_ERROR);
  }
}

/**
 * Read-only licensing machine: loads license data, tracks the active tab, and
 * reacts to a post-install license replacement. Load failures land in `loaded`
 * with `loadError` set (shown as a dismissible banner), matching prior behavior.
 */
export function createLicensingMachine() {
  return createMachine<LicensingContext, LicensingEvent>(
    {
      id: 'licensing',
      initial: 'loading',
      context: { license: {}, loadError: null, activeTab: 'license' },
      states: {
        loading: {
          invoke: {
            id: 'loadLicense',
            src: 'loadLicense',
            onDone: { target: 'loaded', actions: 'setLicense' },
            onError: { target: 'loaded', actions: 'setLoadError' },
          },
        },
        loaded: {
          on: {
            SET_TAB: { actions: 'setTab' },
            LICENSE_INSTALLED: { actions: 'setInstalledLicense' },
            DISMISS_ERROR: { actions: 'clearError' },
          },
        },
      },
    },
    {
      actions: {
        setLicense: assign((_ctx, event) => ({
          license: ((event as any).data as LicenseData) ?? {},
          loadError: null,
        })),
        setLoadError: assign((_ctx, event) => ({
          loadError: (event as any).data?.message ?? LICENSE_LOAD_ERROR,
        })),
        setTab: assign((_ctx, event) => ({ activeTab: (event as any).tab as string })),
        setInstalledLicense: assign((_ctx, event) => ({
          license: (event as any).license as LicenseData,
          loadError: null,
        })),
        // DISMISS_ERROR only hides the load-error banner; it does not reset the
        // license data or trigger a re-fetch.
        clearError: assign({ loadError: (_ctx: LicensingContext) => null }),
      },
      services: {
        loadLicense: () => loadLicense(),
      },
    },
  );
}

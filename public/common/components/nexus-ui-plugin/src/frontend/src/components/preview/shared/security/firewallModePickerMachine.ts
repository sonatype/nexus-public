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
import { fetchIqAuditStatus, fetchPccsSupportedFormats, type FirewallMode } from './useFirewallEnable';

export interface FirewallModePickerContext {
  repositoryName: string;
  /** Mode the repository is in right now, as read from the repository configuration */
  currentMode: FirewallMode | null;
  pccsFormats: string[];
  selectedMode: FirewallMode | null;
}

type FirewallModePickerEvent =
  | { type: 'SELECT'; mode: FirewallMode }
  | { type: 'RETRY' }
  | { type: 'done.invoke.loadMode'; data: { mode: FirewallMode; pccsFormats: string[] } }
  | { type: 'error.platform.loadMode'; data: unknown };

export function createFirewallModePickerContext(repositoryName: string): FirewallModePickerContext {
  return { repositoryName, currentMode: null, pccsFormats: [], selectedMode: null };
}

/**
 * Model for the enable-firewall mode picker. The repository's real mode is read from its
 * configuration before the picker can confirm, so the UI never guesses from the summary row.
 */
export const firewallModePickerMachine = createMachine<FirewallModePickerContext, FirewallModePickerEvent>(
  {
    id: 'firewallModePicker',
    predictableActionArguments: true,
    initial: 'loading',
    context: createFirewallModePickerContext(''),
    states: {
      loading: {
        invoke: {
          id: 'loadMode',
          src: 'loadMode',
          onDone: { target: 'ready', actions: 'storeLoaded' },
          onError: 'failed',
        },
      },
      ready: {
        on: { SELECT: { actions: 'selectMode' } },
      },
      failed: {
        on: { RETRY: 'loading' },
      },
    },
  },
  {
    actions: {
      storeLoaded: assign((_ctx, event) => {
        const { mode, pccsFormats } = (event as { data: { mode: FirewallMode; pccsFormats: string[] } }).data;
        return { currentMode: mode, pccsFormats, selectedMode: null };
      }),
      selectMode: assign({
        selectedMode: (_ctx, event) => (event.type === 'SELECT' ? event.mode : null),
      }),
    },
    services: {
      loadMode: async (ctx) => {
        const [status, pccsFormats] = await Promise.all([
          fetchIqAuditStatus(ctx.repositoryName),
          fetchPccsSupportedFormats(),
        ]);
        if (!status?.mode) {
          throw new Error(`Could not read firewall mode for ${ctx.repositoryName}`);
        }
        return { mode: status.mode, pccsFormats };
      },
    },
  },
);

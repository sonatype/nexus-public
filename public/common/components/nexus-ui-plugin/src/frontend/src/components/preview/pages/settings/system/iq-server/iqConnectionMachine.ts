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
import Axios from 'axios';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { parseVersion } from './iqServerUtils';
import {
  IqServerConfiguration, IqVerificationResult, IqCapabilities, DEFAULT_IQ_CAPABILITIES,
} from './types';

const IQ_CAPABILITIES_API = 'service/rest/v1/iq/capabilities';
const IQ_CAPABILITIES_TEST_API = 'service/rest/v1/iq/capabilities/test';
const IQ_VERIFY_SELFHOSTED = 'service/rest/internal/ui/iq/verify-connection';
const IQ_VERIFY_CLOUD = 'service/rest/v1/iq/test-new-connection';

function getVerifyApi(): string {
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;
  return isCloud ? IQ_VERIFY_CLOUD : IQ_VERIFY_SELFHOSTED;
}

export async function verifyConnection(settings: IqServerConfiguration): Promise<IqVerificationResult> {
  try {
    const response = await Axios.post(getVerifyApi(), settings);
    return { success: true, reason: response.data?.reason };
  } catch (err: any) {
    const raw = err?.response?.data || err?.message || 'Connection verification failed';
    const reason = typeof raw === 'string' ? raw : raw?.message || JSON.stringify(raw);
    return { success: false, reason };
  }
}

export async function fetchCapabilities(): Promise<IqCapabilities> {
  try {
    const response = await Axios.get(IQ_CAPABILITIES_API);
    return { ...DEFAULT_IQ_CAPABILITIES, ...response.data };
  } catch (err) {
    console.warn('Could not load IQ Server capabilities:', err);
    return DEFAULT_IQ_CAPABILITIES;
  }
}

export async function fetchCapabilitiesWithConfig(settings: IqServerConfiguration): Promise<IqCapabilities> {
  try {
    const response = await Axios.post(IQ_CAPABILITIES_TEST_API, settings);
    return { ...DEFAULT_IQ_CAPABILITIES, ...response.data };
  } catch (err) {
    console.warn('Could not load IQ Server capabilities:', err);
    return DEFAULT_IQ_CAPABILITIES;
  }
}

export interface IqConnectionContext {
  message?: string;
  verificationResult: IqVerificationResult | null;
  capabilities: IqCapabilities;
}

export type IqConnectionEvent =
  | { type: 'AUTO_TEST'; settings: IqServerConfiguration }
  | { type: 'TEST'; settings: IqServerConfiguration }
  | { type: 'RESET_CONNECTION' }
  | { type: 'SAVED' }
  | { type: 'SET_CAPABILITIES'; capabilities: IqCapabilities };

export function createIqConnectionMachine() {
  return createMachine<IqConnectionContext, IqConnectionEvent>(
    {
      id: 'iq-connection',
      initial: 'idle',
      context: { verificationResult: null, capabilities: DEFAULT_IQ_CAPABILITIES },
      states: {
        idle: {
          on: {
            AUTO_TEST: { target: 'testing', actions: 'start' },
            TEST: { target: 'testing', actions: 'start' },
            SAVED: { target: 'idle', actions: 'onSaved' },
            SET_CAPABILITIES: { actions: 'setCapabilities' },
          },
        },
        testing: {
          invoke: {
            src: 'verify',
            onDone: [
              { target: 'connected', cond: 'succeeded', actions: 'setConnected' },
              { target: 'failed', actions: 'setFailed' },
            ],
            onError: { target: 'failed', actions: 'setFailedFromError' },
          },
          on: { RESET_CONNECTION: 'idle' },
        },
        connected: {
          on: {
            TEST: { target: 'testing', actions: 'start' },
            RESET_CONNECTION: { target: 'idle', actions: 'clear' },
            SAVED: { target: 'idle', actions: 'onSaved' },
            SET_CAPABILITIES: { actions: 'setCapabilities' },
          },
        },
        failed: {
          on: {
            TEST: { target: 'testing', actions: 'start' },
            AUTO_TEST: { target: 'testing', actions: 'start' },
            RESET_CONNECTION: { target: 'idle', actions: 'clear' },
            SAVED: { target: 'idle', actions: 'onSaved' },
          },
        },
      },
    },
    {
      services: {
        verify: async (_ctx, event) => {
          const settings = (event as Extract<IqConnectionEvent, { settings: IqServerConfiguration }>).settings;
          const result = await verifyConnection(settings);
          const capabilities = result.success ? await fetchCapabilitiesWithConfig(settings) : DEFAULT_IQ_CAPABILITIES;
          return { result, capabilities };
        },
      },
      guards: {
        succeeded: (_c, e: any) => e.data.result.success === true,
      },
      actions: {
        start: assign({ verificationResult: (_c) => null, message: (_c) => undefined }),
        setConnected: assign((_c, e: any) => ({
          verificationResult: e.data.result,
          capabilities: e.data.capabilities,
          message: `Connected to IQ Server${parseVersion(e.data.result.reason)}`,
        })),
        setFailed: assign((_c, e: any) => ({
          verificationResult: e.data.result,
          message: `Connection failed: ${e.data.result.reason || 'Unknown error'}`,
        })),
        setFailedFromError: assign((_c, e: any) => ({
          message: `Connection failed: ${e.data?.message || 'Unknown error'}`,
        })),
        setCapabilities: assign({ capabilities: (_c, e: any) => e.capabilities }),
        clear: assign({ verificationResult: (_c) => null, message: (_c) => undefined }),
        onSaved: assign({
          verificationResult: (_c) => null,
          message: (_c) => 'Saved. Click "Test Connection" to verify.',
        }),
      },
    },
  );
}

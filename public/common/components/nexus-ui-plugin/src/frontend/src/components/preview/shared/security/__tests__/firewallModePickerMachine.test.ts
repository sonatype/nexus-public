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

import { interpret } from 'xstate';
import { firewallModePickerMachine, createFirewallModePickerContext } from '../firewallModePickerMachine';
import { isFirewallPickerModeAllowed } from '../firewallModes';

const mockFetchStatus = jest.fn();
const mockFetchPccs = jest.fn();
jest.mock('../useFirewallEnable', () => ({
  fetchIqAuditStatus: (...args: unknown[]) => mockFetchStatus(...args),
  fetchPccsSupportedFormats: (...args: unknown[]) => mockFetchPccs(...args),
}));

const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

function start() {
  return interpret(
    firewallModePickerMachine.withContext(createFirewallModePickerContext('npm-proxy')),
  ).start();
}

describe('firewallModePickerMachine', () => {
  beforeEach(() => {
    mockFetchStatus.mockReset().mockResolvedValue({ repositoryName: 'npm-proxy', mode: 'AUDIT' });
    mockFetchPccs.mockReset().mockResolvedValue(['npm', 'pypi']);
  });

  it('shouldLoadCurrentModeBeforeBecomingReady', async () => {
    const service = start();
    expect(service.state.matches('loading')).toBe(true);
    await flush();
    expect(service.state.matches('ready')).toBe(true);
    expect(service.state.context.currentMode).toBe('AUDIT');
    expect(service.state.context.pccsFormats).toEqual(['npm', 'pypi']);
    service.stop();
  });

  it('shouldFailWithoutWritingWhenCurrentModeCannotBeRead', async () => {
    mockFetchStatus.mockResolvedValue(null);
    const service = start();
    await flush();
    expect(service.state.matches('failed')).toBe(true);
    expect(service.state.context.currentMode).toBeNull();
    service.stop();
  });

  it('shouldReloadModeOnRetryAfterFailure', async () => {
    mockFetchStatus.mockResolvedValueOnce(null);
    const service = start();
    await flush();
    service.send({ type: 'RETRY' });
    await flush();
    expect(service.state.matches('ready')).toBe(true);
    service.stop();
  });

  it('shouldRecordSelectedMode', async () => {
    const service = start();
    await flush();
    service.send({ type: 'SELECT', mode: 'QUARANTINE' });
    expect(service.state.context.selectedMode).toBe('QUARANTINE');
    service.stop();
  });
});

describe('isFirewallPickerModeAllowed', () => {
  it('shouldOfferPccsOnlyForSupportedFormats', () => {
    expect(isFirewallPickerModeAllowed('PCCS', 'npm', ['npm', 'pypi'])).toBe(true);
    expect(isFirewallPickerModeAllowed('PCCS', 'maven2', ['npm', 'pypi'])).toBe(false);
  });

  it('shouldAlwaysOfferAuditAndQuarantineButNeverDisabled', () => {
    expect(isFirewallPickerModeAllowed('AUDIT', 'maven2', [])).toBe(true);
    expect(isFirewallPickerModeAllowed('QUARANTINE', 'maven2', [])).toBe(true);
    expect(isFirewallPickerModeAllowed('DISABLED', 'maven2', [])).toBe(false);
  });
});

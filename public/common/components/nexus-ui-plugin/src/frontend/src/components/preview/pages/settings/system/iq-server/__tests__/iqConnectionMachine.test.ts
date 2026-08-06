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
import { waitFor } from 'xstate/lib/waitFor';
import Axios from 'axios';
import { createIqConnectionMachine, fetchCapabilities } from '../iqConnectionMachine';
import { DEFAULT_IQ_CONFIGURATION, DEFAULT_IQ_CAPABILITIES } from '../types';

jest.mock('axios');
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: () => ({ getValue: () => false }) },
}));
const mockedAxios = Axios as jest.Mocked<typeof Axios>;
const SETTINGS = { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', password: 'p' };

describe('fetchCapabilities', () => {
  beforeEach(() => jest.clearAllMocks());

  it('logs a warning and falls back to defaults when the request fails', async () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    mockedAxios.get.mockRejectedValueOnce(new Error('403 Forbidden'));

    const capabilities = await fetchCapabilities();

    expect(capabilities).toEqual(DEFAULT_IQ_CAPABILITIES);
    expect(warnSpy).toHaveBeenCalledWith('Could not load IQ Server capabilities:', expect.any(Error));
    warnSpy.mockRestore();
  });
});

describe('iqConnectionMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('AUTO_TEST success → connected', async () => {
    mockedAxios.post
      .mockResolvedValueOnce({ data: { reason: 'Connected v1.2.3' } }) // verify
      .mockResolvedValueOnce({ data: { hasFirewall: true } });         // capabilities-with-config
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'AUTO_TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    expect(service.getSnapshot().context.capabilities.hasFirewall).toBe(true);
    service.stop();
  });

  it('TEST success → connected', async () => {
    mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'ok' } }).mockResolvedValueOnce({ data: {} });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    service.stop();
  });

  it('verify failure → failed with message', async () => {
    mockedAxios.post.mockRejectedValueOnce({ response: { data: 'Authentication failed' } });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('failed'));
    expect(service.getSnapshot().context.message).toMatch(/Authentication failed/);
    service.stop();
  });

  it('verify failure with a ValidationErrorXO body → failed with just the message text, not the raw object', async () => {
    mockedAxios.post.mockRejectedValueOnce({
      response: { data: { id: '*', message: 'Error code 401: Invalid credentials. Please try again.' } },
    });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('failed'));
    expect(service.getSnapshot().context.message).toBe('Connection failed: Error code 401: Invalid credentials. Please try again.');
    expect(service.getSnapshot().context.verificationResult?.reason).toBe('Error code 401: Invalid credentials. Please try again.');
    service.stop();
  });

  it('RESET_CONNECTION returns to idle', async () => {
    mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'ok' } }).mockResolvedValueOnce({ data: {} });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    service.send({ type: 'RESET_CONNECTION' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    service.stop();
  });

  it('SAVED from connected → idle with "Saved..." message', async () => {
    mockedAxios.post
      .mockResolvedValueOnce({ data: { reason: 'ok' } })
      .mockResolvedValueOnce({ data: {} });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    service.send({ type: 'SAVED' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.message).toBe('Saved. Click "Test Connection" to verify.');
    service.stop();
  });

  it('re-TEST from connected uses the latest response', async () => {
    mockedAxios.post
      .mockResolvedValueOnce({ data: { reason: 'first v1.0.0' } })
      .mockResolvedValueOnce({ data: {} });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));

    mockedAxios.post
      .mockResolvedValueOnce({ data: { reason: 'second v2.0.0' } })
      .mockResolvedValueOnce({ data: {} });
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    // Message shows v2.0.0, proving second test's response was used
    expect(service.getSnapshot().context.message).toMatch(/v2\.0\.0/);
    service.stop();
  });

  it('AUTO_TEST from failed → connected (retry works)', async () => {
    // First: fail
    mockedAxios.post.mockRejectedValueOnce({ response: { data: 'Network error' } });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'AUTO_TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('failed'));

    // Retry: succeed
    mockedAxios.post
      .mockResolvedValueOnce({ data: { reason: 'ok' } })
      .mockResolvedValueOnce({ data: {} });
    service.send({ type: 'AUTO_TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('connected'));
    service.stop();
  });

  it('SET_CAPABILITIES updates context without axios call', async () => {
    const service = interpret(createIqConnectionMachine()).start();
    const postCallCount = mockedAxios.post.mock.calls.length;
    const getCallCount = mockedAxios.get.mock.calls.length;
    service.send({ type: 'SET_CAPABILITIES', capabilities: { hasFirewall: true, hasLifecycle: false } });
    expect(service.getSnapshot().context.capabilities.hasFirewall).toBe(true);
    expect(service.getSnapshot().context.capabilities.hasLifecycle).toBe(false);
    expect(mockedAxios.post.mock.calls.length).toBe(postCallCount);
    expect(mockedAxios.get.mock.calls.length).toBe(getCallCount);
    service.stop();
  });

  it('SAVED from idle → idle with "Saved..." message (regression: first-time save without test)', () => {
    const service = interpret(createIqConnectionMachine()).start();
    expect(service.getSnapshot().matches('idle')).toBe(true);
    service.send({ type: 'SAVED' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.message).toBe('Saved. Click "Test Connection" to verify.');
    service.stop();
  });

  it('SAVED from failed → idle with "Saved..." message and clears verificationResult', async () => {
    mockedAxios.post.mockRejectedValueOnce({ response: { data: 'Connection refused' } });
    const service = interpret(createIqConnectionMachine()).start();
    service.send({ type: 'TEST', settings: SETTINGS });
    await waitFor(service, (s) => s.matches('failed'));
    expect(service.getSnapshot().context.verificationResult).not.toBeNull();
    expect(service.getSnapshot().context.message).toMatch(/Connection refused/);
    service.send({ type: 'SAVED' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.message).toBe('Saved. Click "Test Connection" to verify.');
    expect(service.getSnapshot().context.verificationResult).toBeNull();
    service.stop();
  });
});

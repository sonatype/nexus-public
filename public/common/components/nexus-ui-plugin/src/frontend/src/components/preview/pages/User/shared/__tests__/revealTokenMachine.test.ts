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
import { createRevealTokenMachine } from '../revealTokenMachine';

describe('revealTokenMachine — user variant (with status + generate)', () => {
  const buildMachine = (overrides: Partial<Parameters<typeof createRevealTokenMachine>[0]['services']> = {}) => {
    return createRevealTokenMachine<{ enabled: boolean; hasToken: boolean }, { nameCode: string; passCode: string }>({
      tokenType: 'user',
      services: {
        checkTokenStatus: jest.fn().mockResolvedValue({ enabled: true, hasToken: true }),
        fetchToken: jest.fn().mockResolvedValue({ nameCode: 'nc', passCode: 'pc' }),
        generateToken: jest.fn().mockResolvedValue({ nameCode: 'newnc', passCode: 'newpc' }),
        deleteToken: jest.fn().mockResolvedValue(undefined),
        ...overrides,
      },
    });
  };

  it('starts in loading and transitions to idle with status populated', async () => {
    const machine = buildMachine();
    const service = interpret(machine).start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    await waitFor(service, (state) => state.matches('idle'));
    expect(service.getSnapshot().context.status).toEqual({ enabled: true, hasToken: true });

    service.stop();
  });

  it('transitions to loadError on status failure and retries on RETRY', async () => {
    const checkTokenStatus = jest
      .fn()
      .mockRejectedValueOnce(new Error('down'))
      .mockResolvedValueOnce({ enabled: true, hasToken: false });
    const machine = buildMachine({ checkTokenStatus });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loadError'));
    expect(service.getSnapshot().context.loadError).toMatch(/down/);

    service.send({ type: 'RETRY' });
    await waitFor(service, (state) => state.matches('idle'));
    expect(service.getSnapshot().context.status).toEqual({ enabled: true, hasToken: false });

    service.stop();
  });

  it('REVEAL fetches the token and transitions to revealed with value populated', async () => {
    const machine = buildMachine();
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('revealed'));
    expect(service.getSnapshot().context.tokenValue).toEqual({ nameCode: 'nc', passCode: 'pc' });

    service.stop();
  });

  it('REVEAL on failure returns to idle with actionError', async () => {
    const fetchToken = jest.fn().mockRejectedValue(new Error('auth failed'));
    const machine = buildMachine({ fetchToken });
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('idle') && state.context.actionError !== null);
    expect(service.getSnapshot().context.actionError).toMatch(/auth failed/);
    expect(service.getSnapshot().context.tokenValue).toBeNull();

    service.stop();
  });

  it('HIDE from revealed returns to idle and clears tokenValue', async () => {
    const machine = buildMachine();
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('revealed'));

    service.send({ type: 'HIDE' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.tokenValue).toBeNull();

    service.stop();
  });

  it('GENERATE flow: generate → refresh status → revealed', async () => {
    const generateToken = jest.fn().mockResolvedValue({ nameCode: 'newnc', passCode: 'newpc' });
    const checkTokenStatus = jest
      .fn()
      .mockResolvedValueOnce({ enabled: true, hasToken: false })
      .mockResolvedValueOnce({ enabled: true, hasToken: true });
    const machine = buildMachine({ generateToken, checkTokenStatus });
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'GENERATE' });
    await waitFor(service, (state) => state.matches('revealed'));
    expect(service.getSnapshot().context.tokenValue).toEqual({ nameCode: 'newnc', passCode: 'newpc' });
    expect(service.getSnapshot().context.status).toEqual({ enabled: true, hasToken: true });
    expect(generateToken).toHaveBeenCalledTimes(1);
    expect(checkTokenStatus).toHaveBeenCalledTimes(2);

    service.stop();
  });

  it('DELETE flow: delete → reload status → idle', async () => {
    const deleteToken = jest.fn().mockResolvedValue(undefined);
    const checkTokenStatus = jest
      .fn()
      .mockResolvedValueOnce({ enabled: true, hasToken: true })
      .mockResolvedValueOnce({ enabled: true, hasToken: false });
    const machine = buildMachine({ deleteToken, checkTokenStatus });
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'DELETE' });
    await waitFor(
      service,
      (state) => state.matches('idle') && state.context.status?.hasToken === false
    );
    expect(deleteToken).toHaveBeenCalledTimes(1);

    service.stop();
  });

  it('DELETE failure surfaces actionError and stays in idle with token status unchanged', async () => {
    const deleteToken = jest.fn().mockRejectedValue(new Error('cannot delete'));
    const machine = buildMachine({ deleteToken });
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'DELETE' });
    await waitFor(service, (state) => state.matches('idle') && state.context.actionError !== null);
    expect(service.getSnapshot().context.actionError).toMatch(/cannot delete/);

    service.stop();
  });

  it('CLEAR_ERROR clears the actionError while in idle', async () => {
    const fetchToken = jest.fn().mockRejectedValue(new Error('bad auth'));
    const machine = buildMachine({ fetchToken });
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('idle'));

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('idle') && state.context.actionError !== null);

    service.send({ type: 'CLEAR_ERROR' });
    expect(service.getSnapshot().context.actionError).toBeNull();

    service.stop();
  });
});

describe('revealTokenMachine — nuget variant (no status, no generate)', () => {
  const buildMachine = (overrides: Partial<Parameters<typeof createRevealTokenMachine>[0]['services']> = {}) => {
    return createRevealTokenMachine<null, { apiKey: string }>({
      tokenType: 'nuget',
      services: {
        fetchToken: jest.fn().mockResolvedValue({ apiKey: 'k' }),
        deleteToken: jest.fn().mockResolvedValue(undefined),
        ...overrides,
      },
    });
  };

  it('starts directly in idle when no checkTokenStatus is provided', () => {
    const machine = buildMachine();
    const service = interpret(machine).start();

    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.status).toBeNull();

    service.stop();
  });

  it('REVEAL fetches and reveals a token', async () => {
    const machine = buildMachine();
    const service = interpret(machine).start();

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('revealed'));
    expect(service.getSnapshot().context.tokenValue).toEqual({ apiKey: 'k' });

    service.stop();
  });

  it('does not expose the generating state when generateToken is not configured', () => {
    const machine = buildMachine();
    const service = interpret(machine).start();

    // Sending GENERATE on the nuget variant is a no-op (event not handled).
    service.send({ type: 'GENERATE' });
    expect(service.getSnapshot().matches('idle')).toBe(true);
    expect(service.getSnapshot().context.tokenValue).toBeNull();

    service.stop();
  });

  it('DELETE returns to idle immediately (no reload) when no status service is configured', async () => {
    const deleteToken = jest.fn().mockResolvedValue(undefined);
    const machine = buildMachine({ deleteToken });
    const service = interpret(machine).start();

    service.send({ type: 'DELETE' });
    await waitFor(service, (state) => state.matches('idle') && deleteToken.mock.calls.length === 1);
    expect(service.getSnapshot().context.tokenValue).toBeNull();

    service.stop();
  });

  it('DELETE from revealed clears the tokenValue and returns to idle', async () => {
    const machine = buildMachine();
    const service = interpret(machine).start();

    service.send({ type: 'REVEAL' });
    await waitFor(service, (state) => state.matches('revealed'));
    expect(service.getSnapshot().context.tokenValue).toEqual({ apiKey: 'k' });

    service.send({ type: 'DELETE' });
    await waitFor(service, (state) => state.matches('idle'));
    expect(service.getSnapshot().context.tokenValue).toBeNull();

    service.stop();
  });
});

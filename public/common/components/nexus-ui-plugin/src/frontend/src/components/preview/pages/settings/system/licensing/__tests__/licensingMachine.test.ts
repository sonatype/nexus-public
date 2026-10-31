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
import { createLicensingMachine } from '../licensingMachine';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
}));
const { restClient } = jest.requireMock('../../../../../../../interface/api');

describe('licensingMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('loads the license and lands in loaded', async () => {
    restClient.get.mockResolvedValue({ contactCompany: 'Acme', maxRepoRequests: 10, maxRepoComponents: 5 });
    const service = interpret(createLicensingMachine()).start();

    expect(service.getSnapshot().matches('loading')).toBe(true);
    await waitFor(service, (s) => s.matches('loaded'));

    expect(service.getSnapshot().context.license.contactCompany).toBe('Acme');
    expect(service.getSnapshot().context.loadError).toBeNull();
    expect(service.getSnapshot().context.activeTab).toBe('license');

    service.stop();
  });

  it('treats HTTP 402 as no-license (empty, no error)', async () => {
    restClient.get.mockRejectedValue({ response: { status: 402 } });
    const service = interpret(createLicensingMachine()).start();

    await waitFor(service, (s) => s.matches('loaded'));
    expect(service.getSnapshot().context.license).toEqual({});
    expect(service.getSnapshot().context.loadError).toBeNull();

    service.stop();
  });

  it('sets loadError on a non-402 failure but still lands in loaded', async () => {
    restClient.get.mockRejectedValue(new Error('network down'));
    const service = interpret(createLicensingMachine()).start();

    await waitFor(service, (s) => s.matches('loaded') && s.context.loadError !== null);
    expect(service.getSnapshot().context.loadError).toBe('Failed to load license information');
    expect(service.getSnapshot().context.license).toEqual({});

    service.stop();
  });

  it('updates the active tab via SET_TAB', async () => {
    restClient.get.mockResolvedValue({});
    const service = interpret(createLicensingMachine()).start();
    await waitFor(service, (s) => s.matches('loaded'));

    service.send({ type: 'SET_TAB', tab: 'usage' });
    expect(service.getSnapshot().context.activeTab).toBe('usage');

    service.stop();
  });

  it('LICENSE_INSTALLED replaces the license, clears error, and keeps the tab', async () => {
    restClient.get.mockRejectedValue(new Error('network down'));
    const service = interpret(createLicensingMachine()).start();
    await waitFor(service, (s) => s.matches('loaded') && s.context.loadError !== null);

    service.send({ type: 'SET_TAB', tab: 'usage' });
    service.send({ type: 'LICENSE_INSTALLED', license: { contactCompany: 'NewCo' } });

    const ctx = service.getSnapshot().context;
    expect(ctx.license.contactCompany).toBe('NewCo');
    expect(ctx.loadError).toBeNull();
    expect(ctx.activeTab).toBe('usage'); // unchanged by install

    service.stop();
  });

  it('LICENSE_INSTALLED replaces the license when already loaded without error', async () => {
    restClient.get.mockResolvedValue({ contactCompany: 'OldCo' });
    const service = interpret(createLicensingMachine()).start();
    await waitFor(service, (s) => s.matches('loaded'));

    service.send({ type: 'LICENSE_INSTALLED', license: { contactCompany: 'NewCo' } });

    const ctx = service.getSnapshot().context;
    expect(ctx.license.contactCompany).toBe('NewCo');
    expect(ctx.loadError).toBeNull();

    service.stop();
  });

  it('DISMISS_ERROR clears the load error', async () => {
    restClient.get.mockRejectedValue(new Error('network down'));
    const service = interpret(createLicensingMachine()).start();
    await waitFor(service, (s) => s.matches('loaded') && s.context.loadError !== null);

    service.send({ type: 'DISMISS_ERROR' });
    expect(service.getSnapshot().context.loadError).toBeNull();

    service.stop();
  });
});

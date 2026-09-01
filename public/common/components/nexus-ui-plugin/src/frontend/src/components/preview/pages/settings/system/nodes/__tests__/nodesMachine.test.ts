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
import { createNodesMachine } from '../nodesMachine';

jest.mock('../nodesApi', () => ({ fetchNodes: jest.fn() }));
const { fetchNodes } = jest.requireMock('../nodesApi');

const NODES = [
  { name: 'uuid-1', displayName: 'Primary', local: true },
  { name: 'uuid-2', displayName: 'Secondary', local: false },
];

describe('nodesMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('starts in loading then transitions to loaded with nodes', async () => {
    fetchNodes.mockResolvedValue(NODES);
    const service = interpret(createNodesMachine()).start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    await waitFor(service, (s) => s.matches('loaded'));
    expect(service.getSnapshot().context.nodes).toEqual(NODES);
    expect(service.getSnapshot().context.loadError).toBeNull();

    service.stop();
  });

  it('transitions to error with the failure message', async () => {
    fetchNodes.mockRejectedValue(new Error('Network error'));
    const service = interpret(createNodesMachine()).start();

    await waitFor(service, (s) => s.matches('error'));
    expect(service.getSnapshot().context.loadError).toBe('Network error');

    service.stop();
  });

  it('retries from error on RETRY', async () => {
    fetchNodes.mockRejectedValueOnce(new Error('Network error'));
    fetchNodes.mockResolvedValueOnce(NODES);
    const service = interpret(createNodesMachine()).start();

    await waitFor(service, (s) => s.matches('error'));
    service.send({ type: 'RETRY' });

    await waitFor(service, (s) => s.matches('loaded'));
    expect(service.getSnapshot().context.nodes).toEqual(NODES);
    expect(service.getSnapshot().context.loadError).toBeNull();

    service.stop();
  });

  it('re-fetches from loaded on REFRESH', async () => {
    fetchNodes.mockResolvedValue(NODES);
    const service = interpret(createNodesMachine()).start();

    await waitFor(service, (s) => s.matches('loaded'));
    service.send({ type: 'REFRESH' });
    expect(service.getSnapshot().matches('loading')).toBe(true);

    await waitFor(service, (s) => s.matches('loaded'));
    expect(fetchNodes).toHaveBeenCalledTimes(2);

    service.stop();
  });

  it('clears stale nodes when a refresh fails', async () => {
    fetchNodes.mockResolvedValueOnce(NODES);
    fetchNodes.mockRejectedValueOnce(new Error('Network error'));
    const service = interpret(createNodesMachine()).start();

    await waitFor(service, (s) => s.matches('loaded'));
    expect(service.getSnapshot().context.nodes).toEqual(NODES);

    service.send({ type: 'REFRESH' });

    await waitFor(service, (s) => s.matches('error'));
    expect(service.getSnapshot().context.nodes).toEqual([]);
    expect(service.getSnapshot().context.loadError).toBe('Network error');

    service.stop();
  });
});

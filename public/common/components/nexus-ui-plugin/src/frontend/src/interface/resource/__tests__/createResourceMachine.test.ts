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
import { createResourceMachine, toResourceError, DEFAULT_RESOURCE_ERROR_MESSAGE } from '../createResourceMachine';

function waitForState(
  service: ReturnType<typeof interpret>,
  matcher: (state: { matches: (value: unknown) => boolean }) => boolean,
): Promise<void> {
  return new Promise((resolve) => {
    if (matcher(service.state as never)) {
      resolve();
      return;
    }
    const subscription = service.subscribe((state) => {
      if (matcher(state)) {
        subscription.unsubscribe();
        resolve();
      }
    });
  });
}

describe('createResourceMachine', () => {
  it('starts in loading state with initialData', () => {
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => new Promise(() => {}),
      }),
    ).start();
    expect(service.state.matches('loading')).toBe(true);
    expect(service.state.context.data).toEqual([]);
    expect(service.state.context.error).toBeNull();
    service.stop();
  });

  it('transitions loading -> loaded on fetch success and assigns data', async () => {
    const fetched = ['a', 'b', 'c'];
    const service = interpret(
      createResourceMachine<string[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.resolve(fetched),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('loaded'));
    expect(service.state.context.data).toEqual(fetched);
    expect(service.state.context.error).toBeNull();
    service.stop();
  });

  it('transitions loading -> errored on fetch failure and records a structured error', async () => {
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.reject(new Error('boom')),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('errored'));
    expect(service.state.context.error).toEqual({ status: 0, message: 'boom' });
    service.stop();
  });

  it('preserves ThrownApiError-shaped fields (status/code/fieldErrors/requestId)', async () => {
    const thrown = Object.assign(new Error('Bad request'), {
      status: 400,
      code: 'VALIDATION_ERROR',
      fieldErrors: [{ field: 'name', message: 'Name is required' }],
      requestId: 'req-42',
    });
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.reject(thrown),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('errored'));
    expect(service.state.context.error).toEqual({
      status: 400,
      message: 'Bad request',
      code: 'VALIDATION_ERROR',
      fieldErrors: [{ field: 'name', message: 'Name is required' }],
      requestId: 'req-42',
    });
    service.stop();
  });

  it('RETRY re-enters loading from errored', async () => {
    let calls = 0;
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => {
          calls += 1;
          return calls === 1 ? Promise.reject(new Error('boom')) : Promise.resolve([42]);
        },
      }),
    ).start();
    await waitForState(service, (state) => state.matches('errored'));
    service.send({ type: 'RETRY' });
    await waitForState(service, (state) => state.matches('loaded'));
    expect(service.state.context.data).toEqual([42]);
    expect(service.state.context.error).toBeNull();
    service.stop();
  });

  it('REFRESH re-enters loading from loaded', async () => {
    let calls = 0;
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.resolve([++calls]),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('loaded'));
    expect(service.state.context.data).toEqual([1]);
    service.send({ type: 'REFRESH' });
    await waitForState(
      service,
      (state) => state.matches('loaded') && (state as any).context.data[0] === 2,
    );
    expect(service.state.context.data).toEqual([2]);
    service.stop();
  });

  it('uses the string rejection value as the message when the throw is a plain string', async () => {
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.reject('string-rejection'),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('errored'));
    expect(service.state.context.error).toEqual({ status: 0, message: 'string-rejection' });
    service.stop();
  });

  it('falls back to the default message when the rejection has no useful shape', async () => {
    const service = interpret(
      createResourceMachine<number[]>({
        id: 'test',
        initialData: [],
        fetch: () => Promise.reject(undefined),
      }),
    ).start();
    await waitForState(service, (state) => state.matches('errored'));
    expect(service.state.context.error).toEqual({
      status: 0,
      message: DEFAULT_RESOURCE_ERROR_MESSAGE,
    });
    service.stop();
  });
});

describe('toResourceError', () => {
  it('extracts the ThrownApiError fields when present', () => {
    const thrown = Object.assign(new Error('nope'), {
      status: 404,
      code: 'NOT_FOUND',
      requestId: 'req-1',
    });
    expect(toResourceError(thrown)).toEqual({
      status: 404,
      message: 'nope',
      code: 'NOT_FOUND',
      requestId: 'req-1',
    });
  });

  it('omits optional fields when the source values are missing or wrong-typed', () => {
    const thrown = new Error('plain');
    const result = toResourceError(thrown);
    expect(result).toEqual({ status: 0, message: 'plain' });
    expect(Object.prototype.hasOwnProperty.call(result, 'code')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(result, 'fieldErrors')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(result, 'requestId')).toBe(false);
  });

  it('returns a default-message error for null / undefined', () => {
    expect(toResourceError(null)).toEqual({ status: 0, message: DEFAULT_RESOURCE_ERROR_MESSAGE });
    expect(toResourceError(undefined)).toEqual({ status: 0, message: DEFAULT_RESOURCE_ERROR_MESSAGE });
  });
});

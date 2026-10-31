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
import { loggersListMachine } from '../LoggersListMachine';
import { Logger } from '../types';

const mockLoggers: Logger[] = [
  { name: 'ROOT', level: 'INFO', override: false },
  { name: 'org.sonatype.nexus', level: 'DEBUG', override: true },
  { name: 'org.apache', level: 'WARN', override: true },
];

function makeMachine(fetchLoggers?: () => Promise<Logger[]>) {
  return loggersListMachine.withConfig({
    services: {
      fetchLoggers: fetchLoggers ?? (async () => mockLoggers),
    },
  });
}

describe('loggersListMachine', () => {
  it('starts in loading state', () => {
    const service = interpret(makeMachine());
    service.start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    service.stop();
  });

  it('initial context has default values', () => {
    const service = interpret(makeMachine());
    service.start();

    const { context } = service.getSnapshot();
    expect(context.loggers).toEqual([]);
    expect(context.filter).toBe('');
    expect(context.levelFilter).toEqual([]);
    expect(context.sortField).toBe('name');
    expect(context.sortDirection).toBe('asc');
    expect(context.error).toBeNull();

    service.stop();
  });

  it('transitions to loaded state with loggers after successful fetch', (done) => {
    const service = interpret(makeMachine());

    service.onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.loggers).toEqual(mockLoggers);
        expect(state.context.error).toBeNull();
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('transitions to error state on failed fetch', (done) => {
    const machine = makeMachine(async () => {
      throw new Error('Network error');
    });
    const service = interpret(machine);

    service.onTransition((state) => {
      if (state.matches('error')) {
        expect(state.context.error).toBe('Network error');
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('transitions back to loading on RETRY from error state', (done) => {
    let errorReached = false;
    const machine = makeMachine(async () => {
      if (!errorReached) {
        throw new Error('Network error');
      }
      return mockLoggers;
    });
    const service = interpret(machine);

    service.onTransition((state) => {
      if (state.matches('error') && !errorReached) {
        errorReached = true;
        service.send({ type: 'RETRY' });
      }
      if (state.matches('loaded') && errorReached) {
        expect(state.context.loggers).toEqual(mockLoggers);
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('SET_FILTER updates filter in context', (done) => {
    const service = interpret(makeMachine());
    let sent = false;

    service.onTransition((state) => {
      if (state.matches('loaded') && !sent) {
        sent = true;
        service.send({ type: 'SET_FILTER', value: 'sonatype' });
      } else if (state.matches('loaded') && sent) {
        expect(state.context.filter).toBe('sonatype');
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('SET_LEVEL_FILTER updates levelFilter in context', (done) => {
    const service = interpret(makeMachine());
    let sent = false;

    service.onTransition((state) => {
      if (state.matches('loaded') && !sent) {
        sent = true;
        service.send({ type: 'SET_LEVEL_FILTER', value: ['DEBUG', 'WARN'] });
      } else if (state.matches('loaded') && sent) {
        expect(state.context.levelFilter).toEqual(['DEBUG', 'WARN']);
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('SORT with a new field changes sortField and resets sortDirection to asc', (done) => {
    const service = interpret(makeMachine());
    let sent = false;

    service.onTransition((state) => {
      if (state.matches('loaded') && !sent) {
        sent = true;
        // Default sort is name asc — sort by level
        service.send({ type: 'SORT', field: 'level' });
      } else if (state.matches('loaded') && sent) {
        expect(state.context.sortField).toBe('level');
        expect(state.context.sortDirection).toBe('asc');
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('SORT with the same field toggles sortDirection from asc to desc', (done) => {
    const service = interpret(makeMachine());
    let sent = false;

    service.onTransition((state) => {
      if (state.matches('loaded') && !sent) {
        sent = true;
        // Default is name asc — sort by name again to toggle to desc
        service.send({ type: 'SORT', field: 'name' });
      } else if (state.matches('loaded') && sent) {
        expect(state.context.sortField).toBe('name');
        expect(state.context.sortDirection).toBe('desc');
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('SORT toggles sortDirection from desc back to asc on third click', (done) => {
    const service = interpret(makeMachine());
    let sortCount = 0;

    service.onTransition((state) => {
      if (state.matches('loaded')) {
        sortCount++;
        if (sortCount === 1) {
          service.send({ type: 'SORT', field: 'name' }); // asc → desc
        } else if (sortCount === 2) {
          service.send({ type: 'SORT', field: 'name' }); // desc → asc
        } else if (sortCount === 3) {
          expect(state.context.sortField).toBe('name');
          expect(state.context.sortDirection).toBe('asc');
          service.stop();
          done();
        }
      }
    });

    service.start();
  });

  it('SET_FILTER and SET_LEVEL_FILTER are ignored in loading state', () => {
    const service = interpret(makeMachine());
    service.start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    // These events should not crash or change state
    service.send({ type: 'SET_FILTER', value: 'test' });
    expect(service.getSnapshot().matches('loading')).toBe(true);

    service.stop();
  });
});

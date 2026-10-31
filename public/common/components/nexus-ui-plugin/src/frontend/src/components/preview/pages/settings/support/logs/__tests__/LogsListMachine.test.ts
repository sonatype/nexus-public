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
import { logsListMachine } from '../LogsListMachine';

const mockLogs = [
  { fileName: 'nexus.log', size: 1048576, lastModified: 1704110400000 },
  { fileName: 'request.log', size: 524288, lastModified: 1704024000000 },
  { fileName: 'audit.log', size: 262144, lastModified: 1703937600000 },
];

describe('LogsListMachine', () => {
  describe('initial state', () => {
    it('starts in the loading state', () => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => [] },
      });
      const service = interpret(machine);
      service.start();
      expect(service.state.matches('loading')).toBe(true);
      service.stop();
    });

    it('has correct initial context values', () => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => [] },
      });
      const service = interpret(machine);
      service.start();
      const { logs, filter, sortField, sortDirection, error } = service.state.context;
      expect(logs).toEqual([]);
      expect(filter).toBe('');
      expect(sortField).toBe('fileName');
      expect(sortDirection).toBe('asc');
      expect(error).toBeNull();
      service.stop();
    });
  });

  describe('data loading', () => {
    it('transitions to loaded state after successful fetch', (done) => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => mockLogs },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.logs).toEqual(mockLogs);
          expect(state.context.error).toBeNull();
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('transitions to error state on failed fetch', (done) => {
      const machine = logsListMachine.withConfig({
        services: {
          fetchLogs: async () => {
            throw new Error('Network error');
          },
        },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('error')) {
          expect(state.context.error).toBeTruthy();
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('RETRY event', () => {
    it('transitions from error back to loading on RETRY', (done) => {
      let attempt = 0;
      const machine = logsListMachine.withConfig({
        services: {
          fetchLogs: async () => {
            attempt++;
            if (attempt === 1) throw new Error('First attempt failed');
            return mockLogs;
          },
        },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('error') && attempt === 1) {
          service.send({ type: 'RETRY' });
        } else if (state.matches('loaded') && attempt === 2) {
          expect(state.context.logs).toEqual(mockLogs);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('SET_FILTER event', () => {
    it('updates filter in context while staying in loaded state', (done) => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => mockLogs },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.filter === 'nexus') {
          expect(state.context.filter).toBe('nexus');
          expect(state.matches('loaded')).toBe(true);
          service.stop();
          done();
        } else if (state.matches('loaded') && state.context.filter === '') {
          service.send({ type: 'SET_FILTER', value: 'nexus' });
        }
      });

      service.start();
    });
  });

  describe('SORT event', () => {
    it('updates sortField in context', (done) => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => mockLogs },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.sortField === 'size') {
          expect(state.context.sortField).toBe('size');
          service.stop();
          done();
        } else if (state.matches('loaded') && state.context.sortField === 'fileName') {
          service.send({ type: 'SORT', field: 'size' });
        }
      });

      service.start();
    });

    it('toggles sort direction when sorting by the same field', (done) => {
      const machine = logsListMachine.withConfig({
        services: { fetchLogs: async () => mockLogs },
      });
      const service = interpret(machine);
      let sortedOnce = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sortedOnce && state.context.sortDirection === 'asc') {
          sortedOnce = true;
          service.send({ type: 'SORT', field: 'fileName' });
        } else if (state.matches('loaded') && sortedOnce && state.context.sortDirection === 'desc') {
          expect(state.context.sortField).toBe('fileName');
          expect(state.context.sortDirection).toBe('desc');
          service.stop();
          done();
        }
      });

      service.start();
    });
  });
});

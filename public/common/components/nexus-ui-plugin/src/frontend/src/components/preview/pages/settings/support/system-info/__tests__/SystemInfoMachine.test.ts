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
import { systemInfoMachine, SECTION_ORDER } from '../SystemInfoMachine';
import { SystemInformation, HASystemInformation, HANode } from '../types';

const mockSystemInfo: SystemInformation = {
  'nexus-status': { version: '3.88.0-01', edition: 'PRO' },
  'nexus-node': { nodeId: 'node-1', clustered: false },
};

const mockNodes: HANode[] = [{ nodeId: 'node-1', local: true }];

interface FetchResult {
  systemInfo: SystemInformation | null;
  haSystemInfo: HASystemInformation | null;
  nodes: HANode[];
  selectedNode: string | null;
  isHAMode: boolean;
}

function makeMachine(serviceOverrides: {
  fetchData?: () => Promise<FetchResult>;
  refreshData?: () => Promise<FetchResult>;
} = {}) {
  return systemInfoMachine.withConfig({
    services: {
      fetchData:
        serviceOverrides.fetchData ??
        (async () => ({
          systemInfo: mockSystemInfo,
          haSystemInfo: null,
          nodes: mockNodes,
          selectedNode: null,
          isHAMode: false,
        })),
      refreshData:
        serviceOverrides.refreshData ??
        (async () => ({
          systemInfo: mockSystemInfo,
          haSystemInfo: null,
          nodes: mockNodes,
          selectedNode: null,
          isHAMode: false,
        })),
    },
  });
}

describe('systemInfoMachine', () => {
  it('starts in loading state', () => {
    const service = interpret(makeMachine());
    service.start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    service.stop();
  });

  it('initial context has default expandedSections (first 3 SECTION_ORDER entries)', () => {
    const service = interpret(makeMachine());
    service.start();

    const { context } = service.getSnapshot();
    expect(context.expandedSections).toEqual(SECTION_ORDER.slice(0, 3));
    expect(context.systemInfo).toBeNull();
    expect(context.isHAMode).toBe(false);
    expect(context.error).toBeNull();

    service.stop();
  });

  it('transitions to loaded state with data after successful fetch', (done) => {
    const service = interpret(makeMachine());

    service.onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.systemInfo).toEqual(mockSystemInfo);
        expect(state.context.isHAMode).toBe(false);
        expect(state.context.error).toBeNull();
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('transitions to loadError on failed fetch', (done) => {
    const machine = makeMachine({
      fetchData: async () => {
        throw new Error('Network error');
      },
    });
    const service = interpret(machine);

    service.onTransition((state) => {
      if (state.matches('loadError')) {
        expect(state.context.error).toBe('Network error');
        service.stop();
        done();
      }
    });

    service.start();
  });

  it('RETRY from loadError transitions back to loading and retries fetch', (done) => {
    let errorReached = false;
    const machine = makeMachine({
      fetchData: async () => {
        if (!errorReached) {
          throw new Error('Network error');
        }
        return {
          systemInfo: mockSystemInfo,
          haSystemInfo: null,
          nodes: mockNodes,
          selectedNode: null,
          isHAMode: false,
        };
      },
    });
    const service = interpret(machine);

    service.onTransition((state) => {
      if (state.matches('loadError') && !errorReached) {
        errorReached = true;
        service.send({ type: 'RETRY' });
      }
      if (state.matches('loaded') && errorReached) {
        expect(state.context.systemInfo).toEqual(mockSystemInfo);
        service.stop();
        done();
      }
    });

    service.start();
  });

  describe('loaded state — HA fetch result', () => {
    const mockHANodes: HANode[] = [
      { nodeId: 'node-1', local: true },
      { nodeId: 'node-2', local: false },
    ];
    const mockHASystemInfo: HASystemInformation = {
      'node-1': mockSystemInfo,
      'node-2': { 'nexus-status': { version: '3.88.0-01' } },
    };

    it('stores HA data in context when isHAMode is true', (done) => {
      const machine = makeMachine({
        fetchData: async () => ({
          systemInfo: mockHASystemInfo['node-1'],
          haSystemInfo: mockHASystemInfo,
          nodes: mockHANodes,
          selectedNode: 'node-1',
          isHAMode: true,
        }),
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.isHAMode).toBe(true);
          expect(state.context.nodes).toEqual(mockHANodes);
          expect(state.context.selectedNode).toBe('node-1');
          expect(state.context.haSystemInfo).toEqual(mockHASystemInfo);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('SELECT_NODE', () => {
    it('updates selectedNode and systemInfo from haSystemInfo', (done) => {
      const mockHANodes: HANode[] = [
        { nodeId: 'node-1', local: true },
        { nodeId: 'node-2', local: false },
      ];
      const mockHASystemInfo: HASystemInformation = {
        'node-1': mockSystemInfo,
        'node-2': { 'nexus-status': { version: '3.88.0-02' } },
      };

      const machine = makeMachine({
        fetchData: async () => ({
          systemInfo: mockHASystemInfo['node-1'],
          haSystemInfo: mockHASystemInfo,
          nodes: mockHANodes,
          selectedNode: 'node-1',
          isHAMode: true,
        }),
      });
      const service = interpret(machine);
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          service.send({ type: 'SELECT_NODE', nodeId: 'node-2' });
        } else if (state.matches('loaded') && sent) {
          expect(state.context.selectedNode).toBe('node-2');
          expect(state.context.systemInfo).toEqual(mockHASystemInfo['node-2']);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('TOGGLE_SECTION', () => {
    it('adds a section key to expandedSections when open=true', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          service.send({ type: 'TOGGLE_SECTION', sectionKey: 'system-runtime', open: true });
        } else if (state.matches('loaded') && sent) {
          expect(state.context.expandedSections).toContain('system-runtime');
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('removes a section key from expandedSections when open=false', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          // nexus-status is in default expandedSections — collapse it
          service.send({ type: 'TOGGLE_SECTION', sectionKey: 'nexus-status', open: false });
        } else if (state.matches('loaded') && sent) {
          expect(state.context.expandedSections).not.toContain('nexus-status');
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('TOGGLE_SECTION with open=true is a no-op when section is already expanded (idempotent)', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          // nexus-status is already in default expandedSections — toggle open again
          service.send({ type: 'TOGGLE_SECTION', sectionKey: 'nexus-status', open: true });
        } else if (state.matches('loaded') && sent) {
          const count = state.context.expandedSections.filter((k) => k === 'nexus-status').length;
          expect(count).toBe(1);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('EXPAND_ALL / COLLAPSE_ALL', () => {
    it('EXPAND_ALL sets expandedSections to all keys in systemInfo', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          service.send({ type: 'EXPAND_ALL' });
        } else if (state.matches('loaded') && sent) {
          const expected = Object.keys(mockSystemInfo);
          expect(state.context.expandedSections).toEqual(expect.arrayContaining(expected));
          expect(state.context.expandedSections).toHaveLength(expected.length);
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('COLLAPSE_ALL clears expandedSections', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          service.send({ type: 'COLLAPSE_ALL' });
        } else if (state.matches('loaded') && sent) {
          expect(state.context.expandedSections).toEqual([]);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('EXPAND_ALL', () => {
    it('is a no-op when systemInfo is null', () => {
      // Machine starts in loading; snapshot has null systemInfo — expandedSections should be unchanged
      const service = interpret(makeMachine());
      service.start();

      const snapshot = service.getSnapshot();
      // In loading state, systemInfo is null
      expect(snapshot.context.systemInfo).toBeNull();
      const before = snapshot.context.expandedSections;

      // expandAll action guards on systemInfo being non-null, so sending EXPAND_ALL while loading
      // (where EXPAND_ALL is not a valid event) won't fire, but we can test the guard logic
      // directly by checking the loaded state after a null-systemInfo fetch result
      service.stop();

      // Verify via a machine whose fetch returns null systemInfo
      const nullInfoMachine = makeMachine({
        fetchData: async () => ({
          systemInfo: null,
          haSystemInfo: null,
          nodes: mockNodes,
          selectedNode: null,
          isHAMode: false,
        }),
      });
      const service2 = interpret(nullInfoMachine);
      let sent = false;

      return new Promise<void>((resolve) => {
        service2.onTransition((state) => {
          if (state.matches('loaded') && !sent) {
            sent = true;
            const expandedBefore = [...state.context.expandedSections];
            service2.send({ type: 'EXPAND_ALL' });
            // Immediately after send, check next transition
          } else if (state.matches('loaded') && sent) {
            // expandAll with null systemInfo returns context.expandedSections unchanged
            expect(state.context.expandedSections).toEqual(SECTION_ORDER.slice(0, 3));
            service2.stop();
            resolve();
          }
        });
        service2.start();
      });
    });
  });

  describe('REFRESH', () => {
    it('REFRESH from loaded transitions to refreshing', (done) => {
      // Use a slow refresh service so we can observe the refreshing state
      const machine = makeMachine({
        refreshData: () => new Promise(() => {}),
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          service.send({ type: 'REFRESH' });
        }
        if (state.matches('refreshing')) {
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('successful refresh transitions back to loaded with updated data', (done) => {
      const updatedInfo: SystemInformation = {
        'nexus-status': { version: '3.89.0-01' },
      };
      let refreshed = false;

      const machine = makeMachine({
        refreshData: async () => ({
          systemInfo: updatedInfo,
          haSystemInfo: null,
          nodes: mockNodes,
          selectedNode: null,
          isHAMode: false,
        }),
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && !refreshed) {
          refreshed = true;
          service.send({ type: 'REFRESH' });
        } else if (state.matches('loaded') && refreshed) {
          expect(state.context.systemInfo).toEqual(updatedInfo);
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('failed refresh transitions back to loaded with error in context', (done) => {
      let refreshed = false;

      const machine = makeMachine({
        refreshData: async () => {
          throw new Error('Refresh failed');
        },
      });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && !refreshed) {
          refreshed = true;
          service.send({ type: 'REFRESH' });
        } else if (state.matches('loaded') && refreshed && state.context.error) {
          expect(state.context.error).toBe('Refresh failed');
          // Original systemInfo still intact
          expect(state.context.systemInfo).toEqual(mockSystemInfo);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('SET_ERROR / CLEAR_ERROR', () => {
    it('SET_ERROR sets error in context while staying in loaded state', (done) => {
      const service = interpret(makeMachine());
      let sent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !sent) {
          sent = true;
          service.send({ type: 'SET_ERROR', message: 'Failed to copy to clipboard' });
        } else if (state.matches('loaded') && sent) {
          expect(state.context.error).toBe('Failed to copy to clipboard');
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('CLEAR_ERROR clears error in context', (done) => {
      const service = interpret(makeMachine());
      let setErrorSent = false;
      let clearSent = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !setErrorSent) {
          setErrorSent = true;
          service.send({ type: 'SET_ERROR', message: 'Some error' });
        } else if (state.matches('loaded') && setErrorSent && state.context.error && !clearSent) {
          clearSent = true;
          service.send({ type: 'CLEAR_ERROR' });
        } else if (state.matches('loaded') && clearSent && !state.context.error) {
          expect(state.context.error).toBeNull();
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('expandedSections preserved across refresh', () => {
    it('EXPAND_ALL sections are still expanded after a successful refresh', (done) => {
      const service = interpret(makeMachine());
      let expandedSent = false;
      let refreshed = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !expandedSent) {
          expandedSent = true;
          service.send({ type: 'EXPAND_ALL' });
        } else if (state.matches('loaded') && expandedSent && !refreshed) {
          // After EXPAND_ALL fired — now refresh
          refreshed = true;
          service.send({ type: 'REFRESH' });
        } else if (state.matches('loaded') && refreshed) {
          // expandedSections should be preserved (setData doesn't touch expandedSections)
          const expected = Object.keys(mockSystemInfo);
          expect(state.context.expandedSections).toEqual(expect.arrayContaining(expected));
          service.stop();
          done();
        }
      });

      service.start();
    });
  });
});

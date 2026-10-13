/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {interpret} from 'xstate';
import {waitFor} from 'xstate/lib/waitFor';
import Axios from 'axios';

import IqServerConnectedMachine, {buildIqServerConnectedMachine} from './IqServerConnectedMachine';

jest.mock('axios');

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn()
  }
}));

describe('IqServerConnectedMachine', () => {
  let service;

  afterEach(() => {
    if (service) {
      service.stop();
    }
    jest.clearAllMocks();
  });

  describe('fetchData', () => {
    it('fetches IQ configuration and transitions to verifyingConnection', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [
          {id: 'lifecycle', url: '/ui/links/lifecycle'},
          {id: 'firewall', url: '/ui/links/firewall'}
        ]
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(service.state.context.data).toEqual({
        lifecycle: true,
        firewall: true
      });
      expect(service.state.context.iqServerUrl).toBe('http://localhost:8070');
    });

    it('handles fetch error and transitions to loaded with error', async () => {
      const error = new Error('Network error');
      Axios.get.mockRejectedValue(error);

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.error).toBe(error);
      expect(service.state.context.data).toEqual({
        lifecycle: false,
        firewall: false
      });
    });

    it('parses licensed solutions correctly with only lifecycle', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [
          {id: 'lifecycle', url: '/ui/links/lifecycle'}
        ]
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.data).toEqual({
        lifecycle: true,
        firewall: false
      });
    });

    it('parses licensed solutions correctly with no solutions', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.data).toEqual({
        lifecycle: false,
        firewall: false
      });
    });
  });

  describe('verifyConnection', () => {
    it('verifies connection when IQ is enabled and sets connectionStatus to connected', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true, reason: 'Connected'}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
      expect(service.state.context.connectionStatus).toBe('connected');
    });

    it('verifies connection when IQ is disabled and sets connectionStatus to connected', async () => {
      const mockConfig = {
        enabled: false,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true, reason: 'Connected'}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      // Should still attempt verification even when enabled=false
      expect(Axios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
      expect(service.state.context.connectionStatus).toBe('connected');
    });

    it('sets connectionStatus to error when verification fails', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockRejectedValue(new Error('Connection failed'));

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.connectionStatus).toBe('error');
    });

    it('attempts connection verification regardless of enabled status', async () => {
      // Test with enabled=false to ensure we don't check enabled flag
      const mockConfig = {
        enabled: false,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('verifyingConnection'));

      // Verify that post is called even when enabled=false
      expect(Axios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
    });

    it('sets connectionStatus to error when response has success: false (HTTP 200)', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: false, reason: 'Invalid credentials'}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.connectionStatus).toBe('error');
    });

    it('sets connectionStatus to error when success: false with no reason (HTTP 200)', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: false}});

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.connectionStatus).toBe('error');
    });

    it('handles network timeout during verification', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockRejectedValue(new Error('timeout of 30000ms exceeded'));

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.connectionStatus).toBe('error');
    });
  });

  describe('state transitions', () => {
    it('transitions from loading -> verifyingConnection -> loaded', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();

      expect(service.state.value).toBe('loading');

      await waitFor(service, (state) => state.matches('verifyingConnection'));
      expect(service.state.value).toBe('verifyingConnection');

      await waitFor(service, (state) => state.matches('loaded'));
      expect(service.state.value).toBe('loaded');
    });

    it('transitions from loading -> loaded on fetch error', async () => {
      Axios.get.mockRejectedValue(new Error('Fetch failed'));

      service = interpret(IqServerConnectedMachine).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.value).toBe('loaded');
      expect(service.state.context.error).toBeDefined();
    });
  });

  describe('just-saved handoff', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('seeds licensedSolutions from cache, skips GET, but still runs verify-connection', async () => {
      Axios.post.mockResolvedValue({data: {success: true}});
      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [
          {id: 'lifecycle', url: '/ui/links/lifecycle'},
          {id: 'firewall', url: '/ui/links/firewall'}
        ]
      };

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();

      // Data comes from the cache immediately (no GET) — machine starts in verifyingConnection.
      expect(service.state.context.data).toEqual({lifecycle: true, firewall: true});
      expect(service.state.context.iqServerUrl).toBe('http://localhost:8070');
      expect(Axios.get).not.toHaveBeenCalled();

      await waitFor(service, (state) => state.matches('loaded'));

      // Verify actually ran, and the pill flips to connected on success.
      expect(Axios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
      expect(service.state.context.connectionStatus).toBe('connected');
      expect(Axios.get).not.toHaveBeenCalled();
    });

    it('reports connectionStatus=error when the just-saved verify fails', async () => {
      Axios.post.mockRejectedValue(new Error('connection refused'));
      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}]
      };

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();
      await Promise.resolve();
      await Promise.resolve();
      jest.advanceTimersByTime(2000);
      await Promise.resolve();
      await Promise.resolve();
      jest.advanceTimersByTime(2000);
      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.connectionStatus).toBe('error');
      expect(Axios.get).not.toHaveBeenCalled();
    });

    it('falls back to the normal loading path when no justSaved config is provided', async () => {
      const mockConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}]
      };
      Axios.get.mockResolvedValue({data: mockConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(buildIqServerConnectedMachine(null)).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(Axios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
      expect(service.state.context.data).toEqual({lifecycle: true, firewall: false});
    });

    it('falls back to a real GET when justSaved carries an empty licensedSolutions', async () => {
      const settledConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [
          {id: 'lifecycle', url: '/ui/links/lifecycle'},
          {id: 'firewall', url: '/ui/links/firewall'}
        ]
      };
      Axios.get.mockResolvedValue({data: settledConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();
      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(service.state.context.data).toEqual({lifecycle: true, firewall: true});
      expect(service.state.context.connectionStatus).toBe('connected');
    });

    it('ignores hasFirewall — only licensedSolutions decides the Firewall tile', async () => {
      // hasFirewall is a capability property set by detectFirewall() on any IQ >= 200 with a
      // reachable connection; it is not a licence signal. Only /api/v2/solutions/licensed is
      // authoritative for what tiles light up.
      Axios.post.mockResolvedValue({data: {success: true}});
      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}],
        hasFirewall: true
      };

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();
      await waitFor(service, (state) => state.matches('loaded'));

      expect(service.state.context.data).toEqual({lifecycle: true, firewall: false});
    });
  });

  describe('context shape', () => {
    const iqConfig = {
      enabled: true,
      url: 'http://localhost:8070',
      authenticationType: 'USER',
      username: 'iq-context-user',
      licensedSolutions: [{id: 'lifecycle'}]
    };
    const expectedKeys = ['connectionStatus', 'data', 'error', 'iqServerUrl', 'pristineData', 'verifyAttempts'];

    function expectNoConfigLeak(context) {
      expect(Object.keys(context).sort()).toEqual(expectedKeys);
      expect(JSON.stringify(context)).not.toContain('iq-context-user');
    }

    it('holds no raw config or username on the just-saved path', async () => {
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(buildIqServerConnectedMachine(iqConfig)).start();
      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).not.toHaveBeenCalled();
      expectNoConfigLeak(service.state.context);
    });

    it('holds no raw config or username after a GET', async () => {
      Axios.get.mockResolvedValue({data: iqConfig});
      Axios.post.mockResolvedValue({data: {success: true}});

      service = interpret(IqServerConnectedMachine).start();
      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).toHaveBeenCalled();
      expectNoConfigLeak(service.state.context);
    });
  });

  describe('empty licensedSolutions on the just-saved path (IQ < 200)', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('makes a single GET and reaches loaded without waiting on a timer', async () => {
      // IQ < 200 always returns an empty licensedSolutions, so there is no retry and no timer to advance.
      const emptyResponse = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: []
      };
      Axios.get.mockResolvedValue({data: emptyResponse});
      Axios.post.mockResolvedValue({data: {success: true}});

      const justSaved = {enabled: true, url: 'http://localhost:8070', licensedSolutions: []};
      service = interpret(buildIqServerConnectedMachine(justSaved)).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.get).toHaveBeenCalledTimes(1);
      expect(service.state.context.data).toEqual({lifecycle: false, firewall: false});
      expect(service.state.context.connectionStatus).toBe('connected');
    });
  });

  describe('verify-connection retry on the just-saved path', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('retries verify-connection when it fails, then reports connected on the next success', async () => {
      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}]
      };
      Axios.post
        .mockRejectedValueOnce(new Error('post-save verify-connection 400'))
        .mockResolvedValue({data: {success: true}});

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();

      await Promise.resolve();
      await Promise.resolve();
      expect(Axios.post).toHaveBeenCalledTimes(1);

      jest.advanceTimersByTime(2000);
      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.post).toHaveBeenCalledTimes(2);
      expect(service.state.context.connectionStatus).toBe('connected');
    });

    it('reports connectionStatus=error after VERIFY_MAX_ATTEMPTS attempts are exhausted', async () => {
      const justSaved = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}]
      };
      Axios.post.mockRejectedValue(new Error('persistent verify failure'));

      service = interpret(buildIqServerConnectedMachine(justSaved)).start();

      await Promise.resolve();
      await Promise.resolve();
      jest.advanceTimersByTime(2000);
      await Promise.resolve();
      await Promise.resolve();
      jest.advanceTimersByTime(2000);
      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.post).toHaveBeenCalledTimes(3);
      expect(service.state.context.connectionStatus).toBe('error');

      jest.advanceTimersByTime(10000);
      expect(Axios.post).toHaveBeenCalledTimes(3);
    });

    it('makes one GET and one verify-connection on a normal visit with no licensed solutions', async () => {
      Axios.get.mockResolvedValue({data: {enabled: true, url: 'http://localhost:8070', licensedSolutions: []}});
      Axios.post.mockRejectedValue(new Error('connection refused'));

      service = interpret(buildIqServerConnectedMachine(null)).start();
      await waitFor(service, (state) => state.matches('loaded'));
      jest.advanceTimersByTime(10000);

      expect(Axios.get).toHaveBeenCalledTimes(1);
      expect(Axios.post).toHaveBeenCalledTimes(1);
    });

    it('does not retry verify-connection when there is no just-saved handoff (normal page visit)', async () => {
      const settledConfig = {
        enabled: true,
        url: 'http://localhost:8070',
        licensedSolutions: [{id: 'lifecycle', url: '/ui/links/lifecycle'}]
      };
      Axios.get.mockResolvedValue({data: settledConfig});
      Axios.post.mockRejectedValue(new Error('genuine connection failure'));

      service = interpret(buildIqServerConnectedMachine(null)).start();

      await waitFor(service, (state) => state.matches('loaded'));

      expect(Axios.post).toHaveBeenCalledTimes(1);
      expect(service.state.context.connectionStatus).toBe('error');

      jest.advanceTimersByTime(10000);
      expect(Axios.post).toHaveBeenCalledTimes(1);
    });
  });
});

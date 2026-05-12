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

import IqServerConnectedMachine from './IqServerConnectedMachine';

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
});

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

import {createLoginFormMachine} from './loginFormMachine';

// jsdom 20 does not expose TextEncoder; Node 18+ provides it globally
if (!global.TextEncoder) {
  global.TextEncoder = require('util').TextEncoder;
}

import {UIStrings} from '@sonatype/nexus-ui-plugin';

const {ERRORS} = UIStrings;

import {awaitTransition} from '@sonatype/nexus-ui-plugin/src/frontend/__jest__/xstateTestUtils';

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actualModule = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actualModule,
    ExtJS: {
      urlOf: (path) => `/${path}`,
      state: jest.fn(() => ({
        getValue: jest.fn((key, defaultValue) => defaultValue),
      })),
    },
  };
});

describe('loginFormMachine', () => {
  describe('save service', () => {
    let saveFn;
    let originalLocation;

    beforeAll(() => {
      saveFn = createLoginFormMachine().options.services.save;
    });

    beforeEach(() => {
      originalLocation = window.location;
      delete window.location;
      window.location = {hash: '', assign: jest.fn()};
      window.NX = {State: {setUser: jest.fn()}};
    });

    afterEach(() => {
      window.location = originalLocation;
      delete window.NX;
      delete global.fetch;
    });

    it('sets location hash and returns username on successful login', async () => {
      global.fetch = jest.fn().mockResolvedValueOnce({ok: true, status: 200});
      const result = await saveFn({data: {username: 'testuser', password: 'testpass'}});
      expect(result.username).toBe('testuser');
      expect(window.location.hash).toBe('#browse/welcome');
    });

    it('throws rate limit error with retryAfter on 429 response', async () => {
      global.fetch = jest.fn().mockResolvedValueOnce({
        ok: false,
        status: 429,
        headers: {get: jest.fn(() => '30')},
      });
      await expect(saveFn({data: {username: 'testuser', password: 'testpass'}}))
        .rejects.toMatchObject({response: {status: 429, retryAfter: 30}});
    });

    it('throws auth error on non-429 failure', async () => {
      global.fetch = jest.fn().mockResolvedValueOnce({
        ok: false,
        status: 403,
        text: jest.fn().mockResolvedValue('Forbidden'),
      });
      await expect(saveFn({data: {username: 'testuser', password: 'testpass'}}))
        .rejects.toMatchObject({response: {status: 403, data: 'Forbidden'}});
    });

    it('uses empty data when response.text() fails', async () => {
      global.fetch = jest.fn().mockResolvedValueOnce({
        ok: false,
        status: 500,
        text: jest.fn().mockRejectedValue(new Error('read failed')),
      });
      await expect(saveFn({data: {username: 'testuser', password: 'testpass'}}))
        .rejects.toMatchObject({response: {status: 500, data: ''}});
    });

    it('uses empty context prefix when contextPath is slash', async () => {
      const {ExtJS} = require('@sonatype/nexus-ui-plugin');
      ExtJS.state.mockReturnValueOnce({getValue: jest.fn(() => '/')});
      global.fetch = jest.fn().mockResolvedValueOnce({ok: true, status: 200});
      await saveFn({data: {username: 'testuser', password: 'testpass'}});
      expect(global.fetch).toHaveBeenCalledWith('/service/rapture/session', expect.any(Object));
    });
  });


  describe('429 response sets rate limit warning', () => {
    it('transitions to editing with rateLimitWarning and retryAfterSeconds', async () => {
      // The mock rejects with a plain object rather than an Error instance. This is intentional:
      // setSaveError only reads event.data?.response?.status (and optionally .retryAfter), so
      // the plain-object shape is sufficient and keeps the test focused on state transitions.
      const mockSaveService = jest.fn()
        .mockRejectedValueOnce({response: {status: 429, retryAfter: 30}});

      const machine = createLoginFormMachine().withConfig({services: {save: mockSaveService}});

      await awaitTransition(
        machine,
        (state) => {
          // Match when saveError is null (429 clears it) and rateLimitWarning is true
          return state.matches('editing') && state.context.rateLimitWarning;
        },
        (state) => {
          expect(state.context.rateLimitWarning).toBe(true);
          expect(state.context.retryAfterSeconds).toBe(30);
          expect(state.context.saveError).toBeNull();
        },
        (service) => {
          service.send({type: 'UPDATE', name: 'username', value: 'testuser'});
          service.send({type: 'UPDATE', name: 'password', value: 'testpass'});
          service.send({type: 'SUBMIT'});
        }
      );
    });
  });

  describe('403 response uses existing error path', () => {
    it('transitions to editing with saveError set to WRONG_CREDENTIALS', async () => {
      const mockSaveService = jest.fn()
        .mockRejectedValueOnce({response: {status: 403, data: ''}});

      const machine = createLoginFormMachine().withConfig({services: {save: mockSaveService}});

      await awaitTransition(
        machine,
        (state) => {
          return state.matches('editing') && state.context.saveError !== null;
        },
        (state) => {
          expect(state.context.rateLimitWarning).toBe(false);
          expect(state.context.saveError).toBe(ERRORS.WRONG_CREDENTIALS);
        },
        (service) => {
          service.send({type: 'UPDATE', name: 'username', value: 'testuser'});
          service.send({type: 'UPDATE', name: 'password', value: 'testpass'});
          service.send({type: 'SUBMIT'});
        }
      );
    });
  });

  describe('new submit after 429 resets rate limit state', () => {
    it('clears rateLimitWarning when submitting again after rate limit error', async () => {
      const mockSaveService = jest.fn()
        .mockRejectedValueOnce({response: {status: 429, retryAfter: 30}})
        .mockResolvedValueOnce({username: 'testuser'});

      const machine = createLoginFormMachine().withConfig({services: {save: mockSaveService}});

      const service = interpret(machine).start();
      service.send({type: 'UPDATE', name: 'username', value: 'testuser'});
      service.send({type: 'UPDATE', name: 'password', value: 'testpass'});

      // First submit: expect rate limit warning
      await new Promise((resolve) => {
        const sub = service.subscribe((state) => {
          if (state.matches('editing') && state.context.rateLimitWarning === true) {
            sub.unsubscribe();
            resolve();
          }
        });
        service.send({type: 'SUBMIT'});
      });
      expect(service.state.context.rateLimitWarning).toBe(true);
      expect(service.state.context.retryAfterSeconds).toBe(30);

      // Second submit: clearSaveError fires on saving entry, resetting rate limit flags
      await new Promise((resolve) => {
        const sub = service.subscribe((state) => {
          if (state.matches('saving')) {
            expect(state.context.rateLimitWarning).toBe(false);
            expect(state.context.retryAfterSeconds).toBe(null);
            sub.unsubscribe();
            service.stop();
            resolve();
          }
        });
        service.send({type: 'SUBMIT'});
      });
    });
  });
});

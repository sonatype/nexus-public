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
import '@testing-library/jest-dom';
import { configure } from '@testing-library/react';

// Increase default async timeout for findBy/waitFor queries.
// CI machines are CPU-constrained, causing flaky test failures.
configure({ asyncUtilTimeout: 5000 });

// Mock ResizeObserver for tests (required by some Radix components)
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

let lastValue = 0;

window.crypto = {
  getRandomValues: function (buffer) {
    buffer.fill(lastValue++);
  }
};

// Add missing getBoundingClientRect
Range.prototype.getBoundingClientRect = jest.fn(() => {
  return {
    width: 100,
    height: 100,
    top: 100,
    left: 100,
    bottom: 100,
    right: 100
  };
});

// ExtJS Mocks
global.Ext = {
  getApplication: jest.fn().mockReturnValue({
    getStore: jest.fn().mockReturnValue({
      on: jest.fn(),
      un: jest.fn()
    }),
    getController: jest.fn().mockReturnValue({
      on: jest.fn(),
      un: jest.fn()
    })
  }),
  // keep it async so it's closer to real world behavior, but no need to wait longer than we have to
  defer: (callback) => setTimeout(callback, 0),
  isEmpty: jest.fn().mockImplementation((value) => {
    if (value === undefined || value === null) {
      return true;
    } else if (typeof value?.length === 'number') {
      return value.length === 0;
    } else {
      return false;
    }
  }),
  widget: jest.fn()
};

// ExtJS NX Mocks
global.NX = {
  Messages: {
    success: jest.fn(),
    error: jest.fn()
  },
  Permissions: {
    check: jest.fn().mockReturnValue(true)
  },
  app: {
    Application: {
      bundleActive: jest.fn()
    }
  },
  State: {
    getValue: jest.fn(),
    getUser: jest.fn(),
    getEdition: jest.fn(),
    setUser: jest.fn()
  },
  Security: {
    hasUser: jest.fn(),
    requestSession: jest.fn(() => Promise.resolve({status: 204, responseText: ''}))
  }
};

// Default REST bootstrap state — components import ExtJS from relative path and call
// window.__nxRestBootstrap for edition/state fallback when ExtJS isn't initialized.
// Individual tests that need OSS can override window.__nxRestBootstrap.edition.
window.__nxRestBootstrap = { edition: 'PRO' };

// Reset NX.Permissions.check to return true before every test.
// Components import ExtJS from relative path and call NX.Permissions.check directly.
// Individual tests that need canUpdate=false should call:
//   global.NX.Permissions.check.mockReturnValue(false)
beforeEach(() => {
  global.NX?.Permissions?.check?.mockReturnValue(true);
});

// Default axios mock: prevents JSDOM AggregateError from unmocked XHR.
// All methods resolve with {data: undefined} by default so production null-guards
// handle the empty response silently without triggering catch-block console.error.
//
// Overriding per test:
//   jest.spyOn(Axios, 'get').mockResolvedValue({data: ...})  — spies on this mock in any file
//   jest.mock('axios')  (no factory)                         — Jest auto-mock for that file only;
//                                                              all methods are jest.fn() returning
//                                                              undefined, not this factory
//
// Mock isolation: jest.config.js sets clearMocks:true, which clears call/instance
// history after each test but does NOT reset implementations. If a test calls
// jest.spyOn(Axios, 'get').mockResolvedValue(...), restore it with
// jest.restoreAllMocks() in afterEach, or use mockResolvedValueOnce instead.
jest.mock('axios', () => {
  const emptyResponse = {
    data: undefined, status: 200, statusText: 'OK',
    headers: {}, config: { url: '', method: 'get', headers: {} },
  };
  const resolved = () => Promise.resolve(emptyResponse);
  const makeInstance = () => ({
    get: jest.fn(resolved),
    post: jest.fn(resolved),
    put: jest.fn(resolved),
    delete: jest.fn(resolved),
    patch: jest.fn(resolved),
    request: jest.fn(resolved),
    head: jest.fn(resolved),
    options: jest.fn(resolved),
    defaults: { headers: { common: {} }, baseURL: '' },
    interceptors: {
      request: { use: jest.fn(() => 0), eject: jest.fn(), clear: jest.fn() },
      response: { use: jest.fn(() => 0), eject: jest.fn(), clear: jest.fn() },
    },
  });
  const instance = makeInstance();
  const mockAxios = { ...instance, create: jest.fn(() => makeInstance()) };
  return { ...mockAxios, default: mockAxios };
});

// Mock @xstate/react to force devTools: false in tests to reduce console noise
jest.mock('@xstate/react', () => {
  const actual = jest.requireActual('@xstate/react');
  return {
    ...actual,
    useMachine: jest.fn((machine, options = {}) => {
      // Force devTools to false in tests to prevent XState from logging every state transition
      return actual.useMachine(machine, { ...options, devTools: false });
    })
  };
});

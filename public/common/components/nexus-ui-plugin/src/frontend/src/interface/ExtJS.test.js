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
import { renderHook, act } from '@testing-library/react';
import ExtJS from './ExtJS';

describe('ExtJS', () => {
  describe('useVisiblityWithChanges', () => {
    let mockPermissionsController;
    let mockStateController;
    let permissionChangedHandler;
    let stateChangedHandler;
    let userChangedHandler;
    let originalExt;
    let originalNX;

    beforeEach(() => {
      permissionChangedHandler = null;
      stateChangedHandler = null;
      userChangedHandler = null;

      mockPermissionsController = {
        on: jest.fn((event, handler) => {
          if (event === 'changed') {
            permissionChangedHandler = handler;
          }
        }),
        un: jest.fn()
      };

      mockStateController = {
        on: jest.fn((event, handler) => {
          if (event === 'changed') {
            stateChangedHandler = handler;
          } else if (event === 'userchanged') {
            userChangedHandler = handler;
          }
        }),
        un: jest.fn()
      };

      originalExt = global.Ext;
      originalNX = global.NX;

      global.Ext = {
        getApplication: jest.fn(() => ({
          getController: jest.fn((name) => {
            if (name === 'Permissions') return mockPermissionsController;
            if (name === 'State') return mockStateController;
            return null;
          })
        }))
      };

      global.NX = {
        State: {},
        Permissions: {},
        Security: {}
      };
    });

    afterEach(() => {
      global.Ext = originalExt;
      global.NX = originalNX;
    });

    it('returns initial visibility value', () => {
      const isVisibleMethod = jest.fn(() => true);
      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      expect(result.current).toBe(true);
      expect(isVisibleMethod).toHaveBeenCalled();
    });

    it('returns false initially when ExtJS is not ready', () => {
      global.Ext = undefined;
      global.NX = undefined;

      const isVisibleMethod = jest.fn(() => {
        throw new Error('ExtJS not ready');
      });
      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      expect(result.current).toBe(false);
    });

    it('re-evaluates visibility on Permissions#changed event', () => {
      let isVisible = false;
      const isVisibleMethod = jest.fn(() => isVisible);

      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      expect(result.current).toBe(false);

      // Simulate permission change
      isVisible = true;
      act(() => {
        permissionChangedHandler();
      });

      expect(result.current).toBe(true);
    });

    it('re-evaluates visibility on State#changed event', () => {
      let isVisible = false;
      const isVisibleMethod = jest.fn(() => isVisible);

      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      expect(result.current).toBe(false);

      // Simulate state change
      isVisible = true;
      act(() => {
        stateChangedHandler();
      });

      expect(result.current).toBe(true);
    });

    it('re-evaluates visibility on State#userchanged event', () => {
      let isVisible = false;
      const isVisibleMethod = jest.fn(() => isVisible);

      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      expect(result.current).toBe(false);

      // Simulate user change (login/logout)
      isVisible = true;
      act(() => {
        userChangedHandler();
      });

      expect(result.current).toBe(true);
    });

    it('cleans up event listeners on unmount', () => {
      const isVisibleMethod = jest.fn(() => true);
      const { unmount } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      unmount();

      expect(mockPermissionsController.un).toHaveBeenCalledWith('changed', expect.any(Function));
      expect(mockStateController.un).toHaveBeenCalledWith('changed', expect.any(Function));
      expect(mockStateController.un).toHaveBeenCalledWith('userchanged', expect.any(Function));
    });

    it('does not update state when visibility value is unchanged', () => {
      const isVisibleMethod = jest.fn(() => true);
      const { result } = renderHook(() => ExtJS.useVisiblityWithChanges(isVisibleMethod));

      const initialResult = result.current;

      // Trigger event but value stays same
      act(() => {
        permissionChangedHandler();
      });

      expect(result.current).toBe(initialResult);
    });
  });


  describe('setDirtyStatus', () => {
    it('sets the dirty status correctly', () => {
      ExtJS.setDirtyStatus('key', true);

      expect(window.dirty.includes('key')).toEqual(true);
      expect(window.dirty.includes('key2')).toEqual(false);

      // Set key2 twice to ensure that it has the correct value (it was flip-flopping before)
      ExtJS.setDirtyStatus('key2', true);
      ExtJS.setDirtyStatus('key2', true);

      expect(window.dirty.includes('key')).toEqual(true);
      expect(window.dirty.includes('key2')).toEqual(true);

      ExtJS.setDirtyStatus('key', false);

      expect(window.dirty.includes('key')).toEqual(false);
      expect(window.dirty.includes('key2')).toEqual(true);

      ExtJS.setDirtyStatus('key2', false);

      expect(window.dirty).toEqual([]);
    });
  });

  describe('calculateTimeout', () => {
    let mockState;

    beforeEach(() => {
      mockState = {
        getValue: jest.fn((key, defaultValue) => defaultValue)
      };

      jest.spyOn(ExtJS, 'state').mockReturnValue(mockState);
      jest.spyOn(ExtJS, 'hasUser').mockReturnValue(true);
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('returns 10000ms for authenticated users with default settings', () => {
      mockState.getValue.mockReturnValue({});
      ExtJS.hasUser.mockReturnValue(true);
      expect(ExtJS.calculateTimeout()).toBe(10000);
    });

    it('returns 120000ms for anonymous users with default settings', () => {
      mockState.getValue.mockReturnValue({});
      ExtJS.hasUser.mockReturnValue(false);
      expect(ExtJS.calculateTimeout()).toBe(120000);
    });

    it('returns custom timeout based on statusIntervalAuthenticated', () => {
      mockState.getValue.mockReturnValue({ statusIntervalAuthenticated: 10 });
      ExtJS.hasUser.mockReturnValue(true);
      expect(ExtJS.calculateTimeout()).toBe(20000);
    });

    it('returns custom timeout based on statusIntervalAnonymous', () => {
      mockState.getValue.mockReturnValue({ statusIntervalAnonymous: 30 });
      ExtJS.hasUser.mockReturnValue(false);
      expect(ExtJS.calculateTimeout()).toBe(60000);
    });

    it('returns minimum timeout of 2000ms for very low intervals', () => {
      mockState.getValue.mockReturnValue({ statusIntervalAuthenticated: 0.5 });
      ExtJS.hasUser.mockReturnValue(true);
      expect(ExtJS.calculateTimeout()).toBe(2000);
    });
  });

  describe('waitForNextPermissionChange', () => {
    let mockPermissionsController;
    let mockState;
    let originalExt;
    let changedHandler;

    beforeEach(() => {
      jest.useFakeTimers();
      changedHandler = null;

      mockPermissionsController = {
        on: jest.fn((event, handler) => {
          changedHandler = handler;
        }),
        un: jest.fn()
      };

      mockState = {
        getValue: jest.fn((key, defaultValue) => defaultValue)
      };

      originalExt = global.Ext;
      global.Ext = {
        getApplication: jest.fn(() => ({
          getController: jest.fn(() => mockPermissionsController)
        }))
      };

      jest.spyOn(ExtJS, 'state').mockReturnValue(mockState);
      jest.spyOn(ExtJS, 'hasUser').mockReturnValue(true);
      jest.spyOn(console, 'debug').mockImplementation(() => {});
    });

    afterEach(() => {
      jest.restoreAllMocks();
      jest.useRealTimers();
      global.Ext = originalExt;
    });

    it('resolves when permissions change before timeout', async () => {
      const promise = ExtJS.waitForNextPermissionChange();

      expect(mockPermissionsController.on).toHaveBeenCalledWith('changed', expect.any(Function));

      changedHandler();

      await expect(promise).resolves.toBeUndefined();
    });

    it('rejects after timeout when permissions do not change', async () => {
      mockState.getValue.mockReturnValue({
        statusIntervalAuthenticated: 5
      });

      const promise = ExtJS.waitForNextPermissionChange();

      jest.advanceTimersByTime(10000);

      await expect(promise).rejects.toThrow('timed out waiting for permissions to update');
      expect(mockPermissionsController.un).toHaveBeenCalledWith('changed', expect.any(Function));
    });

    it('uses 2x statusIntervalAuthenticated for authenticated users', () => {
      mockState.getValue.mockReturnValue({
        statusIntervalAuthenticated: 5
      });
      ExtJS.hasUser.mockReturnValue(true);

      const promise = ExtJS.waitForNextPermissionChange();

      jest.advanceTimersByTime(9999);
      changedHandler();

      return expect(promise).resolves.toBeUndefined();
    });

    it('uses 2x statusIntervalAnonymous for anonymous users', () => {
      mockState.getValue.mockReturnValue({
        statusIntervalAnonymous: 60
      });
      ExtJS.hasUser.mockReturnValue(false);

      const promise = ExtJS.waitForNextPermissionChange();

      jest.advanceTimersByTime(119999);
      changedHandler();

      return expect(promise).resolves.toBeUndefined();
    });

    it('uses minimum timeout of 2 seconds for very low status intervals', () => {
      mockState.getValue.mockReturnValue({
        statusIntervalAuthenticated: 0.5
      });
      ExtJS.hasUser.mockReturnValue(true);

      const promise = ExtJS.waitForNextPermissionChange();

      jest.advanceTimersByTime(1999);
      changedHandler();

      return expect(promise).resolves.toBeUndefined();
    });

    it('uses default values when uiSettings is empty', () => {
      mockState.getValue.mockReturnValue({});
      ExtJS.hasUser.mockReturnValue(true);

      const promise = ExtJS.waitForNextPermissionChange();

      jest.advanceTimersByTime(9999);
      changedHandler();

      return expect(promise).resolves.toBeUndefined();
    });

    it('clears timeout when permissions change', async () => {
      const clearTimeoutSpy = jest.spyOn(global, 'clearTimeout');

      const promise = ExtJS.waitForNextPermissionChange();

      changedHandler();

      await expect(promise).resolves.toBeUndefined();
      expect(clearTimeoutSpy).toHaveBeenCalled();
    });

    it('anonymous default interval uses 120000ms timeout', async () => {
      mockState.getValue.mockReturnValue({}); // no uiSettings values
      ExtJS.hasUser.mockReturnValue(false);
      const promise = ExtJS.waitForNextPermissionChange();
      jest.advanceTimersByTime(119999);
      changedHandler();
      await expect(promise).resolves.toBeUndefined();
    });

    it('rejects after min 2000ms when very low authenticated interval', async () => {
      mockState.getValue.mockReturnValue({ statusIntervalAuthenticated: 0.5 });
      ExtJS.hasUser.mockReturnValue(true);
      const promise = ExtJS.waitForNextPermissionChange();
      jest.advanceTimersByTime(2001);
      await expect(promise).rejects.toThrow('timed out waiting for permissions to update');
      expect(mockPermissionsController.un).toHaveBeenCalledWith('changed', expect.any(Function));
    });

  });

  describe('arePermissionsReady', () => {
    let originalNX;

    beforeEach(() => {
      originalNX = global.NX;
      delete global.NX;
    });

    afterEach(() => {
      global.NX = originalNX;
    });

    it('returns false when NX.Permissions is not available', () => {
      expect(ExtJS.arePermissionsReady()).toBe(false);
    });

    it('returns true when NX.Permissions.permissions is defined', () => {
      global.NX = { Permissions: { permissions: {} } };
      expect(ExtJS.arePermissionsReady()).toBe(true);
    });

    it('returns false when NX.Permissions.permissions is undefined', () => {
      global.NX = { Permissions: {} };
      expect(ExtJS.arePermissionsReady()).toBe(false);
    });
  });

  describe('waitForPermissions', () => {
    let originalNX;

    beforeEach(() => {
      jest.useFakeTimers();
      originalNX = global.NX;
      delete global.NX;
    });

    afterEach(() => {
      jest.useRealTimers();
      global.NX = originalNX;
    });

    it('resolves immediately when NX.Permissions is already ready', async () => {
      global.NX = { Permissions: { permissions: {} } };
      await expect(ExtJS.waitForPermissions()).resolves.toBeUndefined();
    });

    it('resolves once permissions become available via polling', async () => {
      const promise = ExtJS.waitForPermissions();

      jest.advanceTimersByTime(150);
      global.NX = { Permissions: { permissions: {} } };
      jest.advanceTimersByTime(150);

      await expect(promise).resolves.toBeUndefined();
    });

    it('rejects after the configured timeout when permissions never load', async () => {
      const promise = ExtJS.waitForPermissions(500);
      jest.advanceTimersByTime(501);
      await expect(promise).rejects.toThrow('Permissions load timed out');
    });

    it('uses 30000ms default timeout', async () => {
      const promise = ExtJS.waitForPermissions();
      jest.advanceTimersByTime(29999);
      global.NX = { Permissions: { permissions: {} } };
      jest.advanceTimersByTime(200);
      await expect(promise).resolves.toBeUndefined();
    });
  });
});

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
import { renderHook } from '@testing-library/react-hooks';
import { ExtJS, isVisible } from '@sonatype/nexus-ui-plugin';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { useRedirectOnLogout } from './useRedirectOnLogout';

jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useUser: jest.fn(),
  },
  isVisible: jest.fn()
}));

describe('useRedirectOnLogout', () => {
  let goMock, onMock, offMock;

  beforeEach(() => {
    jest.useFakeTimers();

    // Reset mocks
    goMock = jest.fn();
    onMock = jest.fn();
    offMock = jest.fn();

    useRouter.mockReturnValue({
      stateService: {
        go: goMock,
      },
    });

    useCurrentStateAndParams.mockReturnValue({
      state: {
        data: {
          visibilityRequirements: 'someRequirement',
        },
      },
    });

    global.Ext = {
      getApplication: () => ({
        getController: () => ({
          on: onMock,
          un: offMock,
        }),
      }),
    };

    window.dirty = [];
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.clearAllMocks();
  });

  it('should redirect if user is not authenticated and route is not visible', () => {
    ExtJS.useUser.mockReturnValue(null); // no autenticado
    isVisible.mockReturnValue(false);    // no visible

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).toHaveBeenCalledWith('browse.welcome');
  });

  it('should clear unsaved changes before redirecting', () => {
    ExtJS.useUser.mockReturnValue(null);
    isVisible.mockReturnValue(false);
    window.dirty = ['some unsaved changes'];

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(window.dirty).toEqual([]);
    expect(goMock).toHaveBeenCalledWith('browse.welcome');
  });

  it('should not redirect if user is authenticated', () => {
    ExtJS.useUser.mockReturnValue({ id: 'mockUser' }); // autenticado
    isVisible.mockReturnValue(false);                  // no visible

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });

  it('should not redirect if route is visible', () => {
    ExtJS.useUser.mockReturnValue(null);  
    isVisible.mockReturnValue(true);    

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });

  it('should clean up event listener on unmount', () => {
    ExtJS.useUser.mockReturnValue(null);
    isVisible.mockReturnValue(false);

    const { unmount } = renderHook(() => useRedirectOnLogout());

    unmount();

    const handler = onMock.mock.calls[0][1];
    expect(offMock).toHaveBeenCalledWith('changed', handler);
  });
});

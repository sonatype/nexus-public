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
import {renderHook, act} from '@testing-library/react';
import {useSessionExpiry} from '../useSessionExpiry';

// Source imports NEXUS_SESSION_EXPIRED_EVENT from '@sonatype/nexus-ui-plugin'
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  NEXUS_SESSION_EXPIRED_EVENT: 'nexus:session-expired',
}));

const SESSION_EXPIRED_EVENT = 'nexus:session-expired';
const DEFAULT_MESSAGE = 'Your session has expired. Please sign in again to continue.';

describe('useSessionExpiry', () => {
  it('starts with isExpired=false and the default message', () => {
    const {result} = renderHook(() => useSessionExpiry());
    expect(result.current.isExpired).toBe(false);
    expect(result.current.message).toBe(DEFAULT_MESSAGE);
  });

  it('showExpiryModal sets isExpired=true', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal());
    expect(result.current.isExpired).toBe(true);
  });

  it('showExpiryModal uses the default message when called without args', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal());
    expect(result.current.message).toBe(DEFAULT_MESSAGE);
  });

  it('showExpiryModal uses a custom message when provided', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal('Token expired'));
    expect(result.current.message).toBe('Token expired');
  });

  it('showExpiryModal is idempotent — second call is ignored', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal('first'));
    act(() => result.current.showExpiryModal('second'));
    expect(result.current.message).toBe('first');
  });

  it('hideExpiryModal sets isExpired=false', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal());
    act(() => result.current.hideExpiryModal());
    expect(result.current.isExpired).toBe(false);
  });

  it('hideExpiryModal allows showExpiryModal to be called again', () => {
    const {result} = renderHook(() => useSessionExpiry());
    act(() => result.current.showExpiryModal('first'));
    act(() => result.current.hideExpiryModal());
    act(() => result.current.showExpiryModal('second'));
    expect(result.current.isExpired).toBe(true);
    expect(result.current.message).toBe('second');
  });

  describe('checkSessionError', () => {
    it('returns false and does not show modal for a non-401 error', () => {
      const {result} = renderHook(() => useSessionExpiry());
      let triggered: boolean;
      act(() => {
        triggered = result.current.checkSessionError({status: 403});
      });
      expect(triggered!).toBe(false);
      expect(result.current.isExpired).toBe(false);
    });

    it('returns true and shows modal for a top-level status=401', () => {
      const {result} = renderHook(() => useSessionExpiry());
      let triggered: boolean;
      act(() => {
        triggered = result.current.checkSessionError({status: 401});
      });
      expect(triggered!).toBe(true);
      expect(result.current.isExpired).toBe(true);
    });

    it('returns true for a nested response.status=401', () => {
      const {result} = renderHook(() => useSessionExpiry());
      let triggered: boolean;
      act(() => {
        triggered = result.current.checkSessionError({response: {status: 401}});
      });
      expect(triggered!).toBe(true);
      expect(result.current.isExpired).toBe(true);
    });

    it('returns false for null error', () => {
      const {result} = renderHook(() => useSessionExpiry());
      let triggered: boolean;
      act(() => {
        triggered = result.current.checkSessionError(null);
      });
      expect(triggered!).toBe(false);
    });

    it('returns false for a non-object error', () => {
      const {result} = renderHook(() => useSessionExpiry());
      let triggered: boolean;
      act(() => {
        triggered = result.current.checkSessionError('some string error');
      });
      expect(triggered!).toBe(false);
    });
  });

  describe('event listener', () => {
    it('shows modal when the session expired custom event is dispatched', () => {
      const {result} = renderHook(() => useSessionExpiry());
      act(() => {
        window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
      });
      expect(result.current.isExpired).toBe(true);
    });

    it('removes the event listener on unmount', () => {
      const {result, unmount} = renderHook(() => useSessionExpiry());
      unmount();
      act(() => {
        window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
      });
      expect(result.current.isExpired).toBe(false);
    });
  });
});

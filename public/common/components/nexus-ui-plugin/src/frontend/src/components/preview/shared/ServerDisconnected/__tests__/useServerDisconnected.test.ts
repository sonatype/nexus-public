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
import { useServerDisconnected } from '../useServerDisconnected';
import {
  NEXUS_SERVER_DISCONNECTED_EVENT,
  NEXUS_SERVER_RECONNECTED_EVENT,
  NEXUS_SERVER_RECONNECT_REQUESTED_EVENT,
} from '../../../../../interface/api/serverDisconnectedBroadcast';

describe('useServerDisconnected', () => {
  it('starts with the modal hidden and not retrying', () => {
    const { result } = renderHook(() => useServerDisconnected());
    expect(result.current.isDisconnected).toBe(false);
    expect(result.current.isRetrying).toBe(false);
  });

  it('shows the modal when the server-disconnected window event fires', () => {
    const { result } = renderHook(() => useServerDisconnected());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    });

    expect(result.current.isDisconnected).toBe(true);
    expect(result.current.isRetrying).toBe(false);
  });

  it('enters the retrying state and asks ExtJS to reconnect when requestRetry is called', () => {
    const reconnectHandler = jest.fn();
    window.addEventListener(
      NEXUS_SERVER_RECONNECT_REQUESTED_EVENT,
      reconnectHandler,
    );
    const { result } = renderHook(() => useServerDisconnected());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    });
    act(() => {
      result.current.requestRetry();
    });

    expect(result.current.isDisconnected).toBe(true);
    expect(result.current.isRetrying).toBe(true);
    expect(reconnectHandler).toHaveBeenCalledTimes(1);
    window.removeEventListener(
      NEXUS_SERVER_RECONNECT_REQUESTED_EVENT,
      reconnectHandler,
    );
  });

  it('closes the modal when the server reconnects', () => {
    const { result } = renderHook(() => useServerDisconnected());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    });
    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RECONNECTED_EVENT));
    });

    expect(result.current.isDisconnected).toBe(false);
    expect(result.current.isRetrying).toBe(false);
  });

  it('hides the modal when dismiss is called', () => {
    const { result } = renderHook(() => useServerDisconnected());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    });
    expect(result.current.isDisconnected).toBe(true);

    act(() => {
      result.current.dismiss();
    });

    expect(result.current.isDisconnected).toBe(false);
    expect(result.current.isRetrying).toBe(false);
  });

  it('ignores the window event after unmount (subscription torn down)', () => {
    const { result, unmount } = renderHook(() => useServerDisconnected());
    unmount();

    expect(() =>
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT)),
    ).not.toThrow();
    expect(result.current.isDisconnected).toBe(false);
  });
});

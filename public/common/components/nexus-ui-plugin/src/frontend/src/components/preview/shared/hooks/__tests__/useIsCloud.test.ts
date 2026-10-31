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

import { renderHook } from '@testing-library/react';

import ExtJS from '../../../../../interface/ExtJS';
import {
  CLOUD_DISTRIBUTION,
  DISTRIBUTION_STATE_KEY,
  SELF_HOSTED_DISTRIBUTION,
  useIsCloud,
} from '../useIsCloud';

jest.mock('../../../../../interface/ExtJS');

/**
 * Route `ExtJS.state().getValue(key, defaultValue)` through the caller-supplied map so tests can
 * pin the exact Rapture state the hook reads. Mirrors the production fallback in
 * `ExtJS.state()`: an absent key (or an explicit `undefined` entry) resolves to `defaultValue`.
 */
function stubStateValues(stateValues: Record<string, unknown>): void {
  (ExtJS.state() as { getValue: jest.Mock }).getValue.mockImplementation(
    (key: string, defaultValue?: unknown) =>
      stateValues[key] !== undefined ? stateValues[key] : defaultValue,
  );
}

describe('useIsCloud', () => {
  it('returns true when distribution is the cloud identifier', () => {
    stubStateValues({ [DISTRIBUTION_STATE_KEY]: CLOUD_DISTRIBUTION });

    const { result } = renderHook(() => useIsCloud());

    expect(result.current).toBe(true);
  });

  it('returns false when distribution is the self-hosted identifier (explicit, not inferred from absence)', () => {
    stubStateValues({ [DISTRIBUTION_STATE_KEY]: SELF_HOSTED_DISTRIBUTION });

    const { result } = renderHook(() => useIsCloud());

    expect(result.current).toBe(false);
  });

  it('returns false when the distribution value is absent from the published state', () => {
    stubStateValues({});

    const { result } = renderHook(() => useIsCloud());

    expect(result.current).toBe(false);
  });

  it('returns false when useState is unavailable (ExtJS not yet initialized)', () => {
    const extJSWithMutableUseState = ExtJS as unknown as { useState: unknown };
    const originalUseState = extJSWithMutableUseState.useState;
    extJSWithMutableUseState.useState = undefined;
    try {
      const { result } = renderHook(() => useIsCloud());

      expect(result.current).toBe(false);
    } finally {
      extJSWithMutableUseState.useState = originalUseState;
    }
  });

  it('does not treat the legacy isCloud flag as the source of truth', () => {
    stubStateValues({ isCloud: true });

    const { result } = renderHook(() => useIsCloud());

    expect(result.current).toBe(false);
  });

  it('reads from the published distribution key with an explicit self-hosted default', () => {
    stubStateValues({ [DISTRIBUTION_STATE_KEY]: CLOUD_DISTRIBUTION });

    renderHook(() => useIsCloud());

    expect(ExtJS.state().getValue).toHaveBeenCalledWith(
      DISTRIBUTION_STATE_KEY,
      SELF_HOSTED_DISTRIBUTION,
    );
  });
});

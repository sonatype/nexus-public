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
  EDITION_COMMUNITY,
  EDITION_ID_STATE_KEY,
  EDITION_PRO,
} from '../../../../../interface/editionUtils';
import { useEditionId } from '../useEditionId';

jest.mock('../../../../../interface/ExtJS');

function stubStateValues(stateValues: Record<string, unknown>): void {
  (ExtJS.state() as { getValue: jest.Mock }).getValue.mockImplementation(
    (key: string, defaultValue?: unknown) =>
      stateValues[key] !== undefined ? stateValues[key] : defaultValue,
  );
}

describe('useEditionId', () => {
  it('returns the edition identifier when it is present in state', () => {
    stubStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

    const { result } = renderHook(() => useEditionId());

    expect(result.current).toBe(EDITION_PRO);
  });

  it('returns the community edition identifier when set', () => {
    stubStateValues({ [EDITION_ID_STATE_KEY]: EDITION_COMMUNITY });

    const { result } = renderHook(() => useEditionId());

    expect(result.current).toBe(EDITION_COMMUNITY);
  });

  it('returns undefined when the edition identifier is absent from state', () => {
    stubStateValues({});

    const { result } = renderHook(() => useEditionId());

    expect(result.current).toBeUndefined();
  });

  it('returns undefined when useState is unavailable (ExtJS not yet initialized)', () => {
    const extJSWithMutableUseState = ExtJS as unknown as { useState: unknown };
    const originalUseState = extJSWithMutableUseState.useState;
    extJSWithMutableUseState.useState = undefined;
    try {
      const { result } = renderHook(() => useEditionId());

      expect(result.current).toBeUndefined();
    } finally {
      extJSWithMutableUseState.useState = originalUseState;
    }
  });

  it('reads from the editionId state key', () => {
    stubStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

    renderHook(() => useEditionId());

    expect(ExtJS.state().getValue).toHaveBeenCalledWith(EDITION_ID_STATE_KEY);
  });
});

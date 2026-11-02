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
import { useIsRepositorySizeSupported } from '../useIsRepositorySizeSupported';

jest.mock('../../../../../interface/ExtJS');

const REPOSITORY_SIZE_STATE_KEY = 'nexus.repository.size';

/**
 * Route `ExtJS.state().getValue(key, defaultValue)` through the caller-supplied map so tests can
 * pin the exact Rapture state the hook reads. Mirrors the production fallback in `ExtJS.state()`:
 * an absent key (or an explicit `undefined` entry) resolves to `defaultValue`.
 */
function stubStateValues(stateValues: Record<string, unknown>): void {
  (ExtJS.state() as { getValue: jest.Mock }).getValue.mockImplementation(
    (key: string, defaultValue?: unknown) =>
      stateValues[key] !== undefined ? stateValues[key] : defaultValue,
  );
}

/** Runs `body` with `ExtJS.useState` removed, restoring it afterwards even on failure. */
function withoutExtJsUseState(body: () => void): void {
  const extJSWithMutableUseState = ExtJS as unknown as { useState: unknown };
  const originalUseState = extJSWithMutableUseState.useState;
  extJSWithMutableUseState.useState = undefined;
  try {
    body();
  } finally {
    extJSWithMutableUseState.useState = originalUseState;
  }
}

describe('useIsRepositorySizeSupported', () => {
  it('shouldReportSupportedWhenTheBackendPublishesTrue', () => {
    stubStateValues({ [REPOSITORY_SIZE_STATE_KEY]: true });

    const { result } = renderHook(() => useIsRepositorySizeSupported());

    expect(result.current).toBe(true);
  });

  it('shouldReportUnsupportedWhenTheBackendPublishesFalse', () => {
    stubStateValues({ [REPOSITORY_SIZE_STATE_KEY]: false });

    const { result } = renderHook(() => useIsRepositorySizeSupported());

    expect(result.current).toBe(false);
  });

  // Community Edition: RepositoryMetricsStateContributor ships in a Pro-only plugin, so the key is
  // never published at all rather than published as false (NEXUS-55079).
  it('shouldReportUnsupportedWhenTheStateKeyIsAbsentAsInCommunityEdition', () => {
    stubStateValues({});

    const { result } = renderHook(() => useIsRepositorySizeSupported());

    expect(result.current).toBe(false);
  });

  // The `=== true` comparison, not a truthiness coercion, is what makes these two cases false: the
  // REST bootstrap path copies published state values through without type coercion, so anything
  // other than a real boolean true must read as unsupported rather than as "probably enabled".
  it.each([
    ['the string "true"', 'true'],
    ['the number 1', 1],
    ['a non-empty object', {}],
  ])('shouldReportUnsupportedWhenTheStateValueIsTruthyButNotBoolean (%s)', (_label, stateValue) => {
    stubStateValues({ [REPOSITORY_SIZE_STATE_KEY]: stateValue });

    const { result } = renderHook(() => useIsRepositorySizeSupported());

    expect(result.current).toBe(false);
  });

  it('shouldReportUnsupportedWhenExtJsIsNotYetInitialized', () => {
    stubStateValues({ [REPOSITORY_SIZE_STATE_KEY]: true });

    withoutExtJsUseState(() => {
      const { result } = renderHook(() => useIsRepositorySizeSupported());

      expect(result.current).toBe(false);
    });
  });

  it('shouldReadTheSingleResolvedStateKeyWithAnUnsupportedDefault', () => {
    stubStateValues({ [REPOSITORY_SIZE_STATE_KEY]: true });

    renderHook(() => useIsRepositorySizeSupported());

    expect(ExtJS.state().getValue).toHaveBeenCalledWith(REPOSITORY_SIZE_STATE_KEY, false);
  });

  // The four backend conditions (feature flag, licence, Postgres, finished CopyBlobSizeTask) are
  // collapsed server-side into this one key; re-deriving any of them client-side is what left CE on
  // Postgres showing permanently-empty cards, so the hook must not consult those signals.
  it('shouldNotTreatTheDatastorePostgresFlagAsTheSourceOfTruth', () => {
    stubStateValues({ 'datastore.isPostgresql': true });

    const { result } = renderHook(() => useIsRepositorySizeSupported());

    expect(result.current).toBe(false);
  });
});

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

import { renderHook, waitFor, act } from '@testing-library/react';
import { useFirewallCounts } from '../useFirewallCounts';

const mockGet = jest.fn();
jest.mock('../../../../../interface/api', () => ({
  restClient: { get: (...args: unknown[]) => mockGet(...args) },
  ENDPOINTS: { FIREWALL_STATUS_REPO: (name: string) => `/repo-status/${name}` },
}));

const countsFor = (name: string) => ({
  repositoryName: name,
  affectedComponentCount: 1,
  criticalComponentCount: 2,
  severeComponentCount: 0,
  moderateComponentCount: 0,
  quarantinedComponentCount: 0,
});
const nameOf = (url: string) => url.split('/').pop() as string;
const BAD_REPO = 'bad';

describe('useFirewallCounts', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockImplementation((url: string) => Promise.resolve(countsFor(nameOf(url))));
  });

  it('shouldExposeCountsForTheEnabledReposItIsGiven', async () => {
    const { result } = renderHook(() => useFirewallCounts(['a', 'b']));
    await waitFor(() => expect(Object.keys(result.current.counts).sort()).toEqual(['a', 'b']));
    expect(result.current.unavailableNames.size).toBe(0);
    expect(result.current.loadingNames.size).toBe(0);
  });

  it('shouldReportRepoAsLoadingUntilItsCountArrives', async () => {
    const { result } = renderHook(() => useFirewallCounts(['a']));
    await waitFor(() => expect(result.current.loadingNames.has('a')).toBe(true));
    await waitFor(() => expect(result.current.counts.a).toBeDefined());
    expect(result.current.loadingNames.has('a')).toBe(false);
  });

  it('shouldNotFetchWhenGivenNoRepos', () => {
    renderHook(() => useFirewallCounts([]));
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('shouldFetchOnlyNewReposWhenTheTargetSetChanges', async () => {
    const { result, rerender } = renderHook(({ names }) => useFirewallCounts(names), {
      initialProps: { names: ['a'] },
    });
    await waitFor(() => expect(result.current.counts.a).toBeDefined());
    mockGet.mockClear();
    rerender({ names: ['a', 'b'] });
    await waitFor(() => expect(result.current.counts.b).toBeDefined());
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('shouldMarkOnlyTheFailedRepoUnavailable', async () => {
    mockGet.mockImplementation((url: string) =>
      nameOf(url) === BAD_REPO ? Promise.reject(new Error('boom')) : Promise.resolve(countsFor(nameOf(url))));
    const { result } = renderHook(() => useFirewallCounts(['a', BAD_REPO, 'b']));
    await waitFor(() => expect(result.current.unavailableNames.has(BAD_REPO)).toBe(true));
    await waitFor(() => expect(result.current.counts.b).toBeDefined());
    expect(result.current.counts.a).toBeDefined();
    expect(result.current.unavailableNames.has('a')).toBe(false);
  });

  it('shouldRecoverOnRefreshAfterFailure', async () => {
    mockGet.mockRejectedValueOnce(new Error('IQ down'));
    const { result } = renderHook(() => useFirewallCounts(['a']));
    await waitFor(() => expect(result.current.unavailableNames.has('a')).toBe(true));
    act(() => result.current.refresh());
    await waitFor(() => expect(result.current.counts.a).toBeDefined());
    expect(result.current.unavailableNames.size).toBe(0);
  });
});

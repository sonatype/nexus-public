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

import { fetchRepositoryMetricsDetail } from '../fetchRepositoryMetricsDetail';

const mockRestClient = {
  get: jest.fn(),
};

jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
  },
}));

describe('fetchRepositoryMetricsDetail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('calls /details/filtered with a server-side nameFilter (not the unfiltered /details list)', async () => {
    mockRestClient.get.mockResolvedValue({
      pageData: [{ name: 'test-repo', componentCount: 10, assetCount: 25, size: 1024000 }],
      totalCount: 1,
      page: 1,
      pageSize: 1,
    });

    await fetchRepositoryMetricsDetail('test-repo');

    const [url] = mockRestClient.get.mock.calls[0];
    expect(url).toContain('/internal/ui/repositories/details/filtered');
    expect(url).toContain('nameFilter=test-repo');
  });

  it('returns the matching page entry', async () => {
    mockRestClient.get.mockResolvedValue({
      pageData: [{ name: 'test-repo', componentCount: 10, assetCount: 25, size: 1024000 }],
      totalCount: 1,
      page: 1,
      pageSize: 1,
    });

    const detail = await fetchRepositoryMetricsDetail('test-repo');

    expect(detail).toEqual({ name: 'test-repo', componentCount: 10, assetCount: 25, size: 1024000 });
  });

  it('returns undefined when the page has no matching entry', async () => {
    mockRestClient.get.mockResolvedValue({ pageData: [], totalCount: 0, page: 1, pageSize: 1 });

    const detail = await fetchRepositoryMetricsDetail('missing-repo');

    expect(detail).toBeUndefined();
  });
});

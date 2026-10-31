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

import Axios from 'axios';
import { fetchComponentVersions } from '../componentVersionsApi';

jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('fetchComponentVersions', () => {
  beforeEach(() => jest.clearAllMocks());

  it('requests the versions endpoint with all parameters', async () => {
    mockedAxios.get.mockResolvedValue({
      data: { items: [], total: 0, page: 0, size: 20 },
    });

    await fetchComponentVersions({
      format: 'maven2',
      group: 'org.test',
      name: 'artifact',
      versionFilter: '1.0',
      page: 2,
      size: 50,
      sort: 'lastUpdated',
      direction: 'asc',
    });

    const url = mockedAxios.get.mock.calls[0][0] as string;
    expect(url).toContain('/service/rest/v1/search/versions?');
    expect(url).toContain('format=maven2');
    expect(url).toContain('group=org.test');
    expect(url).toContain('name=artifact');
    expect(url).toContain('version=1.0');
    expect(url).toContain('page=2');
    expect(url).toContain('size=50');
    expect(url).toContain('sort=lastUpdated');
    expect(url).toContain('direction=asc');
  });

  it('omits group and version when not supplied', async () => {
    mockedAxios.get.mockResolvedValue({ data: { items: [], total: 0, page: 0, size: 20 } });

    await fetchComponentVersions({
      format: 'npm',
      name: 'lodash',
      page: 0,
      size: 20,
      sort: 'version',
      direction: 'desc',
    });

    const url = mockedAxios.get.mock.calls[0][0] as string;
    expect(url).not.toContain('group=');
    expect(url).not.toContain('version=');
  });

  it('maps the response into GAVersion rows', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        items: [
          { version: '1.0.10', lastUpdated: '2026-02-01T00:00:00Z', repositories: ['releases'] },
        ],
        total: 4213,
        page: 0,
        size: 20,
      },
    });

    const page = await fetchComponentVersions({
      format: 'maven2',
      name: 'artifact',
      page: 0,
      size: 20,
      sort: 'version',
      direction: 'desc',
    });

    expect(page.total).toBe(4213);
    expect(page.items).toHaveLength(1);
    expect(page.items[0].version).toBe('1.0.10');
    expect(page.items[0].lastUpdated).toBe('2026-02-01T00:00:00Z');
    expect(page.items[0].repositories).toEqual(['releases']);
  });

  it('defaults a missing repositories array to empty', async () => {
    mockedAxios.get.mockResolvedValue({
      data: { items: [{ version: '1.0.0', lastUpdated: null }], total: 1, page: 0, size: 20 },
    });

    const page = await fetchComponentVersions({
      format: 'maven2',
      name: 'artifact',
      page: 0,
      size: 20,
      sort: 'version',
      direction: 'desc',
    });

    expect(page.items[0].repositories).toEqual([]);
  });

  it('degrades to an empty page with the request page/size echoed back when the response body is empty', async () => {
    mockedAxios.get.mockResolvedValue({ data: undefined });

    const page = await fetchComponentVersions({
      format: 'maven2',
      name: 'artifact',
      page: 3,
      size: 50,
      sort: 'version',
      direction: 'desc',
    });

    expect(page.items).toEqual([]);
    expect(page.total).toBe(0);
    expect(page.page).toBe(3);
    expect(page.size).toBe(50);
  });

  it('falls back to the request page/size when the response omits them', async () => {
    mockedAxios.get.mockResolvedValue({ data: { items: [] } });

    const page = await fetchComponentVersions({
      format: 'maven2',
      name: 'artifact',
      page: 2,
      size: 40,
      sort: 'version',
      direction: 'desc',
    });

    expect(page.total).toBe(0);
    expect(page.page).toBe(2);
    expect(page.size).toBe(40);
  });
});

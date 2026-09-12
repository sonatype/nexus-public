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
import { fetchComponentVersionDetail, MAX_DETAIL_PAGES } from '../componentVersionDetailApi';

jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const row = (repository: string, version: string, assets: unknown[] = []) => ({
  repository,
  format: 'maven2',
  group: 'org.sonatype.test',
  name: 'depth-fixture-v3',
  version,
  assets,
});

const asset = (path: string, lastModified?: string) => ({
  id: `asset-${path}`,
  repository: 'maven-hosted-1',
  path,
  downloadUrl: `http://localhost/repository/maven-hosted-1/${path}`,
  format: 'maven2',
  contentType: 'application/java-archive',
  fileSize: 1024,
  lastModified,
  checksum: { sha1: 'abc' },
});

const REQ = {
  format: 'maven2',
  group: 'org.sonatype.test',
  name: 'depth-fixture-v3',
  version: '1.0.500',
};

describe('fetchComponentVersionDetail', () => {
  beforeEach(() => jest.clearAllMocks());

  it('sends the version as a filter', async () => {
    mockedAxios.get.mockResolvedValue({ data: { items: [], continuationToken: null } });

    await fetchComponentVersionDetail(REQ);

    const url = mockedAxios.get.mock.calls[0][0] as string;
    expect(url).toContain('version=1.0.500');
    expect(url).toContain('format=maven2');
    expect(url).toContain('group=org.sonatype.test');
    expect(url).toContain('name=depth-fixture-v3');
  });

  it('sends the blank version versionless formats carry rather than omitting it', async () => {
    mockedAxios.get.mockResolvedValue({ data: { items: [], continuationToken: null } });

    await fetchComponentVersionDetail({ ...REQ, version: '' });

    // `version=` reaches the server as ExactTerm(''), which matches the versionless rows exactly.
    // Omitting it would de-scope the query to every version of this format+group+name.
    const url = mockedAxios.get.mock.calls[0][0] as string;
    expect(url).toContain('version=');
    expect(url).not.toMatch(/version=[^&]/);
  });

  it('collects assets across every repository holding the version', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        items: [
          row('maven-hosted-1', '1.0.500', [asset('a/1.0.500/a-1.0.500.jar', '2026-01-01T00:00:00Z')]),
          row('maven-hosted-3', '1.0.500', [asset('a/1.0.500/a-1.0.500.pom', '2026-01-02T00:00:00Z')]),
        ],
        continuationToken: null,
      },
    });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result.assets).toHaveLength(2);
    expect(result.repositories).toEqual(['maven-hosted-1', 'maven-hosted-3']);
  });

  it('reports the most recent asset timestamp, not the first', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        items: [
          row('maven-hosted-1', '1.0.500', [
            asset('a.jar', '2026-01-01T00:00:00Z'),
            asset('b.pom', '2026-06-01T00:00:00Z'),
          ]),
        ],
        continuationToken: null,
      },
    });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result.lastUpdated).toBe('2026-06-01T00:00:00Z');
  });

  it('returns null lastUpdated when no asset carries a timestamp', async () => {
    mockedAxios.get.mockResolvedValue({
      data: { items: [row('maven-hosted-1', '1.0.500', [asset('a.jar')])], continuationToken: null },
    });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result.lastUpdated).toBeNull();
  });

  it('derives the extension from the asset path', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        items: [row('maven-hosted-1', '1.0.500', [asset('a/1.0.500/a-1.0.500.POM')])],
        continuationToken: null,
      },
    });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result.assets[0].extension).toBe('pom');
  });

  it('stops at MAX_DETAIL_PAGES when the token never clears', async () => {
    mockedAxios.get.mockResolvedValue({
      data: { items: [row('r', '1.0.500')], continuationToken: 'never-ends' },
    });

    await fetchComponentVersionDetail(REQ);

    expect(mockedAxios.get).toHaveBeenCalledTimes(MAX_DETAIL_PAGES);
  });

  it('deduplicates repositories across pages', async () => {
    mockedAxios.get
      .mockResolvedValueOnce({
        data: { items: [row('maven-hosted-1', '1.0.500')], continuationToken: 'p2' },
      })
      .mockResolvedValueOnce({
        data: { items: [row('maven-hosted-1', '1.0.500')], continuationToken: null },
      });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result.repositories).toEqual(['maven-hosted-1']);
  });

  it('tolerates a null items array', async () => {
    mockedAxios.get.mockResolvedValue({ data: { items: null, continuationToken: null } });

    const result = await fetchComponentVersionDetail(REQ);

    expect(result).toEqual({ assets: [], repositories: [], lastUpdated: null });
  });
});

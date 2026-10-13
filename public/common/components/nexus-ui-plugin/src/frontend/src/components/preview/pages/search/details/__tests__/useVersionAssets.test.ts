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

import { renderHook, waitFor } from '@testing-library/react';

import { restClient } from '../../../../../../interface/api';
import { useVersionAssets } from '../useVersionAssets';

jest.mock('../../../../../../interface/api', () => ({
  ENDPOINTS: { SEARCH_ASSETS: '/service/rest/v1/search/assets' },
  restClient: { get: jest.fn() },
}));

const mockGet = restClient.get as jest.Mock;

describe('useVersionAssets', () => {
  beforeEach(() => jest.clearAllMocks());

  it('returns empty and does not fetch when version is null', async () => {
    const { result } = renderHook(() =>
      useVersionAssets({
        gaId: 'maven:org.apache.commons:commons-lang3',
        version: null,
        repository: 'maven-central',
      }),
    );
    expect(result.current.assets).toEqual([]);
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('fetches SEARCH_ASSETS with format/name/version/repository and maps downloadUrl', async () => {
    mockGet.mockResolvedValueOnce({
      items: [
        {
          id: 'a1',
          repository: 'maven-central',
          format: 'maven2',
          path: 'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
          downloadUrl:
            'http://nx/repository/maven-central/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
          contentType: 'application/java-archive',
          lastModified: '2024-01-01T00:00:00Z',
          fileSize: 100,
          checksum: { sha1: 'abc' },
        },
      ],
      continuationToken: null,
    });

    const { result } = renderHook(() =>
      useVersionAssets({
        gaId: 'maven:org.apache.commons:commons-lang3',
        version: '3.14.0',
        repository: 'maven-central',
      }),
    );

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/service/rest/v1/search/assets?'));
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain('format=maven');
    expect(url).toContain('name=commons-lang3');
    expect(url).toContain('version=3.14.0');
    expect(url).toContain('repository=maven-central');
    expect(result.current.assets).toHaveLength(1);
    expect(result.current.assets[0].downloadUrl).toContain('commons-lang3-3.14.0.jar');
    expect(result.current.assets[0].extension).toBe('jar');
  });

  it('labels an extensionless NuGet asset by format (AT-003)', async () => {
    // Shares deriveFileType with the Files tab, so the Download menu cannot show "16343"
    // where the table shows "nupkg". NEXUS-54612.
    mockGet.mockResolvedValueOnce({
      items: [
        {
          id: 'nuget-asset-1',
          repository: 'nuget-hosted',
          format: 'nuget',
          path: '/System.Net.Http/2.0.20126.16343',
          downloadUrl:
            'http://localhost/repository/nuget-hosted/System.Net.Http/2.0.20126.16343',
          contentType: 'application/octet-stream',
        },
      ],
      continuationToken: null,
    });

    const { result } = renderHook(() =>
      useVersionAssets({
        gaId: 'nuget:System.Net.Http',
        version: '2.0.20126.16343',
        repository: 'nuget-hosted',
      }),
    );

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.assets[0].extension).toBe('nupkg');
  });

  it('sends the group param for a namespaced (Maven) coordinate', async () => {
    // Without group, the search matches every artifact sharing this name across groups, so the
    // Download menu can resolve to an asset from the wrong coordinate.
    mockGet.mockResolvedValueOnce({ items: [], continuationToken: null });
    renderHook(() =>
      useVersionAssets({
        gaId: 'maven:org.apache.commons:commons-lang3',
        version: '3.14.0',
        repository: 'maven-central',
      }),
    );
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain('group=org.apache.commons');
  });

  it('omits the group param for a group-less (npm) coordinate', async () => {
    mockGet.mockResolvedValueOnce({ items: [], continuationToken: null });
    renderHook(() =>
      useVersionAssets({ gaId: 'npm:lodash', version: '4.17.21', repository: 'npm-proxy' }),
    );
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).not.toContain('group=');
  });

  it('percent-encodes a continuation token so a "+" is not read back as a space', async () => {
    // A raw token interpolated into the query string reaches the server with '+' decoded to a
    // space — a corrupted token that breaks pagination. URLSearchParams encodes it as %2B.
    mockGet
      .mockResolvedValueOnce({
        items: [{ id: 'p1', repository: 'r', format: 'maven2', path: 'a/lib-1.0.jar', downloadUrl: 'http://nx/1' }],
        continuationToken: 'a+b/c=',
      })
      .mockResolvedValueOnce({ items: [], continuationToken: null });

    renderHook(() => useVersionAssets({ gaId: 'maven:g:lib', version: '1.0', repository: 'r' }));

    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
    const secondUrl = mockGet.mock.calls[1][0] as string;
    expect(secondUrl).toContain('continuationToken=a%2Bb%2Fc%3D');
    expect(secondUrl).not.toContain('continuationToken=a+b');
  });

  it('omits repository param when repository is null (searches all repos for the version)', async () => {
    mockGet.mockResolvedValueOnce({ items: [], continuationToken: null });
    renderHook(() =>
      useVersionAssets({
        gaId: 'maven:org.apache.commons:commons-lang3',
        version: '3.14.0',
        repository: null,
      }),
    );
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).not.toContain('repository=');
  });

  it('sets error message on fetch failure', async () => {
    mockGet.mockRejectedValueOnce(new Error('boom'));
    const { result } = renderHook(() =>
      useVersionAssets({ gaId: 'maven:g:n', version: '1.0.0', repository: 'r' }),
    );
    await waitFor(() => expect(result.current.error).toBe('boom'));
    expect(result.current.assets).toEqual([]);
  });

  it('follows continuation tokens across pages and concatenates the assets', async () => {
    mockGet
      .mockResolvedValueOnce({
        items: [{ id: 'p1', repository: 'r', format: 'maven2', path: 'a/lib-1.0.jar', downloadUrl: 'http://nx/1' }],
        continuationToken: 'tok2',
      })
      .mockResolvedValueOnce({
        items: [{ id: 'p2', repository: 'r', format: 'maven2', path: 'a/lib-1.0.pom', downloadUrl: 'http://nx/2' }],
        continuationToken: null,
      });

    const { result } = renderHook(() =>
      useVersionAssets({ gaId: 'maven:g:lib', version: '1.0', repository: 'r' }),
    );

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mockGet).toHaveBeenCalledTimes(2);
    expect((mockGet.mock.calls[1][0] as string)).toContain('continuationToken=tok2');
    expect(result.current.assets.map((a) => a.id)).toEqual(['p1', 'p2']);
  });

  it('stops after a bounded number of pages if the API never returns a null token', async () => {
    // A malformed API that always echoes a token must not loop forever; the hook caps the walk.
    mockGet.mockResolvedValue({
      items: [{ id: 'x', repository: 'r', format: 'maven2', path: 'a/lib.jar', downloadUrl: 'http://nx/x' }],
      continuationToken: 'always',
    });
    const { result } = renderHook(() =>
      useVersionAssets({ gaId: 'maven:g:lib', version: '1.0', repository: 'r' }),
    );
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mockGet.mock.calls.length).toBeLessThanOrEqual(100);
    expect(mockGet.mock.calls.length).toBeGreaterThan(1);
  });
});

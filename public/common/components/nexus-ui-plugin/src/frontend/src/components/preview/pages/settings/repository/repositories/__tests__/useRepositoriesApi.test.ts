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

import { renderHook, act } from '@testing-library/react';

import { useRepositoriesApi } from '../useRepositoriesApi';

// Mock the REST API at the path the source uses
// Variables used in mock must have 'mock' prefix for hoisting
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

describe('useRepositoriesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchRepositories', () => {
    it('fetches repositories successfully using REST API', async () => {
      const mockData = [
        { name: 'repo1', type: 'proxy', format: 'maven2' },
        { name: 'repo2', type: 'hosted', format: 'npm' },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockData);

      const { result } = renderHook(() => useRepositoriesApi());

      let repositories: any;
      await act(async () => {
        repositories = await result.current.fetchRepositories();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/repositories/details/');
      expect(repositories).toHaveLength(2);
      expect(repositories[0].name).toBe('repo1');
    });

    it('handles fetch error', async () => {
      mockRestClient.get.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useRepositoriesApi());

      await expect(result.current.fetchRepositories()).rejects.toThrow();
    });

    it('returns empty array when result is not an array', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useRepositoriesApi());

      let repositories: any;
      await act(async () => {
        repositories = await result.current.fetchRepositories();
      });

      expect(repositories).toEqual([]);
    });
  });

  describe('createRepository', () => {
    it('creates repository successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'new-repo',
          type: 'hosted',
          format: 'maven2',
          recipe: 'maven2-hosted',
          online: true,
          storage: {
            blobStoreName: 'default',
            strictContentTypeValidation: true,
          },
        });
      });

      expect(result.current.error).toBeNull();
      expect(mockRestClient.post).toHaveBeenCalled();
    });

    it('handles create error and sets error state', async () => {
      mockRestClient.post.mockRejectedValueOnce({
        response: { data: { message: 'Repository already exists' } },
      });

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        try {
          await result.current.createRepository({
            name: 'existing-repo',
            type: 'hosted',
            format: 'maven2',
            recipe: 'maven2-hosted',
            online: true,
            storage: {
              blobStoreName: 'default',
              strictContentTypeValidation: true,
            },
          });
        } catch (_e) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Repository already exists');
    });
  });

  describe('buildRepositoryConfig - alpine signing', () => {
    it('always includes alpineSigning block for alpine group repositories even when keypair is blank', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'alpine-group',
          type: 'group',
          format: 'alpine',
          recipe: 'alpine-group',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          group: { memberNames: ['alpine-hosted'] },
          alpineSigning: { keypair: '', passphrase: '' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).toHaveProperty('alpineSigning');
      expect(postedBody.alpineSigning).toEqual({ keypair: '', passphrase: '' });
    });

    it('includes alpineSigning with keypair when provided for alpine group', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'alpine-group',
          type: 'group',
          format: 'alpine',
          recipe: 'alpine-group',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          group: { memberNames: ['alpine-hosted'] },
          alpineSigning: { keypair: '-----BEGIN PGP PRIVATE KEY-----\ntest\n-----END PGP PRIVATE KEY-----', passphrase: 'secret' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.alpineSigning.keypair).toContain('BEGIN PGP');
      expect(postedBody.alpineSigning.passphrase).toBe('secret');
    });

    it('omits alpineSigning for non-group alpine repositories when keypair is blank', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'alpine-hosted',
          type: 'hosted',
          format: 'alpine',
          recipe: 'alpine-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          alpineSigning: { keypair: '', passphrase: '' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('alpineSigning');
    });
  });

  describe('buildRepositoryConfig - pypi proxy', () => {
    // Regression: the previous build skipped emitting the `pypi` block
    // when `data.pypi` was undefined, so the backend converter never
    // ran on create and the saved repository had no PyPI attributes.
    // Now we always emit the block (with the `/simple` default) for pypi-proxy.
    //
    // Post-migration STL-381: the legacy `removeQuarantinedVersions` flag is gone
    // from PypiConfig — PCCS is now expressed as `firewall.mode = "PCCS"` on the
    // top-level repository config and the migration step strips the field from
    // existing repos. Tests below only assert on `indexPath`.
    it('always emits a pypi block for pypi proxy create, even when data.pypi is missing', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'pypi-proxy',
          type: 'proxy',
          format: 'pypi',
          recipe: 'pypi-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://pypi.org', contentMaxAge: 1440, metadataMaxAge: 1440 },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.pypi).toEqual({
        indexPath: '/simple',
      });
    });

    it('forwards user-provided pypi values verbatim on create', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'pypi-proxy',
          type: 'proxy',
          format: 'pypi',
          recipe: 'pypi-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://pypi.nvidia.com', contentMaxAge: 1440, metadataMaxAge: 1440 },
          pypi: { indexPath: '' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.pypi).toEqual({
        indexPath: '',
      });
    });

    it('forwards user-provided pypi values verbatim on update', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('pypi-proxy', {
          name: 'pypi-proxy',
          type: 'proxy',
          format: 'pypi',
          recipe: 'pypi-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://pypi.org', contentMaxAge: 1440, metadataMaxAge: 1440 },
          pypi: { indexPath: '/simple' },
        });
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody.pypi).toEqual({
        indexPath: '/simple',
      });
    });
  });

  describe('buildRepositoryConfig - nuget proxy', () => {
    // The symbol-server fields (symbolServerUrl, allowAnonymousSymbolAccess) are gated on
    // the nexus.nuget.symbol.server.enabled feature flag — matching the Classic UI (see
    // NugetProxy.js) and the backend routing (see NugetProxyRecipe.addSymSrvRoute) which
    // are both flag-gated on the same key. Emitting these fields when the flag is off
    // would silently persist inert config values that survive across flag toggles and
    // diverge from Classic UI's payload.
    const setFeatureFlag = (enabled: boolean): (() => void) => {
      const state = (window as unknown as { NX: { State: { getValue: jest.Mock } } }).NX.State;
      const original = state.getValue;
      state.getValue = jest.fn((key: string) => {
        if (key === 'nexus.nuget.symbol.server.enabled') return enabled;
        return undefined;
      }) as jest.Mock;
      return () => { state.getValue = original; };
    };

    it('does NOT emit symbolServerUrl or allowAnonymousSymbolAccess when flag is off', async () => {
      // Even if the form data somehow carries these fields (e.g., a stale form state or
      // an operator manipulating them via devtools), the flag-off code path must strip
      // them. This is the parity-with-Classic-UI contract when the feature is disabled.
      const restore = setFeatureFlag(false);
      try {
        mockRestClient.post.mockResolvedValueOnce({});

        const { result } = renderHook(() => useRepositoriesApi());

        await act(async () => {
          await result.current.createRepository({
            name: 'nuget-proxy',
            type: 'proxy',
            format: 'nuget',
            recipe: 'nuget-proxy',
            online: true,
            storage: { blobStoreName: 'default', strictContentTypeValidation: true },
            proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json', contentMaxAge: 1440, metadataMaxAge: 1440 },
            nugetProxy: {
              queryCacheItemMaxAge: 3600,
              nugetVersion: 'V3',
              symbolServerUrl: 'https://symbols.nuget.org/download/symbols',
              allowAnonymousSymbolAccess: false,
            },
          });
        });

        const postedBody = mockRestClient.post.mock.calls[0][1];
        expect(postedBody.nugetProxy).toEqual({
          queryCacheItemMaxAge: 3600,
          nugetVersion: 'V3',
        });
        expect(postedBody.nugetProxy).not.toHaveProperty('symbolServerUrl');
        expect(postedBody.nugetProxy).not.toHaveProperty('allowAnonymousSymbolAccess');
      } finally {
        restore();
      }
    });

    it('emits symbolServerUrl and allowAnonymousSymbolAccess when flag is on and user provides them', async () => {
      const restore = setFeatureFlag(true);
      try {
        mockRestClient.post.mockResolvedValueOnce({});

        const { result } = renderHook(() => useRepositoriesApi());

        await act(async () => {
          await result.current.createRepository({
            name: 'nuget-proxy',
            type: 'proxy',
            format: 'nuget',
            recipe: 'nuget-proxy',
            online: true,
            storage: { blobStoreName: 'default', strictContentTypeValidation: true },
            proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json', contentMaxAge: 1440, metadataMaxAge: 1440 },
            nugetProxy: {
              queryCacheItemMaxAge: 3600,
              nugetVersion: 'V3',
              symbolServerUrl: 'https://symbols.nuget.org/download/symbols',
              allowAnonymousSymbolAccess: false,
            },
          });
        });

        const postedBody = mockRestClient.post.mock.calls[0][1];
        expect(postedBody.nugetProxy).toEqual({
          queryCacheItemMaxAge: 3600,
          nugetVersion: 'V3',
          symbolServerUrl: 'https://symbols.nuget.org/download/symbols',
          allowAnonymousSymbolAccess: false,
        });
      } finally {
        restore();
      }
    });

    it('omits symbolServerUrl when empty (flag on) — backend stores null instead of ""', async () => {
      // Empty-string round-trip contract: an empty text field means "not configured",
      // not "an empty URL". Sending "" would persist an empty-string attribute that the
      // getSymbolServerBaseUrl helper on the backend treats the same as unset, but on
      // API GET it round-trips as "", not null — leaving the operator with a subtly
      // different config file. Omitting the key keeps the persisted attribute null.
      const restore = setFeatureFlag(true);
      try {
        mockRestClient.post.mockResolvedValueOnce({});

        const { result } = renderHook(() => useRepositoriesApi());

        await act(async () => {
          await result.current.createRepository({
            name: 'nuget-proxy',
            type: 'proxy',
            format: 'nuget',
            recipe: 'nuget-proxy',
            online: true,
            storage: { blobStoreName: 'default', strictContentTypeValidation: true },
            proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json', contentMaxAge: 1440, metadataMaxAge: 1440 },
            nugetProxy: {
              queryCacheItemMaxAge: 3600,
              nugetVersion: 'V3',
              symbolServerUrl: '',
              allowAnonymousSymbolAccess: true,
            },
          });
        });

        const postedBody = mockRestClient.post.mock.calls[0][1];
        expect(postedBody.nugetProxy).not.toHaveProperty('symbolServerUrl');
        expect(postedBody.nugetProxy.allowAnonymousSymbolAccess).toBe(true);
      } finally {
        restore();
      }
    });

    it('defaults allowAnonymousSymbolAccess to true (flag on, user did not toggle)', async () => {
      // Mirrors Classic UI's NugetProxy.js which seeds the checkbox `value: true`, and
      // the backend NugetAttributes.allowAnonymousSymbolAccess default. Preview UI
      // should not silently invert this default just because the user hasn't touched
      // the checkbox during a create flow that doesn't scroll past the field.
      const restore = setFeatureFlag(true);
      try {
        mockRestClient.post.mockResolvedValueOnce({});

        const { result } = renderHook(() => useRepositoriesApi());

        await act(async () => {
          await result.current.createRepository({
            name: 'nuget-proxy',
            type: 'proxy',
            format: 'nuget',
            recipe: 'nuget-proxy',
            online: true,
            storage: { blobStoreName: 'default', strictContentTypeValidation: true },
            proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json', contentMaxAge: 1440, metadataMaxAge: 1440 },
            nugetProxy: {
              queryCacheItemMaxAge: 3600,
              nugetVersion: 'V3',
            },
          });
        });

        const postedBody = mockRestClient.post.mock.calls[0][1];
        expect(postedBody.nugetProxy.allowAnonymousSymbolAccess).toBe(true);
      } finally {
        restore();
      }
    });
  });

  describe('buildRepositoryConfig - npm proxy', () => {
    // Post-migration STL-381: the npm block is no longer emitted at all for
    // npm-proxy. Its only field was the legacy `removeQuarantinedVersions` flag,
    // which is now redundant with `firewall.mode = "PCCS"` (set via the Firewall
    // tab) and is stripped from existing repos by the migration step. The
    // `NpmConfig` type was removed accordingly — see types.ts.
    it('does not emit an npm block for npm proxy create', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'npm-proxy',
          type: 'proxy',
          format: 'npm',
          recipe: 'npm-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://registry.npmjs.org', contentMaxAge: 1440, metadataMaxAge: 1440 },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('npm');
    });

    it('does not emit an npm block for npm proxy update', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('npm-proxy', {
          name: 'npm-proxy',
          type: 'proxy',
          format: 'npm',
          recipe: 'npm-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://registry.npmjs.org', contentMaxAge: 1440, metadataMaxAge: 1440 },
        });
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody).not.toHaveProperty('npm');
    });
  });

  describe('updateRepository', () => {
    it('updates repository successfully', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('repo1', {
          name: 'repo1',
          type: 'hosted',
          format: 'maven2',
          recipe: 'maven2-hosted',
          online: false,
          storage: {
            blobStoreName: 'default',
            strictContentTypeValidation: true,
          },
        });
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('deleteRepository', () => {
    it('deletes repository successfully', async () => {
      mockRestClient.delete.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.deleteRepository('repo1');
      });

      expect(result.current.error).toBeNull();
    });

    it('handles delete error', async () => {
      mockRestClient.delete.mockRejectedValueOnce({
        response: { data: { message: 'Repository in use' } },
      });

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        try {
          await result.current.deleteRepository('repo1');
        } catch (_e) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Repository in use');
    });
  });

  describe('fetchBlobStores', () => {
    it('fetches blob stores successfully', async () => {
      mockRestClient.get.mockResolvedValueOnce([
        { name: 'default', type: 'file' },
        { name: 'secondary', type: 's3' },
      ]);

      const { result } = renderHook(() => useRepositoriesApi());

      let blobStores: any;
      await act(async () => {
        blobStores = await result.current.fetchBlobStores();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/blobstores');
      expect(blobStores).toHaveLength(2);
      expect(blobStores[0].name).toBe('default');
    });
  });

  describe('fetchRecipes', () => {
    it('throws error when no recipes available', async () => {
      mockRestClient.get.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useRepositoriesApi());

      await expect(result.current.fetchRecipes()).rejects.toThrow('No repository recipes available');
    });
  });

  describe('fetchRoutingRules', () => {
    it('fetches routing rules successfully', async () => {
      mockRestClient.get.mockResolvedValueOnce([
        { id: '1', name: 'block-snapshots', mode: 'BLOCK' },
      ]);

      const { result } = renderHook(() => useRepositoriesApi());

      let rules: any;
      await act(async () => {
        rules = await result.current.fetchRoutingRules();
      });

      expect(rules).toHaveLength(1);
      expect(rules[0].name).toBe('block-snapshots');
    });
  });

  describe('fetchCleanupPolicies', () => {
    it('calls the internal cleanup-policies endpoint so CE returns 200, not 404', async () => {
      mockRestClient.get.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.fetchCleanupPolicies();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/cleanup-policies');
    });

    it('returns all policies when no format filter is supplied', async () => {
      const policies = [
        { name: 'p1', format: 'maven2' },
        { name: 'p2', format: 'npm' },
      ];
      mockRestClient.get.mockResolvedValueOnce(policies);

      const { result } = renderHook(() => useRepositoriesApi());

      let fetched: any;
      await act(async () => {
        fetched = await result.current.fetchCleanupPolicies();
      });

      expect(fetched).toEqual(policies);
    });

    it('filters policies by format client-side when format is provided', async () => {
      mockRestClient.get.mockResolvedValueOnce([
        { name: 'p1', format: 'maven2' },
        { name: 'p2', format: 'npm' },
        { name: 'p3', format: 'maven2' },
      ]);

      const { result } = renderHook(() => useRepositoriesApi());

      let fetched: any;
      await act(async () => {
        fetched = await result.current.fetchCleanupPolicies('maven2');
      });

      expect(fetched).toEqual([
        { name: 'p1', format: 'maven2' },
        { name: 'p3', format: 'maven2' },
      ]);
    });

    it('includes the all-formats sentinel "*" alongside format-matching policies when format filter is applied', async () => {
      mockRestClient.get.mockResolvedValueOnce([
        { name: 'maven-only', format: 'maven2' },
        { name: 'all-formats', format: '*' },
        { name: 'npm-only', format: 'npm' },
      ]);

      const { result } = renderHook(() => useRepositoriesApi());

      let fetched: any;
      await act(async () => {
        fetched = await result.current.fetchCleanupPolicies('maven2');
      });

      expect(fetched).toEqual([
        { name: 'maven-only', format: 'maven2' },
        { name: 'all-formats', format: '*' },
      ]);
    });

    it('returns empty array when the response is not an array', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useRepositoriesApi());

      let fetched: any;
      await act(async () => {
        fetched = await result.current.fetchCleanupPolicies();
      });

      expect(fetched).toEqual([]);
    });

    it('returns empty array and swallows the error when the request fails', async () => {
      mockRestClient.get.mockRejectedValueOnce(new Error('boom'));
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      const { result } = renderHook(() => useRepositoriesApi());

      let fetched: any;
      await act(async () => {
        fetched = await result.current.fetchCleanupPolicies();
      });

      expect(fetched).toEqual([]);
      consoleErrorSpy.mockRestore();
    });
  });

  describe('invalidateCache', () => {
    it('invalidates cache successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.invalidateCache('maven-central');
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('rebuildIndex', () => {
    it('rebuilds index successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      let message: any;
      await act(async () => {
        message = await result.current.rebuildIndex('maven-releases');
      });

      expect(message).toBe('Index rebuild started');
      expect(result.current.error).toBeNull();
    });
  });

  describe('setError', () => {
    it('allows setting error state manually', async () => {
      const { result } = renderHook(() => useRepositoriesApi());

      act(() => {
        result.current.setError('Custom error');
      });

      expect(result.current.error).toBe('Custom error');

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});

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

  describe('fetchRepository', () => {
    // NEXUS-54019: the edit form must read repository config through the internal admin-gated
    // endpoint (repository-admin READ) FIRST — the SAME permission the list uses. The public v1
    // GET /v1/repositories/{name} (which gates on repository-view read/browse) is now only a
    // best-effort enrichment for UI-only fields. Reading v1 first let an admin-read-only user
    // see the list but 403 opening a repo.
    it('reads detail from the internal admin-gated endpoint first, then enriches from v1', async () => {
      mockRestClient.get
        .mockResolvedValueOnce({ name: 'repo1', format: 'npm', type: 'hosted', online: true })
        .mockResolvedValueOnce({ name: 'repo1', format: 'npm', type: 'hosted', status: { online: true } });

      const { result } = renderHook(() => useRepositoriesApi());

      let repo: any;
      await act(async () => {
        repo = await result.current.fetchRepository('repo1');
      });

      expect(mockRestClient.get).toHaveBeenNthCalledWith(
        1,
        '/service/rest/internal/ui/repositories/repository/repo1',
      );
      expect(mockRestClient.get).toHaveBeenNthCalledWith(2, '/service/rest/v1/repositories/repo1');
      expect(repo.name).toBe('repo1');
      expect(repo.status).toEqual({ online: true });
    });

    it('returns the admin config when the v1 enrichment call 403s (admin-read-only user)', async () => {
      mockRestClient.get
        .mockResolvedValueOnce({ name: 'repo1', format: 'npm', type: 'hosted', online: true })
        .mockRejectedValueOnce({ response: { status: 403 }, message: 'Forbidden' });

      const { result } = renderHook(() => useRepositoriesApi());

      let repo: any;
      await act(async () => {
        repo = await result.current.fetchRepository('repo1');
      });

      expect(repo).toEqual({ name: 'repo1', format: 'npm', type: 'hosted', online: true });
    });

    it('returns null when the repository is not found (404)', async () => {
      mockRestClient.get.mockRejectedValueOnce({ response: { status: 404 }, message: 'Not Found' });

      const { result } = renderHook(() => useRepositoriesApi());

      let repo: any;
      await act(async () => {
        repo = await result.current.fetchRepository('missing');
      });

      expect(repo).toBeNull();
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

  describe('buildRepositoryConfig - globalDistribution', () => {
    it('omits globalDistribution when the form data has no globalDistribution field', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'maven-hosted',
          type: 'hosted',
          format: 'maven2',
          recipe: 'maven2-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.storage.globalDistribution).toBeUndefined();
    });

    it('sends globalDistribution when the form data carries it', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'maven-hosted',
          type: 'hosted',
          format: 'maven2',
          recipe: 'maven2-hosted',
          online: true,
          storage: {
            blobStoreName: 'default',
            strictContentTypeValidation: true,
            globalDistribution: true,
          },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.storage.globalDistribution).toBe(true);
    });

    it('sends globalDistribution=false when the checkbox is cleared', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'maven-hosted',
          type: 'hosted',
          format: 'maven2',
          recipe: 'maven2-hosted',
          online: true,
          storage: {
            blobStoreName: 'default',
            strictContentTypeValidation: true,
            globalDistribution: false,
          },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.storage.globalDistribution).toBe(false);
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

  describe('buildRepositoryConfig - terraform signing', () => {
    it('includes terraformSigning with keypair and passphrase for terraform hosted', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'terraform-hosted',
          type: 'hosted',
          format: 'terraform',
          recipe: 'terraform-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          terraformSigning: {
            keypair: '-----BEGIN PGP PRIVATE KEY BLOCK-----\ntest\n-----END PGP PRIVATE KEY BLOCK-----',
            passphrase: 'secret',
          },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.terraformSigning.keypair).toContain('BEGIN PGP');
      expect(postedBody.terraformSigning.passphrase).toBe('secret');
    });

    // Regression: the Preview UI previously omitted terraformSigning entirely, so the
    // backend @NotNull on TerraformHostedRepositoryApiRequest.terraformSigning rejected
    // every terraform hosted create with a bare "must not be null".
    it('always includes terraformSigning block for terraform hosted so create is not rejected as null', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'terraform-hosted',
          type: 'hosted',
          format: 'terraform',
          recipe: 'terraform-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).toHaveProperty('terraformSigning');
      expect(postedBody.terraformSigning).toEqual({ keypair: '', passphrase: null });
    });

    it('sends null passphrase when none is provided for terraform hosted', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'terraform-hosted',
          type: 'hosted',
          format: 'terraform',
          recipe: 'terraform-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          terraformSigning: { keypair: 'key-content', passphrase: '' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.terraformSigning.keypair).toBe('key-content');
      expect(postedBody.terraformSigning.passphrase).toBeNull();
    });

    it('omits terraformSigning for terraform proxy repositories', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'terraform-proxy',
          type: 'proxy',
          format: 'terraform',
          recipe: 'terraform-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://registry.terraform.io', contentMaxAge: 1440, metadataMaxAge: 1440 },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('terraformSigning');
    });

    it('omits terraformSigning for terraform group repositories', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'terraform-group',
          type: 'group',
          format: 'terraform',
          recipe: 'terraform-group',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          group: { memberNames: ['terraform-hosted'] },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('terraformSigning');
    });
  });

  describe('buildRepositoryConfig - oci (NEXUS-54771)', () => {
    // Regression: the Preview UI emitted no `oci` block at all, so every create
    // POST was rejected with 400 `request.oci: must not be null` and no repository
    // was created. `v1Enabled` and `forceBasicAuth` are @NotNull on the REST model,
    // so the block must always be present with non-null booleans.
    const ociProxyFormData = {
      name: 'oci-proxy',
      type: 'proxy' as const,
      format: 'oci',
      recipe: 'oci-proxy',
      online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://registry-1.docker.io', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };

    it('always emits an oci block with non-null required booleans, even when untouched', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({ ...ociProxyFormData });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.oci).toEqual({
        v1Enabled: false,
        forceBasicAuth: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
        pathEnabled: true,
      });
      expect(postedBody.oci.v1Enabled).not.toBeNull();
      expect(postedBody.oci.forceBasicAuth).not.toBeNull();
    });

    it('emits an ociProxy block for oci proxy create', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({ ...ociProxyFormData });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.ociProxy).toEqual({
        indexType: 'REGISTRY',
        indexUrl: null,
        cacheForeignLayers: false,
        foreignLayerUrlWhitelist: [],
      });
    });

    it('forwards user-provided oci values verbatim', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          ...ociProxyFormData,
          oci: {
            v1Enabled: true,
            forceBasicAuth: false,
            pathEnabled: false,
            httpPort: 8082,
            httpsPort: 8083,
            subdomain: 'oci-hosted',
          },
          ociProxy: {
            indexType: 'CUSTOM',
            indexUrl: 'https://idx.example.com',
            cacheForeignLayers: true,
            foreignLayerUrlWhitelist: ['^https://good\\.example\\.com/.*$'],
          },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.oci).toEqual({
        v1Enabled: true,
        forceBasicAuth: false,
        httpPort: 8082,
        httpsPort: 8083,
        subdomain: 'oci-hosted',
        pathEnabled: false,
      });
      expect(postedBody.ociProxy).toEqual({
        indexType: 'CUSTOM',
        indexUrl: 'https://idx.example.com',
        cacheForeignLayers: true,
        foreignLayerUrlWhitelist: ['^https://good\\.example\\.com/.*$'],
      });
    });

    // Guard against re-adding a field the REST model does not have. Unlike Docker, the OCI
    // backend has no truststore-for-index concept at any layer, so emitting one would be
    // meaningless. The form type no longer carries it either; the cast keeps this test honest
    // if someone re-adds the field to the type.
    it('never emits useTrustStoreForIndexAccess (no such field on the REST model)', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          ...ociProxyFormData,
          ociProxy: {
            indexType: 'CUSTOM',
            indexUrl: 'https://idx.example.com',
            useTrustStoreForIndexAccess: true,
          } as typeof ociProxyFormData.ociProxy,
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.ociProxy).not.toHaveProperty('useTrustStoreForIndexAccess');
    });

    it('emits cosign as a top-level sibling of oci, not nested inside it', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          ...ociProxyFormData,
          cosign: { enforcement: 'NONE' },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.cosign).toEqual({
        enforcement: 'NONE',
        identityRegex: null,
        issuerRegex: null,
      });
      expect(postedBody.oci).not.toHaveProperty('cosign');
    });

    // The server rejects cosign.enforcement=KEYLESS unconditionally on every OCI recipe with
    // HTTP 400 ("Cosign keyless enforcement is not yet available"), because the shipped verifier
    // is a no-op stub. Verified against a live 3.97.0-SNAPSHOT instance. The UI must therefore
    // never put that value on the wire, or the save fails and nothing is persisted.
    it('downgrades a legacy KEYLESS policy to NONE instead of emitting a payload the server rejects', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          ...ociProxyFormData,
          cosign: {
            enforcement: 'KEYLESS',
            identityRegex: '^https://github\\.com/acme/.*$',
            issuerRegex: '^https://token\\.example$',
          },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody.cosign.enforcement).toBe('NONE');
      // Regexes only carry meaning under KEYLESS and the server drops them on NONE, so they are
      // not forwarded when downgrading.
      expect(postedBody.cosign.identityRegex).toBeNull();
      expect(postedBody.cosign.issuerRegex).toBeNull();
    });

    // setRepositoryOnline GETs the full config and PUTs it straight back with `online` flipped.
    // For a legacy KEYLESS repository that round-trips the rejected value, so the Online/Offline
    // toggle used to fail with a 400 for exactly the repositories this handling exists for. Both
    // write paths share sanitizeCosignForWrite so the invariant holds in one place.
    it('downgrades a legacy KEYLESS policy to NONE when toggling a repository online', async () => {
      mockRestClient.get.mockResolvedValueOnce({
        name: 'oci-legacy',
        format: 'oci',
        type: 'proxy',
        online: false,
        proxy: { remoteUrl: 'https://ghcr.io' },
        cosign: {
          enforcement: 'KEYLESS',
          identityRegex: '^https://github\\.com/acme/.*$',
          issuerRegex: '^https://token\\.example$',
        },
      });
      mockRestClient.put.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.setRepositoryOnline('oci-legacy', 'oci', 'proxy', true);
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody.online).toBe(true);
      expect(putBody.cosign.enforcement).toBe('NONE');
      expect(putBody.cosign.identityRegex).toBeNull();
      expect(putBody.cosign.issuerRegex).toBeNull();
    });

    it('leaves a non-KEYLESS cosign policy untouched when toggling a repository online', async () => {
      mockRestClient.get.mockResolvedValueOnce({
        name: 'oci-plain',
        format: 'oci',
        type: 'proxy',
        online: true,
        proxy: { remoteUrl: 'https://ghcr.io' },
        cosign: { enforcement: 'NONE', identityRegex: null, issuerRegex: null },
      });
      mockRestClient.put.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.setRepositoryOnline('oci-plain', 'oci', 'proxy', false);
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody.online).toBe(false);
      expect(putBody.cosign).toEqual({
        enforcement: 'NONE',
        identityRegex: null,
        issuerRegex: null,
      });
    });

    it('omits cosign when the user never selected a policy', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({ ...ociProxyFormData });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('cosign');
    });

    it('omits ociProxy for oci hosted repositories', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'oci-hosted',
          type: 'hosted',
          format: 'oci',
          recipe: 'oci-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('ociProxy');
      // `oci` is required on hosted and group recipes too.
      expect(postedBody.oci).toMatchObject({ v1Enabled: false, forceBasicAuth: true });
    });

    it('does not emit oci blocks for non-oci formats', async () => {
      mockRestClient.post.mockResolvedValueOnce({});
      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.createRepository({
          name: 'raw-hosted',
          type: 'hosted',
          format: 'raw',
          recipe: 'raw-hosted',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        });
      });

      const postedBody = mockRestClient.post.mock.calls[0][1];
      expect(postedBody).not.toHaveProperty('oci');
      expect(postedBody).not.toHaveProperty('ociProxy');
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

  describe('buildRepositoryConfig - huggingface proxy', () => {
    // Regression NEXUS-48685: without an `xet` block on the update payload,
    // the backend request-to-configuration converter short-circuits (see
    // HuggingFaceProxyRepositoryApiRequestToConfigurationConverter) and the
    // user's toggle change never lands on the repository configuration —
    // saving from the React UI silently reverts to server defaults, and
    // downstream deserialization surfaces a null error on the client.
    it('emits xet.enabled on update when the user toggled the XET checkbox off', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('huggingface-proxy', {
          name: 'huggingface-proxy',
          type: 'proxy',
          format: 'huggingface',
          recipe: 'huggingface-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://huggingface.co/', contentMaxAge: 1440, metadataMaxAge: 1440 },
          xet: { enabled: false },
        });
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody.xet).toEqual({ enabled: false });
    });

    it('emits xet.enabled on update when the user toggled the XET checkbox on', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('huggingface-proxy', {
          name: 'huggingface-proxy',
          type: 'proxy',
          format: 'huggingface',
          recipe: 'huggingface-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://huggingface.co/', contentMaxAge: 1440, metadataMaxAge: 1440 },
          xet: { enabled: true },
        });
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody.xet).toEqual({ enabled: true });
    });

    it('omits xet from the payload when the form data has no xet block (non-HF-proxy or unset)', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRepositoriesApi());

      await act(async () => {
        await result.current.updateRepository('huggingface-proxy', {
          name: 'huggingface-proxy',
          type: 'proxy',
          format: 'huggingface',
          recipe: 'huggingface-proxy',
          online: true,
          storage: { blobStoreName: 'default', strictContentTypeValidation: true },
          proxy: { remoteUrl: 'https://huggingface.co/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        });
      });

      const putBody = mockRestClient.put.mock.calls[0][1];
      expect(putBody).not.toHaveProperty('xet');
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

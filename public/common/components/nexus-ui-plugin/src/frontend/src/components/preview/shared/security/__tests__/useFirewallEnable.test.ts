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

const mockGet = jest.fn();
const mockPut = jest.fn();
jest.mock('../../../../../interface/api', () => {
  const actual = jest.requireActual<typeof import('../../../../../interface/api')>(
    '../../../../../interface/api',
  );
  return {
    ...actual,
    restClient: {
      ...actual.restClient,
      get: (...args: unknown[]) => mockGet(...args),
      put: (...args: unknown[]) => mockPut(...args),
    },
  };
});

import {
  __resetPccsFormatsCacheForTests,
  disableFirewall,
  enableFirewallAudit,
  enableFirewallPccs,
  enableFirewallQuarantine,
  fetchIqAuditStatus,
  fetchPccsSupportedFormats,
  setFirewallMode,
} from '../useFirewallEnable';

describe('useFirewallEnable module', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetPccsFormatsCacheForTests();
  });

  describe('fetchPccsSupportedFormats', () => {
    it('returns formats marked as pccsModeSupported', async () => {
      mockGet.mockResolvedValueOnce([
        { format: 'npm', pccsModeSupported: true },
        { format: 'pypi', pccsModeSupported: true },
        { format: 'maven2', pccsModeSupported: false },
      ]);

      const formats = await fetchPccsSupportedFormats();

      expect(formats).toEqual(['npm', 'pypi']);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/repositories/firewall/format-capabilities');
    });

    it('caches result across calls (single network round-trip)', async () => {
      mockGet.mockResolvedValueOnce([{ format: 'npm', pccsModeSupported: true }]);

      await fetchPccsSupportedFormats();
      await fetchPccsSupportedFormats();
      await fetchPccsSupportedFormats();

      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    it('falls back to npm/pypi when the API call fails (matches legacy fallback)', async () => {
      mockGet.mockRejectedValueOnce(new Error('network down'));

      const formats = await fetchPccsSupportedFormats();

      expect(formats).toEqual(['npm', 'pypi']);
    });
  });

  describe('setFirewallMode', () => {
    it('reads the typed repo, sets firewall.mode, and PUTs the modified body', async () => {
      mockGet
        // basic
        .mockResolvedValueOnce({ format: 'maven2', type: 'proxy' })
        // typed
        .mockResolvedValueOnce({
          name: 'maven-central',
          format: 'maven2',
          type: 'proxy',
          url: 'http://example/maven-central',
          online: true,
          storage: { blobStoreName: 'default' },
          firewall: null,
        });
      mockPut.mockResolvedValueOnce(undefined);

      await setFirewallMode('maven-central', 'AUDIT');

      // Maven format is mapped maven2 -> maven for the typed REST path.
      expect(mockGet).toHaveBeenNthCalledWith(2, '/service/rest/v1/repositories/maven/proxy/maven-central');
      expect(mockPut).toHaveBeenCalledTimes(1);
      const [putUrl, putBody] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(putUrl).toBe('/service/rest/v1/repositories/maven/proxy/maven-central');
      // Read-only fields stripped on the way out
      expect(putBody.format).toBeUndefined();
      expect(putBody.type).toBeUndefined();
      expect(putBody.url).toBeUndefined();
      // Other fields preserved
      expect(putBody.name).toBe('maven-central');
      expect(putBody.storage).toEqual({ blobStoreName: 'default' });
      // Mode applied
      expect(putBody.firewall).toEqual({ mode: 'AUDIT' });
    });

    it('uses the format directly when it is not maven (no mapping)', async () => {
      mockGet
        .mockResolvedValueOnce({ format: 'npm', type: 'proxy' })
        .mockResolvedValueOnce({ name: 'npm-proxy', online: true });
      mockPut.mockResolvedValueOnce(undefined);

      await setFirewallMode('npm-proxy', 'PCCS');

      expect(mockGet).toHaveBeenNthCalledWith(2, '/service/rest/v1/repositories/npm/proxy/npm-proxy');
      const [, putBody] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(putBody.firewall).toEqual({ mode: 'PCCS' });
    });

    it('throws if the basic repository lookup returns nothing', async () => {
      mockGet.mockResolvedValueOnce(null);

      await expect(setFirewallMode('ghost-repo', 'AUDIT')).rejects.toThrow(/not found/);
      expect(mockPut).not.toHaveBeenCalled();
    });
  });

  describe('convenience wrappers', () => {
    function stubLookup() {
      mockGet
        .mockResolvedValueOnce({ format: 'npm', type: 'proxy' })
        .mockResolvedValueOnce({ name: 'r', online: true });
      mockPut.mockResolvedValueOnce(undefined);
    }

    it('enableFirewallAudit funnels through setFirewallMode("AUDIT")', async () => {
      stubLookup();
      await enableFirewallAudit('r');
      const [, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(body.firewall).toEqual({ mode: 'AUDIT' });
    });

    it('enableFirewallQuarantine funnels through setFirewallMode("QUARANTINE")', async () => {
      stubLookup();
      await enableFirewallQuarantine('r');
      const [, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(body.firewall).toEqual({ mode: 'QUARANTINE' });
    });

    it('enableFirewallPccs funnels through setFirewallMode("PCCS")', async () => {
      stubLookup();
      await enableFirewallPccs('r');
      const [, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(body.firewall).toEqual({ mode: 'PCCS' });
    });

    it('disableFirewall funnels through setFirewallMode("DISABLED")', async () => {
      stubLookup();
      await disableFirewall('r');
      const [, body] = mockPut.mock.calls[0] as [string, Record<string, unknown>];
      expect(body.firewall).toEqual({ mode: 'DISABLED' });
    });
  });

  describe('fetchIqAuditStatus', () => {
    it('reads firewall.mode from the typed repo and returns full + boolean view', async () => {
      mockGet
        .mockResolvedValueOnce({ format: 'npm', type: 'proxy' })
        .mockResolvedValueOnce({ firewall: { mode: 'PCCS' } });

      const status = await fetchIqAuditStatus('npm-proxy');

      expect(status).toEqual({
        repositoryName: 'npm-proxy',
        enabled: true,
        enabledQuarantine: true, // PCCS shows as quarantine in the boolean view (legacy callers)
        mode: 'PCCS',
      });
    });

    it('returns DISABLED + booleans for repos with no firewall configured', async () => {
      mockGet
        .mockResolvedValueOnce({ format: 'maven2', type: 'proxy' })
        .mockResolvedValueOnce({ firewall: null });

      const status = await fetchIqAuditStatus('maven-central');

      expect(status).toEqual({
        repositoryName: 'maven-central',
        enabled: false,
        enabledQuarantine: false,
        mode: 'DISABLED',
      });
    });

    it('returns null when the basic lookup fails', async () => {
      mockGet.mockRejectedValueOnce(new Error('boom'));
      const status = await fetchIqAuditStatus('ghost');
      expect(status).toBeNull();
    });

    it('treats unknown mode strings as DISABLED (defensive)', async () => {
      mockGet
        .mockResolvedValueOnce({ format: 'maven2', type: 'proxy' })
        .mockResolvedValueOnce({ firewall: { mode: 'BOGUS' } });

      const status = await fetchIqAuditStatus('maven-central');

      expect(status?.mode).toBe('DISABLED');
      expect(status?.enabled).toBe(false);
    });
  });
});

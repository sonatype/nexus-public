/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import axios from 'axios';
import { IpAllowListApi, transformEntryToUI } from '../IpAllowListApi';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
}));

const API_BASE = '/service/rest/v1/security/ip-allowlist';

describe('IpAllowListApi', () => {
  describe('getSettings', () => {
    it('calls GET on the base URL and returns response data', async () => {
      const mockSettings = { mode: 'DISABLED', totalEntries: 5, maxEntries: 256 };
      axios.get.mockResolvedValue({ data: mockSettings });

      const result = await IpAllowListApi.getSettings();

      expect(axios.get).toHaveBeenCalledWith(API_BASE);
      expect(result).toEqual(mockSettings);
    });
  });

  describe('updateMode', () => {
    it('uppercases the mode before sending to PUT /mode', async () => {
      axios.put.mockResolvedValue({ data: {} });

      await IpAllowListApi.updateMode('enforce');

      expect(axios.put).toHaveBeenCalledWith(`${API_BASE}/mode`, { mode: 'ENFORCE' });
    });

    it('handles already-uppercase mode', async () => {
      axios.put.mockResolvedValue({ data: {} });

      await IpAllowListApi.updateMode('MONITOR');

      expect(axios.put).toHaveBeenCalledWith(`${API_BASE}/mode`, { mode: 'MONITOR' });
    });
  });

  describe('getEntries', () => {
    it('sends page and pageSize as query params', async () => {
      axios.get.mockResolvedValue({ data: { entries: [], totalEntries: 0, totalPages: 0 } });

      await IpAllowListApi.getEntries(2, 10);

      expect(axios.get).toHaveBeenCalledWith(`${API_BASE}/entries`, {
        params: { page: 2, pageSize: 10 },
      });
    });

    it('includes search param when non-empty string is provided', async () => {
      axios.get.mockResolvedValue({ data: { entries: [], totalEntries: 0, totalPages: 0 } });

      await IpAllowListApi.getEntries(0, 20, '192.168');

      expect(axios.get).toHaveBeenCalledWith(`${API_BASE}/entries`, {
        params: { page: 0, pageSize: 20, search: '192.168' },
      });
    });

    it('omits search param when null', async () => {
      axios.get.mockResolvedValue({ data: { entries: [], totalEntries: 0, totalPages: 0 } });

      await IpAllowListApi.getEntries(0, 20, null);

      expect(axios.get).toHaveBeenCalledWith(`${API_BASE}/entries`, {
        params: { page: 0, pageSize: 20 },
      });
    });

    it('omits search param when whitespace only', async () => {
      axios.get.mockResolvedValue({ data: { entries: [], totalEntries: 0, totalPages: 0 } });

      await IpAllowListApi.getEntries(0, 20, '   ');

      expect(axios.get).toHaveBeenCalledWith(`${API_BASE}/entries`, {
        params: { page: 0, pageSize: 20 },
      });
    });
  });

  describe('bulkUploadCsv', () => {
    it('sends csvContent in POST body to /entries/bulk', async () => {
      const csvContent = '192.168.1.1,Office\n10.0.0.0/24,Internal';
      const mockResult = { addedCount: 2, skippedCount: 0, rejectedCount: 0, rejectedEntries: [] };
      axios.post.mockResolvedValue({ data: mockResult });

      const result = await IpAllowListApi.bulkUploadCsv(csvContent);

      expect(axios.post).toHaveBeenCalledWith(`${API_BASE}/entries/bulk`, { csvContent });
      expect(result.addedCount).toBe(2);
    });
  });

  describe('bulkDelete', () => {
    it('sends ids array as DELETE body and returns deleted count', async () => {
      axios.delete.mockResolvedValue({ data: { deleted: 2, failedIds: [] } });

      const result = await IpAllowListApi.bulkDelete(['id-1', 'id-2']);

      expect(axios.delete).toHaveBeenCalledWith(`${API_BASE}/entries/bulk`, {
        data: ['id-1', 'id-2'],
      });
      expect(result.deleted).toBe(2);
      expect(result.failed).toHaveLength(0);
    });

    it('maps failedIds in the response to failed entries', async () => {
      axios.delete.mockResolvedValue({ data: { deleted: 1, failedIds: ['bad-id'] } });

      const result = await IpAllowListApi.bulkDelete(['good-id', 'bad-id']);

      expect(result.failed).toHaveLength(1);
      expect(result.failed[0].id).toBe('bad-id');
      expect(result.failed[0].reason).toBe('Entry not found');
    });

    it('returns empty result and does not call axios for an empty array', async () => {
      const result = await IpAllowListApi.bulkDelete([]);

      expect(result).toEqual({ deleted: 0, failed: [] });
      expect(axios.delete).not.toHaveBeenCalled();
    });
  });

  describe('bulkAdd', () => {
    it('calls addEntry for each item and counts successes', async () => {
      axios.post.mockResolvedValue({ data: {} });

      const result = await IpAllowListApi.bulkAdd([
        { ipAddress: '192.168.1.1', description: 'Office' },
        { ipAddress: '10.0.0.0/24', description: 'Internal' },
      ]);

      expect(axios.post).toHaveBeenCalledTimes(2);
      expect(result.added).toBe(2);
      expect(result.failed).toHaveLength(0);
    });

    it('counts failed entries when a call rejects', async () => {
      axios.post
        .mockResolvedValueOnce({ data: {} })
        .mockRejectedValueOnce({ response: { data: { error: 'Duplicate entry' } } });

      const result = await IpAllowListApi.bulkAdd([
        { ipAddress: '192.168.1.1' },
        { ipAddress: '10.0.0.0/24' },
      ]);

      expect(result.added).toBe(1);
      expect(result.failed).toHaveLength(1);
      expect(result.failed[0].ip).toBe('10.0.0.0/24');
      expect(result.failed[0].reason).toBe('Duplicate entry');
    });

    it('handles plain string entries', async () => {
      axios.post.mockResolvedValue({ data: {} });

      const result = await IpAllowListApi.bulkAdd(['10.0.0.1']);

      expect(axios.post).toHaveBeenCalledWith(
        `${API_BASE}/entries`,
        { ipOrCidr: '10.0.0.1', description: null }
      );
      expect(result.added).toBe(1);
    });
  });

  describe('getCurrentIp', () => {
    it('calls GET /current-ip and returns data', async () => {
      axios.get.mockResolvedValue({ data: { ip: '203.0.113.5', allowed: true } });

      const result = await IpAllowListApi.getCurrentIp();

      expect(axios.get).toHaveBeenCalledWith(`${API_BASE}/current-ip`);
      expect(result.ip).toBe('203.0.113.5');
      expect(result.allowed).toBe(true);
    });
  });

  describe('updateEntry', () => {
    it('sends PUT to /entries/{id} with ipOrCidr and description', async () => {
      const updated = { id: 'abc', entry: '10.0.0.1' };
      axios.put.mockResolvedValue({ data: updated });

      const result = await IpAllowListApi.updateEntry('abc', '10.0.0.1', 'My server');

      expect(axios.put).toHaveBeenCalledWith(`${API_BASE}/entries/abc`, {
        ipOrCidr: '10.0.0.1',
        description: 'My server',
      });
      expect(result).toEqual(updated);
    });
  });
});

describe('transformEntryToUI', () => {
  it('maps backend "entry" field to UI "ipAddress"', () => {
    const backend = {
      id: 'abc-123',
      entry: '192.168.1.0/24',
      entryType: 'CIDR_IPV4',
      createdBy: 'admin',
      createdAt: '2024-01-15T10:00:00Z',
      updatedBy: null,
      updatedAt: null,
      description: 'Office network',
    };

    const ui = transformEntryToUI(backend);

    expect(ui.id).toBe('abc-123');
    expect(ui.ipAddress).toBe('192.168.1.0/24');
    expect(ui.entryType).toBe('CIDR_IPV4');
    expect(ui.createdBy).toBe('admin');
    expect(ui.description).toBe('Office network');
  });

  it('uses updatedAt for lastUpdated when present', () => {
    const backend = {
      id: '1',
      entry: '10.0.0.1',
      entryType: 'IPV4',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-02-01T00:00:00Z',
    };

    const ui = transformEntryToUI(backend);

    expect(ui.lastUpdated).toBe('2024-02-01T00:00:00Z');
    expect(ui.updatedAt).toBe('2024-02-01T00:00:00Z');
  });

  it('falls back to createdAt for lastUpdated when updatedAt is null', () => {
    const backend = {
      id: '1',
      entry: '10.0.0.1',
      entryType: 'IPV4',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: null,
    };

    const ui = transformEntryToUI(backend);

    expect(ui.lastUpdated).toBe('2024-01-01T00:00:00Z');
  });

  it('defaults description to empty string when absent', () => {
    const backend = { id: '1', entry: '10.0.0.1', entryType: 'IPV4' };

    const ui = transformEntryToUI(backend);

    expect(ui.description).toBe('');
  });
});

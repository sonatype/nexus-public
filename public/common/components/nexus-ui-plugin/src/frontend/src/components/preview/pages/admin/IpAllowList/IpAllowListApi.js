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

const API_BASE = '/service/rest/v1/security/ip-allowlist';

/**
 * API service for IP Allow List management.
 */
export const IpAllowListApi = {
  /**
   * Get IP Allow List settings (mode, entry counts).
   * @returns {Promise<{mode: string, totalEntries: number, maxEntries: number}>}
   */
  getSettings: async () => {
    const response = await axios.get(API_BASE);
    return response.data;
  },

  /**
   * Update the IP Allow List mode.
   * @param {string} mode - DISABLED, MONITOR, or ENFORCE
   * @returns {Promise<void>}
   */
  updateMode: async (mode) => {
    await axios.put(`${API_BASE}/mode`, { mode: mode.toUpperCase() });
  },

  /**
   * Get paginated list of IP Allow List entries with optional search filter.
   * @param {number} page - Page number (0-based)
   * @param {number} pageSize - Number of entries per page
   * @param {string} [search] - Optional search query (matches against IP address, CIDR notation, and description)
   * @returns {Promise<{entries: Array, page: number, pageSize: number, totalEntries: number, totalPages: number}>}
   */
  getEntries: async (page = 0, pageSize = 20, search = null) => {
    const params = { page, pageSize };
    if (search && search.trim()) {
      params.search = search.trim();
    }
    const response = await axios.get(`${API_BASE}/entries`, { params });
    return response.data;
  },

  /**
   * Add an IP address or CIDR range to the allow list.
   * @param {string} ipOrCidr - IP address or CIDR notation
   * @param {string} [description] - Optional description
   * @returns {Promise<void>}
   */
  addEntry: async (ipOrCidr, description = null) => {
    await axios.post(`${API_BASE}/entries`, { ipOrCidr, description });
  },

  /**
   * Update an existing entry in the allow list.
   * @param {string} id - Entry ID
   * @param {string} ipOrCidr - New IP address or CIDR notation
   * @param {string} [description] - Optional description
   * @returns {Promise<Object>} Updated entry
   */
  updateEntry: async (id, ipOrCidr, description = null) => {
    const response = await axios.put(`${API_BASE}/entries/${id}`, { ipOrCidr, description });
    return response.data;
  },

  /**
   * Add multiple IP addresses to the allow list in parallel.
   * @param {Array<string|{ipAddress: string, description?: string}>} entries - Array of IP addresses or entry objects
   * @returns {Promise<{added: number, failed: Array}>}
   */
  bulkAdd: async (entries) => {
    const settled = await Promise.allSettled(
      entries.map(entry => {
        const ipAddress = typeof entry === 'string' ? entry : entry.ipAddress;
        const description = typeof entry === 'string' ? null : (entry.description || null);
        return IpAllowListApi.addEntry(ipAddress, description);
      })
    );
    return settled.reduce((acc, outcome, i) => {
      if (outcome.status === 'fulfilled') {
        acc.added++;
      } else {
        const ip = typeof entries[i] === 'string' ? entries[i] : entries[i].ipAddress;
        acc.failed.push({
          ip,
          reason: outcome.reason?.response?.data?.error || outcome.reason?.message
        });
      }
      return acc;
    }, { added: 0, failed: [] });
  },

  /**
   * Delete multiple entries from the allow list by ID in a single request.
   * @param {string[]} ids - Array of entry IDs
   * @returns {Promise<{deleted: number, failed: Array}>}
   */
  bulkDelete: async (ids) => {
    if (!ids || ids.length === 0) {
      return { deleted: 0, failed: [] };
    }

    const response = await axios.delete(`${API_BASE}/entries/bulk`, { data: ids });

    const result = response.data;
    return {
      deleted: result.deleted || 0,
      failed: (result.failedIds || []).map(id => ({
        id,
        reason: 'Entry not found'
      }))
    };
  },

  /**
   * Get the current user's IP address as seen by the server.
   * @returns {Promise<{ip: string}>}
   */
  getCurrentIp: async () => {
    const response = await axios.get(`${API_BASE}/current-ip`);
    return response.data;
  },

  /**
   * Bulk upload entries from CSV content.
   * This is more efficient than bulkAdd as it sends all entries in a single request.
   * @param {string} csvContent - CSV content with IP addresses/CIDRs and optional descriptions
   * @returns {Promise<{addedCount: number, skippedCount: number, errorCount: number, rejectedEntries: string[]}>}
   */
  bulkUploadCsv: async (csvContent) => {
    const response = await axios.post(`${API_BASE}/entries/bulk`, { csvContent });
    return response.data;
  },

};

/**
 * Transform backend entry to UI format.
 * @param {Object} entry - Backend entry from IpAllowListEntryXO
 * @returns {Object} UI-formatted entry
 */
export const transformEntryToUI = (entry) => ({
  id: entry.id,
  // Backend JSON field is 'entry' (see IpAllowListEntryXO.java), UI uses 'ipAddress' for clarity
  ipAddress: entry.entry,
  entryType: entry.entryType,
  createdBy: entry.createdBy,
  createdAt: entry.createdAt,
  updatedBy: entry.updatedBy,
  updatedAt: entry.updatedAt,
  // Use updatedAt for lastUpdated, fallback to createdAt
  lastUpdated: entry.updatedAt || entry.createdAt,
  description: entry.description || ''
});

export default IpAllowListApi;

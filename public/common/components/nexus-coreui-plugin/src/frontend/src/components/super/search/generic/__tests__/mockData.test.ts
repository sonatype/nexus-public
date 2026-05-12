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

import { mockGenericSearchApi, mockGenericResults } from '../mockData';

describe('mockGenericSearchApi', () => {
  it('returns all results when no filters are provided', async () => {
    const response = await mockGenericSearchApi({});

    expect(response.items.length).toBe(mockGenericResults.length);
    expect(response.totalCount).toBe(mockGenericResults.length);
  });

  it('filters by query string (q)', async () => {
    const response = await mockGenericSearchApi({ q: 'react' });

    expect(response.items.length).toBe(2); // react and @types/react
    expect(response.items.every((item) => 
      item.name.toLowerCase().includes('react') || 
      item.displayName.toLowerCase().includes('react')
    )).toBe(true);
  });

  it('filters by query string case-insensitively', async () => {
    const response = await mockGenericSearchApi({ q: 'REACT' });

    expect(response.items.length).toBe(2);
  });

  it('filters by format', async () => {
    const response = await mockGenericSearchApi({ format: 'maven2' });

    expect(response.items.length).toBe(2); // commons-lang3 and guava
    expect(response.items.every((item) => item.format === 'maven2')).toBe(true);
  });

  it('filters by repository', async () => {
    const response = await mockGenericSearchApi({ repository: 'npm-proxy-v1' });

    expect(response.items.length).toBe(3); // react, @types/react, lodash
    expect(response.items.every((item) => item.repository === 'npm-proxy-v1')).toBe(true);
  });

  it('filters by group', async () => {
    const response = await mockGenericSearchApi({ group: 'apache' });

    expect(response.items.length).toBe(1);
    expect(response.items[0].name).toBe('commons-lang3');
  });

  it('filters by name', async () => {
    const response = await mockGenericSearchApi({ name: 'nginx' });

    expect(response.items.length).toBe(1);
    expect(response.items[0].name).toBe('nginx');
  });

  it('filters by version', async () => {
    const response = await mockGenericSearchApi({ version: '3.12.0' });

    expect(response.items.length).toBe(1);
    expect(response.items[0].name).toBe('commons-lang3');
  });

  it('combines multiple filters with AND logic', async () => {
    const response = await mockGenericSearchApi({
      format: 'npm',
      q: 'react',
    });

    expect(response.items.length).toBe(2);
    expect(response.items.every((item) => item.format === 'npm')).toBe(true);
  });

  it('returns empty array when no matches found', async () => {
    const response = await mockGenericSearchApi({ q: 'nonexistent' });

    expect(response.items.length).toBe(0);
    expect(response.totalCount).toBe(0);
  });

  it('returns correct response shape', async () => {
    const response = await mockGenericSearchApi({ q: 'react' });

    expect(response).toHaveProperty('items');
    expect(response).toHaveProperty('totalCount');
    expect(response).toHaveProperty('continuationToken');
    expect(Array.isArray(response.items)).toBe(true);
    expect(typeof response.totalCount).toBe('number');
  });
});

describe('mockGenericResults', () => {
  it('has diverse format types', () => {
    const formats = new Set(mockGenericResults.map((r) => r.format));

    expect(formats.has('maven2')).toBe(true);
    expect(formats.has('npm')).toBe(true);
    expect(formats.has('nuget')).toBe(true);
    expect(formats.has('docker')).toBe(true);
    expect(formats.has('pypi')).toBe(true);
    expect(formats.has('helm')).toBe(true);
    expect(formats.has('go')).toBe(true);
  });

  it('includes items with and without groups', () => {
    const withGroup = mockGenericResults.filter((r) => r.group !== null);
    const withoutGroup = mockGenericResults.filter((r) => r.group === null);

    expect(withGroup.length).toBeGreaterThan(0);
    expect(withoutGroup.length).toBeGreaterThan(0);
  });

  it('all items have required fields', () => {
    for (const result of mockGenericResults) {
      expect(result.id).toBeTruthy();
      expect(result.format).toBeTruthy();
      expect(result.repository).toBeTruthy();
      expect(result.name).toBeTruthy();
      expect(result.version).toBeTruthy();
      expect(result.displayName).toBeTruthy();
      expect(Array.isArray(result.assets)).toBe(true);
    }
  });

  it('all items have at least one asset', () => {
    for (const result of mockGenericResults) {
      expect(result.assets.length).toBeGreaterThan(0);
      expect(result.assets[0].id).toBeTruthy();
      expect(result.assets[0].path).toBeTruthy();
      expect(result.assets[0].downloadUrl).toBeTruthy();
    }
  });
});



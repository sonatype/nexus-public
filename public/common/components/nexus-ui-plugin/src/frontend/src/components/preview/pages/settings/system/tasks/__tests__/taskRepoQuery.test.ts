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

import { buildRepoQuery, deriveAllEntries, queryFromStaticFilter } from '../taskRepoQuery';

describe('deriveAllEntries', () => {
  it('maps readReferencesAddingEntriesForAllFormats to withAll+withFormats', () => {
    expect(deriveAllEntries('coreui_Repository.readReferencesAddingEntriesForAllFormats'))
      .toEqual({ withAll: true, withFormats: true });
  });
  it('maps readReferencesAddingEntryForAll to withAll only', () => {
    expect(deriveAllEntries('coreui_Repository.readReferencesAddingEntryForAll'))
      .toEqual({ withAll: true, withFormats: false });
  });
  it('maps plain readReferences to neither', () => {
    expect(deriveAllEntries('coreui_Repository.readReferences'))
      .toEqual({ withAll: false, withFormats: false });
  });
  it('defaults to neither when storeApi is undefined', () => {
    expect(deriveAllEntries(undefined)).toEqual({ withAll: false, withFormats: false });
  });
  it('defaults to neither for an empty storeApi string', () => {
    expect(deriveAllEntries('')).toEqual({ withAll: false, withFormats: false });
  });
});

describe('buildRepoQuery', () => {
  const parse = (q: string) => new URLSearchParams(q);

  it('passes facets through and adds withAll for the AddingEntryForAll storeApi', () => {
    const q = parse(buildRepoQuery(
      'coreui_Repository.readReferencesAddingEntryForAll',
      { facets: 'org.sonatype.nexus.repository.purge.PurgeUnusedFacet' }));
    expect(q.get('facets')).toBe('org.sonatype.nexus.repository.purge.PurgeUnusedFacet');
    expect(q.get('withAll')).toBe('true');
    expect(q.get('withFormats')).toBeNull();
  });

  it('passes facets + versionPolicies (maven snapshot purge)', () => {
    const q = parse(buildRepoQuery(
      'coreui_Repository.readReferencesAddingEntryForAll',
      { facets: 'org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet', versionPolicies: '!RELEASE' }));
    expect(q.get('facets')).toBe('org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet');
    expect(q.get('versionPolicies')).toBe('!RELEASE');
  });

  it('passes format + type (alpine)', () => {
    const q = parse(buildRepoQuery(
      'coreui_Repository.readReferencesAddingEntryForAll', { format: 'alpine', type: 'hosted,proxy' }));
    expect(q.get('format')).toBe('alpine');
    expect(q.get('type')).toBe('hosted,proxy');
    expect(q.get('withAll')).toBe('true');
  });

  it('passes a type-exclude (rebuild browse) and no All entry for plain readReferences', () => {
    const q = parse(buildRepoQuery('coreui_Repository.readReferences', { type: '!group' }));
    expect(q.get('type')).toBe('!group');
    expect(q.get('withAll')).toBeNull();
  });

  it('adds withAll+withFormats for the AddingEntriesForAllFormats storeApi (tags cleanup)', () => {
    const q = parse(buildRepoQuery('coreui_Repository.readReferencesAddingEntriesForAllFormats', undefined));
    expect(q.get('withAll')).toBe('true');
    expect(q.get('withFormats')).toBe('true');
  });

  it('returns an empty query for no storeApi and no filters (move task)', () => {
    expect(buildRepoQuery(undefined, undefined)).toBe('');
  });

  it('omits empty-string filter values rather than sending them', () => {
    expect(buildRepoQuery('coreui_Repository.readReferences', { facets: '', format: '' })).toBe('');
  });

  it('passes multiple comma-separated facets through verbatim', () => {
    const q = parse(buildRepoQuery('coreui_Repository.readReferences', { facets: 'a.B,c.D' }));
    expect(q.get('facets')).toBe('a.B,c.D');
  });
});

describe('queryFromStaticFilter (fallback)', () => {
  const parse = (q: string) => new URLSearchParams(q);
  it('serializes a static facet filter with includeAll', () => {
    const q = parse(queryFromStaticFilter({ facets: ['a.B'], includeAll: true }));
    expect(q.get('facets')).toBe('a.B');
    expect(q.get('withAll')).toBe('true');
  });
  it('serializes format/type arrays', () => {
    const q = parse(queryFromStaticFilter({ formats: ['maven2'], types: ['hosted', 'proxy'] }));
    expect(q.get('format')).toBe('maven2');
    expect(q.get('type')).toBe('hosted,proxy');
  });
  it('returns empty for undefined', () => {
    expect(queryFromStaticFilter(undefined)).toBe('');
  });
});

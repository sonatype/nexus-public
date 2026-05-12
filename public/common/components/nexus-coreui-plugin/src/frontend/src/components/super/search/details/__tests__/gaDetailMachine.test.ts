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

import { interpret } from 'xstate';
import {
  createGaDetailMachine,
  getActiveTabMeta,
  ALL_TABS,
} from '../gaDetailMachine';

// =============================================================================
// MOCK DATA
// =============================================================================

const MOCK_DETAIL = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven' as const,
  displayName: 'commons-lang3',
  description: 'org.apache.commons:commons-lang3',
  projectUrl: undefined,
  license: 'Apache-2.0',
  repositories: [
    { name: 'maven-central', format: 'maven2', type: 'proxy' as const, versionsCount: 3 },
  ],
  versions: [
    { version: '3.14.0', lastUpdated: '2024-03-01T00:00:00Z', repositories: ['maven-central'], status: 'none' as const },
  ],
};

const MOCK_ASSETS = [
  {
    id: 'asset-1',
    repository: 'maven-central',
    path: '/test.jar',
    downloadUrl: 'http://test.com/test.jar',
    format: 'application/java-archive',
    extension: 'jar',
    size: 654321,
    contentType: 'application/java-archive',
    lastModified: '2024-03-01T00:00:00Z',
    checksums: { sha1: 'abc123' },
  },
] as const;

// =============================================================================
// HELPERS
// =============================================================================

const TEST_GA_ID = 'maven:org.apache.commons:commons-lang3';

// =============================================================================
// TESTS
// =============================================================================

describe('gaDetailMachine', () => {
  let service: any;

  afterEach(() => {
    if (service) {
      try {
        service.stop();
      } catch {
        // Ignore
      }
    }
  });

  // ===========================================================================
  // DATA LOADING LIFECYCLE
  // ===========================================================================

  describe('data loading lifecycle', () => {
    it('starts in loading state', () => {
      const machine = createGaDetailMachine(TEST_GA_ID);
      service = interpret(
        machine.withConfig({
          services: {
            loadDetail: () => Promise.resolve(MOCK_DETAIL),
            loadAssets: () => Promise.resolve(MOCK_ASSETS),
          },
        })
      );
      service.start();

      const state = service.getSnapshot();
      expect(state.matches({ data: 'loading' })).toBe(true);
      expect(state.context.loading).toBe(true);
      expect(state.context.detail).toBeNull();
      expect(state.context.lastLoadedVersion).toBeNull();
    });

    it('preserves gaId in context', () => {
      const machine = createGaDetailMachine(TEST_GA_ID);
      service = interpret(machine);
      service.start();
      expect(service.getSnapshot().context.gaId).toBe(TEST_GA_ID);
    });

    it('preserves initialVersion when provided', () => {
      const machine = createGaDetailMachine(TEST_GA_ID, '3.13.0');
      service = interpret(machine);
      service.start();
      expect(service.getSnapshot().context.selectedVersion).toBe('3.13.0');
    });
  });

  // ===========================================================================
  // TAB SUB-STATES
  // ===========================================================================

  describe('tab sub-states', () => {
    it('starts in overview tab by default', () => {
      const machine = createGaDetailMachine(TEST_GA_ID);
      service = interpret(machine);
      service.start();
      expect(service.getSnapshot().matches({ tab: 'overview' })).toBe(true);
    });

    it('transitions between tabs', () => {
      const machine = createGaDetailMachine(TEST_GA_ID);
      service = interpret(machine);
      service.start();

      for (const tab of ALL_TABS) {
        service.send({ type: 'SELECT_TAB', tab });
        expect(service.getSnapshot().matches({ tab })).toBe(true);
      }
    });
  });

  // ===========================================================================
  // TAB METADATA
  // ===========================================================================

  describe('tab metadata', () => {
    it('returns correct metadata for each tab', () => {
      const machine = createGaDetailMachine(TEST_GA_ID);
      service = interpret(machine);
      service.start();

      service.send({ type: 'SELECT_TAB', tab: 'overview' });
      expect(getActiveTabMeta(service.getSnapshot())?.requiresVersion).toBe(false);

      service.send({ type: 'SELECT_TAB', tab: 'files' });
      expect(getActiveTabMeta(service.getSnapshot())?.requiresVersion).toBe(true);
    });
  });

  // ===========================================================================
  // ALL_TABS EXPORT
  // ===========================================================================

  describe('ALL_TABS export', () => {
    it('contains exactly 5 tabs', () => {
      expect(ALL_TABS).toHaveLength(5);
    });

    it('contains expected tabs', () => {
      expect(ALL_TABS).toEqual(['overview', 'versions', 'repositories', 'files', 'security']);
    });
  });

  // ===========================================================================
  // HELPER: getActiveTabMeta
  // ===========================================================================

  describe('getActiveTabMeta', () => {
    it('returns undefined for null state', () => {
      expect(getActiveTabMeta(null)).toBeUndefined();
    });

    it('returns undefined for state without meta', () => {
      expect(getActiveTabMeta({})).toBeUndefined();
    });
  });
});

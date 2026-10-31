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

/**
 * The whole of `detail` now — it is the shell and nothing else, derived synchronously from the
 * gaId by buildShellDetail. `repositories` and `versions` stay permanently empty: they were
 * filled by an aggregate walk over every page of /v1/search, which no tab reads any more.
 */
const SHELL_DETAIL = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven' as const,
  displayName: 'commons-lang3',
  description: 'org.apache.commons:commons-lang3',
  projectUrl: undefined,
  license: undefined,
  repositories: [],
  versions: [],
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

/**
 * What `loadAssets` resolves: the full ComponentVersionDetail, not a bare asset array. The
 * repositories and timestamp travel with the assets so that setAssets can put all three into
 * context in one commit — see the machine's versionRepositories doc.
 */
const VERSION_DETAIL = {
  assets: MOCK_ASSETS,
  repositories: ['maven-central', 'maven-releases'],
  lastUpdated: '2024-03-01T00:00:00Z',
};

const RAW_GA_ID = 'raw:/animport/abc:file194.txt';

// =============================================================================
// HELPERS
// =============================================================================

const TEST_GA_ID = 'maven:org.apache.commons:commons-lang3';
const FIRST_VERSION = '3.14.0';
const EMPTY_VERSION = '';

const flushMicrotasks = async (iterations = 20) => {
  for (let i = 0; i < iterations; i++) {
    await Promise.resolve();
  }
};

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
    /*
     * The machine has no component-wide fetch left to start.
     *
     * It used to carry a second `aggregate` region invoking loadDetail, which drained every page
     * of /v1/search to build detail.repositories and detail.versions — ~101 requests on a
     * 5,000-version component. Both readers now have bounded per-version sources (NEXUS-54201 for
     * Files, NEXUS-54220 for Repositories), so the region is gone along with NEED_AGGREGATES,
     * LOAD and RETRY. Nothing but a selected version can cause a request.
     */
    it('fetches nothing on start when no version was supplied, and serves the shell immediately', () => {
      const loadAssets = jest.fn(() => Promise.resolve(VERSION_DETAIL));
      const machine = createGaDetailMachine(TEST_GA_ID, undefined, SHELL_DETAIL);
      service = interpret(machine.withConfig({ services: { loadAssets } }));
      service.start();

      const state = service.getSnapshot();
      expect(state.matches({ data: { asset: 'idle' } })).toBe(true);
      expect(loadAssets).not.toHaveBeenCalled();
      // The shell is usable immediately: name and description come from the gaId, not the API.
      expect(state.context.detail).toEqual(SHELL_DETAIL);
    });

    it('no longer accepts the removed aggregate events', () => {
      const loadAssets = jest.fn(() => Promise.resolve(VERSION_DETAIL));
      const machine = createGaDetailMachine(TEST_GA_ID, undefined, SHELL_DETAIL);
      service = interpret(machine.withConfig({ services: { loadAssets } }));
      service.start();
      const before = service.getSnapshot();

      // Cast: these are no longer in GaDetailMachineEvent. Sending them must be inert rather
      // than resurrect a drain, which is the regression this guards.
      for (const type of ['NEED_AGGREGATES', 'LOAD', 'RETRY']) {
        service.send({ type } as any);
      }

      expect(service.getSnapshot().value).toEqual(before.value);
      expect(loadAssets).not.toHaveBeenCalled();
    });

    it('loads assets on SELECT_VERSION', () => {
      const loadAssets = jest.fn(() => Promise.resolve(VERSION_DETAIL));
      const machine = createGaDetailMachine(TEST_GA_ID, undefined, SHELL_DETAIL);
      service = interpret(machine.withConfig({ services: { loadAssets } }));
      service.start();

      service.send({ type: 'SELECT_VERSION', version: '3.13.0' });

      expect(service.getSnapshot().context.selectedVersion).toBe('3.13.0');
      expect(service.getSnapshot().matches({ data: { asset: 'loadingAssets' } })).toBe(true);
      expect(loadAssets).toHaveBeenCalledTimes(1);
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
  // VERSION SELECTION (NEXUS-54201)
  // ===========================================================================

  describe('version selection', () => {
    it('does not invent a version when none was supplied', async () => {
      const loadAssets = jest.fn(() => Promise.resolve(VERSION_DETAIL));
      const machine = createGaDetailMachine(TEST_GA_ID, undefined, SHELL_DETAIL).withConfig({
        services: { loadAssets },
      });
      service = interpret(machine);
      service.start();
      await flushMicrotasks();

      // Stays parked in asset.idle: shouldLoadAssets gates on selectedVersion !== null, and the
      // machine never picks a version for itself — GADetailPage resolves it from the URL.
      expect(service.getSnapshot().matches({ data: { asset: 'idle' } })).toBe(true);
      expect(service.getSnapshot().context.selectedVersion).toBeNull();
      expect(loadAssets).not.toHaveBeenCalled();
    });

    it('keeps an empty-string version as selected', () => {
      const machine = createGaDetailMachine(RAW_GA_ID, EMPTY_VERSION, SHELL_DETAIL);
      service = interpret(machine);
      service.start();

      expect(service.getSnapshot().context.selectedVersion).toBe(EMPTY_VERSION);
    });

    it('loads assets on creation for a version supplied via initialVersion', async () => {
      // The asset region's own `idle.always` guard fires on start, with no event needed — this is
      // what lets the Files tab render for a deep link without any component-wide fetch first.
      let loadAssetsCallCount = 0;
      const machine = createGaDetailMachine(TEST_GA_ID, FIRST_VERSION).withConfig({
        services: {
          loadAssets: () => {
            loadAssetsCallCount++;
            return Promise.resolve(VERSION_DETAIL);
          },
        },
      });
      service = interpret(machine);
      service.start();
      await flushMicrotasks();

      expect(service.getSnapshot().matches({ data: { asset: 'loaded' } })).toBe(true);
      expect(service.getSnapshot().context.selectedVersion).toBe(FIRST_VERSION);
      expect(loadAssetsCallCount).toBe(1);
    });

    it('puts the resolved version detail into context, not just the assets', async () => {
      const machine = createGaDetailMachine(TEST_GA_ID, FIRST_VERSION).withConfig({
        services: { loadAssets: () => Promise.resolve(VERSION_DETAIL) },
      });
      service = interpret(machine);
      service.start();
      await flushMicrotasks();

      const { context } = service.getSnapshot();
      expect(context.assets).toEqual(VERSION_DETAIL.assets);
      expect(context.versionRepositories).toEqual(VERSION_DETAIL.repositories);
      expect(context.versionLastUpdated).toBe(VERSION_DETAIL.lastUpdated);
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

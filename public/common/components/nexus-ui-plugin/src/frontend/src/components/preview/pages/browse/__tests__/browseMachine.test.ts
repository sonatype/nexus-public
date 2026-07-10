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
import { waitFor } from 'xstate/lib/waitFor';
import { createBrowseMachine, getActiveViewMeta } from '../browseMachine';
import ExtJS from '../../../../../interface/ExtJS';

function startMachine(initialRepo?: string, serviceOverrides?: Record<string, any>) {
  const machine = createBrowseMachine(initialRepo);
  return interpret(
    serviceOverrides
      ? machine.withConfig({ services: serviceOverrides })
      : machine
  ).start();
}

const mockComponentNode = {
  id: 'org/apache/maven',
  text: 'maven',
  type: 'component' as const,
  leaf: false,
  componentId: 'comp-123',
  assetId: null,
  packageUrl: null,
};

const mockAssetNode = {
  id: 'org/apache/maven/pom.xml',
  text: 'pom.xml',
  type: 'asset' as const,
  leaf: true,
  componentId: null,
  assetId: 'asset-456',
  packageUrl: null,
};

const mockFolderNode = {
  id: 'org/apache',
  text: 'apache',
  type: 'folder' as const,
  leaf: false,
  componentId: null,
  assetId: null,
  packageUrl: null,
};

describe('browseMachine', () => {
  describe('initial state', () => {
    it('starts in repoList when no initial repository', () => {
      const service = startMachine();
      expect(service.getSnapshot().matches('repoList')).toBe(true);
      expect(service.getSnapshot().context.selectedRepository).toBeNull();
      service.stop();
    });

    it('starts in treeView when initial repository is provided', () => {
      const service = startMachine('maven-central');
      expect(service.getSnapshot().matches('treeView')).toBe(true);
      expect(service.getSnapshot().context.selectedRepository).toBe('maven-central');
      service.stop();
    });
  });

  describe('repository selection flow', () => {
    it('transitions from repoList to treeView on SELECT_REPO', () => {
      const service = startMachine();

      service.send({ type: 'SELECT_REPO', repoName: 'maven-central' });

      const state = service.getSnapshot();
      expect(state.matches('treeView')).toBe(true);
      expect(state.context.selectedRepository).toBe('maven-central');
      expect(state.context.repositoryUrl).toContain('maven-central');

      service.stop();
    });

    it('transitions from treeView back to repoList on BACK', () => {
      const service = startMachine('maven-central');

      service.send({ type: 'BACK' });

      const state = service.getSnapshot();
      expect(state.matches('repoList')).toBe(true);
      expect(state.context.selectedRepository).toBeNull();
      expect(state.context.selectedNode).toBeNull();

      service.stop();
    });

    it('clears selected node when going back to repo list', () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.resolve({ component: { id: '1' } }),
      });

      service.send({ type: 'SELECT_NODE', node: mockComponentNode });

      service.send({ type: 'BACK' });

      expect(service.getSnapshot().context.selectedNode).toBeNull();
      service.stop();
    });

    it('handles switching between repositories', () => {
      const service = startMachine();

      service.send({ type: 'SELECT_REPO', repoName: 'maven-central' });
      expect(service.getSnapshot().context.selectedRepository).toBe('maven-central');

      service.send({ type: 'BACK' });
      service.send({ type: 'SELECT_REPO', repoName: 'npm-hosted' });
      expect(service.getSnapshot().context.selectedRepository).toBe('npm-hosted');

      service.stop();
    });
  });

  describe('tree view - node selection', () => {
    it('transitions to loadingDetail when node selected', () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => new Promise(() => {}), // Never resolves
      });

      service.send({ type: 'SELECT_NODE', node: mockComponentNode });

      const state = service.getSnapshot();
      expect(state.matches({ treeView: 'loadingDetail' })).toBe(true);
      expect(state.context.selectedNode).toBe(mockComponentNode);

      service.stop();
    });

    it('transitions to viewingDetail after successful detail load', async () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.resolve({
          component: { id: 'comp-123', name: 'maven', format: 'maven2' },
        }),
      });

      service.send({ type: 'SELECT_NODE', node: mockComponentNode });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      const state = service.getSnapshot();
      expect(state.context.detailData.component).toBeTruthy();
      expect(state.context.detailData.error).toBeNull();

      service.stop();
    });

    it('shows error on detail load failure', async () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.reject(new Error('Not found')),
      });

      service.send({ type: 'SELECT_NODE', node: mockAssetNode });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      expect(service.getSnapshot().context.detailData.error).toBe('Not found');

      service.stop();
    });

    it.each([
      ['component', mockComponentNode],
      ['asset', mockAssetNode],
      ['folder', mockFolderNode],
    ])('can select %s node type', async (type, node) => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.resolve({}),
      });

      service.send({ type: 'SELECT_NODE', node });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      expect(service.getSnapshot().context.selectedNode).toBe(node);

      service.stop();
    });

    it('clears node on NODE_DELETED', async () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.resolve({}),
      });

      service.send({ type: 'SELECT_NODE', node: mockAssetNode });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      service.send({ type: 'NODE_DELETED' });

      expect(service.getSnapshot().matches({ treeView: 'browsing' })).toBe(true);
      expect(service.getSnapshot().context.selectedNode).toBeNull();

      service.stop();
    });

    it('can select a different node while viewing detail', async () => {
      const service = startMachine('maven-central', {
        loadNodeDetail: () => Promise.resolve({ component: { id: '1' } }),
      });

      service.send({ type: 'SELECT_NODE', node: mockComponentNode });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      service.send({ type: 'SELECT_NODE', node: mockAssetNode });
      await waitFor(service, (s) =>
        s.matches({ treeView: 'viewingDetail' }) && s.context.selectedNode === mockAssetNode
      );

      expect(service.getSnapshot().context.selectedNode).toBe(mockAssetNode);

      service.stop();
    });
  });

  describe('tree view - initial state', () => {
    it('starts in browsing sub-state (empty detail)', () => {
      const service = startMachine('maven-central');

      expect(service.getSnapshot().matches({ treeView: 'browsing' })).toBe(true);

      service.stop();
    });
  });

  describe('filters', () => {
    it('updates format filter', () => {
      const service = startMachine();

      service.send({ type: 'SET_FILTER', section: 'formats', value: ['maven2', 'npm'] });

      expect(service.getSnapshot().context.filters.formats).toEqual(['maven2', 'npm']);

      service.stop();
    });

    it('updates name filter', () => {
      const service = startMachine();

      service.send({ type: 'SET_NAME_FILTER', value: 'maven-central' });

      expect(service.getSnapshot().context.filters.nameFilter).toBe('maven-central');

      service.stop();
    });

    it('clears all filters', () => {
      const service = startMachine();

      service.send({ type: 'SET_FILTER', section: 'formats', value: ['npm'] });
      service.send({ type: 'SET_NAME_FILTER', value: 'test' });

      service.send({ type: 'CLEAR_FILTERS' });

      const filters = service.getSnapshot().context.filters;
      expect(filters.formats).toEqual([]);
      expect(filters.nameFilter).toBe('');

      service.stop();
    });

    it('filters only available in repoList state', () => {
      const service = startMachine('maven-central');

      // In treeView - SET_FILTER should be ignored (not defined on treeView)
      service.send({ type: 'SET_FILTER', section: 'formats', value: ['npm'] });

      // Filters shouldn't change (event not handled in treeView)
      expect(service.getSnapshot().context.filters.formats).toEqual([]);

      service.stop();
    });
  });

  describe('view metadata', () => {
    it('repoList has list view metadata', () => {
      const service = startMachine();
      const meta = getActiveViewMeta(service.getSnapshot());

      expect(meta).toBeDefined();
      expect(meta!.view).toBe('list');

      service.stop();
    });

    it('treeView has tree view metadata', () => {
      const service = startMachine('maven-central');
      const meta = getActiveViewMeta(service.getSnapshot());

      expect(meta).toBeDefined();
      expect(meta!.view).toBe('tree');

      service.stop();
    });
  });

  describe('repositoryUrl context path', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('includes context path in repositoryUrl when NX.util.Url.urlOf is available (initial repo)', () => {
      jest.spyOn(ExtJS, 'urlOf').mockImplementation((path: string) =>
        'http://localhost:8081/nexus' + path
      );

      const service = startMachine('maven-central');
      expect(service.getSnapshot().context.repositoryUrl)
        .toBe('http://localhost:8081/nexus/repository/maven-central/');
      service.stop();
    });

    it('includes context path in repositoryUrl when NX.util.Url.urlOf is available (SELECT_REPO)', () => {
      jest.spyOn(ExtJS, 'urlOf').mockImplementation((path: string) =>
        'http://localhost:8081/nexus' + path
      );

      const service = startMachine();
      service.send({ type: 'SELECT_REPO', repoName: 'maven-central' });
      expect(service.getSnapshot().context.repositoryUrl)
        .toBe('http://localhost:8081/nexus/repository/maven-central/');
      service.stop();
    });
  });

  describe('full user journey', () => {
    it('list → select repo → browse → select node → detail → delete → browse → back → list', async () => {
      const service = startMachine(undefined, {
        loadNodeDetail: () => Promise.resolve({ asset: { id: 'a1' } }),
      });

      // Start in list
      expect(service.getSnapshot().matches('repoList')).toBe(true);

      // Select repository
      service.send({ type: 'SELECT_REPO', repoName: 'maven-central' });
      expect(service.getSnapshot().matches({ treeView: 'browsing' })).toBe(true);

      // Select a node
      service.send({ type: 'SELECT_NODE', node: mockAssetNode });
      await waitFor(service, (s) => s.matches({ treeView: 'viewingDetail' }));

      // Delete the node
      service.send({ type: 'NODE_DELETED' });
      expect(service.getSnapshot().matches({ treeView: 'browsing' })).toBe(true);
      expect(service.getSnapshot().context.selectedNode).toBeNull();

      // Go back to list
      service.send({ type: 'BACK' });
      expect(service.getSnapshot().matches('repoList')).toBe(true);

      service.stop();
    });
  });
});

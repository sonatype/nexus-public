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

/**
 * Delete must not be offered for group repositories.
 *
 * The authoritative rule is Classic UI's mixin/ComponentUtils.updateDeleteButton:
 * the button is only shown when `currentRepository.get('type') !== 'group'`, then
 * requires an authenticated user, and only then runs the server preflight
 * (canDeleteAsset / canDeleteComponent / canDeleteFolder).  Group repositories
 * have no ContentMaintenanceFacet, so a delete would fail server-side with
 * IllegalOperationException — the UI must never offer it.
 *
 * These tests assert the full matrix: every repository type against every node
 * type, plus the unknown/loading case (which must not optimistically allow).
 */

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn(),
  },
}));
jest.mock('../browse.api', () => ({
  canDeleteAsset: jest.fn(),
  canDeleteComponent: jest.fn(),
  canDeleteFolder: jest.fn(),
}));

import { renderHook, waitFor } from '@testing-library/react';

import { ExtJS } from '../../../../../interface/ExtJS';
import { canDeleteAsset, canDeleteComponent, canDeleteFolder } from '../browse.api';
import type { ComponentData } from '../detail/DetailPanel';
import type { BrowseNode } from '../tree/browse-tree.types';
import { useCanDelete } from '../useCanDelete';

const REPO_NAME = 'maven-repo';

const ASSET_NODE: BrowseNode = {
  id: 'org/example/lib/1.0/lib-1.0.jar',
  text: 'lib-1.0.jar',
  type: 'asset',
  leaf: true,
  assetId: 'asset-42',
  componentId: 'comp-9',
};

const COMPONENT_NODE: BrowseNode = {
  id: 'org/example/lib/1.0',
  text: '1.0',
  type: 'component',
  leaf: false,
  componentId: 'comp-9',
};

const FOLDER_NODE: BrowseNode = {
  id: 'org/example',
  text: 'example',
  type: 'folder',
  leaf: false,
};

const COMPONENT_DATA: ComponentData = {
  id: 'cmVwbzpjb21wLTk',
  repositoryName: REPO_NAME,
  format: 'maven2',
  group: 'org.example',
  name: 'lib',
  version: '1.0',
};

const mockCanDeleteAsset = canDeleteAsset as jest.MockedFunction<typeof canDeleteAsset>;
const mockCanDeleteComponent = canDeleteComponent as jest.MockedFunction<typeof canDeleteComponent>;
const mockCanDeleteFolder = canDeleteFolder as jest.MockedFunction<typeof canDeleteFolder>;
const mockUseUser = ExtJS.useUser as jest.MockedFunction<typeof ExtJS.useUser>;

const NODES: Array<{ label: string; node: BrowseNode; data: ComponentData | null }> = [
  { label: 'asset', node: ASSET_NODE, data: null },
  { label: 'component', node: COMPONENT_NODE, data: COMPONENT_DATA },
  { label: 'folder', node: FOLDER_NODE, data: null },
];

beforeEach(() => {
  jest.clearAllMocks();
  mockUseUser.mockReturnValue({
    id: 'alice',
    authenticated: true,
    administrator: false,
  } as ReturnType<typeof ExtJS.useUser>);
  // The server would say yes for every node — only repository type may veto.
  mockCanDeleteAsset.mockResolvedValue(true);
  mockCanDeleteComponent.mockResolvedValue(true);
  mockCanDeleteFolder.mockResolvedValue(true);
});

describe('delete gating by repository type', () => {
  for (const repositoryType of ['hosted', 'proxy'] as const) {
    for (const { label, node, data } of NODES) {
      it(`allows delete of a ${label} in a ${repositoryType} repository when the server permits it`, async () => {
        const { result } = renderHook(() =>
          useCanDelete(node, REPO_NAME, data, repositoryType),
        );

        await waitFor(() => expect(result.current).toBe(true));
      });
    }
  }

  for (const { label, node, data } of NODES) {
    it(`never allows delete of a ${label} in a group repository`, async () => {
      const { result } = renderHook(() => useCanDelete(node, REPO_NAME, data, 'group'));

      await waitFor(() => expect(result.current).toBe(false));
      expect(result.current).toBe(false);
    });

    it(`does not even run the server preflight for a ${label} in a group repository`, async () => {
      renderHook(() => useCanDelete(node, REPO_NAME, data, 'group'));

      await waitFor(() => {
        expect(mockCanDeleteAsset).not.toHaveBeenCalled();
        expect(mockCanDeleteComponent).not.toHaveBeenCalled();
        expect(mockCanDeleteFolder).not.toHaveBeenCalled();
      });
    });
  }

  it('does not optimistically allow delete while the repository type is still unknown', async () => {
    const { result, rerender } = renderHook(
      ({ type }: { type: 'hosted' | 'proxy' | 'group' | undefined }) =>
        useCanDelete(ASSET_NODE, REPO_NAME, null, type),
      { initialProps: { type: undefined } },
    );

    await waitFor(() => expect(result.current).toBe(false));
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();

    // Once the type resolves to a deletable one, the preflight runs.
    rerender({ type: 'hosted' });
    await waitFor(() => expect(result.current).toBe(true));
  });

  it('revokes delete when the selection moves from a hosted repository to a group repository', async () => {
    const { result, rerender } = renderHook(
      ({ type }: { type: 'hosted' | 'group' }) =>
        useCanDelete(ASSET_NODE, REPO_NAME, null, type),
      { initialProps: { type: 'hosted' as const } },
    );

    await waitFor(() => expect(result.current).toBe(true));

    rerender({ type: 'group' });

    await waitFor(() => expect(result.current).toBe(false));
  });

  it('still requires authentication in a deletable repository type', async () => {
    mockUseUser.mockReturnValue(undefined as ReturnType<typeof ExtJS.useUser>);

    const { result } = renderHook(() => useCanDelete(ASSET_NODE, REPO_NAME, null, 'hosted'));

    await waitFor(() => expect(result.current).toBe(false));
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();
  });

  it('still honours a server denial in a deletable repository type', async () => {
    mockCanDeleteAsset.mockResolvedValue(false);

    const { result } = renderHook(() => useCanDelete(ASSET_NODE, REPO_NAME, null, 'hosted'));

    await waitFor(() => expect(mockCanDeleteAsset).toHaveBeenCalled());
    expect(result.current).toBe(false);
  });

  it('never offers delete for a pathless folder node', async () => {
    // Mirrors Classic UI's updateDeleteFolderButton, which only wires the
    // preflight when a path is present. canDeleteFolder scopes deletion by path
    // prefix, so an empty path must never be sent.
    mockCanDeleteFolder.mockResolvedValue(true);

    const { result } = renderHook(() =>
      useCanDelete({ ...FOLDER_NODE, id: '' }, REPO_NAME, null, 'hosted')
    );

    await waitFor(() => expect(result.current).toBe(false));
    expect(mockCanDeleteFolder).not.toHaveBeenCalled();
  });
});

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

const REPO_NAME = 'maven-hosted';

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
  id: 'comp-9',
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

function authenticated() {
  mockUseUser.mockReturnValue({
    id: 'alice',
    authenticated: true,
    administrator: false,
    authenticatedRealms: [],
  });
}

function anonymous() {
  mockUseUser.mockReturnValue({
    id: 'anonymous',
    authenticated: false,
    administrator: false,
    authenticatedRealms: [],
  });
}

describe('useCanDelete (NEXUS-53861)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    authenticated();
  });

  it('returns false and skips the server call when the user is not authenticated', () => {
    anonymous();

    const { result } = renderHook(() => useCanDelete(ASSET_NODE, REPO_NAME, null));

    expect(result.current).toBe(false);
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();
  });

  it('returns false and skips the server call when no node is selected', () => {
    const { result } = renderHook(() => useCanDelete(null, REPO_NAME, null));

    expect(result.current).toBe(false);
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();
  });

  it('returns false and skips the server call when no repository is selected', () => {
    const { result } = renderHook(() => useCanDelete(ASSET_NODE, undefined, null));

    expect(result.current).toBe(false);
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();
  });

  it('calls canDeleteAsset for an asset node and returns the server verdict', async () => {
    mockCanDeleteAsset.mockResolvedValue(true);

    const { result } = renderHook(() => useCanDelete(ASSET_NODE, REPO_NAME, null));

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockCanDeleteAsset).toHaveBeenCalledWith('asset-42', REPO_NAME);
    expect(mockCanDeleteComponent).not.toHaveBeenCalled();
    expect(mockCanDeleteFolder).not.toHaveBeenCalled();
  });

  it('returns false when the server denies delete on the asset', async () => {
    mockCanDeleteAsset.mockResolvedValue(false);

    const { result } = renderHook(() => useCanDelete(ASSET_NODE, REPO_NAME, null));

    // Give the effect + promise a chance to flush.
    await waitFor(() => expect(mockCanDeleteAsset).toHaveBeenCalled());
    expect(result.current).toBe(false);
  });

  it('skips the asset call when the node has no assetId', async () => {
    const nodeWithoutId: BrowseNode = { ...ASSET_NODE, assetId: null };

    const { result } = renderHook(() => useCanDelete(nodeWithoutId, REPO_NAME, null));

    expect(result.current).toBe(false);
    expect(mockCanDeleteAsset).not.toHaveBeenCalled();
  });

  it('returns false for a component node while componentData is still loading', () => {
    const { result } = renderHook(() => useCanDelete(COMPONENT_NODE, REPO_NAME, null));

    expect(result.current).toBe(false);
    expect(mockCanDeleteComponent).not.toHaveBeenCalled();
  });

  it('calls canDeleteComponent with the ComponentXO shape once componentData is present', async () => {
    mockCanDeleteComponent.mockResolvedValue(true);

    const { result } = renderHook(() =>
      useCanDelete(COMPONENT_NODE, REPO_NAME, COMPONENT_DATA)
    );

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockCanDeleteComponent).toHaveBeenCalledWith({
      id: COMPONENT_DATA.id,
      repositoryName: COMPONENT_DATA.repositoryName,
      format: COMPONENT_DATA.format,
      group: COMPONENT_DATA.group,
      name: COMPONENT_DATA.name,
      version: COMPONENT_DATA.version,
    });
  });

  it('coerces nullable group and version to XO-safe defaults', async () => {
    mockCanDeleteComponent.mockResolvedValue(true);
    const dataWithNulls: ComponentData = {
      ...COMPONENT_DATA,
      group: null,
      version: null,
    };

    renderHook(() => useCanDelete(COMPONENT_NODE, REPO_NAME, dataWithNulls));

    await waitFor(() => expect(mockCanDeleteComponent).toHaveBeenCalled());
    expect(mockCanDeleteComponent).toHaveBeenCalledWith(
      expect.objectContaining({ group: undefined, version: '' })
    );
  });

  it('calls canDeleteFolder for a folder node using the node id as path', async () => {
    mockCanDeleteFolder.mockResolvedValue(true);

    const { result } = renderHook(() => useCanDelete(FOLDER_NODE, REPO_NAME, null));

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockCanDeleteFolder).toHaveBeenCalledWith(FOLDER_NODE.id, REPO_NAME);
  });

  it('caches results per node so re-selection does not re-fire the network call', async () => {
    mockCanDeleteAsset.mockResolvedValue(true);

    const { result, rerender } = renderHook(
      ({ node }: { node: BrowseNode | null }) => useCanDelete(node, REPO_NAME, null),
      { initialProps: { node: ASSET_NODE as BrowseNode | null } }
    );

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockCanDeleteAsset).toHaveBeenCalledTimes(1);

    // Deselect, then re-select the same asset. The cache should serve the answer
    // without a second server call.
    rerender({ node: null });
    rerender({ node: ASSET_NODE });

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockCanDeleteAsset).toHaveBeenCalledTimes(1);
  });

  it('drops the response from a superseded preflight when the selection changes mid-flight', async () => {
    // First preflight (asset A) hangs indefinitely so we can decide when it
    // resolves. Second preflight (asset B) resolves immediately with false.
    let resolveStalePreflight: (allowed: boolean) => void = () => {};
    const stalePreflight = new Promise<boolean>((resolve) => {
      resolveStalePreflight = resolve;
    });
    mockCanDeleteAsset
      .mockReturnValueOnce(stalePreflight)
      .mockResolvedValueOnce(false);

    const ASSET_B: BrowseNode = {
      ...ASSET_NODE,
      id: 'org/example/lib/2.0/lib-2.0.jar',
      assetId: 'asset-99',
    };

    const { result, rerender } = renderHook(
      ({ node }: { node: BrowseNode }) => useCanDelete(node, REPO_NAME, null),
      { initialProps: { node: ASSET_NODE } }
    );

    // Switch to asset B before asset A's preflight resolves. The effect
    // cleanup must set cancelled = true on the stale request.
    rerender({ node: ASSET_B });

    await waitFor(() => expect(mockCanDeleteAsset).toHaveBeenCalledTimes(2));
    expect(mockCanDeleteAsset).toHaveBeenNthCalledWith(2, 'asset-99', REPO_NAME);
    await waitFor(() => expect(result.current).toBe(false));

    // Asset A's stale preflight now resolves with true. The cancelled guard
    // must prevent this from overwriting asset B's verdict.
    resolveStalePreflight(true);
    await new Promise((r) => setTimeout(r, 0));

    expect(result.current).toBe(false);
  });
});

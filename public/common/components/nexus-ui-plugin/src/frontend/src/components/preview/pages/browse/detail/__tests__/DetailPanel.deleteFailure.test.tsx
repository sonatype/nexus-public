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
 * Regression: a *rejected* delete must not run the
 * successful-deletion path.
 *
 * BrowsePage wires DetailPanel's `onDeleted` to
 * `browseTreeRef.current.removeNode(...)` plus a route change, so invoking it
 * after a server rejection removes a node the server still holds. A rejection
 * instead runs `onDeleteFailed` -> `reconcileNode(...)`, which re-reads the
 * affected level without removing anything: the node survives if the server still
 * lists it and disappears if the delete partially succeeded. This suite
 * reproduces that wiring (tree + detail panel + the real tree actions) instead of
 * asserting on the callbacks alone, so the observable outcome is the tree.
 */

jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response: any) => response.data.result.data),
  },
}));

// Build on the real constants instead of listing only BROWSE: DetailPanel also pulls in
// useComponentIntelligence, which destructures APIConstants.EXT.HEALTH_CHECK_INFO at module load.
// `__esModule`/`default` must be restated because useComponentIntelligence default-imports the module.
jest.mock('../../../../../../constants/APIConstants', () => {
  const actual = jest.requireActual('../../../../../../constants/APIConstants');
  const APIConstants = {
    ...actual.APIConstants,
    EXT: {
      ...actual.APIConstants.EXT,
      BROWSE: { ACTION: 'coreui_Browse', METHODS: { READ: 'read' } },
    },
  };
  return { __esModule: true, APIConstants, default: APIConstants };
});

const mockToast = {
  showToast: jest.fn(),
  success: jest.fn(),
  error: jest.fn(),
  warning: jest.fn(),
  info: jest.fn(),
};

jest.mock('../../../../shared', () => ({
  useToast: () => mockToast,
  DeepResearchLink: ({ href }: { href: string }) => <a href={href}>Deep Research</a>,
  EmptyState: ({ title }: { title: string }) => <div>{title}</div>,
}));

const mockDeleteAsset = jest.fn();

jest.mock('../../browse.api', () => ({
  deleteAsset: (...args: unknown[]) => mockDeleteAsset(...args),
  deleteComponent: jest.fn(),
  deleteFolder: jest.fn(),
  fetchBrowseNodes: jest.fn().mockResolvedValue([]),
}));

import React, { useRef, useCallback } from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
import { BrowseTree } from '../../tree/BrowseTree';
import type { BrowseNode, BrowseTreeRef } from '../../tree/browse-tree.types';
import { DetailPanel, type AssetData } from '../DetailPanel';

const mockRequest = ExtAPIUtils.extAPIRequest as jest.Mock;

const REPO = 'maven-releases';

const DOOMED: BrowseNode = {
  id: 'a.jar',
  text: 'a.jar',
  type: 'asset',
  leaf: true,
  componentId: null,
  assetId: 'asset-a',
  packageUrl: null,
};

const SIBLING: BrowseNode = { ...DOOMED, id: 'b.jar', text: 'b.jar', assetId: 'asset-b' };

const ASSET_DATA: AssetData = {
  id: 'asset-a',
  name: 'a.jar',
  format: 'maven2',
  contentType: 'application/java-archive',
  size: 10,
  repositoryName: REPO,
  blobCreated: '2024-01-15T10:30:00Z',
  blobUpdated: '2024-01-15T10:30:00Z',
  lastDownloaded: null,
  downloadUrl: 'https://server.example.com/repository/maven-releases/a.jar',
};

/** Root has two children so nothing auto-expands and both rows stay visible. */
let rootChildren: BrowseNode[] = [];
let requested: string[] = [];
/** Times the successful-deletion path (remove + navigate) ran. */
let successPathRuns = 0;

/**
 * Mirrors BrowsePage's delete wiring: the detail panel's onDeleted is the
 * successful-deletion path, and it removes the node from the live tree.
 */
function Harness(): JSX.Element {
  const treeRef = useRef<BrowseTreeRef>(null);

  const handleDeleted = useCallback(() => {
    successPathRuns++;
    treeRef.current?.removeNode(DOOMED.id);
  }, []);

  const handleDeleteFailed = useCallback(() => {
    treeRef.current?.reconcileNode(DOOMED.id);
  }, []);

  return (
    <Theme>
      <BrowseTree ref={treeRef} repositoryName={REPO} />
      <DetailPanel
        node={DOOMED}
        repositoryName={REPO}
        assetData={ASSET_DATA}
        canDelete={true}
        onDeleted={handleDeleted}
        onDeleteFailed={handleDeleteFailed}
      />
    </Theme>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  (window as any).__nexusToast = mockToast;
  rootChildren = [DOOMED, SIBLING];
  requested = [];
  successPathRuns = 0;
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => {
    const nodeId = data[0].node;
    requested.push(nodeId);
    return {
      data: { result: { success: true, data: nodeId === '/' ? rootChildren : [] } },
    };
  });
});

afterEach(() => {
  delete (window as any).__nexusToast;
});

async function confirmDelete(): Promise<number> {
  await waitFor(() => expect(screen.getByTestId(`tree-node-${DOOMED.id}`)).toBeInTheDocument());

  await userEvent.click(screen.getByRole('button', { name: /delete asset/i }));
  const requestsBeforeDelete = requested.length;
  await userEvent.click(screen.getByRole('button', { name: /^Delete$/i }));
  return requestsBeforeDelete;
}

describe('failed deletion', () => {
  it('leaves the node in the tree and never reports success when the server rejects', async () => {
    mockDeleteAsset.mockRejectedValue(new Error('Server error (500). Please try again later.'));

    render(<Harness />);
    const requestsBeforeDelete = await confirmDelete();

    await waitFor(() =>
      expect(mockToast.error).toHaveBeenCalledWith(expect.stringContaining('Server error')),
    );
    // Flush any continuation the failed delete may have scheduled.
    await act(async () => {});

    // The successful-deletion path — which removes the node and navigates away —
    // must not run. The level is re-read instead, and because the server still
    // lists the node it stays exactly where it was.
    expect(successPathRuns).toBe(0);
    expect(requested.slice(requestsBeforeDelete)).toEqual(['/']);
    expect(screen.getByTestId(`tree-node-${DOOMED.id}`)).toBeInTheDocument();
    expect(screen.getByTestId(`tree-node-${SIBLING.id}`)).toBeInTheDocument();
    expect(mockToast.success).not.toHaveBeenCalled();

    // Error UX is unchanged: dialog closed, inline reason shown.
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(screen.getByText(/Cannot delete: Server error/)).toBeInTheDocument();
  });

  it('still removes the node from the tree when the delete succeeds', async () => {
    mockDeleteAsset.mockImplementation(async () => {
      // The server really did remove it, so the reload must not resurrect it.
      rootChildren = [SIBLING];
    });

    render(<Harness />);
    await confirmDelete();

    await waitFor(() =>
      expect(mockToast.success).toHaveBeenCalledWith('Item deleted successfully'),
    );
    await waitFor(() =>
      expect(screen.queryByTestId(`tree-node-${DOOMED.id}`)).not.toBeInTheDocument(),
    );
    expect(screen.getByTestId(`tree-node-${SIBLING.id}`)).toBeInTheDocument();
  });

  it('drops the node on a rejected delete that the server actually performed', async () => {
    // Cloud/APT: the asset is removed and the follow-up metadata update fails, so
    // the request is reported as an error even though the node is gone. The
    // reconcile re-read has to converge on the server's listing.
    mockDeleteAsset.mockImplementation(async () => {
      rootChildren = [SIBLING];
      throw new Error('No facet of type AptHostedMetadataFacet');
    });

    render(<Harness />);
    await confirmDelete();

    await waitFor(() =>
      expect(screen.queryByTestId(`tree-node-${DOOMED.id}`)).not.toBeInTheDocument(),
    );
    expect(screen.getByTestId(`tree-node-${SIBLING.id}`)).toBeInTheDocument();
    // Still a failure as far as the user is concerned, and still not the success
    // path: no success toast, no remove-and-navigate.
    expect(successPathRuns).toBe(0);
    expect(mockToast.success).not.toHaveBeenCalled();
    expect(screen.getByText(/Cannot delete: No facet of type/)).toBeInTheDocument();
  });
});

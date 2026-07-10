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

import React from 'react';
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { SupportZipHA } from '../SupportZipHA';
import { DEFAULT_SUPPORT_ZIP_PARAMS, NodeInfo } from '../types';

const mockFetchActiveNodes = jest.fn();
const mockFetchNodeStatus = jest.fn();
const mockGenerateForNode = jest.fn();
const mockClearNode = jest.fn();
const mockGetDownloadUrl = jest.fn(
  (filename: string) => `service/rest/wonderland/download/${filename}`
);
const mockSetError = jest.fn();

jest.mock('../useSupportZipApi', () => ({
  useSupportZipApi: () => ({
    loading: false,
    error: null,
    setError: mockSetError,
    createSupportZip: jest.fn(),
    fetchActiveNodes: mockFetchActiveNodes,
    fetchNodeStatus: mockFetchNodeStatus,
    generateForNode: mockGenerateForNode,
    clearNode: mockClearNode,
    getDownloadUrl: mockGetDownloadUrl,
  }),
}));

const mockRestGet = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestGet(...args),
  },
  parseApiError: jest.fn((err: any) => ({ message: err?.message || 'Unknown error' })),
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const sampleNodes: NodeInfo[] = [
  { nodeId: 'node-a', hostname: 'host-a', status: 'NOT_CREATED' },
  { nodeId: 'node-b', hostname: 'host-b', status: 'NOT_CREATED' },
];

describe('SupportZipHA', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchActiveNodes.mockResolvedValue(sampleNodes);
    mockRestGet.mockResolvedValue([{ name: 'default' }]);
  });

  async function renderHA() {
    let utils: ReturnType<typeof render>;
    await act(async () => {
      utils = render(
        <SupportZipHA params={DEFAULT_SUPPORT_ZIP_PARAMS} onParamChange={jest.fn()} />,
        { wrapper: TestWrapper }
      );
    });
    return utils!;
  }

  it('fetches active nodes and blob stores on mount and renders one card per node', async () => {
    await renderHA();

    await waitFor(() => {
      expect(mockFetchActiveNodes).toHaveBeenCalled();
      expect(mockRestGet).toHaveBeenCalledWith('service/rest/v1/blobstores');
    });

    expect(screen.getByTestId('support-zip-node-card-node-a')).toBeInTheDocument();
    expect(screen.getByTestId('support-zip-node-card-node-b')).toBeInTheDocument();
    expect(screen.getByText('host-a')).toBeInTheDocument();
    expect(screen.getByText('host-b')).toBeInTheDocument();
  });

  it('renders the all-nodes button enabled when blob store is configured and a node is active', async () => {
    await renderHA();

    const allBtn = await screen.findByTestId('support-zip-create-all-button');
    expect(allBtn).toBeEnabled();
  });

  it('disables the all-nodes button when no blob store is configured', async () => {
    mockRestGet.mockResolvedValueOnce([]);

    await renderHA();

    const allBtn = await screen.findByTestId('support-zip-create-all-button');
    expect(allBtn).toBeDisabled();
    expect(screen.getByTestId('support-zip-ha-no-blob-store')).toBeInTheDocument();
  });

  it('opens the modal targeting a single node when its Generate button is clicked', async () => {
    await renderHA();

    const generateBtn = await screen.findByTestId('support-zip-node-card-generate-node-a');
    fireEvent.click(generateBtn);

    expect(screen.getByTestId('support-zip-ha-modal')).toBeInTheDocument();
    expect(screen.getByText(/Generate Support ZIP for host-a/)).toBeInTheDocument();
  });

  it('runs cleanNode then generateForNode on modal confirm', async () => {
    mockClearNode.mockResolvedValue(undefined);
    mockGenerateForNode.mockResolvedValue({
      nodeId: 'node-a',
      hostname: 'host-a',
      status: 'CREATING',
    });

    await renderHA();

    const generateBtn = await screen.findByTestId('support-zip-node-card-generate-node-a');
    fireEvent.click(generateBtn);

    const confirm = await screen.findByTestId('support-zip-ha-modal-confirm');
    await act(async () => {
      fireEvent.click(confirm);
    });

    await waitFor(() => {
      expect(mockClearNode).toHaveBeenCalledWith('node-a');
      expect(mockGenerateForNode).toHaveBeenCalledWith(
        'node-a',
        DEFAULT_SUPPORT_ZIP_PARAMS,
        'host-a'
      );
    });
  });

  it('runs per-node clean+generate for every node on all-nodes confirm', async () => {
    mockClearNode.mockResolvedValue(undefined);
    mockGenerateForNode.mockImplementation(
      (nodeId: string, _params: unknown, hostname: string) =>
        Promise.resolve({ nodeId, hostname, status: 'CREATING' })
    );

    await renderHA();

    const allBtn = await screen.findByTestId('support-zip-create-all-button');
    fireEvent.click(allBtn);

    const confirm = await screen.findByTestId('support-zip-ha-modal-confirm');
    await act(async () => {
      fireEvent.click(confirm);
    });

    await waitFor(() => {
      expect(mockClearNode).toHaveBeenCalledWith('node-a');
      expect(mockClearNode).toHaveBeenCalledWith('node-b');
      expect(mockGenerateForNode).toHaveBeenCalledWith(
        'node-a',
        DEFAULT_SUPPORT_ZIP_PARAMS,
        'host-a'
      );
      expect(mockGenerateForNode).toHaveBeenCalledWith(
        'node-b',
        DEFAULT_SUPPORT_ZIP_PARAMS,
        'host-b'
      );
    });
  });

  it('closes the modal immediately when Generate is clicked, before generation completes', async () => {
    // clearNode never resolves so generation is still in-flight
    mockClearNode.mockReturnValue(new Promise<void>(() => {}));

    await renderHA();

    fireEvent.click(await screen.findByTestId('support-zip-node-card-generate-node-a'));
    expect(screen.getByTestId('support-zip-ha-modal')).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(screen.getByTestId('support-zip-ha-modal-confirm'));
    });

    expect(screen.queryByTestId('support-zip-ha-modal')).not.toBeInTheDocument();
  });

  it('node card enters CREATING state immediately after modal closes', async () => {
    mockClearNode.mockReturnValue(new Promise<void>(() => {}));

    await renderHA();

    fireEvent.click(await screen.findByTestId('support-zip-node-card-generate-node-a'));

    await act(async () => {
      fireEvent.click(screen.getByTestId('support-zip-ha-modal-confirm'));
    });

    expect(screen.queryByTestId('support-zip-ha-modal')).not.toBeInTheDocument();
    expect(screen.getByText('Creating ZIP...')).toBeInTheDocument();
  });

  it('node card shows FAILED state when generation fails after modal closes', async () => {
    mockClearNode.mockRejectedValue(new Error('network error'));

    await renderHA();

    fireEvent.click(await screen.findByTestId('support-zip-node-card-generate-node-a'));

    await act(async () => {
      fireEvent.click(screen.getByTestId('support-zip-ha-modal-confirm'));
    });

    expect(screen.queryByTestId('support-zip-ha-modal')).not.toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Generation failed.')).toBeInTheDocument();
    });
  });

  it('shows a load error when fetchActiveNodes fails', async () => {
    mockFetchActiveNodes.mockRejectedValueOnce(new Error('nope'));

    await renderHA();

    expect(await screen.findByTestId('support-zip-ha-error')).toBeInTheDocument();
  });
});

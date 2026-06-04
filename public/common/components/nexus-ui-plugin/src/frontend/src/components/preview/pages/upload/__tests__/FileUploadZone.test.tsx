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
import { render, screen, waitFor } from '@testing-library/react';
import { fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { FileUploadZone } from '../FileUploadZone';

// jsdom doesn't ship TextDecoder; inject Node.js implementation
// eslint-disable-next-line @typescript-eslint/no-require-imports
(global as unknown as Record<string, unknown>).TextDecoder = require('util').TextDecoder;

jest.mock('fflate', () => ({
  __esModule: true,
  unzipSync: jest.fn(),
}));

// eslint-disable-next-line @typescript-eslint/no-require-imports
const fflate = require('fflate') as { unzipSync: jest.Mock };

describe('FileUploadZone', () => {
  const mockAssets = [{ file: null }];
  const mockAssetFields = [];
  const mockValidationErrors = {};

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the file upload zone', () => {
    render(
      <Theme>
        <FileUploadZone
          assets={mockAssets}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    expect(screen.getByTestId('file-upload-zone')).toBeInTheDocument();
    expect(screen.getByText('Drop files here')).toBeInTheDocument();
  });

  it('shows go module path callout when zip is selected with go format', async () => {
    const encoder = { encode: (s: string) => Buffer.from(s) };
    fflate.unzipSync.mockReturnValue({
      'github.com/user/repo@v1.0.0/go.mod': encoder.encode('module github.com/user/repo\n\ngo 1.21\n'),
    });

    render(
      <Theme>
        <FileUploadZone
          assets={[{ file: null }]}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          format="go"
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    const file = new File(['fake zip'], 'module.zip', { type: 'application/zip' });
    Object.defineProperty(file, 'arrayBuffer', {
      value: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    const input = screen.getByTestId('input-asset-file-0-input');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('go-module-path-0')).toBeInTheDocument();
      expect(screen.getByText('github.com/user/repo')).toBeInTheDocument();
    });
  });

  it('does not show callout when a non-zip file is selected with go format', async () => {
    render(
      <Theme>
        <FileUploadZone
          assets={[{ file: null }]}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          format="go"
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    const file = new File(['module data'], 'module.mod', { type: 'text/plain' });
    const input = screen.getByTestId('input-asset-file-0-input');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.queryByTestId('go-module-path-0')).not.toBeInTheDocument();
    });
    expect(fflate.unzipSync).not.toHaveBeenCalled();
  });

  it('shows warning callout when zip exceeds 50 MB', async () => {
    render(
      <Theme>
        <FileUploadZone
          assets={[{ file: null }]}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          format="go"
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    const file = new File(['fake zip'], 'module.zip', { type: 'application/zip' });
    Object.defineProperty(file, 'size', { value: 51 * 1024 * 1024 });
    const input = screen.getByTestId('input-asset-file-0-input');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('go-module-path-large-0')).toBeInTheDocument();
      expect(screen.getByText(/File exceeds 50 MB/)).toBeInTheDocument();
      expect(screen.getByText(/Upload will still proceed normally/)).toBeInTheDocument();
    });
    expect(fflate.unzipSync).not.toHaveBeenCalled();
  });

  it('ignores go.mod in a subdirectory and shows not-found callout', async () => {
    const encoder = { encode: (s: string) => Buffer.from(s) };
    fflate.unzipSync.mockReturnValue({
      'github.com/user/repo@v1.0.0/subpkg/go.mod': encoder.encode('module github.com/user/repo/subpkg\n\ngo 1.21\n'),
    });

    render(
      <Theme>
        <FileUploadZone
          assets={[{ file: null }]}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          format="go"
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    const file = new File(['fake zip'], 'module.zip', { type: 'application/zip' });
    Object.defineProperty(file, 'arrayBuffer', {
      value: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    const input = screen.getByTestId('input-asset-file-0-input');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.queryByTestId('go-module-path-0')).not.toBeInTheDocument();
      expect(screen.getByTestId('go-module-no-mod-0')).toBeInTheDocument();
    });
  });

  it('shows warning callout when zip contains no valid .mod file', async () => {
    const encoder = { encode: (s: string) => Buffer.from(s) };
    fflate.unzipSync.mockReturnValue({ 'main.go': encoder.encode('package main') });

    render(
      <Theme>
        <FileUploadZone
          assets={[{ file: null }]}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          format="go"
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    const file = new File(['fake zip'], 'module.zip', { type: 'application/zip' });
    Object.defineProperty(file, 'arrayBuffer', {
      value: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    const input = screen.getByTestId('input-asset-file-0-input');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.queryByTestId('go-module-path-0')).not.toBeInTheDocument();
      expect(screen.getByTestId('go-module-no-mod-0')).toBeInTheDocument();
      expect(screen.getByText(/No Go module path found in the zip/)).toBeInTheDocument();
    });
  });
});

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

import '@testing-library/jest-dom';
import { renderHook, waitFor } from '@testing-library/react';
import { useAssetDetail } from '../useAssetDetail';

// Mock the relative paths that the source imports from
jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForError: jest.fn(),
  },
}));

jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
}));

// Mock useToast
const mockToastError = jest.fn();
const mockToastSuccess = jest.fn();
jest.mock('../../../../shared', () => ({
  useToast: () => ({
    error: mockToastError,
    success: mockToastSuccess,
  }),
}));

// Get mock references
import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
const mockExtAPIRequest = ExtAPIUtils.extAPIRequest as jest.MockedFunction<typeof ExtAPIUtils.extAPIRequest>;
const mockCheckForError = ExtAPIUtils.checkForError as jest.MockedFunction<typeof ExtAPIUtils.checkForError>;

describe('useAssetDetail - decodeAssetId', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('decodes URL-encoded special characters in base64 asset IDs', async () => {
    // Plain text: "maven-repo:my-artifact/path+version"
    // Base64 encoded: "bWF2ZW4tcmVwbzpteS1hcnRpZmFjdC9wYXRoK3ZlcnNpb24="
    // URL-encoded (= becomes %3D): "bWF2ZW4tcmVwbzpteS1hcnRpZmFjdC9wYXRoK3ZlcnNpb24%3D"
    const urlEncodedAssetId = 'bWF2ZW4tcmVwbzpteS1hcnRpZmFjdC9wYXRoK3ZlcnNpb24%3D';
    const expectedDecodedValue = 'maven-repo:my-artifact/path+version';

    mockExtAPIRequest.mockResolvedValue({});
    mockCheckForError.mockReturnValue({
      success: true,
      data: {
        id: 'asset-123',
        name: 'test-asset.jar',
        path: '/path/to/asset',
        format: 'maven2',
      },
    });

    renderHook(() =>
      useAssetDetail({
        repositoryName: 'maven-releases',
        assetId: urlEncodedAssetId,
      })
    );

    await waitFor(() => {
      expect(mockExtAPIRequest).toHaveBeenCalledWith('coreui_Component', 'readAsset', [
        expectedDecodedValue,
        'maven-releases',
      ]);
    });
  });

  it('handles base64 asset IDs with spaces (from + character conversion)', async () => {
    // '>>>' encodes to 'Pj4+' which contains a + character
    // URL parsing converts + to space during route parameter parsing
    const assetIdWithSpaces = 'Pj4 ';
    const expectedDecodedValue = '>>>';

    mockExtAPIRequest.mockResolvedValue({});
    mockCheckForError.mockReturnValue({
      success: true,
      data: {
        id: 'asset-123',
        name: 'test-asset.jar',
        path: '/path/to/asset',
        format: 'maven2',
      },
    });

    renderHook(() =>
      useAssetDetail({
        repositoryName: 'maven-releases',
        assetId: assetIdWithSpaces,
      })
    );

    await waitFor(() => {
      expect(mockExtAPIRequest).toHaveBeenCalledWith('coreui_Component', 'readAsset', [
        expectedDecodedValue,
        'maven-releases',
      ]);
    });
  });

  it('handles plain base64 asset IDs without special encoding', async () => {
    const plainBase64AssetId = 'bWF2ZW46c2ltcGxlYXNzZXQ=';
    const expectedDecodedValue = 'maven:simpleasset';

    mockExtAPIRequest.mockResolvedValue({});
    mockCheckForError.mockReturnValue({
      success: true,
      data: {
        id: 'asset-123',
        name: 'test-asset.jar',
        path: '/path/to/asset',
        format: 'maven2',
      },
    });

    renderHook(() =>
      useAssetDetail({
        repositoryName: 'maven-releases',
        assetId: plainBase64AssetId,
      })
    );

    await waitFor(() => {
      expect(mockExtAPIRequest).toHaveBeenCalledWith('coreui_Component', 'readAsset', [
        expectedDecodedValue,
        'maven-releases',
      ]);
    });
  });

  it('returns original ID if decoding fails', async () => {
    const invalidBase64 = 'not-valid-base64!!!';

    mockExtAPIRequest.mockResolvedValue({});
    mockCheckForError.mockReturnValue({
      success: true,
      data: {
        id: 'asset-123',
        name: 'test-asset.jar',
        path: '/path/to/asset',
        format: 'maven2',
      },
    });

    renderHook(() =>
      useAssetDetail({
        repositoryName: 'maven-releases',
        assetId: invalidBase64,
      })
    );

    await waitFor(() => {
      expect(mockExtAPIRequest).toHaveBeenCalledWith('coreui_Component', 'readAsset', [
        invalidBase64,
        'maven-releases',
      ]);
    });
  });
});

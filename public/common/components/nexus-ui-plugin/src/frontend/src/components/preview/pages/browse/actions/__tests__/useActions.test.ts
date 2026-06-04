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

// Mock the REST API from the relative path that the source imports from
// Note: jest.mock is hoisted, so we use jest.fn() inside the factory
jest.mock('../../../../../../interface/api', () => ({
  restClient: {
    delete: jest.fn(),
  },
  parseApiError: jest.fn((err: unknown) => ({
    message:
      (err as any)?.response?.data?.message ||
      (err as any)?.message ||
      'An error occurred',
    status: (err as any)?.response?.status,
  })),
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

import { renderHook, act, waitFor } from '@testing-library/react';
import { restClient } from '../../../../../../interface/api';

import { useActions } from '../useActions';
import { DELETE_ENDPOINTS, ACTION_STRINGS } from '../actions.types';

// Get mock reference
const mockDelete = restClient.delete as jest.MockedFunction<typeof restClient.delete>;

describe('useActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('starts with isDeleting as false', () => {
      const { result } = renderHook(() => useActions());
      expect(result.current.isDeleting).toBe(false);
    });

    it('starts with deletingItem as null', () => {
      const { result } = renderHook(() => useActions());
      expect(result.current.deletingItem).toBeNull();
    });

    it('provides deleteComponent function', () => {
      const { result } = renderHook(() => useActions());
      expect(typeof result.current.deleteComponent).toBe('function');
    });

    it('provides deleteAsset function', () => {
      const { result } = renderHook(() => useActions());
      expect(typeof result.current.deleteAsset).toBe('function');
    });

    it('provides deleteFolder function', () => {
      const { result } = renderHook(() => useActions());
      expect(typeof result.current.deleteFolder).toBe('function');
    });
  });

  describe('deleteComponent', () => {
    const componentId = 'comp-123';
    const repositoryName = 'maven-releases';

    it('calls the correct API endpoint', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteComponent(componentId);
      });

      expect(mockDelete).toHaveBeenCalledWith(
        `${DELETE_ENDPOINTS.COMPONENT}/${encodeURIComponent(componentId)}`
      );
    });

    it('sets isDeleting to true during operation', async () => {
      let resolvePromise: () => void;
      const promise = new Promise<void>((resolve) => {
        resolvePromise = resolve;
      });
      mockDelete.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useActions());

      act(() => {
        result.current.deleteComponent(componentId);
      });

      await waitFor(() => {
        expect(result.current.isDeleting).toBe(true);
      });

      await act(async () => {
        resolvePromise!();
        await promise;
      });

      expect(result.current.isDeleting).toBe(false);
    });

    it('sets deletingItem during operation', async () => {
      let resolvePromise: () => void;
      const promise = new Promise<void>((resolve) => {
        resolvePromise = resolve;
      });
      mockDelete.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useActions());

      act(() => {
        result.current.deleteComponent(componentId, repositoryName);
      });

      await waitFor(() => {
        expect(result.current.deletingItem).not.toBeNull();
        expect(result.current.deletingItem?.type).toBe('component');
        expect(result.current.deletingItem?.id).toBe(componentId);
        expect(result.current.deletingItem?.repositoryName).toBe(repositoryName);
      });

      await act(async () => {
        resolvePromise!();
        await promise;
      });

      expect(result.current.deletingItem).toBeNull();
    });

    it('returns success result on successful delete', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useActions());

      let deleteResult;
      await act(async () => {
        deleteResult = await result.current.deleteComponent(componentId);
      });

      expect(deleteResult).toEqual({ success: true });
    });

    it('calls onDeleteSuccess callback on success', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      const onDeleteSuccess = jest.fn();

      const { result } = renderHook(() => useActions({ onDeleteSuccess }));

      await act(async () => {
        await result.current.deleteComponent(componentId, repositoryName);
      });

      expect(onDeleteSuccess).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'component',
          id: componentId,
          repositoryName,
        })
      );
    });

    it('returns error result on failed delete', async () => {
      const errorMessage = 'Component not found';
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useActions());

      let deleteResult;
      await act(async () => {
        deleteResult = await result.current.deleteComponent(componentId);
      });

      expect(deleteResult).toEqual({
        success: false,
        error: errorMessage,
      });
    });

    it('shows error message via toast on failure', async () => {
      const errorMessage = 'Component not found';
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteComponent(componentId);
      });

      expect(mockToastError).toHaveBeenCalledWith(
        expect.stringContaining(ACTION_STRINGS.errors.deleteComponentFailed)
      );
    });

    it('calls onDeleteError callback on failure', async () => {
      const errorMessage = 'Component not found';
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: errorMessage } },
      });
      const onDeleteError = jest.fn();

      const { result } = renderHook(() => useActions({ onDeleteError }));

      await act(async () => {
        await result.current.deleteComponent(componentId);
      });

      expect(onDeleteError).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'component', id: componentId }),
        errorMessage
      );
    });

    it('handles network errors gracefully', async () => {
      mockDelete.mockRejectedValueOnce(new Error('Network Error'));

      const { result } = renderHook(() => useActions());

      let deleteResult;
      await act(async () => {
        deleteResult = await result.current.deleteComponent(componentId);
      });

      expect(deleteResult?.success).toBe(false);
      expect(mockToastError).toHaveBeenCalled();
    });
  });

  describe('deleteAsset', () => {
    const assetId = 'asset-456';
    const repositoryName = 'npm-hosted';

    it('calls the correct API endpoint', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteAsset(assetId);
      });

      expect(mockDelete).toHaveBeenCalledWith(
        `${DELETE_ENDPOINTS.ASSET}/${encodeURIComponent(assetId)}`
      );
    });

    it('sets isDeleting to true during operation', async () => {
      let resolvePromise: () => void;
      const promise = new Promise<void>((resolve) => {
        resolvePromise = resolve;
      });
      mockDelete.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useActions());

      act(() => {
        result.current.deleteAsset(assetId);
      });

      await waitFor(() => {
        expect(result.current.isDeleting).toBe(true);
      });

      await act(async () => {
        resolvePromise!();
        await promise;
      });

      expect(result.current.isDeleting).toBe(false);
    });

    it('returns success result on successful delete', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useActions());

      let deleteResult;
      await act(async () => {
        deleteResult = await result.current.deleteAsset(assetId, repositoryName);
      });

      expect(deleteResult).toEqual({ success: true });
    });

    it('shows correct error message on failure', async () => {
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: 'Not Found' } },
      });

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteAsset(assetId);
      });

      expect(mockToastError).toHaveBeenCalledWith(
        expect.stringContaining(ACTION_STRINGS.errors.deleteAssetFailed)
      );
    });
  });

  describe('deleteFolder', () => {
    const path = '/com/example/artifact';
    const repositoryName = 'maven-releases';

    it('successfully deletes folder using REST API', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      const { result } = renderHook(() => useActions());

      let deleteResult;
      await act(async () => {
        deleteResult = await result.current.deleteFolder(path, repositoryName);
      });

      expect(deleteResult?.success).toBe(true);
      expect(mockDelete).toHaveBeenCalledWith(
        `/service/rest/v1/repositories/${repositoryName}/browse?path=${encodeURIComponent(path)}`
      );
    });

    it('shows error message on folder deletion failure', async () => {
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: 'Not Found' } },
      });

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteFolder(path, repositoryName);
      });

      expect(mockToastError).toHaveBeenCalledWith(
        expect.stringContaining(ACTION_STRINGS.errors.deleteFolderFailed)
      );
    });

    it('calls onDeleteError callback on failure', async () => {
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: 'Forbidden' } },
      });
      const onDeleteError = jest.fn();

      const { result } = renderHook(() => useActions({ onDeleteError }));

      await act(async () => {
        await result.current.deleteFolder(path, repositoryName);
      });

      expect(onDeleteError).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'folder',
          id: path,
          repositoryName,
        }),
        expect.any(String)
      );
    });

    it('decodes URL-encoded folder paths before sending (e.g., Go @v)', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      // Backend returns node.id with URL-encoded segments: @v → %40v
      const encodedPath = 'github.com/cespare/xxhash/v2/%40v';
      const goRepo = 'go-proxy';

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteFolder(encodedPath, goRepo);
      });

      // Path should be decoded first, then re-encoded by encodeURIComponent
      const decodedPath = 'github.com/cespare/xxhash/v2/@v';
      expect(mockDelete).toHaveBeenCalledWith(
        `/service/rest/v1/repositories/${goRepo}/browse?path=${encodeURIComponent(decodedPath)}`
      );
    });

    it('extracts folder name from path for error callback', async () => {
      mockDelete.mockRejectedValueOnce(new Error('Network Error'));
      const onDeleteError = jest.fn();

      const { result } = renderHook(() => useActions({ onDeleteError }));

      await act(async () => {
        await result.current.deleteFolder(path, repositoryName);
      });

      expect(onDeleteError).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'artifact',
        }),
        expect.any(String)
      );
    });
  });

  describe('URL encoding', () => {
    it('encodes component IDs with special characters', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      const specialId = 'com.example:artifact:1.0.0';

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteComponent(specialId);
      });

      expect(mockDelete).toHaveBeenCalledWith(
        `${DELETE_ENDPOINTS.COMPONENT}/${encodeURIComponent(specialId)}`
      );
    });

    it('encodes asset IDs with special characters', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      const specialId = 'path/to/file with spaces.jar';

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteAsset(specialId);
      });

      expect(mockDelete).toHaveBeenCalledWith(
        `${DELETE_ENDPOINTS.ASSET}/${encodeURIComponent(specialId)}`
      );
    });

    it('handles Go asset IDs with plus signs', async () => {
      mockDelete.mockResolvedValueOnce(undefined);
      const goAssetId = 'v2.2.1+incompatible';

      const { result } = renderHook(() => useActions());

      await act(async () => {
        await result.current.deleteAsset(goAssetId);
      });

      expect(mockDelete).toHaveBeenCalledWith(
        `${DELETE_ENDPOINTS.ASSET}/${encodeURIComponent(goAssetId)}`
      );
    });
  });
});

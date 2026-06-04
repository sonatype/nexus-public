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

import { renderHook, act, waitFor } from '@testing-library/react';

import { restClient } from '../../../../../interface/api';
import { useUploadForm } from '../hooks/useUploadForm';
import type { UploadComponentField, UploadFieldDefinition } from '../upload.types';

// Mock restClient used by useUploadForm
// jest.mock is hoisted, so the factory must not reference block-scoped variables.
// We mock the module and capture the mock reference after import.
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    post: jest.fn(),
  },
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  APIConstants: {
    REST: {
      INTERNAL: {
        UPLOAD: 'service/rest/internal/ui/upload/',
      },
    },
  },
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
}));

const mockPost = restClient.post as jest.Mock;

describe('useUploadForm', () => {
  const componentFields: UploadComponentField[] = [
    { name: 'groupId', type: 'STRING', displayName: 'Group ID', group: 'Component coordinates', optional: false },
    { name: 'artifactId', type: 'STRING', displayName: 'Artifact ID', group: 'Component coordinates', optional: false },
    { name: 'version', type: 'STRING', displayName: 'Version', group: 'Component coordinates', optional: false },
    { name: 'generate-pom', type: 'BOOLEAN', displayName: 'Generate POM', group: 'Options', optional: true },
  ];

  const assetFields: UploadFieldDefinition[] = [
    { name: 'extension', type: 'STRING', displayName: 'Extension', optional: false },
    { name: 'classifier', type: 'STRING', displayName: 'Classifier', optional: true },
  ];

  const defaultProps = {
    repositoryName: 'maven-releases',
    componentFields,
    assetFields,
    multipleUpload: true,
    regexMap: null,
    disabledFields: new Set<string>(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('initializes with default form data', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    expect(result.current.formData.assets).toHaveLength(1);
    expect(result.current.formData.assets[0].file).toBeNull();
    expect(result.current.formData.assets[0].extension).toBe('');
    expect(result.current.formData.assets[0].classifier).toBe('');
    expect(result.current.formData.componentFields.groupId).toBe('');
    expect(result.current.formData.componentFields.artifactId).toBe('');
    expect(result.current.formData.componentFields.version).toBe('');
    expect(result.current.formData.componentFields['generate-pom']).toBe(false);
  });

  it('sets asset file correctly', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));
    const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

    act(() => {
      result.current.setAssetFile(0, mockFile);
    });

    expect(result.current.formData.assets[0].file).toBe(mockFile);
  });

  it('sets asset field correctly', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    act(() => {
      result.current.setAssetField(0, 'extension', 'jar');
    });

    expect(result.current.formData.assets[0].extension).toBe('jar');
  });

  it('sets component field correctly', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    act(() => {
      result.current.setComponentField('groupId', 'com.example');
    });

    expect(result.current.formData.componentFields.groupId).toBe('com.example');
  });

  it('adds new asset when multipleUpload is true', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    act(() => {
      result.current.addAsset();
    });

    expect(result.current.formData.assets).toHaveLength(2);
    expect(result.current.formData.assets[1].file).toBeNull();
  });

  it('does not add asset when multipleUpload is false', () => {
    const { result } = renderHook(() => useUploadForm({ ...defaultProps, multipleUpload: false }));

    act(() => {
      result.current.addAsset();
    });

    expect(result.current.formData.assets).toHaveLength(1);
  });

  it('removes asset correctly', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    act(() => {
      result.current.addAsset();
      result.current.addAsset();
    });

    expect(result.current.formData.assets).toHaveLength(3);

    act(() => {
      result.current.removeAsset(1);
    });

    expect(result.current.formData.assets).toHaveLength(2);
  });

  it('does not remove last asset', () => {
    const { result } = renderHook(() => useUploadForm(defaultProps));

    act(() => {
      result.current.removeAsset(0);
    });

    expect(result.current.formData.assets).toHaveLength(1);
  });

  describe('validation', () => {
    it('validates required file', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      act(() => {
        result.current.validate();
      });

      expect(result.current.validationErrors.assets?.[0]?.file).toBe('File is required');
    });

    it('validates required asset fields', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.validate();
      });

      expect(result.current.validationErrors.assets?.[0]?.extension).toBe('This field is required');
    });

    it('validates required component fields', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.validate();
      });

      expect(result.current.validationErrors.componentFields?.groupId).toBe('This field is required');
      expect(result.current.validationErrors.componentFields?.artifactId).toBe('This field is required');
    });

    it('skips validation for optional fields', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
        result.current.validate();
      });

      // Optional fields should not have errors
      expect(result.current.validationErrors.assets?.[0]?.classifier).toBeFalsy();
      expect(result.current.validationErrors.componentFields?.['generate-pom']).toBeFalsy();
    });

    it('validates asset uniqueness when multiple assets have same field values', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile1 = new File(['content1'], 'test1.jar', { type: 'application/java-archive' });
      const mockFile2 = new File(['content2'], 'test2.jar', { type: 'application/java-archive' });

      act(() => {
        // Set up first asset with all required fields
        result.current.setAssetFile(0, mockFile1);
        result.current.setAssetField(0, 'extension', 'jar');

        // Set up component fields
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');

        // Add second asset with identical extension (should trigger uniqueness error)
        result.current.addAsset();
        result.current.setAssetFile(1, mockFile2);
        result.current.setAssetField(1, 'extension', 'jar'); // Same as first asset
      });

      let isValid: boolean = true;
      act(() => {
        isValid = result.current.validate();
      });

      // Should be invalid due to non-unique assets
      expect(isValid).toBe(false);
      
      // Both assets should have uniqueness errors on their extension field
      expect(result.current.validationErrors.assets?.[0]?.extension).toBe('Asset fields must be unique');
      expect(result.current.validationErrors.assets?.[1]?.extension).toBe('Asset fields must be unique');
    });

    it('skips disabled fields during validation', () => {
      const { result } = renderHook(() =>
        useUploadForm({
          ...defaultProps,
          disabledFields: new Set(['groupId']),
        })
      );
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
        result.current.validate();
      });

      expect(result.current.validationErrors.componentFields?.groupId).toBeFalsy();
    });

    it('returns true when form is valid', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let isValid = false;
      act(() => {
        isValid = result.current.validate();
      });

      expect(isValid).toBe(true);
      expect(result.current.isValid).toBe(true);
    });
  });

  describe('submission', () => {
    it('submits form successfully', async () => {
      // restClient.post returns data directly (not wrapped in {data: ...})
      mockPost.mockResolvedValue({ success: true, data: 'com.example:test:1.0.0' });

      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let submitResult: { success: boolean; componentName?: string } = { success: false };
      await act(async () => {
        submitResult = await result.current.submit();
      });

      expect(submitResult.success).toBe(true);
      expect(submitResult.componentName).toBe('com.example:test:1.0.0');
      expect(mockPost).toHaveBeenCalledWith(
        'service/rest/internal/ui/upload/maven-releases',
        expect.any(FormData)
      );
    });

    it('does not submit if validation fails', async () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      let submitResult: { success: boolean; error?: string } = { success: false };
      await act(async () => {
        submitResult = await result.current.submit();
      });

      expect(submitResult.success).toBe(false);
      expect(submitResult.error).toBe('Please fix validation errors');
      expect(mockPost).not.toHaveBeenCalled();
    });

    it('handles API errors', async () => {
      mockPost.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let submitResult: { success: boolean; error?: string } = { success: false };
      await act(async () => {
        submitResult = await result.current.submit();
      });

      expect(submitResult.success).toBe(false);
      expect(submitResult.error).toContain('Network error');
    });

    it('handles API response errors', async () => {
      // restClient.post returns data directly
      mockPost.mockResolvedValue({ success: false, 0: { message: 'Invalid component' } });

      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let submitResult: { success: boolean; error?: string } = { success: false };
      await act(async () => {
        submitResult = await result.current.submit();
      });

      expect(submitResult.success).toBe(false);
      expect(submitResult.error).toContain('Invalid component');
    });

    it('sets isSubmitting during submission', async () => {
      let resolvePromise: (value: unknown) => void;
      mockPost.mockReturnValue(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      // Start submission
      act(() => {
        result.current.submit();
      });

      expect(result.current.isSubmitting).toBe(true);

      // Resolve promise
      await act(async () => {
        resolvePromise!({ success: true, data: 'test' });
      });

      await waitFor(() => {
        expect(result.current.isSubmitting).toBe(false);
      });
    });
  });

  describe('regex map', () => {
    // Extended asset fields for regex map tests
    const regexMapAssetFields: UploadFieldDefinition[] = [
      { name: 'extension', type: 'STRING', displayName: 'Extension', optional: false },
      { name: 'classifier', type: 'STRING', displayName: 'Classifier', optional: true },
      { name: 'artifactId', type: 'STRING', displayName: 'Artifact ID', optional: true },
      { name: 'version', type: 'STRING', displayName: 'Version', optional: true },
    ];

    it('applies regex map when file is selected', () => {
      // Simpler regex for testing: captures name, version, extension
      const regexMap = {
        regex: '^([a-zA-Z]+)-([0-9.]+)\\.([a-z]+)$',
        fieldList: ['artifactId', 'version', 'extension'],
      };

      const { result } = renderHook(() =>
        useUploadForm({
          ...defaultProps,
          assetFields: regexMapAssetFields,
          regexMap,
        })
      );

      const mockFile = new File(['content'], 'myapp-1.0.0.jar', {
        type: 'application/java-archive',
      });

      act(() => {
        result.current.setAssetFile(0, mockFile);
      });

      expect(result.current.formData.assets[0].artifactId).toBe('myapp');
      expect(result.current.formData.assets[0].version).toBe('1.0.0');
      expect(result.current.formData.assets[0].extension).toBe('jar');
    });

    it('handles non-matching filenames gracefully', () => {
      const regexMap = {
        regex: '^([a-zA-Z]+)-([0-9.]+)\\.([a-z]+)$',
        fieldList: ['artifactId', 'version', 'extension'],
      };

      const { result } = renderHook(() =>
        useUploadForm({
          ...defaultProps,
          assetFields: regexMapAssetFields,
          regexMap,
        })
      );

      const mockFile = new File(['content'], 'invalid-filename', {
        type: 'application/octet-stream',
      });

      act(() => {
        result.current.setAssetFile(0, mockFile);
      });

      // Fields should remain empty when regex doesn't match
      expect(result.current.formData.assets[0].extension).toBe('');
    });
  });

  describe('reset', () => {
    it('resets form to initial state', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      act(() => {
        result.current.setComponentField('groupId', 'com.example');
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.addAsset();
      });

      expect(result.current.formData.assets).toHaveLength(2);
      expect(result.current.formData.componentFields.groupId).toBe('com.example');

      act(() => {
        result.current.reset();
      });

      expect(result.current.formData.assets).toHaveLength(1);
      expect(result.current.formData.componentFields.groupId).toBe('');
      expect(result.current.validationErrors).toEqual({});
    });
  });

  describe('character validation', () => {
    it('rejects groupId with spaces', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let isValid = true;
      act(() => {
        isValid = result.current.validate();
      });

      expect(isValid).toBe(false);
      expect(result.current.validationErrors.componentFields?.groupId).toContain(
        'Group ID must contain only letters, numbers, dots, hyphens, and underscores'
      );
    });

    it('accepts groupId with valid dotted notation', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0');
      });

      let isValid = false;
      act(() => {
        isValid = result.current.validate();
      });

      expect(isValid).toBe(true);
      expect(result.current.validationErrors.componentFields?.groupId).toBeFalsy();
    });

    it('rejects artifactId with special characters', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'my@artifact!');
        result.current.setComponentField('version', '1.0.0');
      });

      let isValid = true;
      act(() => {
        isValid = result.current.validate();
      });

      expect(isValid).toBe(false);
      expect(result.current.validationErrors.componentFields?.artifactId).toContain(
        'Artifact ID must contain only letters, numbers, dots, hyphens, and underscores'
      );
    });

    it('accepts version with SNAPSHOT suffix', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        result.current.setAssetField(0, 'extension', 'jar');
        result.current.setComponentField('groupId', 'com.example');
        result.current.setComponentField('artifactId', 'test');
        result.current.setComponentField('version', '1.0.0-SNAPSHOT');
      });

      let isValid = false;
      act(() => {
        isValid = result.current.validate();
      });

      expect(isValid).toBe(true);
      expect(result.current.validationErrors.componentFields?.version).toBeFalsy();
    });
  });

  describe('blur validation', () => {
    it('blurComponentField triggers validation without prior submit', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      act(() => {
        result.current.setComponentField('groupId', 'bad value!');
      });

      expect(result.current.validationErrors.componentFields?.groupId).toBeFalsy();

      act(() => {
        result.current.blurComponentField('groupId');
      });

      expect(result.current.validationErrors.componentFields?.groupId).toContain(
        'Group ID must contain only letters, numbers, dots, hyphens, and underscores'
      );
    });

    it('blurComponentField adds field to touchedFields', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      expect(result.current.touchedFields.has('groupId')).toBe(false);

      act(() => {
        result.current.blurComponentField('groupId');
      });

      expect(result.current.touchedFields.has('groupId')).toBe(true);
    });

    it('blurAssetField validates asset field on blur', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));
      const mockFile = new File(['content'], 'test.jar', { type: 'application/java-archive' });

      act(() => {
        result.current.setAssetFile(0, mockFile);
        // Leave extension empty (required field)
        result.current.setAssetField(0, 'extension', '');
      });

      expect(result.current.validationErrors.assets?.[0]?.extension).toBeFalsy();

      act(() => {
        result.current.blurAssetField(0, 'extension');
      });

      expect(result.current.validationErrors.assets?.[0]?.extension).toBe('This field is required');
    });
  });

  describe('isDirty', () => {
    it('returns false initially', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      expect(result.current.isDirty).toBe(false);
    });

    it('returns true after changes', () => {
      const { result } = renderHook(() => useUploadForm(defaultProps));

      act(() => {
        result.current.setComponentField('groupId', 'com.example');
      });

      expect(result.current.isDirty).toBe(true);
    });
  });
});


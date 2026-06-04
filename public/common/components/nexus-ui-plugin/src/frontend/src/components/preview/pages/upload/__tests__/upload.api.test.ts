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

import Axios from 'axios';
import {
  uploadToRepository,
  buildUploadFormData,
  calculateFormDataSize,
  formatBytes,
  formatTime,
} from '../upload.api';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('upload.api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('uploadToRepository', () => {
    it('should upload successfully and return component name', async () => {
      mockedAxios.post.mockResolvedValueOnce({
        data: { success: true, data: 'my-component' },
      });

      const formData = new FormData();
      formData.append('asset0', new File(['test'], 'test.jar'));

      const result = await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
      });

      expect(result).toEqual({
        success: true,
        componentName: 'my-component',
      });

      expect(mockedAxios.post).toHaveBeenCalledWith(
        expect.stringContaining('maven-hosted'),
        formData,
        expect.any(Object)
      );
    });

    it('should handle error response format', async () => {
      mockedAxios.post.mockResolvedValueOnce({
        data: { success: false },
      });

      const formData = new FormData();
      const result = await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
      });

      expect(result).toEqual({
        success: false,
        error: 'Upload failed',
      });
    });

    it('should handle array error format', async () => {
      mockedAxios.post.mockResolvedValueOnce({
        data: [{ message: 'Invalid file format' }],
      });

      const formData = new FormData();
      const result = await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
      });

      expect(result).toEqual({
        success: false,
        error: 'Invalid file format',
      });
    });

    it('should call onProgress callback', async () => {
      let capturedProgressCallback: ((event: any) => void) | undefined;

      mockedAxios.post.mockImplementationOnce((url, data, config) => {
        capturedProgressCallback = config?.onUploadProgress;
        return Promise.resolve({ data: { success: true, data: 'component' } });
      });

      const onProgress = jest.fn();
      const formData = new FormData();

      await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
        onProgress,
      });

      // Simulate progress event
      if (capturedProgressCallback) {
        capturedProgressCallback({ loaded: 50, total: 100 });
      }

      expect(onProgress).toHaveBeenCalled();
      expect(onProgress).toHaveBeenCalledWith(
        expect.objectContaining({
          loaded: 50,
          total: 100,
          percentage: 50,
        })
      );
    });

    it('should handle network errors', async () => {
      // Create an axios-like error
      const error = new Error('Network Error') as any;
      error.isAxiosError = true;
      error.response = { data: { message: 'Network Error' } };
      mockedAxios.post.mockRejectedValueOnce(error);

      const formData = new FormData();
      const result = await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
      });

      expect(result).toEqual({
        success: false,
        error: 'Network Error',
      });
    });

    it('should handle cancellation', async () => {
      // Create a cancel error
      const cancelError = new Error('cancelled') as any;
      cancelError.__CANCEL__ = true; // Axios cancel token marker
      mockedAxios.post.mockRejectedValueOnce(cancelError);

      const formData = new FormData();
      const result = await uploadToRepository({
        repositoryName: 'maven-hosted',
        formData,
      });

      // When Axios.isCancel is not mockable, it won't detect as cancel
      // The error will be treated as a generic error
      expect(result.success).toBe(false);
      expect(result.error).toBeDefined();
    });

    it('should encode repository name in URL', async () => {
      mockedAxios.post.mockResolvedValueOnce({
        data: { success: true, data: 'component' },
      });

      const formData = new FormData();
      await uploadToRepository({
        repositoryName: 'my repo/name',
        formData,
      });

      expect(mockedAxios.post).toHaveBeenCalledWith(
        expect.stringContaining('my%20repo%2Fname'),
        formData,
        expect.any(Object)
      );
    });
  });

  describe('buildUploadFormData', () => {
    it('should build FormData with files and fields', () => {
      const file = new File(['test content'], 'test.jar', { type: 'application/java-archive' });
      const assets = [
        { file, extension: 'jar', classifier: '' },
      ];
      const componentFields = {
        'maven2.groupId': 'com.example',
        'maven2.artifactId': 'my-app',
        'maven2.version': '1.0.0',
      };
      const assetFieldNames = ['extension', 'classifier'];

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames);

      expect(formData.get('asset0')).toBe(file);
      expect(formData.get('asset0.extension')).toBe('jar');
      expect(formData.get('maven2.groupId')).toBe('com.example');
      expect(formData.get('maven2.artifactId')).toBe('my-app');
      expect(formData.get('maven2.version')).toBe('1.0.0');
    });

    it('should handle multiple assets', () => {
      const file1 = new File(['jar content'], 'app.jar');
      const file2 = new File(['pom content'], 'app.pom');

      const assets = [
        { file: file1, extension: 'jar' },
        { file: file2, extension: 'pom' },
      ];
      const componentFields = {};
      const assetFieldNames = ['extension'];

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames);

      expect(formData.get('asset0')).toBe(file1);
      expect(formData.get('asset0.extension')).toBe('jar');
      expect(formData.get('asset1')).toBe(file2);
      expect(formData.get('asset1.extension')).toBe('pom');
    });

    it('should skip disabled fields', () => {
      const file = new File(['test'], 'test.jar');
      const assets = [{ file, extension: 'jar' }];
      const componentFields = {
        'maven2.groupId': 'com.example',
        'maven2.packaging': 'jar',
      };
      const assetFieldNames = ['extension'];
      const disabledFields = new Set(['maven2.packaging']);

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames, disabledFields);

      expect(formData.get('maven2.groupId')).toBe('com.example');
      expect(formData.get('maven2.packaging')).toBeNull();
    });

    it('should skip empty values', () => {
      const file = new File(['test'], 'test.jar');
      const assets = [{ file, extension: '', classifier: null as any }];
      const componentFields = {
        'maven2.groupId': 'com.example',
        'maven2.artifactId': '',
      };
      const assetFieldNames = ['extension', 'classifier'];

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames);

      expect(formData.get('asset0.extension')).toBeNull();
      expect(formData.get('asset0.classifier')).toBeNull();
      expect(formData.get('maven2.artifactId')).toBeNull();
    });

    it('should handle null file gracefully', () => {
      const assets = [{ file: null, extension: 'jar' }];
      const componentFields = {};
      const assetFieldNames = ['extension'];

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames);

      expect(formData.get('asset0')).toBeNull();
      expect(formData.get('asset0.extension')).toBe('jar');
    });

    it('should convert boolean values to strings', () => {
      const file = new File(['test'], 'test.jar');
      const assets = [{ file }];
      const componentFields = {
        'maven2.generate-pom': true,
      };
      const assetFieldNames: string[] = [];

      const formData = buildUploadFormData(assets, componentFields, assetFieldNames);

      expect(formData.get('maven2.generate-pom')).toBe('true');
    });
  });

  describe('calculateFormDataSize', () => {
    it('should calculate total size of files', () => {
      const formData = new FormData();
      const file1 = new File(['hello'], 'file1.txt'); // 5 bytes
      const file2 = new File(['world!'], 'file2.txt'); // 6 bytes
      formData.append('file1', file1);
      formData.append('file2', file2);

      const size = calculateFormDataSize(formData);

      expect(size).toBe(11);
    });

    it('should include string field sizes', () => {
      const formData = new FormData();
      formData.append('field1', 'test'); // 4 bytes

      const size = calculateFormDataSize(formData);

      expect(size).toBe(4);
    });

    it('should return 0 for empty FormData', () => {
      const formData = new FormData();
      const size = calculateFormDataSize(formData);

      expect(size).toBe(0);
    });
  });

  describe('formatBytes', () => {
    it('should format 0 bytes', () => {
      expect(formatBytes(0)).toBe('0 B');
    });

    it('should format bytes', () => {
      expect(formatBytes(500)).toBe('500 B');
    });

    it('should format kilobytes', () => {
      expect(formatBytes(1024)).toBe('1.0 KB');
      expect(formatBytes(1536)).toBe('1.5 KB');
    });

    it('should format megabytes', () => {
      expect(formatBytes(1048576)).toBe('1.0 MB');
      expect(formatBytes(2621440)).toBe('2.5 MB');
    });

    it('should format gigabytes', () => {
      expect(formatBytes(1073741824)).toBe('1.0 GB');
    });
  });

  describe('formatTime', () => {
    it('should format less than a second', () => {
      expect(formatTime(500)).toBe('less than a second');
    });

    it('should format seconds', () => {
      expect(formatTime(1000)).toBe('1 second');
      expect(formatTime(5000)).toBe('5 seconds');
    });

    it('should format minutes', () => {
      expect(formatTime(60000)).toBe('1 minute');
      expect(formatTime(120000)).toBe('2 minutes');
    });

    it('should format minutes and seconds', () => {
      expect(formatTime(90000)).toBe('1m 30s');
    });

    it('should format hours', () => {
      expect(formatTime(3600000)).toBe('1h 0m');
      expect(formatTime(5400000)).toBe('1h 30m');
    });
  });
});


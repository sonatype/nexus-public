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
  getFormatLabel,
  getFormatColor,
  buildDisplayName,
  buildComponentId,
  searchNpm,
  searchNuGet,
  searchDocker,
  searchGeneric,
  FORMAT_DISPLAY,
} from '../searchUtils';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('searchUtils', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getFormatLabel', () => {
    it('returns the correct label for known formats', () => {
      expect(getFormatLabel('maven2')).toBe('Maven');
      expect(getFormatLabel('npm')).toBe('npm');
      expect(getFormatLabel('nuget')).toBe('NuGet');
      expect(getFormatLabel('docker')).toBe('Docker');
      expect(getFormatLabel('pypi')).toBe('PyPI');
      expect(getFormatLabel('helm')).toBe('Helm');
    });

    it('returns the format string for unknown formats', () => {
      expect(getFormatLabel('unknown')).toBe('unknown');
      expect(getFormatLabel('custom-format')).toBe('custom-format');
    });
  });

  describe('getFormatColor', () => {
    it('returns the correct color for known formats', () => {
      expect(getFormatColor('maven2')).toBe('orange');
      expect(getFormatColor('npm')).toBe('red');
      expect(getFormatColor('nuget')).toBe('blue');
      expect(getFormatColor('docker')).toBe('cyan');
    });

    it('returns gray for unknown formats', () => {
      expect(getFormatColor('unknown')).toBe('gray');
      expect(getFormatColor('custom-format')).toBe('gray');
    });
  });

  describe('buildDisplayName', () => {
    it('combines group and name when group is present', () => {
      expect(buildDisplayName('org.apache.commons', 'commons-lang3')).toBe(
        'org.apache.commons:commons-lang3'
      );
      expect(buildDisplayName('@angular', 'core')).toBe('@angular:core');
    });

    it('returns just the name when group is null', () => {
      expect(buildDisplayName(null, 'lodash')).toBe('lodash');
      expect(buildDisplayName(null, 'nginx')).toBe('nginx');
    });

    it('handles empty group as falsy', () => {
      expect(buildDisplayName('', 'package-name')).toBe('package-name');
    });
  });

  describe('buildComponentId', () => {
    it('builds ID with group when present', () => {
      expect(
        buildComponentId('maven2', 'org.apache.commons', 'commons-lang3', '3.14.0')
      ).toBe('maven2:org.apache.commons:commons-lang3:3.14.0');
    });

    it('builds ID without group when null', () => {
      expect(buildComponentId('npm', null, 'lodash', '4.17.21')).toBe(
        'npm:lodash:4.17.21'
      );
    });

    it('builds ID for Docker images', () => {
      expect(buildComponentId('docker', null, 'nginx', 'latest')).toBe(
        'docker:nginx:latest'
      );
    });
  });

  describe('FORMAT_DISPLAY', () => {
    it('contains expected format configurations', () => {
      expect(FORMAT_DISPLAY.maven2).toEqual({ label: 'Maven', color: 'orange' });
      expect(FORMAT_DISPLAY.npm).toEqual({ label: 'npm', color: 'red' });
      expect(FORMAT_DISPLAY.nuget).toEqual({ label: 'NuGet', color: 'blue' });
      expect(FORMAT_DISPLAY.docker).toEqual({ label: 'Docker', color: 'cyan' });
    });

    it('has all common formats defined', () => {
      const expectedFormats = [
        'maven2',
        'npm',
        'nuget',
        'docker',
        'pypi',
        'raw',
        'helm',
        'go',
        'rubygems',
        'apt',
        'yum',
        'conda',
      ];

      for (const format of expectedFormats) {
        expect(FORMAT_DISPLAY[format]).toBeDefined();
        expect(FORMAT_DISPLAY[format].label).toBeTruthy();
        expect(FORMAT_DISPLAY[format].color).toBeTruthy();
      }
    });
  });

  describe('searchNpm', () => {
    const mockResponse = {
      data: {
        items: [
          {
            id: '1',
            repository: 'npm-proxy',
            format: 'npm',
            group: '@types',
            name: 'react',
            version: '18.0.0',
            assets: [],
          },
        ],
        continuationToken: 'token123',
      },
    };

    it('calls the API with correct format parameter', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchNpm({ q: 'react' });

      expect(mockedAxios.get).toHaveBeenCalledTimes(1);
      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('format=npm');
      expect(calledUrl).toContain('q=react');
    });

    it('includes scope parameter when provided', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchNpm({ scope: '@types', name: 'react' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('npm.scope=%40types');
      expect(calledUrl).toContain('name=react');
    });

    it('includes continuationToken when provided', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchNpm({ q: 'react', continuationToken: 'abc123' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('continuationToken=abc123');
    });

    it('returns the API response', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      const result = await searchNpm({ q: 'react' });

      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('searchNuGet', () => {
    const mockResponse = {
      data: {
        items: [
          {
            id: '1',
            repository: 'nuget-proxy',
            format: 'nuget',
            group: null,
            name: 'Newtonsoft.Json',
            version: '13.0.3',
            assets: [],
          },
        ],
      },
    };

    it('calls the API with correct format parameter', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchNuGet({ q: 'Newtonsoft' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('format=nuget');
      expect(calledUrl).toContain('q=Newtonsoft');
    });

    it('includes packageId parameter correctly', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchNuGet({ packageId: 'Newtonsoft.Json', version: '13.0.3' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('nuget.id=Newtonsoft.Json');
      expect(calledUrl).toContain('version=13.0.3');
    });
  });

  describe('searchDocker', () => {
    const mockResponse = {
      data: {
        items: [
          {
            id: '1',
            repository: 'docker-hosted',
            format: 'docker',
            group: null,
            name: 'nginx',
            version: 'latest',
            assets: [],
          },
        ],
      },
    };

    it('calls the API with correct format parameter', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchDocker({ imageName: 'nginx' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('format=docker');
      expect(calledUrl).toContain('docker.imageName=nginx');
    });

    it('includes imageTag parameter when provided', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchDocker({ imageName: 'nginx', imageTag: 'latest' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('docker.imageTag=latest');
    });
  });

  describe('searchGeneric', () => {
    const mockResponse = {
      data: {
        items: [
          {
            id: '1',
            repository: 'maven-central',
            format: 'maven2',
            group: 'org.apache',
            name: 'commons-lang3',
            version: '3.14.0',
            assets: [],
          },
        ],
      },
    };

    it('calls the API with search query', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchGeneric({ q: 'commons' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('q=commons');
    });

    it('includes format parameter when provided', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchGeneric({ q: 'spring', format: 'maven2' });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('format=maven2');
    });

    it('includes all optional parameters when provided', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchGeneric({
        q: 'spring',
        format: 'maven2',
        group: 'org.springframework',
        name: 'spring-core',
        version: '6.0.0',
        repository: 'maven-central',
      });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).toContain('format=maven2');
      expect(calledUrl).toContain('group=org.springframework');
      expect(calledUrl).toContain('name=spring-core');
      expect(calledUrl).toContain('version=6.0.0');
      expect(calledUrl).toContain('repository=maven-central');
    });

    it('excludes undefined parameters from query string', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      await searchGeneric({ q: 'test', format: undefined });

      const calledUrl = mockedAxios.get.mock.calls[0][0];
      expect(calledUrl).not.toContain('format=');
    });
  });
});



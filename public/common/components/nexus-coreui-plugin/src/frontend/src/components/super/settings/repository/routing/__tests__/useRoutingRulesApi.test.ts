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

import { renderHook, act } from '@testing-library/react';
import { useRoutingRulesApi } from '../useRoutingRulesApi';
import { RoutingRule, RoutingRuleFormData, RoutingRulesPreview } from '../types';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

const mockRoutingRules: RoutingRule[] = [
  {
    id: '1',
    name: 'block-sources',
    description: 'Block source artifacts',
    mode: 'BLOCK',
    matchers: ['.*-sources\\.jar'],
    assignedRepositoryCount: 2,
    assignedRepositoryNames: ['maven-central', 'maven-snapshots'],
  },
  {
    id: '2',
    name: 'allow-releases',
    description: 'Allow release artifacts only',
    mode: 'ALLOW',
    matchers: ['.*-SNAPSHOT.*'],
    assignedRepositoryCount: 0,
    assignedRepositoryNames: [],
  },
];

const mockPreviewData: RoutingRulesPreview = {
  children: [
    {
      repository: 'maven-group',
      type: 'group',
      format: 'maven2',
      rule: null,
      allowed: true,
      expanded: true,
      expandable: true,
      children: [
        {
          repository: 'maven-central',
          type: 'proxy',
          format: 'maven2',
          rule: 'block-sources',
          allowed: false,
          expanded: false,
          expandable: false,
          children: null,
        },
      ],
    },
  ],
  expanded: true,
  expandable: true,
};

describe('useRoutingRulesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchRoutingRules', () => {
    it('should fetch routing rules successfully', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockRoutingRules);

      const { result } = renderHook(() => useRoutingRulesApi());

      let rules: RoutingRule[] = [];
      await act(async () => {
        rules = await result.current.fetchRoutingRules(true);
      });

      expect(rules).toEqual(mockRoutingRules);
      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules?includeRepositoryNames=true'
      );
    });

    it('should handle empty response', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useRoutingRulesApi());

      let rules: RoutingRule[] = [];
      await act(async () => {
        rules = await result.current.fetchRoutingRules();
      });

      expect(rules).toEqual([]);
    });

    it('should throw error on failure', async () => {
      mockRestClient.get.mockRejectedValueOnce({
        response: { data: { message: 'Server error' } },
      });

      const { result } = renderHook(() => useRoutingRulesApi());

      await expect(result.current.fetchRoutingRules()).rejects.toThrow('Server error');
    });
  });

  describe('fetchRoutingRule', () => {
    it('should fetch a single routing rule', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockRoutingRules[0]);

      const { result } = renderHook(() => useRoutingRulesApi());

      let rule: RoutingRule | null = null;
      await act(async () => {
        rule = await result.current.fetchRoutingRule('block-sources');
      });

      expect(rule).toEqual(mockRoutingRules[0]);
      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules/block-sources'
      );
    });

    it('should return null for 404 response', async () => {
      mockRestClient.get.mockRejectedValueOnce({
        response: { status: 404 },
      });

      const { result } = renderHook(() => useRoutingRulesApi());

      let rule: RoutingRule | null = mockRoutingRules[0];
      await act(async () => {
        rule = await result.current.fetchRoutingRule('nonexistent');
      });

      expect(rule).toBeNull();
    });
  });

  describe('createRoutingRule', () => {
    it('should create a routing rule successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRoutingRulesApi());

      const formData: RoutingRuleFormData = {
        name: 'new-rule',
        description: 'New rule description',
        mode: 'BLOCK',
        matchers: ['.*\\.zip'],
      };

      await act(async () => {
        await result.current.createRoutingRule(formData);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules',
        expect.objectContaining({
          name: 'new-rule',
          description: 'New rule description',
          mode: 'BLOCK',
          matchers: ['.*\\.zip'],
        })
      );
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should filter empty matchers', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRoutingRulesApi());

      const formData: RoutingRuleFormData = {
        name: 'new-rule',
        description: '',
        mode: 'BLOCK',
        matchers: ['.*\\.zip', '', '  ', '.*\\.tar'],
      };

      await act(async () => {
        await result.current.createRoutingRule(formData);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules',
        expect.objectContaining({
          matchers: ['.*\\.zip', '.*\\.tar'],
        })
      );
    });

    it('should set error on failure', async () => {
      mockRestClient.post.mockRejectedValueOnce({
        response: { data: { message: 'Validation error' } },
      });

      const { result } = renderHook(() => useRoutingRulesApi());

      await act(async () => {
        try {
          await result.current.createRoutingRule({
            name: 'test',
            description: '',
            mode: 'BLOCK',
            matchers: ['.*'],
          });
        } catch (e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Validation error');
    });
  });

  describe('updateRoutingRule', () => {
    it('should update a routing rule successfully', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRoutingRulesApi());

      const formData: RoutingRuleFormData = {
        name: 'block-sources',
        description: 'Updated description',
        mode: 'ALLOW',
        matchers: ['.*-sources\\.jar', '.*-javadoc\\.jar'],
      };

      await act(async () => {
        await result.current.updateRoutingRule('block-sources', formData);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules/block-sources',
        expect.objectContaining({
          name: 'block-sources',
          mode: 'ALLOW',
        })
      );
    });
  });

  describe('deleteRoutingRule', () => {
    it('should delete a routing rule successfully', async () => {
      mockRestClient.delete.mockResolvedValueOnce({});

      const { result } = renderHook(() => useRoutingRulesApi());

      await act(async () => {
        await result.current.deleteRoutingRule('block-sources');
      });

      expect(mockRestClient.delete).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules/block-sources'
      );
    });

    it('should set error on deletion failure', async () => {
      mockRestClient.delete.mockRejectedValueOnce({
        response: { data: { message: 'Rule is in use' } },
      });

      const { result } = renderHook(() => useRoutingRulesApi());

      await act(async () => {
        try {
          await result.current.deleteRoutingRule('block-sources');
        } catch (e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Rule is in use');
    });
  });

  describe('testRoutingRule', () => {
    it('should test a routing rule successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce(true);

      const { result } = renderHook(() => useRoutingRulesApi());

      let isAllowed = false;
      await act(async () => {
        isAllowed = await result.current.testRoutingRule({
          mode: 'BLOCK',
          matchers: ['.*-sources\\.jar'],
          path: '/com/example/lib-1.0-sources.jar',
        });
      });

      expect(isAllowed).toBe(true);
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules/test',
        expect.objectContaining({
          mode: 'BLOCK',
          matchers: ['.*-sources\\.jar'],
          path: '/com/example/lib-1.0-sources.jar',
        })
      );
    });
  });

  describe('fetchRoutingRulesPreview', () => {
    it('should fetch routing rules preview', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockPreviewData);

      const { result } = renderHook(() => useRoutingRulesApi());

      let preview: RoutingRulesPreview | null = null;
      await act(async () => {
        preview = await result.current.fetchRoutingRulesPreview('/com/example/lib.jar');
      });

      expect(preview).toEqual(mockPreviewData);
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/internal/ui/routing-rules/preview')
      );
    });

    it('should apply filter parameter', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockPreviewData);

      const { result } = renderHook(() => useRoutingRulesApi());

      await act(async () => {
        await result.current.fetchRoutingRulesPreview('/com/example/lib.jar', 'groups');
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('filter=groups')
      );
    });
  });

  describe('setError', () => {
    it('should allow setting and clearing error', () => {
      const { result } = renderHook(() => useRoutingRulesApi());

      act(() => {
        result.current.setError('Test error');
      });

      expect(result.current.error).toBe('Test error');

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});



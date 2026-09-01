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
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { RepositorySettingsUsageTab } from '../RepositorySettingsUsageTab';
import { useRepositorySettingsUsageTab } from '../useRepositorySettingsUsageTab';

jest.mock('../useRepositorySettingsUsageTab');

jest.mock('@radix-ui/themes', () => ({
  ...jest.requireActual('@radix-ui/themes'),
  Tooltip: ({ children, content }: any) => <div title={content}>{children}</div>,
  TooltipProvider: ({ children }: any) => <div>{children}</div>,
}));

interface HookOverrides {
  metrics?: { componentCount?: number; assetCount?: number; totalSize?: number } | null;
  groupMembers?: string[];
  whereUsed?: string[];
  loading?: boolean;
  loaded?: boolean;
  error?: string | null;
  membershipError?: string | null;
  componentCountPending?: boolean;
  assetCountPending?: boolean;
  totalSizePending?: boolean;
}

function mockHook(overrides: HookOverrides = {}): jest.Mock {
  const metrics = overrides.metrics === undefined
    ? { componentCount: 0, assetCount: 0, totalSize: 0 }
    : overrides.metrics;
  const value = {
    metrics,
    groupMembers: overrides.groupMembers ?? [],
    whereUsed: overrides.whereUsed ?? [],
    loading: overrides.loading ?? false,
    loaded: overrides.loaded ?? true,
    error: overrides.error ?? null,
    membershipError: overrides.membershipError ?? null,
    componentCountPending: overrides.componentCountPending
      ?? (metrics?.componentCount === undefined || metrics?.componentCount === null),
    assetCountPending: overrides.assetCountPending
      ?? (metrics?.assetCount === undefined || metrics?.assetCount === null),
    totalSizePending: overrides.totalSizePending
      ?? (metrics?.totalSize === undefined || metrics?.totalSize === null),
    refresh: jest.fn(),
    retry: jest.fn(),
  };
  (useRepositorySettingsUsageTab as jest.Mock).mockReturnValue(value);
  return useRepositorySettingsUsageTab as jest.Mock;
}

describe('RepositorySettingsUsageTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading state', () => {
    it('shows loading state while fetching', () => {
      mockHook({ loading: true, loaded: false, metrics: null });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getAllByText(/loading usage data/i).length).toBeGreaterThan(0);
    });
  });

  describe('loaded state - metrics', () => {
    it('displays component count', () => {
      mockHook({ metrics: { componentCount: 10, assetCount: 25, totalSize: 1024000 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText('10')).toBeInTheDocument();
    });

    it('displays asset count', () => {
      mockHook({ metrics: { componentCount: 10, assetCount: 25, totalSize: 1024000 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText('25')).toBeInTheDocument();
    });

    it('displays formatted repository size', () => {
      mockHook({ metrics: { componentCount: 10, assetCount: 25, totalSize: 1024000 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText(/MB|kB|KB/)).toBeInTheDocument();
    });

    it('shows zeroes for genuine zero metrics (not Unavailable)', () => {
      mockHook({ metrics: { componentCount: 0, assetCount: 0, totalSize: 0 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText(/0\.?0*\s*(B|Bytes)/i)).toBeInTheDocument();
      expect(screen.getAllByText('0').length).toBe(2);
      expect(screen.queryByText(/^Unavailable$/)).not.toBeInTheDocument();
    });

    it('shows Unavailable for metrics that are undefined (not yet calculated)', () => {
      mockHook({ metrics: { componentCount: undefined, assetCount: undefined, totalSize: undefined } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getAllByText('Unavailable').length).toBe(3);
    });

    it('shows Unavailable for metrics that are NaN', () => {
      mockHook({ metrics: { componentCount: NaN, assetCount: NaN, totalSize: NaN } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getAllByText('Unavailable').length).toBe(3);
    });
  });

  describe('loaded state - group members', () => {
    it('displays group members for group repository', () => {
      mockHook({ groupMembers: ['repo1', 'repo2'] });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      expect(screen.getByText('repo1')).toBeInTheDocument();
      expect(screen.getByText('repo2')).toBeInTheDocument();
    });

    it('shows empty state for group with no members', () => {
      mockHook({ groupMembers: [] });

      render(
        <RepositorySettingsUsageTab
          repositoryName="empty-group"
          repositoryType="group"
        />
      );

      expect(screen.getByText(/no member repositories/i)).toBeInTheDocument();
    });

    it('shows membership error inline without blocking metrics', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        groupMembers: [],
        membershipError: 'Failed to load member repositories',
      });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText(/failed to load member repositories/i)).toBeInTheDocument();
    });
  });

  describe('loaded state - where used', () => {
    it('displays where-used for hosted repository', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        whereUsed: ['group1', 'group2'],
      });

      render(
        <RepositorySettingsUsageTab
          repositoryName="hosted-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText('group1')).toBeInTheDocument();
      expect(screen.getByText('group2')).toBeInTheDocument();
    });

    it('shows empty state when not in any groups', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        whereUsed: [],
      });

      render(
        <RepositorySettingsUsageTab
          repositoryName="standalone-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText(/not a member of any groups/i)).toBeInTheDocument();
    });

    it('shows membership error inline without blocking metrics', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        whereUsed: [],
        membershipError: 'Failed to load group membership',
      });

      render(
        <RepositorySettingsUsageTab
          repositoryName="hosted-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText(/failed to load group membership/i)).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error message with retry button', () => {
      mockHook({ metrics: null, loaded: false, error: 'Failed to load usage data' });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText(/failed to load usage data/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });

    it('calls retry when retry button clicked', () => {
      const retry = jest.fn();
      (useRepositorySettingsUsageTab as jest.Mock).mockReturnValue({
        metrics: null,
        groupMembers: [],
        whereUsed: [],
        loading: false,
        loaded: false,
        error: 'Failed to load usage data',
        membershipError: null,
        componentCountPending: true,
        assetCountPending: true,
        totalSizePending: true,
        refresh: jest.fn(),
        retry,
      });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      fireEvent.click(screen.getByRole('button', { name: /retry/i }));
      expect(retry).toHaveBeenCalledTimes(1);
    });
  });
});

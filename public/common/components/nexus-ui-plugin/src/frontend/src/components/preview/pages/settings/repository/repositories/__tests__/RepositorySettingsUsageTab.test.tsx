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

const mockExtState: Record<string, unknown> = { 'datastore.isPostgresql': true };

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: () => ({
      getValue: (key: string) => mockExtState[key],
    }),
  },
}));

interface GroupMember {
  name: string;
  type: 'hosted' | 'proxy';
}

interface HookOverrides {
  metrics?: { componentCount?: number; assetCount?: number; totalSize?: number } | null;
  groupMembers?: GroupMember[];
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
    mockExtState['datastore.isPostgresql'] = true;
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
      mockHook({ groupMembers: [{ name: 'repo1', type: 'hosted' }, { name: 'repo2', type: 'proxy' }] });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      expect(screen.getByText('repo1')).toBeInTheDocument();
      expect(screen.getByText('repo2')).toBeInTheDocument();
    });

    it('renders Database icon for hosted members and Cloud icon for proxy members', () => {
      mockHook({ groupMembers: [{ name: 'hosted-repo', type: 'hosted' }, { name: 'proxy-repo', type: 'proxy' }] });

      const { container } = render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      // Check for lucide-database (hosted) and lucide-cloud (proxy) icons
      const databaseIcon = container.querySelector('.lucide-database');
      const cloudIcon = container.querySelector('.lucide-cloud');

      expect(databaseIcon).toBeTruthy();
      expect(cloudIcon).toBeTruthy();
    });

    it('makes the entire member repository row clickable (icon and text are within the same link)', () => {
      mockHook({ groupMembers: [{ name: 'hosted-repo', type: 'hosted' }] });

      const { container } = render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      // The link element with accessible name 'hosted-repo'
      const link = screen.getByRole('link', { name: 'hosted-repo' });

      // The icon should be a DOM descendant of the link (whole row is clickable)
      const databaseIcon = container.querySelector('.lucide-database');
      expect(databaseIcon).toBeTruthy();
      expect(link.contains(databaseIcon)).toBe(true);
    });

    it('links each member repository name to its own repository edit page', () => {
      mockHook({ groupMembers: [{ name: 'repo1', type: 'hosted' }, { name: 'repo2', type: 'proxy' }] });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-group"
          repositoryType="group"
        />
      );

      expect(screen.getByRole('link', { name: 'repo1' })).toHaveAttribute(
        'href',
        '#preview/admin/repository/repositories/repo1'
      );
      expect(screen.getByRole('link', { name: 'repo2' })).toHaveAttribute(
        'href',
        '#preview/admin/repository/repositories/repo2'
      );
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

    it('renders FolderTree icon for group membership items', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        whereUsed: ['group1', 'group2'],
      });

      const { container } = render(
        <RepositorySettingsUsageTab
          repositoryName="hosted-repo"
          repositoryType="hosted"
        />
      );

      // Check for lucide-folder-tree icon (groups)
      const folderTreeIcons = container.querySelectorAll('.lucide-folder-tree');
      expect(folderTreeIcons.length).toBeGreaterThan(0);

      // Should NOT have database, cloud, or globe icons in group membership section
      const databaseIcon = container.querySelector('.lucide-database');
      const cloudIcon = container.querySelector('.lucide-cloud');
      const globeIcon = container.querySelector('.lucide-globe');
      expect(databaseIcon).toBeFalsy();
      expect(cloudIcon).toBeFalsy();
      expect(globeIcon).toBeFalsy();
    });

    it('makes the entire group membership row clickable (icon and text are within the same link)', () => {
      mockHook({
        metrics: { componentCount: 5, assetCount: 10, totalSize: 512000 },
        whereUsed: ['group1'],
      });

      const { container } = render(
        <RepositorySettingsUsageTab
          repositoryName="hosted-repo"
          repositoryType="hosted"
        />
      );

      // The link element with accessible name 'group1'
      const link = screen.getByRole('link', { name: 'group1' });

      // The icon should be a DOM descendant of the link (whole row is clickable)
      const folderTreeIcon = container.querySelector('.lucide-folder-tree');
      expect(folderTreeIcon).toBeTruthy();
      expect(link.contains(folderTreeIcon)).toBe(true);
    });

    it('links each group name to its own repository edit page', () => {
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

      expect(screen.getByRole('link', { name: 'group1' })).toHaveAttribute(
        'href',
        '#preview/admin/repository/repositories/group1'
      );
      expect(screen.getByRole('link', { name: 'group2' })).toHaveAttribute(
        'href',
        '#preview/admin/repository/repositories/group2'
      );
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

  describe('H2 vs Postgres stat card gating', () => {
    it('renders the Repository Size and Assets cards on Postgres', () => {
      mockExtState['datastore.isPostgresql'] = true;
      mockHook({ metrics: { componentCount: 10, assetCount: 25, totalSize: 1024000 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.getByText('Repository Size')).toBeInTheDocument();
      expect(screen.getByText('Assets')).toBeInTheDocument();
      expect(screen.getByText('Components')).toBeInTheDocument();
    });

    it('hides the Repository Size and Assets cards on H2, but keeps Components', () => {
      mockExtState['datastore.isPostgresql'] = false;
      mockHook({ metrics: { componentCount: 10, assetCount: 25, totalSize: 1024000 } });

      render(
        <RepositorySettingsUsageTab
          repositoryName="test-repo"
          repositoryType="hosted"
        />
      );

      expect(screen.queryByText('Repository Size')).not.toBeInTheDocument();
      expect(screen.queryByText('Assets')).not.toBeInTheDocument();
      expect(screen.getByText('Components')).toBeInTheDocument();
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

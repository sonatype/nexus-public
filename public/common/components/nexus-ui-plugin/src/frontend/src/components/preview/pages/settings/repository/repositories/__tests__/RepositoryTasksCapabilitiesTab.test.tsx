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
import { Theme } from '@radix-ui/themes';
import { RepositoryTasksCapabilitiesTab } from '../RepositoryTasksCapabilitiesTab';
import ExtJS from '../../../../../../../interface/ExtJS';

jest.mock('../../../../../../../interface/ExtJS');

jest.mock('../useRepositoryTasksCapabilities', () => ({
  useRepositoryTasksCapabilities: jest.fn(),
}));

const mockUseRepositoryTasksCapabilities =
  require('../useRepositoryTasksCapabilities').useRepositoryTasksCapabilities;

const mockExtJS = ExtJS as unknown as { checkPermission: jest.Mock };

function renderTab(repositoryName = 'my-repo') {
  return render(
    <Theme>
      <RepositoryTasksCapabilitiesTab repositoryName={repositoryName} />
    </Theme>
  );
}

const mockTask = {
  id: 't1',
  name: 'Cleanup my-repo',
  type: 'repository.cleanup',
  schedule: '0 0 * * *',
  lastRun: new Date().toISOString(),
  lastRunResult: 'ok',
};

const mockCapability = {
  id: 'c1',
  type: 'firewall.audit',
  enabled: true,
  notes: 'PCCS audit mode',
};

describe('RepositoryTasksCapabilitiesTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockExtJS.checkPermission = jest.fn(() => true);
  });

  describe('loading state', () => {
    it('shouldRenderSpinnerWhenLoading', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [],
        loading: true,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByTestId('tasks-capabilities-loading')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shouldRenderErrorCardWhenErrorOccurs', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [],
        loading: false,
        error: 'Fetch failed',
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('Failed to load tasks and capabilities.')).toBeInTheDocument();
      expect(screen.getByText('Fetch failed')).toBeInTheDocument();
    });

    it('shouldInvokeRefetchWhenRetryClicked', () => {
      const refetch = jest.fn();
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [],
        loading: false,
        error: 'Fetch failed',
        refetch,
      });

      renderTab();

      fireEvent.click(screen.getByRole('button', { name: /Retry/ }));
      expect(refetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('successful data rendering', () => {
    it('shouldRenderTasksTableWhenTasksAreReturned', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('Scheduled Tasks')).toBeInTheDocument();
      expect(screen.getByText('Cleanup my-repo')).toBeInTheDocument();
      expect(screen.getByText('repository.cleanup')).toBeInTheDocument();
    });

    it('shouldRenderCapabilitiesTableWhenCapabilitiesAreReturned', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [mockCapability],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('Capabilities')).toBeInTheDocument();
      expect(screen.getByText('firewall.audit')).toBeInTheDocument();
      expect(screen.getByText('PCCS audit mode')).toBeInTheDocument();
    });

    it('shouldRenderEmptyStateForZeroTasks', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [mockCapability],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('No scheduled tasks target this repository.')).toBeInTheDocument();
    });

    it('shouldRenderEmptyStateForZeroCapabilities', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('No capabilities are scoped to this repository.')).toBeInTheDocument();
    });

    it('shouldSingularizeTasksCountLabel', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('1 task')).toBeInTheDocument();
    });

    it('shouldPluralizeTasksCountLabel', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask, { ...mockTask, id: 't2' }],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('2 tasks')).toBeInTheDocument();
    });
  });

  describe('permission gating (per-section)', () => {
    it('shouldHideTasksSectionWhenUserLacksTasksReadPermission', () => {
      mockExtJS.checkPermission = jest.fn((perm: string) => perm !== 'nexus:tasks:read');
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [mockCapability],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.queryByText('Scheduled Tasks')).not.toBeInTheDocument();
      expect(screen.getByText('Capabilities')).toBeInTheDocument();
    });

    it('shouldHideCapabilitiesSectionWhenUserLacksCapabilitiesReadPermission', () => {
      mockExtJS.checkPermission = jest.fn((perm: string) => perm !== 'nexus:capabilities:read');
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [mockCapability],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('Scheduled Tasks')).toBeInTheDocument();
      expect(screen.queryByText('Capabilities')).not.toBeInTheDocument();
    });

    it('shouldRenderNoSectionsWhenUserLacksBothPermissions', () => {
      mockExtJS.checkPermission = jest.fn(() => false);
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [mockTask],
        capabilities: [mockCapability],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.queryByText('Scheduled Tasks')).not.toBeInTheDocument();
      expect(screen.queryByText('Capabilities')).not.toBeInTheDocument();
    });
  });

  describe('hook integration', () => {
    it('shouldPassRepositoryNameAndPermissionFlagsToHook', () => {
      mockExtJS.checkPermission = jest.fn((perm: string) => perm === 'nexus:tasks:read');
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab('some-other-repo');

      expect(mockUseRepositoryTasksCapabilities).toHaveBeenCalledWith('some-other-repo', {
        canReadTasks: true,
        canReadCapabilities: false,
      });
    });
  });

  describe('lastRun formatting', () => {
    it('shouldRenderJustNowWhenLastRunIsInTheFuture', () => {
      const futureIso = new Date(Date.now() + 60_000).toISOString();
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [{ ...mockTask, lastRun: futureIso }],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('just now')).toBeInTheDocument();
    });

    it('shouldRenderJustNowWhenLastRunIsUnderOneMinuteAgo', () => {
      const nearNowIso = new Date(Date.now() - 5_000).toISOString();
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [{ ...mockTask, lastRun: nearNowIso }],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('just now')).toBeInTheDocument();
    });
  });

  describe('lastRunResult badge', () => {
    it('shouldRenderRawValueForUnrecognizedLastRunResult', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [{ ...mockTask, lastRunResult: 'canceled' }],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      expect(screen.getByText('canceled')).toBeInTheDocument();
    });

    it('shouldRenderDashForMissingLastRunResult', () => {
      mockUseRepositoryTasksCapabilities.mockReturnValue({
        tasks: [{ ...mockTask, lastRunResult: undefined }],
        capabilities: [],
        loading: false,
        error: null,
        refetch: jest.fn(),
      });

      renderTab();

      // Look for the em-dash inside the Status column badge. The Notes column
      // (capabilities table) is not rendered when capabilities is empty.
      expect(screen.getByText('—')).toBeInTheDocument();
    });
  });
});

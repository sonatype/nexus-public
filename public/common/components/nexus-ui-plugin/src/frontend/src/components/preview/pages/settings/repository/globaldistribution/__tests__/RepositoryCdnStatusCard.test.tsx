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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

// Create mock functions at module scope
const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
const mockToastWarning = jest.fn();

jest.mock('../../../../../shared', () => ({
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
    warning: mockToastWarning,
  }),
}));

const mockStateGetValue = jest.fn();

function givenState(distribution: 'selfhosted' | 'cloud', editionId = 'nexus-enterprise-edition') {
  const values: Record<string, unknown> = { distribution, editionId };
  mockStateGetValue.mockImplementation((key: string, defaultValue?: unknown) =>
    key in values ? values[key] : defaultValue,
  );
}

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn(() => ({
      getValue: mockStateGetValue,
    })),
    useState: jest.fn((getValue: () => unknown) => getValue()),
  },
}));

jest.mock('../useRepositoryCdnList');
jest.mock('../useBlobStoreCdnStatuses');

import { ExtJS } from '../../../../../../../interface/ExtJS';
import { useRepositoryCdnList } from '../useRepositoryCdnList';
import { useBlobStoreCdnStatuses } from '../useBlobStoreCdnStatuses';
import { RepositoryCdnStatusCard } from '../RepositoryCdnStatusCard';

const mockedUseList = useRepositoryCdnList as jest.MockedFunction<typeof useRepositoryCdnList>;
const mockedUseStatuses = useBlobStoreCdnStatuses as jest.MockedFunction<
  typeof useBlobStoreCdnStatuses
>;
const mockedCheckPermission = ExtJS.checkPermission as jest.Mock;

const rowMavenReleases = {
  id: 'maven-releases',
  name: 'maven-releases',
  type: 'hosted' as const,
  format: 'maven2',
  blobStoreName: 'default',
  globalDistribution: false,
};

const rowMavenCentral = {
  id: 'maven-central',
  name: 'maven-central',
  type: 'proxy' as const,
  format: 'maven2',
  blobStoreName: 'default',
  globalDistribution: true,
};

const rowNoCdnBlobStore = {
  id: 'oci-artifacts',
  name: 'oci-artifacts',
  type: 'hosted' as const,
  format: 'oci',
  blobStoreName: 's3-blob-no-cdn',
  globalDistribution: false,
};

const rowUnsupportedBlobStore = {
  id: 'file-repo',
  name: 'file-repo',
  type: 'hosted' as const,
  format: 'raw',
  blobStoreName: 'file-blob-store',
  globalDistribution: false,
};

const rowGroupRepo = {
  id: 'maven-group',
  name: 'maven-group',
  type: 'group' as const,
  format: 'maven2',
  blobStoreName: '',
  globalDistribution: false,
};

function renderCard() {
  return render(
    <Theme>
      <RepositoryCdnStatusCard />
    </Theme>,
  );
}

function useListStub(overrides: Partial<ReturnType<typeof useRepositoryCdnList>> = {}) {
  const applyCdnState = jest.fn().mockResolvedValue({ updated: [], failed: [] });
  const base = {
    rows: [rowMavenReleases, rowMavenCentral],
    isLoading: false,
    error: null,
    isUpdating: false,
    applyCdnState,
    refetch: jest.fn().mockResolvedValue(undefined),
  };
  mockedUseList.mockReturnValue({ ...base, ...overrides });
  return { ...base, ...overrides };
}

function useStatusesStub(overrides: Partial<ReturnType<typeof useBlobStoreCdnStatuses>> = {})
{
  const base = {
    blobStores: [
      { name: 'default', cdnEnabled: true, type: 's3' },
      { name: 's3-blob-no-cdn', cdnEnabled: false, type: 's3' },
    ],
    isLoading: false,
    error: null,
    canReadBlobStores: true,
  };
  mockedUseStatuses.mockReturnValue({ ...base, ...overrides });
  return { ...base, ...overrides };
}

describe('RepositoryCdnStatusCard', () => {
  beforeEach(() => {
    mockedUseList.mockReset();
    mockedUseStatuses.mockReset();
    mockedCheckPermission.mockReturnValue(true);
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
    mockToastWarning.mockReset();
    givenState('selfhosted');
    useStatusesStub();
  });

  it('renders heading, subtitle, filter, and toolbar', () => {
    useListStub();
    renderCard();

    expect(screen.getByRole('heading', { name: 'Repository Distribution Status' })).toBeInTheDocument();
    expect(screen.getByText('Manage Global Distribution for repositories')).toBeInTheDocument();
    expect(screen.getByLabelText(/Filter repositories by name/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Disable Global Distribution for selected repositories' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' })).toHaveTextContent('Enable');
    expect(screen.getByRole('button', { name: 'Disable Global Distribution for selected repositories' })).toHaveTextContent('Disable');
    expect(screen.getByRole('columnheader', { name: 'Distribution Status' })).toBeInTheDocument();
  });

  it('renders one row per repository', () => {
    useListStub();
    renderCard();

    expect(screen.getByRole('link', { name: 'maven-releases' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'maven-central' })).toBeInTheDocument();
  });

  it('shows the empty state when there are no repositories', () => {
    useListStub({ rows: [] });
    renderCard();

    expect(screen.getByText(/There are no repositories available/i)).toBeInTheDocument();
  });

  it('shows the no-match state when the filter excludes every row', async () => {
    useListStub();
    renderCard();

    await userEvent.type(screen.getByLabelText(/Filter repositories by name/i), 'zzz');
    expect(screen.getByText(/No repositories match the filter/i)).toBeInTheDocument();
  });

  it('narrows rows by name via the filter input', async () => {
    useListStub();
    renderCard();

    await userEvent.type(screen.getByLabelText(/Filter repositories by name/i), 'central');
    expect(screen.queryByRole('link', { name: 'maven-releases' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'maven-central' })).toBeInTheDocument();
  });

  it('disables bulk buttons without a selection', () => {
    useListStub();
    renderCard();

    expect(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Disable Global Distribution for selected repositories' })).toBeDisabled();
  });

  it('hides bulk buttons and row selection when the user lacks nexus:settings:update', () => {
    mockedCheckPermission.mockReturnValue(false);
    useListStub();
    renderCard();

    expect(screen.getByText('maven-releases')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Enable Global Distribution for selected repositories' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Disable Global Distribution for selected repositories' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Select all repositories')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Select maven-releases')).not.toBeInTheDocument();
  });

  it('shows Configured / Not configured when the user cannot read blob stores', () => {
    useStatusesStub({ blobStores: [], canReadBlobStores: false });
    useListStub({
      rows: [
        { ...rowMavenReleases, globalDistribution: true },
        { ...rowMavenCentral, globalDistribution: false },
      ],
    });
    renderCard();

    expect(screen.getByText('Configured')).toBeInTheDocument();
    expect(screen.getByText('Not configured')).toBeInTheDocument();
    expect(screen.queryByText('Not enabled for blob store')).not.toBeInTheDocument();
  });

  it('fires applyCdnState(true) and shows a success toast on Enable', async () => {
    const applyCdnState = jest
      .fn()
      .mockResolvedValue({ updated: ['maven-releases'], failed: [] });
    useListStub({ applyCdnState });
    renderCard();

    await userEvent.click(screen.getByLabelText('Select maven-releases'));
    await userEvent.click(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' }));

    expect(applyCdnState).toHaveBeenCalledWith(['maven-releases'], true);
    expect(mockToastSuccess).toHaveBeenCalledWith('Global Distribution enabled', '1 repository updated.');
  });

  it('fires applyCdnState(false) on Disable', async () => {
    const applyCdnState = jest
      .fn()
      .mockResolvedValue({ updated: ['maven-central'], failed: [] });
    useListStub({ applyCdnState });
    renderCard();

    await userEvent.click(screen.getByLabelText('Select maven-central'));
    await userEvent.click(screen.getByRole('button', { name: 'Disable Global Distribution for selected repositories' }));

    expect(applyCdnState).toHaveBeenCalledWith(['maven-central'], false);
    expect(mockToastSuccess).toHaveBeenCalledWith('Global Distribution disabled', '1 repository updated.');
  });

  it('skips blob-store-off rows on Enable and reports them in the toast', async () => {
    const applyCdnState = jest
      .fn()
      .mockResolvedValue({ updated: ['maven-releases'], failed: [] });
    useStatusesStub();
    useListStub({
      rows: [rowMavenReleases, rowNoCdnBlobStore],
      applyCdnState,
    });
    renderCard();

    // Select-all header checkbox
    await userEvent.click(screen.getByLabelText('Select all repositories'));
    await userEvent.click(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' }));

    // Only the eligible row is passed to the hook
    expect(applyCdnState).toHaveBeenCalledWith(['maven-releases'], true);
    expect(mockToastSuccess).toHaveBeenCalledWith(
      'Global Distribution enabled',
      '1 repository updated. 1 skipped because Global Distribution is not enabled for their blob store.',
    );
  });

  it('does NOT skip blob-store-off rows on Disable', async () => {
    const applyCdnState = jest
      .fn()
      .mockResolvedValue({ updated: ['maven-releases', 'oci-artifacts'], failed: [] });
    useStatusesStub();
    useListStub({
      rows: [rowMavenReleases, rowNoCdnBlobStore],
      applyCdnState,
    });
    renderCard();

    await userEvent.click(screen.getByLabelText('Select all repositories'));
    await userEvent.click(screen.getByRole('button', { name: 'Disable Global Distribution for selected repositories' }));

    expect(applyCdnState).toHaveBeenCalledWith(
      ['maven-releases', 'oci-artifacts'],
      false,
    );
  });

  it('surfaces a warning toast on partial failure and keeps failed rows selected', async () => {
    const applyCdnState = jest.fn().mockResolvedValue({
      updated: ['maven-releases'],
      failed: [{ id: 'maven-central', message: '403 Forbidden' }],
    });
    useListStub({ applyCdnState });
    renderCard();

    await userEvent.click(screen.getByLabelText('Select all repositories'));
    await userEvent.click(screen.getByRole('button', { name: 'Enable Global Distribution for selected repositories' }));

    expect(mockToastWarning).toHaveBeenCalledWith('1 repository enabled, 1 failed', '403 Forbidden');
    // Wait for the async state update to complete, then check selection state
    await waitFor(() => {
      // Failed row remains checked
      expect(screen.getByLabelText('Select maven-central')).toBeChecked();
      // Succeeded row is deselected
      expect(screen.getByLabelText('Select maven-releases')).not.toBeChecked();
    });
  });

  it('renders the blob-store-off badge with a Configure link using the blob store type', () => {
    useStatusesStub();
    useListStub({ rows: [rowNoCdnBlobStore] });
    renderCard();

    const row = screen.getByRole('link', { name: 'oci-artifacts' }).closest('tr');
    expect(row).not.toBeNull();
    expect(within(row as HTMLElement).getByText('Not enabled for blob store')).toBeInTheDocument();
    const configureLink = within(row as HTMLElement).getByRole('link', { name: /Configure/i });
    expect(configureLink).toHaveAttribute(
      'href',
      '#preview/admin/repository/blobstores/s3/s3-blob-no-cdn',
    );
  });

  it('uses blob store typeId in the Configure link for different blob store types', () => {
    useStatusesStub({
      blobStores: [
        { name: 'azure-blob-no-cdn', cdnEnabled: false, type: 'azure' },
      ],
    });
    useListStub({
      rows: [
        {
          id: 'azure-repo',
          name: 'azure-repo',
          type: 'hosted' as const,
          format: 'maven2',
          blobStoreName: 'azure-blob-no-cdn',
          globalDistribution: false,
        },
      ],
    });
    renderCard();

    const row = screen.getByRole('link', { name: 'azure-repo' }).closest('tr');
    expect(row).not.toBeNull();
    const configureLink = within(row as HTMLElement).getByRole('link', { name: /Configure/i });
    expect(configureLink).toHaveAttribute(
      'href',
      '#preview/admin/repository/blobstores/azure/azure-blob-no-cdn',
    );
  });

  describe.each([
    ['Pro', 'nexus-professional-edition'],
    ['Pro+', 'nexus-professional-plus-edition'],
    ['Enterprise', 'nexus-enterprise-edition'],
  ])('on a cloud deployment with a %s license', (_label, editionId) => {
    beforeEach(() => {
      givenState('cloud', editionId);
    });

    it('shows the real blob store status without a Configure link', () => {
      useListStub({ rows: [rowMavenCentral, rowNoCdnBlobStore] });
      renderCard();

      const row = screen.getByRole('link', { name: 'oci-artifacts' }).closest('tr') as HTMLElement;
      expect(within(row).getByText('Not enabled for blob store')).toBeInTheDocument();
      expect(within(row).queryByRole('link', { name: /Configure/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /Configure/i })).not.toBeInTheDocument();
    });

    it('excludes repositories whose blob stores do not support global distribution', () => {
      useListStub({ rows: [rowMavenReleases, rowUnsupportedBlobStore] });
      renderCard();

      expect(screen.getByRole('link', { name: 'maven-releases' })).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'file-repo' })).not.toBeInTheDocument();
    });
  });

  it('excludes repositories whose blob stores do not support global distribution', () => {
    useStatusesStub();
    useListStub({ rows: [rowMavenReleases, rowUnsupportedBlobStore] });
    renderCard();

    // Only the repository with a supported blob store should appear
    expect(screen.getByRole('link', { name: 'maven-releases' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'file-repo' })).not.toBeInTheDocument();
  });

  it('includes group repositories even though they have no blob store', () => {
    useStatusesStub();
    useListStub({ rows: [rowMavenReleases, rowGroupRepo] });
    renderCard();

    expect(screen.getByRole('link', { name: 'maven-releases' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'maven-group' })).toBeInTheDocument();
  });

  it('sorts repositories by name regardless of input order', () => {
    useStatusesStub();
    useListStub({ rows: [rowMavenReleases, rowGroupRepo, rowMavenCentral] });
    renderCard();

    const names = screen
      .getAllByRole('link')
      .map((link) => link.textContent);
    expect(names).toEqual(['maven-central', 'maven-group', 'maven-releases']);
  });

  it('sorts repository names case-insensitively', () => {
    useStatusesStub();
    useListStub({
      rows: [
        { ...rowMavenReleases, id: 'zeta', name: 'Zeta' },
        { ...rowMavenCentral, id: 'alpha', name: 'alpha' },
        { ...rowGroupRepo, id: 'beta', name: 'Beta' },
      ],
    });
    renderCard();

    const names = screen
      .getAllByRole('link')
      .map((link) => link.textContent);
    expect(names).toEqual(['alpha', 'Beta', 'Zeta']);
  });
});

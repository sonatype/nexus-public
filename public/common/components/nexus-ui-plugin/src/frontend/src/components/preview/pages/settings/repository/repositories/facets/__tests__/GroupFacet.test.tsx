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
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import axios from 'axios';

import { GroupFacet } from '../GroupFacet';
import { RepositoryFormData, RepositoryReference } from '../../types';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

const mockGetValue = jest.fn().mockReturnValue(false);

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: () => ({
      getValue: mockGetValue,
    }),
  },
}));

const memberOptions: RepositoryReference[] = [
  { name: 'nuget-v2-proxy', type: 'proxy', format: 'nuget' },
  { name: 'nuget-v3-proxy', type: 'proxy', format: 'nuget' },
  { name: 'nuget-hosted', type: 'hosted', format: 'nuget' },
];

const defaultFormData: RepositoryFormData = {
  name: 'test-group',
  format: 'nuget',
  type: 'group',
  group: {
    memberNames: ['nuget-v2-proxy', 'nuget-v3-proxy'],
  },
};

function renderFacet(props: Partial<React.ComponentProps<typeof GroupFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    errors: {},
    memberOptions,
    format: 'nuget',
  };
  return render(
    <Theme>
      <GroupFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('GroupFacet', () => {
  beforeEach(() => {
    mockedAxios.get.mockResolvedValue({ data: [] });
    mockGetValue.mockReturnValue(false);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders Member Repositories label', () => {
    renderFacet();
    expect(screen.getByText('Member Repositories')).toBeInTheDocument();
  });

  it('renders current members in the list', () => {
    renderFacet();
    expect(screen.getByText('nuget-v2-proxy')).toBeInTheDocument();
    expect(screen.getByText('nuget-v3-proxy')).toBeInTheDocument();
  });

  it('shows empty message when no members selected', () => {
    renderFacet({
      formData: { ...defaultFormData, group: { memberNames: [] } },
    });
    expect(screen.getByText('No member repositories selected')).toBeInTheDocument();
  });

  it('does not fetch repositories for non-nuget formats', () => {
    renderFacet({ format: 'maven2' });
    expect(mockedAxios.get).not.toHaveBeenCalled();
  });

  it('fetches repositories from beta endpoint for nuget format', async () => {
    mockedAxios.get.mockResolvedValue({ data: [] });
    renderFacet({ format: 'nuget' });
    await waitFor(() => {
      expect(mockedAxios.get).toHaveBeenCalledWith('/service/rest/beta/repositories');
    });
  });

  it('shows blocking warning when chocolatey is disabled and group has mixed nuget versions', async () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nugetChocolateyEnabled' ? false : false
    );
    mockedAxios.get.mockResolvedValue({
      data: [
        { format: 'nuget', type: 'proxy', name: 'nuget-v2-proxy', nugetProxy: { nugetVersion: 'V2' } },
        { format: 'nuget', type: 'proxy', name: 'nuget-v3-proxy', nugetProxy: { nugetVersion: 'V3' } },
      ],
    });

    renderFacet();

    await waitFor(() => {
      expect(
        screen.getByText(/Group repositories cannot include a mix of NuGet v2 and v3 members/i)
      ).toBeInTheDocument();
    });
  });

  it('does not show warning when all members use the same nuget version', async () => {
    mockedAxios.get.mockResolvedValue({
      data: [
        { format: 'nuget', type: 'proxy', name: 'nuget-v2-proxy', nugetProxy: { nugetVersion: 'V2' } },
        { format: 'nuget', type: 'proxy', name: 'nuget-v3-proxy', nugetProxy: { nugetVersion: 'V2' } },
      ],
    });

    renderFacet();

    await waitFor(() => {
      expect(mockedAxios.get).toHaveBeenCalled();
    });
    expect(
      screen.queryByText(/Group repositories cannot include a mix of NuGet v2 and v3 members/i)
    ).not.toBeInTheDocument();
  });

  it('hides mixed-version warning when chocolatey is enabled', async () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nugetChocolateyEnabled' ? true : false
    );
    mockedAxios.get.mockResolvedValue({
      data: [
        { format: 'nuget', type: 'proxy', name: 'nuget-v2-proxy', nugetProxy: { nugetVersion: 'V2' } },
        { format: 'nuget', type: 'proxy', name: 'nuget-v3-proxy', nugetProxy: { nugetVersion: 'V3' } },
      ],
    });

    renderFacet();

    await waitFor(() => {
      expect(mockedAxios.get).toHaveBeenCalled();
    });
    expect(
      screen.queryByText(/Group repositories cannot include a mix of NuGet v2 and v3 members/i)
    ).not.toBeInTheDocument();
  });
});

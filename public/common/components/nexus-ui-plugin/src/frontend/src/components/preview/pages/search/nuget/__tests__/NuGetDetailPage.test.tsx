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
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { NuGetDetailPage } from '../NuGetDetailPage';
import { mockNuGetDetail } from '../mockData';

jest.mock('../mockData', () => {
  const original = jest.requireActual('../mockData');
  return {
    ...original,
    mockNuGetDetailApi: jest.fn().mockResolvedValue(original.mockNuGetDetail),
  };
});

import { mockNuGetDetailApi } from '../mockData';
const mockApi = mockNuGetDetailApi as jest.Mock;

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('NuGetDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockApi.mockResolvedValue(mockNuGetDetail);
  });

  it('renders loading state initially', () => {
    mockApi.mockReturnValue(new Promise(() => {}));
    wrap(<NuGetDetailPage packageId="Newtonsoft.Json" />);

    expect(screen.getByText(/loading package details/i)).toBeInTheDocument();
  });

  it('renders package details after loading', async () => {
    wrap(<NuGetDetailPage packageId="Newtonsoft.Json" />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /newtonsoft\.json/i })).toBeInTheDocument();
    });
  });

  it('renders description when present', async () => {
    wrap(<NuGetDetailPage packageId="Newtonsoft.Json" />);

    await waitFor(() => {
      expect(screen.getByText(/Json\.NET is a popular/)).toBeInTheDocument();
    });
  });

  it('renders error state when API throws', async () => {
    mockApi.mockRejectedValueOnce(new Error('Package not found'));
    wrap(<NuGetDetailPage packageId="Unknown.Package" />);

    await waitFor(() => {
      expect(screen.getByText(/Package not found/)).toBeInTheDocument();
    });
  });

  it('calls onBack when back button clicked', async () => {
    const onBack = jest.fn();
    wrap(<NuGetDetailPage packageId="Newtonsoft.Json" onBack={onBack} />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /newtonsoft\.json/i })).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /back to search/i });
    await userEvent.click(backButton);

    expect(onBack).toHaveBeenCalled();
  });

  it('calls onBack when back button clicked in error state', async () => {
    const onBack = jest.fn();
    mockApi.mockRejectedValueOnce(new Error('Not found'));
    wrap(<NuGetDetailPage packageId="Unknown" onBack={onBack} />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /back to search/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /back to search/i }));
    expect(onBack).toHaveBeenCalled();
  });
});

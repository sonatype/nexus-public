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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';
import { RepositoryGroupUsageTab } from '../RepositoryGroupUsageTab';
import { useRepositoryTree } from '../useRepositoryTree';

// Mock the hook
jest.mock('../useRepositoryTree');

const renderWithTheme = (component: React.ReactNode) =>
  render(<Theme>{component}</Theme>);

describe('RepositoryGroupUsageTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders usage list when repository belongs to groups', () => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      usages: ['group1', 'group2'],
      loading: false,
      error: undefined,
      refresh: jest.fn(),
    });

    renderWithTheme(<RepositoryGroupUsageTab repositoryName="hosted1" />);

    expect(screen.getByText('group1')).toBeInTheDocument();
    expect(screen.getByText('group2')).toBeInTheDocument();
  });

  it('renders empty message when repository belongs to no groups', () => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      usages: [],
      loading: false,
      error: undefined,
      refresh: jest.fn(),
    });

    renderWithTheme(<RepositoryGroupUsageTab repositoryName="hosted1" />);

    expect(screen.getByText(/not a member of any groups/i)).toBeInTheDocument();
  });

  it('shows loading state', () => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      usages: [],
      loading: true,
      error: undefined,
      refresh: jest.fn(),
    });

    renderWithTheme(<RepositoryGroupUsageTab repositoryName="hosted1" />);
    expect(screen.getAllByText(/checking group membership/i)[0]).toBeInTheDocument();
  });
});

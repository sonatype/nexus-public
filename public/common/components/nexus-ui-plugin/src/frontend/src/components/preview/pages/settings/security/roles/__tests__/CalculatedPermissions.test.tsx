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
import { Theme } from '@radix-ui/themes';
import { CalculatedPermissions } from '../CalculatedPermissions';
import { Privilege } from '../../privileges/types';

const mockPrivileges: Privilege[] = [
  {
    id: 'priv1',
    version: '1',
    name: 'Browse Maven',
    description: 'Allows browsing maven repositories',
    type: 'repository-view',
    readOnly: false,
    properties: { format: 'maven2', repository: '*', actions: 'browse' },
    permission: 'nexus:repository-view:maven2:*:browse',
  },
  {
    id: 'priv2',
    version: '1',
    name: 'Read npm',
    description: 'Allows reading npm repositories',
    type: 'repository-view',
    readOnly: false,
    properties: { format: 'npm', repository: '*', actions: 'read' },
    permission: 'nexus:repository-view:npm:*:read',
  }
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('CalculatedPermissions', () => {
  it('should render the header', () => {
    renderWithTheme(
      <CalculatedPermissions privileges={mockPrivileges} loading={false} />
    );
    expect(screen.getByText('Calculated Permissions')).toBeInTheDocument();
  });

  it('should render the flattened privileges list', () => {
    renderWithTheme(
      <CalculatedPermissions privileges={mockPrivileges} loading={false} />
    );
    expect(screen.getByText('Browse Maven')).toBeInTheDocument();
    expect(screen.getByText('nexus:repository-view:maven2:*:browse')).toBeInTheDocument();
    expect(screen.getByText('Read npm')).toBeInTheDocument();
  });

  it('should filter privileges by search query', () => {
    renderWithTheme(
      <CalculatedPermissions privileges={mockPrivileges} loading={false} />
    );
    
    const searchInput = screen.getByPlaceholderText('Search permissions...');
    fireEvent.change(searchInput, { target: { value: 'maven' } });

    expect(screen.getByText('Browse Maven')).toBeInTheDocument();
    // EntityTable might not immediately remove non-matching rows in Jest without wait
    expect(screen.queryByText('Read npm')).not.toBeInTheDocument();
  });

  it('should show empty state when no matches found', () => {
    renderWithTheme(
      <CalculatedPermissions privileges={mockPrivileges} loading={false} />
    );
    
    const searchInput = screen.getByPlaceholderText('Search permissions...');
    fireEvent.change(searchInput, { target: { value: 'non-existent' } });

    expect(screen.getByText('No permissions found')).toBeInTheDocument();
  });
});

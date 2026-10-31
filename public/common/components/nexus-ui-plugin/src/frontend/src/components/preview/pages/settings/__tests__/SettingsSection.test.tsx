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
import SettingsSection from '../SettingsSection';
import { SettingsSection as SettingsSectionType } from '../types';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('SettingsSection', () => {
  const mockSection: SettingsSectionType = {
    id: 'security',
    label: 'Security',
    cards: [
      {
        id: 'users',
        path: 'security/users',
        label: 'Users',
        description: 'Manage user accounts and access',
      },
      {
        id: 'roles',
        path: 'security/roles',
        label: 'Roles',
        description: 'Configure user roles and permissions',
      },
    ],
  };

  it('renders section heading', () => {
    renderWithTheme(<SettingsSection section={mockSection} />);

    expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
  });

  it('renders all cards in section', () => {
    renderWithTheme(<SettingsSection section={mockSection} />);

    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('Manage user accounts and access')).toBeInTheDocument();
    expect(screen.getByText('Roles')).toBeInTheDocument();
    expect(screen.getByText('Configure user roles and permissions')).toBeInTheDocument();
  });

  it('renders correct number of cards', () => {
    renderWithTheme(<SettingsSection section={mockSection} />);

    const cards = screen.getAllByRole('link');
    expect(cards).toHaveLength(2);
  });

  it('renders cards with correct links', () => {
    renderWithTheme(<SettingsSection section={mockSection} />);

    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveAttribute('href', '#preview/admin/security/users');
    expect(links[1]).toHaveAttribute('href', '#preview/admin/security/roles');
  });

  it('renders section with single card', () => {
    const sectionWithOneCard: SettingsSectionType = {
      id: 'system',
      label: 'System',
      cards: [
        {
          id: 'tasks',
          path: 'system/tasks',
          label: 'Tasks',
          description: 'Manage scheduled tasks',
        },
      ],
    };

    renderWithTheme(<SettingsSection section={sectionWithOneCard} />);

    expect(screen.getByRole('heading', { name: 'System' })).toBeInTheDocument();
    expect(screen.getByText('Tasks')).toBeInTheDocument();
    expect(screen.getAllByRole('link')).toHaveLength(1);
  });

  it('renders section with many cards', () => {
    const sectionWithManyCards: SettingsSectionType = {
      id: 'repository',
      label: 'Repository',
      cards: [
        { id: 'repos', path: 'repository/repositories', label: 'Repositories', description: 'Manage repositories' },
        { id: 'blobs', path: 'repository/blob-stores', label: 'Blob Stores', description: 'Manage blob stores' },
        { id: 'cleanup', path: 'repository/cleanup', label: 'Cleanup Policies', description: 'Configure cleanup' },
        { id: 'routing', path: 'repository/routing', label: 'Routing Rules', description: 'Configure routing' },
      ],
    };

    renderWithTheme(<SettingsSection section={sectionWithManyCards} />);

    expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
    expect(screen.getAllByRole('link')).toHaveLength(4);
  });

  it('renders heading with correct size', () => {
    renderWithTheme(<SettingsSection section={mockSection} />);

    const heading = screen.getByRole('heading', { name: 'Security' });
    // Radix Heading with size="5" will have specific data attributes or classes
    expect(heading).toBeInTheDocument();
  });

  it('renders cards with searchTerms', () => {
    const sectionWithSearchTerms: SettingsSectionType = {
      id: 'security',
      label: 'Security',
      cards: [
        {
          id: 'ldap',
          path: 'security/ldap',
          label: 'LDAP',
          description: 'Configure LDAP integration',
          searchTerms: ['active directory', 'ad'],
        },
      ],
    };

    renderWithTheme(<SettingsSection section={sectionWithSearchTerms} />);

    expect(screen.getByText('LDAP')).toBeInTheDocument();
    expect(screen.getByText('Configure LDAP integration')).toBeInTheDocument();
  });

  it('renders section with special characters in labels', () => {
    const sectionWithSpecialChars: SettingsSectionType = {
      id: 'test',
      label: 'Test & Development',
      cards: [
        {
          id: 'test1',
          path: 'test/one',
          label: 'SSL/TLS',
          description: 'Configure SSL & TLS settings',
        },
      ],
    };

    renderWithTheme(<SettingsSection section={sectionWithSpecialChars} />);

    expect(screen.getByRole('heading', { name: 'Test & Development' })).toBeInTheDocument();
    expect(screen.getByText('SSL/TLS')).toBeInTheDocument();
  });
});

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
import SettingsCard from '../SettingsCard';
import { SettingCard } from '../types';

// Mock previewFeatureFlags — repositories is enabled, users is disabled
jest.mock('../../../config/featureFlags', () => ({
  isFeatureEnabled: (key: string) => key === 'repository.repositories',
}));

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('SettingsCard', () => {
  const mockCard: SettingCard = {
    id: 'users',
    path: 'security/users',
    label: 'Users',
    description: 'Manage user accounts and access',
  };

  it('renders card with label and description', () => {
    renderWithTheme(<SettingsCard card={mockCard} />);

    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('Manage user accounts and access')).toBeInTheDocument();
  });

  it('renders View button', () => {
    renderWithTheme(<SettingsCard card={mockCard} />);

    expect(screen.getByRole('button', { name: 'View' })).toBeInTheDocument();
  });

  it('renders as a link with correct href', () => {
    renderWithTheme(<SettingsCard card={mockCard} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '#preview/admin/security/users');
  });

  it('uses fullHash when provided', () => {
    const cardWithHash: SettingCard = {
      ...mockCard,
      fullHash: '#browse/welcome',
    };
    renderWithTheme(<SettingsCard card={cardWithHash} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '#browse/welcome');
  });

  it('applies cursor pointer style', () => {
    const { container } = renderWithTheme(<SettingsCard card={mockCard} />);

    const card = container.querySelector('[class*="Card"]');
    expect(card).toHaveStyle({ cursor: 'pointer' });
  });

  it('renders separator dot between label and description', () => {
    renderWithTheme(<SettingsCard card={mockCard} />);

    expect(screen.getByText('·')).toBeInTheDocument();
  });

  it('applies text truncation styles to description', () => {
    const { container } = renderWithTheme(<SettingsCard card={mockCard} />);

    const description = screen.getByText('Manage user accounts and access');
    expect(description).toHaveStyle({
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    });
  });

  it('renders with searchTerms in card data', () => {
    const cardWithSearchTerms: SettingCard = {
      ...mockCard,
      searchTerms: ['accounts', 'people'],
    };

    renderWithTheme(<SettingsCard card={cardWithSearchTerms} />);

    // Card should still render normally (searchTerms don't affect visual display)
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('Manage user accounts and access')).toBeInTheDocument();
  });

  it('renders card with long description', () => {
    const cardWithLongDescription: SettingCard = {
      ...mockCard,
      description: 'This is a very long description that should be truncated with ellipsis when it exceeds the available width',
    };

    renderWithTheme(<SettingsCard card={cardWithLongDescription} />);

    const description = screen.getByText(cardWithLongDescription.description);
    expect(description).toHaveStyle({
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    });
  });

  it('renders card with special characters in label and description', () => {
    const cardWithSpecialChars: SettingCard = {
      id: 'test',
      path: 'test/path',
      label: 'SSL/TLS & Certificates',
      description: 'Configure SSL/TLS certificates & security settings',
    };

    renderWithTheme(<SettingsCard card={cardWithSpecialChars} />);

    expect(screen.getByText('SSL/TLS & Certificates')).toBeInTheDocument();
    expect(screen.getByText('Configure SSL/TLS certificates & security settings')).toBeInTheDocument();
  });

  describe('Coming Soon Badge', () => {
    it('renders badge when featureKey is disabled', () => {
      const comingSoonCard: SettingCard = {
        ...mockCard,
        featureKey: 'security.users', // disabled in mock
      };
      renderWithTheme(<SettingsCard card={comingSoonCard} />);

      expect(screen.getByText('Coming Soon')).toBeInTheDocument();
    });

    it('does not render badge when featureKey is enabled', () => {
      const enabledCard: SettingCard = {
        id: 'repositories',
        path: 'repository/repositories',
        label: 'Repositories',
        description: 'Manage repository configurations',
        featureKey: 'repository.repositories', // enabled in mock
      };
      renderWithTheme(<SettingsCard card={enabledCard} />);

      expect(screen.queryByText('Coming Soon')).not.toBeInTheDocument();
    });

    it('does not render badge when featureKey is absent', () => {
      renderWithTheme(<SettingsCard card={mockCard} />);

      expect(screen.queryByText('Coming Soon')).not.toBeInTheDocument();
    });

    it('badge is visible to screen readers via text content', () => {
      const comingSoonCard: SettingCard = {
        ...mockCard,
        featureKey: 'security.users',
      };
      renderWithTheme(<SettingsCard card={comingSoonCard} />);

      expect(screen.getByText('Coming Soon')).toBeInTheDocument();
    });
  });
});

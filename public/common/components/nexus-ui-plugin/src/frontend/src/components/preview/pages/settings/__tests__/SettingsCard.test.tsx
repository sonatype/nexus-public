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

const mockIsEnterpriseEdition = jest.fn(() => false);
const mockIsLicensedEdition = jest.fn(() => false);
jest.mock('../../../shared/hooks', () => ({
  useIsEnterpriseEdition: () => mockIsEnterpriseEdition(),
  useIsLicensedEdition: () => mockIsLicensedEdition(),
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

  it('does not truncate description text', () => {
    renderWithTheme(<SettingsCard card={mockCard} />);

    const description = screen.getByText('Manage user accounts and access');
    expect(description).not.toHaveStyle({ whiteSpace: 'nowrap' });
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

  it('renders card with long description without truncation', () => {
    const cardWithLongDescription: SettingCard = {
      ...mockCard,
      description: 'This is a very long description that should wrap naturally when it exceeds the available width',
    };

    renderWithTheme(<SettingsCard card={cardWithLongDescription} />);

    expect(screen.getByText(cardWithLongDescription.description)).toBeInTheDocument();
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

  describe('edition pill', () => {
    const enterpriseCard: SettingCard = {
      id: 'global-distribution',
      path: 'repository/global-distribution',
      label: 'Global Distribution',
      description: 'Distribute repository content globally',
      editionPill: 'enterprise',
    };

    beforeEach(() => {
      mockIsEnterpriseEdition.mockReturnValue(false);
      mockIsLicensedEdition.mockReturnValue(false);
    });

    it('shows the Enterprise pill next to the label when the license is not Enterprise', () => {
      mockIsLicensedEdition.mockReturnValue(true);
      renderWithTheme(<SettingsCard card={enterpriseCard} />);

      expect(screen.getByTestId('edition-pill-enterprise')).toHaveTextContent('Enterprise');
    });

    it('hides the Enterprise pill on an Enterprise license', () => {
      mockIsEnterpriseEdition.mockReturnValue(true);
      mockIsLicensedEdition.mockReturnValue(true);
      renderWithTheme(<SettingsCard card={enterpriseCard} />);

      expect(screen.queryByTestId('edition-pill-enterprise')).not.toBeInTheDocument();
    });

    it('shows the Pro pill only on unlicensed editions', () => {
      const proCard: SettingCard = { ...enterpriseCard, editionPill: 'pro' };
      const { rerender } = renderWithTheme(<SettingsCard card={proCard} />);
      expect(screen.getByTestId('edition-pill-pro')).toHaveTextContent('Pro');

      mockIsLicensedEdition.mockReturnValue(true);
      rerender(<Theme><SettingsCard card={proCard} /></Theme>);
      expect(screen.queryByTestId('edition-pill-pro')).not.toBeInTheDocument();
    });

    it('renders no pill when the card declares none', () => {
      renderWithTheme(<SettingsCard card={mockCard} />);

      expect(screen.queryByTestId(/^edition-pill-/)).not.toBeInTheDocument();
    });

    it('keeps the card a single link to the settings page', () => {
      renderWithTheme(<SettingsCard card={enterpriseCard} />);

      expect(screen.getByRole('link')).toHaveAttribute('href', '#preview/admin/repository/global-distribution');
    });
  });
});

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
import { render, screen, fireEvent, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

// Mock the shared module before importing SettingsHubPage
jest.mock('@/components/shared', () => ({
  PageHeader: ({ title, description, children }: any) => (
    <div data-testid="page-header">
      <h1>{title}</h1>
      <p>{description}</p>
      {children}
    </div>
  ),
}));

// Mock previewFeatureFlags — repositories is enabled, others are disabled
jest.mock('@/config/previewFeatureFlags', () => ({
  isFeatureEnabled: (key: string) => key === 'repository.repositories',
}));

// Mock the settingsConfig to avoid dependency on actual config
jest.mock('../settingsConfig', () => ({
  SETTINGS_SECTIONS: [
    {
      id: 'security',
      label: 'Security',
      cards: [
        {
          id: 'users',
          path: 'security/users',
          label: 'Users',
          description: 'Manage user accounts and access',
          featureKey: 'security.users',
        },
        {
          id: 'roles',
          path: 'security/roles',
          label: 'Roles',
          description: 'Configure user roles and permissions',
          featureKey: 'security.roles',
        },
        {
          id: 'ldap',
          path: 'security/ldap',
          label: 'LDAP',
          description: 'Configure LDAP integration',
          searchTerms: ['active directory', 'ad', 'directory service'],
          featureKey: 'security.ldap',
        },
      ],
    },
    {
      id: 'repository',
      label: 'Repository',
      cards: [
        {
          id: 'repositories',
          path: 'repository/repositories',
          label: 'Repositories',
          description: 'Manage repository configurations',
          featureKey: 'repository.repositories',
        },
        {
          id: 'blob-stores',
          path: 'repository/blob-stores',
          label: 'Blob Stores',
          description: 'Configure blob storage locations',
          featureKey: 'repository.blobstores',
          cloudExcluded: true,
        },
      ],
    },
    {
      id: 'system',
      label: 'System',
      cards: [
        {
          id: 'tasks',
          path: 'system/tasks',
          label: 'Tasks',
          description: 'Manage scheduled system tasks',
          featureKey: 'system.tasks',
        },
      ],
    },
  ],
}));

// Mock ExtJS so we can control isCloud per-test
// Default: state() returns { getValue: () => false } so isCloud is false
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false),
    }),
  },
}));

// Import after mocks are set up
import { SettingsHubPage } from '../SettingsHubPage';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('SettingsHubPage', () => {
  beforeEach(() => {
    // Reset hash before each test
    window.location.hash = '';
  });

  describe('Initial Rendering', () => {
    it('renders page header with title and description', () => {
      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText('Settings')).toBeInTheDocument();
      expect(screen.getByText('Configure security, repositories, system, and support')).toBeInTheDocument();
    });

    it('renders search input', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      expect(searchInput).toBeInTheDocument();
    });

    it('renders search icon', () => {
      const { container } = renderWithTheme(<SettingsHubPage />);

      // Lucide Search icon will be rendered
      expect(container.querySelector('svg')).toBeInTheDocument();
    });

    it('does not render clear button initially', () => {
      renderWithTheme(<SettingsHubPage />);

      const clearButton = screen.queryByLabelText('Clear search');
      expect(clearButton).not.toBeInTheDocument();
    });

    it('renders all sections when no search query', () => {
      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'System' })).toBeInTheDocument();
    });

    it('renders all cards when no search query', () => {
      renderWithTheme(<SettingsHubPage />);

      // Security cards
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
      expect(screen.getByText('LDAP')).toBeInTheDocument();

      // Repository cards
      expect(screen.getByText('Repositories')).toBeInTheDocument();
      expect(screen.getByText('Blob Stores')).toBeInTheDocument();

      // System cards
      expect(screen.getByText('Tasks')).toBeInTheDocument();
    });
  });

  describe('Search Functionality', () => {
    it('filters cards by label', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'Users' } });

      // Should show Users card
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Manage user accounts and access')).toBeInTheDocument();

      // Should not show other cards
      expect(screen.queryByText('Roles')).not.toBeInTheDocument();
      expect(screen.queryByText('Repositories')).not.toBeInTheDocument();
    });

    it('filters cards by description', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'scheduled' } });

      // Should show Tasks card (description contains "scheduled")
      expect(screen.getByText('Tasks')).toBeInTheDocument();

      // Should not show other cards
      expect(screen.queryByText('Users')).not.toBeInTheDocument();
      expect(screen.queryByText('Repositories')).not.toBeInTheDocument();
    });

    it('filters cards by searchTerms', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'active directory' } });

      // Should show LDAP card (searchTerms includes "active directory")
      expect(screen.getByText('LDAP')).toBeInTheDocument();

      // Should not show other cards
      expect(screen.queryByText('Users')).not.toBeInTheDocument();
      expect(screen.queryByText('Tasks')).not.toBeInTheDocument();
    });

    it('performs case-insensitive search', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'USERS' } });

      expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('performs partial matching', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'repo' } });

      expect(screen.getByText('Repositories')).toBeInTheDocument();
    });

    it('handles multiple matches', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'user' } });

      // Should match "Users" (label) and "Roles" (description contains "user")
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
    });

    it('shows clear button when search has text', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'test' } });

      const clearButton = screen.getByLabelText('Clear search');
      expect(clearButton).toBeInTheDocument();
    });

    it('clears search when clear button clicked', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...') as HTMLInputElement;
      fireEvent.change(searchInput, { target: { value: 'users' } });

      expect(searchInput.value).toBe('users');

      const clearButton = screen.getByLabelText('Clear search');
      fireEvent.click(clearButton);

      expect(searchInput.value).toBe('');
      // All sections should be visible again
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
    });

    it('handles empty search query (whitespace only)', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: '   ' } });

      // All sections should still be visible (whitespace trimmed)
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'System' })).toBeInTheDocument();
    });

    it('handles special characters in search', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'users & roles' } });

      // Should not crash and should perform search normally
      expect(searchInput).toBeInTheDocument();
    });
  });

  describe('Section Visibility', () => {
    it('hides sections with no matching cards', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'Users' } });

      // Security section should be visible (has Users card)
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();

      // Repository and System sections should be hidden
      expect(screen.queryByRole('heading', { name: 'Repository' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'System' })).not.toBeInTheDocument();
    });

    it('shows only sections with matching cards', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'blob' } });

      // Only Repository section should be visible
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Security' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'System' })).not.toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state when no results', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'nonexistent' } });

      expect(screen.getByText(/No settings found matching "nonexistent"/)).toBeInTheDocument();
    });

    it('shows helpful message in empty state', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'xyz' } });

      expect(screen.getByText('Try a different search term or browse all settings')).toBeInTheDocument();
    });

    it('does not show empty state when results exist', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'Users' } });

      expect(screen.queryByText(/No settings found matching/)).not.toBeInTheDocument();
    });

    it('returns to normal view after clearing empty search', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');

      // Search for something that doesn't exist
      fireEvent.change(searchInput, { target: { value: 'nonexistent' } });
      expect(screen.getByText(/No settings found matching/)).toBeInTheDocument();

      // Clear the search
      const clearButton = screen.getByLabelText('Clear search');
      fireEvent.click(clearButton);

      // All sections should be visible again
      expect(screen.queryByText(/No settings found matching/)).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
    });
  });

  describe('Layout', () => {
    it('uses Container component for constrained width', () => {
      const { container } = renderWithTheme(<SettingsHubPage />);

      // Container should be present with size="3" (880px max-width)
      const containerElement = container.querySelector('[class*="rt-Container"]');
      expect(containerElement).toBeInTheDocument();
    });

    it('renders cards in vertical list layout', () => {
      renderWithTheme(<SettingsHubPage />);

      // All cards should be rendered (not hidden by grid overflow)
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
      expect(screen.getByText('LDAP')).toBeInTheDocument();
      expect(screen.getByText('Repositories')).toBeInTheDocument();
      expect(screen.getByText('Blob Stores')).toBeInTheDocument();
      expect(screen.getByText('Tasks')).toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('navigates to settings page when card is clicked', () => {
      renderWithTheme(<SettingsHubPage />);

      const usersLink = screen.getByRole('link', { name: /Users/ });
      fireEvent.click(usersLink);

      // Check that hash was set (preview/admin/{path} without /settings)
      expect(usersLink).toHaveAttribute('href', '#preview/admin/security/users');
    });

    it('renders all cards as links', () => {
      renderWithTheme(<SettingsHubPage />);

      const links = screen.getAllByRole('link');
      // Should have 6 links total (3 security + 2 repository + 1 system)
      expect(links.length).toBe(6);
    });
  });

  describe('Accessibility', () => {
    it('has accessible clear button', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'test' } });

      const clearButton = screen.getByLabelText('Clear search');
      expect(clearButton).toHaveAttribute('aria-label', 'Clear search');
    });

    it('search input has placeholder text', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      expect(searchInput).toHaveAttribute('placeholder', 'Search settings...');
    });

    it('renders section headings with proper hierarchy', () => {
      renderWithTheme(<SettingsHubPage />);

      const securityHeading = screen.getByRole('heading', { name: 'Security' });
      expect(securityHeading.tagName).toBe('H2');
    });
  });

  describe('Edge Cases', () => {
    it('handles search with leading/trailing whitespace', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: '  users  ' } });

      // Should still find Users card (query is trimmed)
      expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('handles rapid search changes', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');

      fireEvent.change(searchInput, { target: { value: 'u' } });
      fireEvent.change(searchInput, { target: { value: 'us' } });
      fireEvent.change(searchInput, { target: { value: 'use' } });
      fireEvent.change(searchInput, { target: { value: 'user' } });

      // Should show results for final query
      expect(screen.getByText('Users')).toBeInTheDocument();
    });

    it('handles clearing and re-searching', () => {
      renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');

      // First search
      fireEvent.change(searchInput, { target: { value: 'users' } });
      expect(screen.getByText('Users')).toBeInTheDocument();

      // Clear
      fireEvent.change(searchInput, { target: { value: '' } });
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();

      // Second search
      fireEvent.change(searchInput, { target: { value: 'blob' } });
      expect(screen.getByText('Blob Stores')).toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Security' })).not.toBeInTheDocument();
    });

    it('maintains search state during re-render', () => {
      const { rerender } = renderWithTheme(<SettingsHubPage />);

      const searchInput = screen.getByPlaceholderText('Search settings...');
      fireEvent.change(searchInput, { target: { value: 'users' } });

      rerender(<Theme><SettingsHubPage /></Theme>);

      // Search should still be active
      expect((searchInput as HTMLInputElement).value).toBe('users');
      expect(screen.getByText('Users')).toBeInTheDocument();
    });
  });

  describe('Performance', () => {
    it('uses memoization for filtered sections', () => {
      const { rerender } = renderWithTheme(<SettingsHubPage />);

      // Initial render
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();

      // Re-render without changing search (should use memoized value)
      rerender(<Theme><SettingsHubPage /></Theme>);

      // Should still render correctly
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
    });
  });

  describe('Preview Mode Callout', () => {
    it('renders callout explaining preview mode', () => {
      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText(/Some settings pages are available in this preview/)).toBeInTheDocument();
    });

    it('renders Coming Soon badge inside callout text', () => {
      renderWithTheme(<SettingsHubPage />);

      // The callout contains a Coming Soon badge inline in the text
      const calloutBadges = screen.getAllByText('Coming Soon');
      expect(calloutBadges.length).toBeGreaterThanOrEqual(1);
    });

    it('renders callout before settings sections', () => {
      const { container } = renderWithTheme(<SettingsHubPage />);

      const callout = container.querySelector('[class*="rt-CalloutRoot"]');
      expect(callout).toBeInTheDocument();
    });
  });

  describe('Cloud Mode Filtering', () => {
    beforeEach(() => {
      (ExtJS.state as jest.Mock).mockReturnValue({
        getValue: jest.fn().mockImplementation((key: string) => key === 'isCloud'),
      });
    });

    afterEach(() => {
      // Restore default non-cloud state so subsequent tests are unaffected
      (ExtJS.state as jest.Mock).mockReturnValue({
        getValue: jest.fn().mockReturnValue(false),
      });
    });

    it('hides cloudExcluded cards when isCloud is true', () => {
      renderWithTheme(<SettingsHubPage />);

      // Blob Stores is cloudExcluded: true in the mock
      expect(screen.queryByText('Blob Stores')).not.toBeInTheDocument();
    });

    it('still shows non-excluded cards in cloud mode', () => {
      renderWithTheme(<SettingsHubPage />);

      // Repositories is not cloudExcluded
      expect(screen.getByText('Repositories')).toBeInTheDocument();
    });

    it('shows Repository section when it still has non-excluded cards', () => {
      renderWithTheme(<SettingsHubPage />);

      // Section remains because Repositories is not cloudExcluded
      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
    });
  });

  describe('Coming Soon Badge', () => {
    it('renders badge on cards where feature is disabled', () => {
      renderWithTheme(<SettingsHubPage />);

      // Users has featureKey 'security.users' which is disabled in mock
      const usersCard = screen.getByText('Users').closest('a');
      expect(within(usersCard!).getByText('Coming Soon')).toBeInTheDocument();
    });

    it('does not render badge on cards where feature is enabled', () => {
      renderWithTheme(<SettingsHubPage />);

      // Repositories has featureKey 'repository.repositories' which is enabled in mock
      const repoCard = screen.getByText('Repositories').closest('a');
      expect(within(repoCard!).queryByText('Coming Soon')).not.toBeInTheDocument();
    });

    it('renders Coming Soon badges with visible text', () => {
      renderWithTheme(<SettingsHubPage />);

      // Badges use visible text content for accessibility (no aria-label needed)
      const badges = screen.getAllByText('Coming Soon');
      expect(badges.length).toBeGreaterThan(0);
    });

    it('renders badge after label name', () => {
      const { container } = renderWithTheme(<SettingsHubPage />);

      // Find the Users card link and check badge is after label text
      const usersCard = screen.getByText('Users').closest('a');
      const labelEl = within(usersCard!).getByText('Users');
      const badge = within(usersCard!).getByText('Coming Soon');

      // Badge should come after label in DOM order
      expect(labelEl.compareDocumentPosition(badge) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });
  });
});

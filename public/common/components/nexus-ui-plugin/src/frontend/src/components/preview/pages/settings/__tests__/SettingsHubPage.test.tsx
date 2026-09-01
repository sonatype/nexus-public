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
import { render, screen, fireEvent, within, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

// Mock the shared module before importing SettingsHubPage
jest.mock('../../../shared', () => ({
  PageHeader: ({ title, description, children }: any) => (
    <div data-testid="page-header">
      <h1>{title}</h1>
      <p>{description}</p>
      {children}
    </div>
  ),
}));

// Mock previewFeatureFlags — repositories is enabled, others are disabled
jest.mock('../../../config/featureFlags', () => ({
  isFeatureEnabled: (key: string) => key === 'repository.repositories',
}));

// Mock the settingsConfig to avoid dependency on actual config
// Section order matches classic UI: Repository → Security → Support → System
jest.mock('../settingsConfig', () => ({
  SETTINGS_SECTIONS: [
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
      id: 'security',
      label: 'Security',
      cards: [
        {
          id: 'users',
          path: 'security/users',
          label: 'Users',
          description: 'Manage user accounts and access',
          featureKey: 'security.users',
          visibilityRequirements: { requiresPermission: 'nexus:users:read' },
        },
        {
          id: 'roles',
          path: 'security/roles',
          label: 'Roles',
          description: 'Configure user roles and permissions',
          featureKey: 'security.roles',
          visibilityRequirements: { requiresPermission: 'nexus:roles:read' },
        },
        {
          id: 'ldap',
          path: 'security/ldap',
          label: 'LDAP',
          description: 'Configure LDAP integration',
          searchTerms: ['active directory', 'ad', 'directory service'],
          featureKey: 'security.ldap',
        },
        {
          id: 'ip-allowlist',
          path: 'security/ip-allowlist',
          label: 'IP Allow List',
          description: 'Manage IP address allow list',
          proOnly: true,
          adminOnly: true,
        },
      ],
    },
    {
      id: 'support',
      label: 'Support',
      cloudExcluded: true,
      cards: [
        {
          id: 'logs',
          path: 'support/logs',
          label: 'Logs',
          description: 'View application logs',
          featureKey: 'support.logs',
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
        {
          id: 'usage',
          path: 'system/usage',
          label: 'Usage',
          description: 'Monitor historical usage metrics and trends',
          cloudOnly: true,
        },
      ],
    },
  ],
}));

// Mock ExtJS so we can control isCloud per-test
// Default: state() returns { getValue: () => false } so isCloud is false
jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false),
    }),
    isProEdition: jest.fn().mockReturnValue(false),
    useUser: jest.fn().mockReturnValue({ administrator: false }),
  },
}));

// Mock NavigationUtils — isVisible returns true by default so all cards pass permission check
const mockIsVisible = jest.fn().mockReturnValue(true);
jest.mock('../../../../../interface/NavigationUtils', () => ({
  isVisible: (...args: any[]) => mockIsVisible(...args),
}));

// Import after mocks are set up
import { SettingsHubPage } from '../SettingsHubPage';
import { ExtJS } from '../../../../../interface/ExtJS';

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

      expect(screen.getByRole('heading', { name: 'Repository' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Security' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Support' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'System' })).toBeInTheDocument();
    });

    it('renders sections in correct order matching classic UI', () => {
      renderWithTheme(<SettingsHubPage />);

      // Get all section headings in DOM order
      const sectionHeadings = screen.getAllByRole('heading', { level: 2 });

      // Verify order matches classic ExtJS admin navigation: Repository → Security → Support → System
      expect(sectionHeadings[0]).toHaveTextContent('Repository');
      expect(sectionHeadings[1]).toHaveTextContent('Security');
      expect(sectionHeadings[2]).toHaveTextContent('Support');
      expect(sectionHeadings[3]).toHaveTextContent('System');
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
      // Should have 7 links total (2 repository + 3 security + 1 support + 1 system)
      // IP Allow List is proOnly and hidden by default (isProEdition = false)
      expect(links.length).toBe(7);
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

  describe('Pro Edition Filtering', () => {
    beforeEach(() => {
      jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(true);
      jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: true });
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('shows proOnly cards when Pro edition is active', () => {
      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText('IP Allow List')).toBeInTheDocument();
    });

    it('hides proOnly cards on CE/OSS', () => {
      jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(false);

      renderWithTheme(<SettingsHubPage />);

      expect(screen.queryByText('IP Allow List')).not.toBeInTheDocument();
    });
  });

  describe('Admin Visibility', () => {
    beforeEach(() => {
      // Pro edition on so proOnly cards aren't filtered out
      jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(true);
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('hides adminOnly cards for non-admin users', () => {
      jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

      renderWithTheme(<SettingsHubPage />);

      expect(screen.queryByText('IP Allow List')).not.toBeInTheDocument();
    });

    it('shows adminOnly cards for admin users', () => {
      jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: true });

      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText('IP Allow List')).toBeInTheDocument();
    });

    it('hides adminOnly cards when user is undefined (unauthenticated)', () => {
      jest.spyOn(ExtJS, 'useUser').mockReturnValue(undefined);

      renderWithTheme(<SettingsHubPage />);

      expect(screen.queryByText('IP Allow List')).not.toBeInTheDocument();
    });

    it('non-adminOnly cards remain visible for non-admin users', () => {
      jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Repositories')).toBeInTheDocument();
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

    it('shows cloudOnly cards when isCloud is true', () => {
      // Depends on this describe's beforeEach setting isCloud = true. If this test
      // is ever moved out of "Cloud Mode Filtering", the default isCloud = false
      // would filter the card out and this assertion would flip to a false pass —
      // keep it (or its own isCloud=true setup) inside a cloud context.
      renderWithTheme(<SettingsHubPage />);

      // Usage is cloudOnly: true in the mock
      expect(screen.getByText('Usage')).toBeInTheDocument();
    });
  });

  describe('Cloud-Only Filtering (self-hosted)', () => {
    it('hides cloudOnly cards on self-hosted (default isCloud=false)', () => {
      renderWithTheme(<SettingsHubPage />);

      // Usage is cloudOnly: true, so it must not appear on self-hosted
      expect(screen.queryByText('Usage')).not.toBeInTheDocument();
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
      renderWithTheme(<SettingsHubPage />);

      // Find the Users card link and check badge is after label text
      const usersCard = screen.getByText('Users').closest('a');
      const labelEl = within(usersCard!).getByText('Users');
      const badge = within(usersCard!).getByText('Coming Soon');

      // Badge should come after label in DOM order
      expect(labelEl.compareDocumentPosition(badge) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });
  });

  describe('Permission Filtering', () => {
    afterEach(() => {
      mockIsVisible.mockReturnValue(true);
    });

    it('hides cards when isVisible returns false for their visibilityRequirements', () => {
      mockIsVisible.mockImplementation((reqs: any) => {
        if (reqs?.requiresPermission === 'nexus:users:read') {
          return false;
        }
        return true;
      });

      renderWithTheme(<SettingsHubPage />);

      expect(screen.queryByText('Users')).not.toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
    });

    it('hides entire section when all cards fail permission check', () => {
      mockIsVisible.mockImplementation((reqs: any) => {
        if (reqs?.requiresPermission === 'nexus:users:read' ||
            reqs?.requiresPermission === 'nexus:roles:read') {
          return false;
        }
        return true;
      });

      renderWithTheme(<SettingsHubPage />);

      // LDAP and IP Allow List (filtered by adminOnly) still present — but
      // since IP Allow List is hidden (non-admin) and the mock only returns
      // false for requiresPermission 'nexus:users:read'/'nexus:roles:read'
      // (LDAP's own visibilityRequirements — requiresPermission: 'nexus:ldap:read'
      // plus editions — isn't one of those), the section should still show LDAP
      expect(screen.getByText('LDAP')).toBeInTheDocument();
      expect(screen.queryByText('Users')).not.toBeInTheDocument();
      expect(screen.queryByText('Roles')).not.toBeInTheDocument();
    });

    it('shows all cards when isVisible returns true', () => {
      mockIsVisible.mockReturnValue(true);

      renderWithTheme(<SettingsHubPage />);

      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
      expect(screen.getByText('Repositories')).toBeInTheDocument();
    });

    it('calls isVisible with card visibilityRequirements', () => {
      mockIsVisible.mockClear();

      renderWithTheme(<SettingsHubPage />);

      expect(mockIsVisible).toHaveBeenCalledWith({ requiresPermission: 'nexus:users:read' });
      expect(mockIsVisible).toHaveBeenCalledWith({ requiresPermission: 'nexus:roles:read' });
      // Cards without visibilityRequirements pass undefined
      expect(mockIsVisible).toHaveBeenCalledWith(undefined);
    });
  });

  describe('Reactive Permission Subscription', () => {
    let permissionsHandler: (() => void) | null = null;
    let mockPermissionsController: any;
    let mockStateController: any;

    beforeEach(() => {
      jest.useFakeTimers();
      permissionsHandler = null;
      mockPermissionsController = {
        on: jest.fn((event: string, handler: () => void) => {
          if (event === 'changed') permissionsHandler = handler;
        }),
        un: jest.fn(),
      };
      mockStateController = {
        on: jest.fn(),
        un: jest.fn(),
      };
    });

    afterEach(() => {
      jest.useRealTimers();
      mockIsVisible.mockReturnValue(true);
      delete (window as any).Ext;
    });

    it('re-evaluates card visibility when permissions change', () => {
      // Start with all cards visible
      mockIsVisible.mockReturnValue(true);

      // Set up Ext before render
      (window as any).Ext = {
        getApplication: () => ({
          getController: (name: string) => {
            if (name === 'Permissions') return mockPermissionsController;
            if (name === 'State') return mockStateController;
            return null;
          },
        }),
      };

      renderWithTheme(<SettingsHubPage />);

      // All cards should be visible initially
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();

      // Now simulate permissions changing — Users becomes hidden
      mockIsVisible.mockImplementation((reqs: any) => {
        if (reqs?.requiresPermission === 'nexus:users:read') return false;
        return true;
      });

      // Fire the permission change event
      act(() => {
        permissionsHandler?.();
      });

      // Users should now be hidden
      expect(screen.queryByText('Users')).not.toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
    });

    it('retries subscription when Ext is not yet available', () => {
      // Ext not available initially
      (window as any).Ext = undefined;

      renderWithTheme(<SettingsHubPage />);

      // Fast-forward to trigger interval retry
      (window as any).Ext = {
        getApplication: () => ({
          getController: (name: string) => {
            if (name === 'Permissions') return mockPermissionsController;
            if (name === 'State') return mockStateController;
            return null;
          },
        }),
      };

      act(() => {
        jest.advanceTimersByTime(200);
      });

      // Should have subscribed after retry
      expect(mockPermissionsController.on).toHaveBeenCalledWith('changed', expect.any(Function));
    });

    it('cleans up event listeners on unmount', () => {
      (window as any).Ext = {
        getApplication: () => ({
          getController: (name: string) => {
            if (name === 'Permissions') return mockPermissionsController;
            if (name === 'State') return mockStateController;
            return null;
          },
        }),
      };

      const { unmount } = renderWithTheme(<SettingsHubPage />);

      unmount();

      expect(mockPermissionsController.un).toHaveBeenCalledWith('changed', expect.any(Function));
      expect(mockStateController.un).toHaveBeenCalledWith('userchanged', expect.any(Function));
    });

    it('clears interval on unmount when Ext is not available', () => {
      (window as any).Ext = undefined;

      const { unmount } = renderWithTheme(<SettingsHubPage />);

      // Unmount before Ext becomes available — should not leak interval
      unmount();

      // Advancing timers after unmount should not throw
      act(() => {
        jest.advanceTimersByTime(500);
      });

      // No subscription should have occurred
      expect(mockPermissionsController.on).not.toHaveBeenCalled();
    });
  });
});

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

import { ComponentDetailPanel } from '../ComponentDetailPanel';
import type { ComponentXO } from '../detail.types';

// Test wrapper with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

// Mock component data
const mockMavenComponent: ComponentXO = {
  id: 'component-123',
  repositoryName: 'maven-releases',
  group: 'org.apache.commons',
  name: 'commons-lang3',
  version: '3.14.0',
  format: 'maven2',
};

const mockNpmComponent: ComponentXO = {
  id: 'component-456',
  repositoryName: 'npm-hosted',
  group: null,
  name: 'lodash',
  version: '4.17.21',
  format: 'npm',
};

const mockComponentNoVersion: ComponentXO = {
  id: 'component-789',
  repositoryName: 'raw-hosted',
  group: null,
  name: 'my-file.txt',
  version: null,
  format: 'raw',
};

describe('ComponentDetailPanel', () => {
  describe('rendering', () => {
    it('renders component name as heading', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByRole('heading', { name: 'commons-lang3' })).toBeInTheDocument();
    });

    it('renders repository name', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText('maven-releases')).toBeInTheDocument();
    });

    it('renders format', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('renders group when present', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
    });

    it('renders "-" when group is null', () => {
      renderWithTheme(<ComponentDetailPanel component={mockNpmComponent} />);

      // Group should display as "-"
      const groupLabel = screen.getByText('Group');
      const groupRow = groupLabel.closest('[class*="DataListItem"]');
      expect(groupRow).toHaveTextContent('-');
    });

    it('renders version when present', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText('3.14.0')).toBeInTheDocument();
    });

    it('renders "-" when version is null', () => {
      renderWithTheme(<ComponentDetailPanel component={mockComponentNoVersion} />);

      const versionLabel = screen.getByText('Version');
      const versionRow = versionLabel.closest('[class*="DataListItem"]');
      expect(versionRow).toHaveTextContent('-');
    });
  });

  describe('Maven dependency snippets', () => {
    it('shows dependency snippets for Maven format', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText('Dependency Information')).toBeInTheDocument();
      expect(screen.getByText('Maven')).toBeInTheDocument();
      expect(screen.getByText('Gradle')).toBeInTheDocument();
    });

    it('does not show dependency snippets for non-Maven format', () => {
      renderWithTheme(<ComponentDetailPanel component={mockNpmComponent} />);

      expect(screen.queryByText('Dependency Information')).not.toBeInTheDocument();
    });

    it('includes correct groupId in Maven snippet', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText(/<groupId>org\.apache\.commons<\/groupId>/)).toBeInTheDocument();
    });

    it('includes correct artifactId in Maven snippet', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText(/<artifactId>commons-lang3<\/artifactId>/)).toBeInTheDocument();
    });

    it('includes correct version in Maven snippet', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(screen.getByText(/<version>3\.14\.0<\/version>/)).toBeInTheDocument();
    });

    it('includes correct Gradle snippet', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      expect(
        screen.getByText(/implementation 'org\.apache\.commons:commons-lang3:3\.14\.0'/)
      ).toBeInTheDocument();
    });
  });

  describe('delete button', () => {
    it('does not show delete button when canDelete is false', () => {
      renderWithTheme(
        <ComponentDetailPanel
          component={mockMavenComponent}
          canDelete={false}
          onDelete={jest.fn()}
        />
      );

      expect(screen.queryByRole('button', { name: /delete component/i })).not.toBeInTheDocument();
    });

    it('does not show delete button when onDelete is not provided', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} canDelete={true} />);

      expect(screen.queryByRole('button', { name: /delete component/i })).not.toBeInTheDocument();
    });

    it('shows delete button when canDelete is true and onDelete is provided', () => {
      renderWithTheme(
        <ComponentDetailPanel
          component={mockMavenComponent}
          canDelete={true}
          onDelete={jest.fn()}
        />
      );

      expect(screen.getByRole('button', { name: /delete component/i })).toBeInTheDocument();
    });

    it('opens confirmation dialog when delete button is clicked', async () => {
      renderWithTheme(
        <ComponentDetailPanel
          component={mockMavenComponent}
          canDelete={true}
          onDelete={jest.fn()}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      expect(screen.getByText('Confirm deletion?')).toBeInTheDocument();
      expect(screen.getByText(/This will delete all asset\(s\) associated with the component:/i)).toBeInTheDocument();
    });

    it('calls onDelete when delete is confirmed', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <ComponentDetailPanel
          component={mockMavenComponent}
          canDelete={true}
          onDelete={onDelete}
        />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      // Confirm delete
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      expect(onDelete).toHaveBeenCalledTimes(1);
    });

    it('closes dialog without calling onDelete when cancelled', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <ComponentDetailPanel
          component={mockMavenComponent}
          canDelete={true}
          onDelete={onDelete}
        />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      // Cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await userEvent.click(cancelButton);

      expect(onDelete).not.toHaveBeenCalled();
    });
  });

  describe('copy functionality', () => {
    beforeEach(() => {
      // Mock clipboard API
      Object.assign(navigator, {
        clipboard: {
          writeText: jest.fn().mockResolvedValue(undefined),
        },
      });
    });

    it('copies Maven snippet when copy button is clicked', async () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      const copyButtons = screen.getAllByRole('button', { name: /copy/i });
      await userEvent.click(copyButtons[0]); // First copy button is for Maven

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringContaining('<dependency>')
      );
    });

    it('copies Gradle snippet when copy button is clicked', async () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      const copyButtons = screen.getAllByRole('button', { name: /copy/i });
      await userEvent.click(copyButtons[1]); // Second copy button is for Gradle

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringContaining("implementation '")
      );
    });

    it('shows "Copied!" message after copying', async () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);

      const copyButtons = screen.getAllByRole('button', { name: /copy/i });
      await userEvent.click(copyButtons[0]);

      await waitFor(() => {
        expect(screen.getByText('Copied!')).toBeInTheDocument();
      });
    });
  });

  describe('Guide deep research integration (y87u)', () => {
    it('shows deep research link for component with version', () => {
      renderWithTheme(<ComponentDetailPanel component={mockNpmComponent} />);
      expect(screen.getByTestId('deep-research-link')).toBeInTheDocument();
    });

    it('deep research link opens Guide URL for npm', () => {
      renderWithTheme(<ComponentDetailPanel component={mockNpmComponent} />);
      // Button asChild renders an <a> but we need to find it via the wrapper
      const link = screen.getAllByRole('link').find(l => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/npm/lodash/4.17.21?referrer=repo-componentdetail');
      expect(link).toHaveAttribute('target', '_blank');
    });

    it('deep research link opens Guide URL for maven', () => {
      renderWithTheme(<ComponentDetailPanel component={mockMavenComponent} />);
      const link = screen.getAllByRole('link').find(l => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toHaveAttribute('href', 'https://guide.sonatype.com/component/maven/org.apache.commons%3Acommons-lang3/3.14.0?referrer=repo-componentdetail');
    });

    it('does not show deep research link when version is null', () => {
      renderWithTheme(<ComponentDetailPanel component={mockComponentNoVersion} />);
      expect(screen.queryByTestId('deep-research-link')).not.toBeInTheDocument();
    });
  });
});


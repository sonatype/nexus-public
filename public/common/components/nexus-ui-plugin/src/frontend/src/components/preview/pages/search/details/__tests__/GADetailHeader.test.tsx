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
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GADetailHeader } from '../GADetailHeader';

jest.mock('../GADownloadMenu', () => ({
  GADownloadMenu: () => <div data-testid="download-menu" />,
}));

const rows = [
  { repositoryName: 'maven-public', type: 'group' as const, versionCount: 3 },
  { repositoryName: 'maven-releases', type: 'hosted' as const, versionCount: 1 },
  { repositoryName: 'maven-central', type: 'proxy' as const, versionCount: 5 },
];
const base = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  displayName: 'commons-lang3',
  coordinate: 'org.apache.commons',
  selectedVersion: '3.14.0',
  selectedRepository: 'maven-releases',
  onRepositoryChange: jest.fn(),
  selectedGroup: 'maven-public',
  onGroupChange: jest.fn(),
};

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('GADetailHeader', () => {
  it('shows name, coordinate and the Download menu', () => {
    wrap(<GADetailHeader {...base} repoRows={rows} />);
    expect(screen.getByText('commons-lang3')).toBeInTheDocument();
    expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
    expect(screen.getByTestId('download-menu')).toBeInTheDocument();
  });

  it('renders a Group selector when a group-type repo is present', () => {
    wrap(<GADetailHeader {...base} repoRows={rows} />);
    expect(screen.getByLabelText('Group')).toBeInTheDocument();
  });

  it('hides the Group selector when no group-type repo serves the version', () => {
    const noGroup = rows.filter((r) => r.type !== 'group');
    wrap(<GADetailHeader {...base} selectedGroup={null} repoRows={noGroup} />);
    expect(screen.queryByLabelText('Group')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Repository')).toBeInTheDocument();
  });

  it('shows the version as read-only labeled text, not an editable control', () => {
    // Version switching lives in the Versions tab now (NEXUS-54428): the header only labels
    // and displays the current version — it must not offer a control to change it.
    wrap(<GADetailHeader {...base} repoRows={rows} />);
    expect(screen.getByText('Version')).toBeInTheDocument();
    expect(screen.getByText('3.14.0')).toBeInTheDocument();
    // No editable Version control in the header.
    expect(screen.queryByLabelText('Version')).not.toBeInTheDocument();
    // The name heading holds only the name — never the version glued onto it.
    const heading = screen.getByRole('heading', { name: 'commons-lang3' });
    expect(heading.textContent).toBe('commons-lang3');
  });

  it('disables the Repository selector when a single repository serves the version', () => {
    const single = [{ repositoryName: 'maven-releases', type: 'hosted' as const, versionCount: 1 }];
    wrap(
      <GADetailHeader
        {...base}
        selectedGroup={null}
        selectedRepository="maven-releases"
        repoRows={single}
      />
    );
    // Still shown (so the user sees which repo they are on) but not switchable.
    expect(screen.getByLabelText('Repository')).toBeDisabled();
  });

  it('explains via tooltip why the Repository selector is disabled', async () => {
    // A disabled control with no explanation reads as broken; the tooltip states that only one
    // repository serves the component so the disabled state is understood, not guessed at.
    const single = [{ repositoryName: 'maven-releases', type: 'hosted' as const, versionCount: 1 }];
    wrap(
      <GADetailHeader
        {...base}
        selectedGroup={null}
        selectedRepository="maven-releases"
        repoRows={single}
      />
    );
    await userEvent.hover(screen.getByLabelText('Repository'));
    await waitFor(() => {
      // Radix renders the content twice (visible + a11y mirror); assert at least one is present.
      expect(
        screen.getAllByText('Only one repository serves this component.').length,
      ).toBeGreaterThan(0);
    });
  });

  it('keeps the Repository selector enabled when multiple repositories serve the version', () => {
    wrap(<GADetailHeader {...base} repoRows={rows} />);
    expect(screen.getByLabelText('Repository')).not.toBeDisabled();
  });

  // NEXUS-54430: the former copy-path button was replaced with a "View in Browse" action that
  // deep-links this component/version into the new Browse tree. The header only renders it and
  // fires the callback; the caller decides the destination (and whether it is available).
  describe('View in Browse action', () => {
    it('renders the action with an accessible name bound to the component when a handler is given', () => {
      wrap(<GADetailHeader {...base} repoRows={rows} onViewInBrowse={jest.fn()} />);
      expect(
        screen.getByRole('button', { name: 'View commons-lang3 in Browse' }),
      ).toBeInTheDocument();
    });

    it('invokes the handler when the action is activated', async () => {
      const onViewInBrowse = jest.fn();
      wrap(<GADetailHeader {...base} repoRows={rows} onViewInBrowse={onViewInBrowse} />);
      await userEvent.click(
        screen.getByRole('button', { name: 'View commons-lang3 in Browse' }),
      );
      expect(onViewInBrowse).toHaveBeenCalledTimes(1);
    });

    it('hides the action when no handler is provided', () => {
      wrap(<GADetailHeader {...base} repoRows={rows} />);
      expect(screen.queryByRole('button', { name: /in Browse/i })).not.toBeInTheDocument();
    });
  });

  describe('Research in Guide', () => {
    it('renders a "Research in Guide" link to the guide component page in a new tab', () => {
      wrap(<GADetailHeader {...base} repoRows={rows} />);
      expect(screen.getByText('Research in Guide')).toBeInTheDocument();
      const link = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toBeTruthy();
      // gaId maven:org.apache.commons:commons-lang3 → ecosystem maven, package group:name, version.
      expect(link).toHaveAttribute(
        'href',
        expect.stringContaining('/component/maven/org.apache.commons%3Acommons-lang3/3.14.0'),
      );
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('omits the guide link when no version is selected', () => {
      wrap(<GADetailHeader {...base} selectedVersion={null} repoRows={rows} />);
      expect(screen.queryByText('Research in Guide')).not.toBeInTheDocument();
    });

    it('omits the guide link for an ecosystem Guide does not support', () => {
      wrap(
        <GADetailHeader
          {...base}
          gaId="docker:library:nginx"
          selectedVersion="latest"
          repoRows={rows}
        />,
      );
      expect(screen.queryByText('Research in Guide')).not.toBeInTheDocument();
    });
  });
});

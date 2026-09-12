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
import { render, screen, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { GAOverviewTab } from '../GAOverviewTab';
import type { GADetail } from '../../core';

const mockTrackSnippetCopy = jest.fn();
jest.mock('../dependencySnippets/trackSnippetCopy', () => ({
  trackSnippetCopy: (...args: unknown[]) => mockTrackSnippetCopy(...args),
}));

const writeText = jest.fn();

/**
 * `detail` is the shell only. `repositories` and `versions` are empty because the aggregate walk
 * that filled them is gone (NEXUS-54201 / NEXUS-54220); the tab takes the repository names and the
 * timestamp as props from the caller's per-version sources instead.
 */
function detail(overrides: Partial<GADetail> & { gaId: string; format: string }): GADetail {
  return {
    displayName: 'lib',
    description: undefined,
    license: undefined,
    repositories: [],
    versions: [],
    ...overrides,
  } as any;
}

function renderTab(
  d: GADetail,
  selectedVersion: string | null = '1.0.0',
  repositories: readonly string[] = ['repo-1'],
  lastUpdated: string | null = '2026-01-15T00:00:00Z',
) {
  return render(
    <Theme>
      <GAOverviewTab
        detail={d}
        selectedVersion={selectedVersion}
        repositories={repositories}
        lastUpdated={lastUpdated}
      />
    </Theme>,
  );
}

// Opens the Radix Select snippet picker and chooses the option with the given label.
async function chooseSnippet(name: string) {
  await userEvent.click(screen.getByRole('combobox', { name: /dependency snippet/i }));
  await userEvent.click(await screen.findByRole('option', { name }));
}

// jsdom lacks the pointer-capture / scrollIntoView APIs Radix Select touches when opening.
beforeAll(() => {
  window.HTMLElement.prototype.hasPointerCapture = jest.fn(() => false);
  window.HTMLElement.prototype.releasePointerCapture = jest.fn();
  window.HTMLElement.prototype.scrollIntoView = jest.fn();
});

describe('GAOverviewTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Object.assign(navigator, { clipboard: { writeText } });
    writeText.mockResolvedValue(undefined);
  });

  it('renders a snippet-type dropdown listing the maven snippet options', async () => {
    renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }));
    await userEvent.click(screen.getByRole('combobox', { name: /dependency snippet/i }));
    expect(await screen.findByRole('option', { name: 'Apache Maven' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Gradle Groovy DSL' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'PURL' })).toBeInTheDocument();
  });

  it('shows only the first snippet usage example by default (not all at once)', () => {
    renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }));
    // Default selection = Apache Maven pom.xml fragment
    expect(screen.getByText(/<groupId>org\.apache\.commons<\/groupId>/)).toBeInTheDocument();
    // The PURL snippet's usage text is NOT shown until it is selected
    expect(screen.queryByText('pkg:maven/org.apache.commons/commons-lang3@1.0.0')).not.toBeInTheDocument();
  });

  it('switches the visible usage example when another snippet type is chosen', async () => {
    renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }));
    await chooseSnippet('PURL');
    expect(screen.getByText('pkg:maven/org.apache.commons/commons-lang3@1.0.0')).toBeInTheDocument();
    expect(screen.queryByText(/<groupId>org\.apache\.commons<\/groupId>/)).not.toBeInTheDocument();
  });

  it('emits maven coordinates without a synthesized extension at component level (matching Classic)', async () => {
    renderTab(
      detail({
        gaId: 'maven:com.esd:mylib',
        format: 'maven',
        displayName: 'mylib',
        versions: [{ version: '1.2.3', lastUpdated: '2026-01-15T00:00:00Z', repositories: ['repo-1'], status: 'none', statusReason: undefined }] as never,
      }),
      '1.2.3',
    );
    // Classic emits no extension at component level: the Ivy block has no <artifact> child and
    // Gradle coordinates carry no @jar, so transitive resolution and PURL stay format-neutral.
    await chooseSnippet('Apache Ivy');
    await userEvent.click(screen.getByRole('button', { name: /copy apache ivy snippet/i }));
    expect(writeText).toHaveBeenCalledWith('<dependency org="com.esd" name="mylib" rev="1.2.3"></dependency>');

    await chooseSnippet('Gradle Groovy DSL');
    await userEvent.click(screen.getByRole('button', { name: /copy gradle groovy dsl snippet/i }));
    expect(writeText).toHaveBeenCalledWith("implementation 'com.esd:mylib:1.2.3'");
  });

  it('renders npm snippet options and shows the npm command by default', async () => {
    renderTab(detail({ gaId: 'npm:angular:core', format: 'npm', displayName: 'core' }));
    expect(screen.getByText('npm install @angular/core@1.0.0')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('combobox', { name: /dependency snippet/i }));
    expect(await screen.findByRole('option', { name: 'Yarn' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Apache Maven' })).not.toBeInTheDocument();
  });

  it('copies the currently selected snippet text and fires the copy analytics event', async () => {
    renderTab(detail({ gaId: 'npm:angular:core', format: 'npm', displayName: 'core' }));

    await userEvent.click(screen.getByRole('button', { name: /copy npm snippet/i }));
    expect(writeText).toHaveBeenCalledWith('npm install @angular/core@1.0.0');
    expect(mockTrackSnippetCopy).toHaveBeenCalledWith('npm', 'npm');

    await chooseSnippet('Yarn');
    await userEvent.click(screen.getByRole('button', { name: /copy yarn snippet/i }));
    expect(writeText).toHaveBeenCalledWith('yarn add @angular/core@1.0.0');
    expect(mockTrackSnippetCopy).toHaveBeenCalledWith('npm', 'Yarn');
  });

  it('does not raise an unhandled rejection when the clipboard write fails', async () => {
    const rejections: unknown[] = [];
    const onRejection = (reason: unknown) => rejections.push(reason);
    process.on('unhandledRejection', onRejection);
    writeText.mockRejectedValue(new Error('clipboard blocked'));

    renderTab(detail({ gaId: 'npm:angular:core', format: 'npm', displayName: 'core' }));
    await userEvent.click(screen.getByRole('button', { name: /copy npm snippet/i }));
    // Let any rejected clipboard promise settle and surface as an unhandledRejection if uncaught.
    await new Promise((resolve) => setTimeout(resolve, 0));
    process.off('unhandledRejection', onRejection);

    expect(rejections).toEqual([]);
    // The copy intent is still tracked even though the clipboard write failed.
    expect(mockTrackSnippetCopy).toHaveBeenCalledWith('npm', 'npm');
  });

  it('clears the pending copied-reset timer when unmounted', () => {
    jest.useFakeTimers();
    const setSpy = jest.spyOn(global, 'setTimeout');
    const clearSpy = jest.spyOn(global, 'clearTimeout');
    try {
      const { unmount } = renderTab(detail({ gaId: 'npm:angular:core', format: 'npm', displayName: 'core' }));
      fireEvent.click(screen.getByRole('button', { name: /copy npm snippet/i }));

      // The copied-reset timer is the 2000ms timeout handleCopy schedules.
      const timerIdx = setSpy.mock.calls.findIndex((call) => call[1] === 2000);
      expect(timerIdx).toBeGreaterThanOrEqual(0);
      const copyTimer = setSpy.mock.results[timerIdx].value;

      unmount();
      expect(clearSpy).toHaveBeenCalledWith(copyTimer);
    } finally {
      setSpy.mockRestore();
      clearSpy.mockRestore();
      jest.useRealTimers();
    }
  });

  it('resets the selected snippet type to the default when the component format changes', async () => {
    const { rerender } = render(
      <Theme>
        <GAOverviewTab detail={detail({ gaId: 'composer:symfony:console', format: 'composer', displayName: 'console' })} selectedVersion="1.0.0" />
      </Theme>,
    );
    // Pick the second snippet type ('manual'), which both composer and cargo happen to expose.
    await chooseSnippet('manual');
    expect(screen.getByText('symfony/console: "1.0.0"')).toBeInTheDocument();

    // Navigating to a different-format component must not silently carry the stale selection over.
    rerender(
      <Theme>
        <GAOverviewTab detail={detail({ gaId: 'cargo:mycrate', format: 'cargo', displayName: 'mycrate' })} selectedVersion="1.0.0" />
      </Theme>,
    );
    // The Radix Select trigger shows the active value; after reset it falls back to cargo's default.
    expect(screen.getByRole('combobox', { name: /dependency snippet/i })).toHaveTextContent('cargo');
  });

  it('shows Component Details with the human-readable format label, name, group, version and repository', () => {
    renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', displayName: 'commons-lang3' }));
    const details = screen.getByRole('table');
    expect(within(details).getByText('Maven')).toBeInTheDocument();
    expect(within(details).getByText('org.apache.commons')).toBeInTheDocument();
    expect(within(details).getByText('commons-lang3')).toBeInTheDocument();
    expect(within(details).getByText('1.0.0')).toBeInTheDocument();
    expect(within(details).getByText('repo-1')).toBeInTheDocument();
  });

  /*
   * These three pin the prop wiring that replaced detail.repositories / detail.versions.
   *
   * Before, this tab read both off the aggregate walk. On the Overview tab that walk did not
   * run, so the Repository row was absent and Last Updated showed an em dash whenever the user
   * landed here first — the values only appeared if they had visited another tab. Now they come
   * from the caller's per-version sources and are correct on first paint.
   */
  it('lists every repository holding the selected version, from the prop', () => {
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }),
      '1.0.0',
      ['maven-releases', 'maven-snapshots'],
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('maven-releases, maven-snapshots')).toBeInTheDocument();
  });

  it('omits the Repository row when no repository is known yet', () => {
    renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }), '1.0.0', []);
    expect(screen.queryByText('Repository')).not.toBeInTheDocument();
  });

  it('renders an em dash for Last Updated when the version carries no timestamp', () => {
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }),
      '1.0.0',
      ['repo-1'],
      null,
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('—')).toBeInTheDocument();
    expect(within(details).queryByText('Invalid Date')).not.toBeInTheDocument();
  });

  it('omits the Group row when the component has no group', () => {
    renderTab(detail({ gaId: 'npm:lodash', format: 'npm', displayName: 'lodash' }));
    expect(screen.queryByText('Group')).not.toBeInTheDocument();
  });

  it('renders no dependency snippets when the component has no resolvable version', () => {
    // npm/pypi/maven and others don't guard on empty version, so without this gate the panel would
    // emit a truncated, un-copyable coordinate like "npm install @angular/core@".
    renderTab(detail({ gaId: 'npm:angular:core', format: 'npm', displayName: 'core', versions: [] as never }), null);
    expect(screen.queryByRole('combobox', { name: /dependency snippet/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/npm install/)).not.toBeInTheDocument();
  });

  it('renders no dependency snippets section for a format without a generator', () => {
    renderTab(detail({ gaId: 'raw:some/path', format: 'raw', displayName: 'path' }));
    expect(screen.queryByText('Dependencies')).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: /dependency snippet/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /copy .* snippet/i })).not.toBeInTheDocument();
  });
});

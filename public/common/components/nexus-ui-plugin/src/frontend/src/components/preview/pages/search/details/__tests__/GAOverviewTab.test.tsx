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
import type { RepoRow } from '../gaRepositoriesMachine';

import ExtAPIUtils from '../../../../../../interface/ExtAPIUtils';

const mockTrackSnippetCopy = jest.fn();
jest.mock('../dependencySnippets/trackSnippetCopy', () => ({
  trackSnippetCopy: (...args: unknown[]) => mockTrackSnippetCopy(...args),
}));

/*
 * The Overview tab resolves Repository Health Check intelligence over Ext.Direct. Spy on the
 * transport (matching DetectTable's use of jest.spyOn for ExtJS) so the snippet and detail tests
 * neither reach the network nor depend on RHC state. checkForErrorAndExtract stays real: the
 * envelope shape it unwraps is part of what these tests verify.
 *
 * The default is a request that never settles, leaving the intelligence rows in their loading
 * state, so it adds no value text that the assertions below could collide with.
 */
const mockExtAPIRequest = jest.spyOn(ExtAPIUtils, 'extAPIRequest');

/** Ext.Direct success envelope carrying one HealthCheckXO. */
function healthCheckEnvelope(healthCheck: Record<string, unknown> | null) {
  return { data: { result: { success: true, data: [{ healthCheck }] } } };
}

const writeText = jest.fn();

function detail(overrides: Partial<GADetail> & { gaId: string; format: string }): GADetail {
  return {
    displayName: 'lib',
    description: undefined,
    license: undefined,
    repositories: [{ name: 'repo-1', format: overrides.format, type: 'hosted', versionsCount: 1 }],
    versions: [{ version: '1.0.0', lastUpdated: '2026-01-15T00:00:00Z', repositories: ['repo-1'], status: 'none', statusReason: undefined }],
    ...overrides,
  } as any;
}

function renderTab(
  d: GADetail,
  selectedVersion: string | null = '1.0.0',
  selectedRepository: string | null = null,
  repoRows: readonly RepoRow[] = [],
  versionLastUpdated: string | null = null,
) {
  return render(
    <Theme>
      <GAOverviewTab
        detail={d}
        selectedVersion={selectedVersion}
        selectedRepository={selectedRepository}
        repoRows={repoRows}
        versionLastUpdated={versionLastUpdated}
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
    mockExtAPIRequest.mockReturnValue(new Promise(() => {}));
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

  it('always shows a Repository row, sourced from the selected repository when the aggregate list is empty', () => {
    // On the Overview tab the aggregate repository walk has not run, so detail.repositories is
    // empty. The Repository row must still appear, filled from the header's selected repository.
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', displayName: 'commons-lang3', repositories: [] as never }),
      '1.0.0',
      'maven-releases',
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('Repository')).toBeInTheDocument();
    expect(within(details).getByText('maven-releases')).toBeInTheDocument();
  });

  /*
   * Last Updated goes through the shared formatter (NEXUS-54427), so the row carries a date, a
   * 24-hour time and a timezone label. Asserted by shape: the wall clock and offset depend on the
   * timezone of the machine running the suite, and jest pins none. Exact per-zone output is
   * covered in shared/__tests__/dateFormat.test.ts.
   */
  it('renders Last Updated with date, time and timezone', () => {
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }),
      '1.0.0',
      'repo-1',
      [],
      '2026-01-15T00:00:00Z',
    );
    const details = screen.getByRole('table');
    expect(
      within(details).getByText(/^Jan \d{1,2}, 2026, \d{2}:\d{2} GMT([+-]\d{1,2}(:\d{2})?)?$/),
    ).toBeInTheDocument();
  });

  it('shows Last Updated from the threaded version timestamp when the aggregate versions list is empty', () => {
    // detail.versions is always empty on this page, so the "Last Updated" value can only come from
    // the per-version timestamp threaded in via versionLastUpdated.
    const ts = '2026-03-20T12:00:00Z';
    renderTab(
      detail({
        gaId: 'maven:org.apache.commons:commons-lang3',
        format: 'maven',
        displayName: 'commons-lang3',
        versions: [] as never,
      }),
      '1.0.0',
      'maven-releases',
      [],
      ts,
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('Last Updated')).toBeInTheDocument();
    // With formatDateTime (NEXUS-54427), the row shows date+time+timezone by shape.
    expect(
      within(details).getByText(/^Mar \d{1,2}, 2026, \d{2}:\d{2} GMT([+-]\d{1,2}(:\d{2})?)?$/),
    ).toBeInTheDocument();
    // Guard against the pre-fix behavior: versionLastUpdated must actually drive the row, not the
    // em-dash placeholder formatDateTime returns for an absent value.
    expect(within(details).queryByText('—')).not.toBeInTheDocument();
  });

  it('renders an em dash for Last Updated when the version carries no timestamp', () => {
    // No threaded versionLastUpdated and an empty aggregate versions list: nothing can supply the
    // timestamp, so the row must fall back to the shared formatter's placeholder.
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', versions: [] as never }),
      '1.0.0',
      'repo-1',
      [],
      null,
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('Last Updated')).toBeInTheDocument();
    expect(within(details).getByText('—')).toBeInTheDocument();
  });

  it('feeds the selected repository into the NuGet snippet source when the aggregate list is empty', async () => {
    // repositoryName drives the NuGet Chocolatey --source URL. detail.repositories is empty here,
    // so without deriving it from the header selection the snippet would omit --source entirely.
    renderTab(
      detail({
        gaId: 'nuget:Newtonsoft.Json',
        format: 'nuget',
        displayName: 'Newtonsoft.Json',
        repositories: [] as never,
      }),
      '13.0.3',
      'nuget-hosted',
    );
    await chooseSnippet('Chocolatey CLI');
    await userEvent.click(screen.getByRole('button', { name: /copy chocolatey cli snippet/i }));
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('/repository/nuget-hosted/'));
  });

  it('still renders the Repository row when no repository is known at all', () => {
    renderTab(
      detail({ gaId: 'npm:lodash', format: 'npm', displayName: 'lodash', repositories: [] as never }),
      '1.0.0',
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('Repository')).toBeInTheDocument();
  });

  const memberRepoRows: RepoRow[] = [
    { repositoryName: 'repo-beta', type: 'hosted', versionCount: 2 },
    { repositoryName: 'maven-releases', type: 'hosted', versionCount: 1 },
    { repositoryName: 'maven-central', type: 'proxy', versionCount: 5 },
    { repositoryName: 'maven-public', type: 'group', versionCount: 3 },
  ];

  it('lists every member repository serving the version, the selected one first and marked current', () => {
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', displayName: 'commons-lang3' }),
      '1.0.0',
      'maven-central',
      memberRepoRows,
    );
    const details = screen.getByRole('table');
    const list = within(details).getByRole('list', { name: /repositor/i });
    const items = within(list).getAllByRole('listitem');
    // Group repositories are represented by the header's Group facet, not listed here.
    expect(items).toHaveLength(3);
    // The selected repository leads the list and is flagged as the current one.
    expect(items[0]).toHaveTextContent('maven-central');
    expect(items[0]).toHaveAttribute('aria-current', 'true');
    // The remaining member repositories follow.
    expect(within(list).getByText('repo-beta')).toBeInTheDocument();
    expect(within(list).getByText('maven-releases')).toBeInTheDocument();
    expect(within(list).queryByText('maven-public')).not.toBeInTheDocument();
  });

  it('renders the Overview repository list as read-only (no switch controls)', () => {
    // The header dropdown remains the sole control that changes the active repository;
    // the Overview list is purely informational, so it must expose no clickable controls.
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', displayName: 'commons-lang3' }),
      '1.0.0',
      'maven-central',
      memberRepoRows,
    );
    const details = screen.getByRole('table');
    const list = within(details).getByRole('list', { name: /repositor/i });
    expect(within(list).queryByRole('button')).not.toBeInTheDocument();
    expect(within(list).queryByRole('link')).not.toBeInTheDocument();
  });

  it('falls back to the single repository value when no member rows are supplied', () => {
    // Backward compatibility: without repoRows the row keeps its plain string value and
    // renders no list, so existing callers and the aggregate-empty path are unaffected.
    renderTab(
      detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven', displayName: 'commons-lang3', repositories: [] as never }),
      '1.0.0',
      'maven-releases',
      [],
    );
    const details = screen.getByRole('table');
    expect(within(details).getByText('maven-releases')).toBeInTheDocument();
    expect(within(details).queryByRole('list', { name: /repositor/i })).not.toBeInTheDocument();
  });

  it("treats '' as the real selected version and never falls through to versions[0]", () => {
    // Regression guard for the `??` vs `||` distinction: '' is the valid selected version of a
    // versionless (raw) component and is falsy, so `||` would wrongly fall through to
    // detail.versions[0].version and render a Version row for the wrong value. `??` keeps ''.
    renderTab(
      detail({
        gaId: 'raw:some/path',
        format: 'raw',
        displayName: 'path',
        versions: [
          { version: '9.9.9', lastUpdated: '2026-01-15T00:00:00Z', repositories: ['repo-1'], status: 'none', statusReason: undefined },
        ] as never,
      }),
      '', // the empty-string selected version of a raw component
    );
    const details = screen.getByRole('table');
    // ver resolves to '', so the Version row is omitted entirely — the fallback 9.9.9 must not leak.
    expect(within(details).queryByText('Version')).not.toBeInTheDocument();
    expect(within(details).queryByText('9.9.9')).not.toBeInTheDocument();
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

  /*
   * NEXUS-54432: parity with the Classic component-details panel, which shows these five signals
   * through two cooperating controllers — NX.coreui.controller.HealthCheckInfo (most popular
   * version, age, popularity) and NX.proui.controller.HealthCheckInfo (security, license). The
   * expected labels, units, colour mapping and zero handling are all taken from those renderers.
   */
  describe('Repository Health Check intelligence (NEXUS-54432)', () => {
    const MAVEN = detail({
      gaId: 'maven2:org.apache.commons:commons-lang3',
      format: 'maven2',
      displayName: 'commons-lang3',
    });

    // 730 days: past the 364-day cutoff, so Classic renders years to one decimal place.
    const TWO_YEARS_MILLIS = 730 * 86400000;

    const FULL_INFO = {
      mostPopularVersion: '3.12.0',
      age: TWO_YEARS_MILLIS,
      popularity: 6,
      securityAlerts: 1,
      criticalSecurityAlerts: 0,
      severeSecurityAlerts: 1,
      moderateSecurityAlerts: 0,
      licenseThreat: 1,
      licenseThreatName: 'Apache-2.0',
    };

    // The security and license rows require a resolved, valid license (see resolveAlertsStatus,
    // which fails closed while the state store has published nothing). Default to an entitled Pro
    // instance so these tests exercise the rendering rather than the entitlement gate; the
    // entitlement cases below override this.
    beforeEach(() => {
      (global as unknown as { NX: { State: { getValue: jest.Mock } } }).NX.State.getValue.mockImplementation(
        (key: string) => (key === 'license' ? { installed: true, valid: true } : undefined),
      );
    });

    // clearMocks clears recorded calls but leaves implementations in place, so the license state
    // above would otherwise leak into every test that follows this describe.
    afterEach(() => {
      (global as unknown as { NX: { State: { getValue: jest.Mock } } }).NX.State.getValue.mockReset();
    });

    it('shows all five signals Classic shows, with Classic labels, units and license name', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
      renderTab(MAVEN, '3.19.0', 'maven-central');

      const details = screen.getByRole('table');
      expect(await within(details).findByText('3.12.0')).toBeInTheDocument();
      expect(within(details).getByText('Most popular version')).toBeInTheDocument();
      expect(within(details).getByText('2.0 yrs')).toBeInTheDocument();
      expect(within(details).getByText('6 %')).toBeInTheDocument();
      expect(within(details).getByText('Security')).toBeInTheDocument();
      expect(within(details).getByLabelText('1 severe')).toBeInTheDocument();
      expect(within(details).getByText('License')).toBeInTheDocument();
      expect(within(details).getByText('Apache-2.0')).toBeInTheDocument();
    });

    it("sends the component array as the Ext.Direct method's single argument", async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
      renderTab(MAVEN, '3.19.0', 'maven-central');

      await screen.findByText('3.12.0');
      expect(mockExtAPIRequest).toHaveBeenCalledWith('healthcheck_Info', 'read', {
        // Nested: `data` is the argument list, and the method's one parameter is itself an array.
        data: [[{ group: 'org.apache.commons', name: 'commons-lang3', version: '3.19.0', format: 'maven2' }]],
      });
    });

    it('normalizes the maven format alias to the maven2 id the backend compares against', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
      renderTab(detail({ gaId: 'maven:org.apache.commons:commons-lang3', format: 'maven' }), '3.19.0');

      await screen.findByText('3.12.0');
      expect(mockExtAPIRequest.mock.calls[0][2]).toEqual({
        data: [[expect.objectContaining({ format: 'maven2' })]],
      });
    });

    it('renders a real zero age, popularity and alert count as zero, not as unavailable', async () => {
      // The Classic renderers guard with `value === 0 || value > 0` precisely so a brand-new or
      // unused component is not misreported as having no data.
      mockExtAPIRequest.mockResolvedValue(
        healthCheckEnvelope({
          mostPopularVersion: '1.0.0',
          age: 0,
          popularity: 0,
          securityAlerts: 0,
          criticalSecurityAlerts: 0,
          severeSecurityAlerts: 0,
          moderateSecurityAlerts: 0,
          licenseThreat: 1,
          licenseThreatName: 'Apache-2.0',
        }),
      );
      renderTab(MAVEN, '1.0.0');

      const details = screen.getByRole('table');
      expect(await within(details).findByText('0 d')).toBeInTheDocument();
      expect(within(details).getByText('0 %')).toBeInTheDocument();
      expect(within(details).getByLabelText('0 critical')).toBeInTheDocument();
      expect(within(details).getByLabelText('0 severe')).toBeInTheDocument();
      expect(within(details).getByLabelText('0 moderate')).toBeInTheDocument();
    });

    it('renders an age below the year cutoff as a day count', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ age: 364 * 86400000 }));
      renderTab(MAVEN, '1.0.0');
      expect(await screen.findByText('364 d')).toBeInTheDocument();
    });

    it('clamps a popularity above 100, matching Classic', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ popularity: 140 }));
      renderTab(MAVEN, '1.0.0');
      expect(await screen.findByText('100 %')).toBeInTheDocument();
    });

    it('omits Security and License when the response carries no alert data', async () => {
      // Community Edition's HealthCheckInfoComponent returns includeAlerts() == false, and
      // UrlServiceImpl drops the flag when isSearchAlertDataSupported() is false. Absent counts
      // mean "not evaluated", so showing zeros would report an unevaluated component as clean.
      mockExtAPIRequest.mockResolvedValue(
        healthCheckEnvelope({ mostPopularVersion: '3.12.0', age: TWO_YEARS_MILLIS, popularity: 6 }),
      );
      renderTab(MAVEN, '3.19.0');

      const details = screen.getByRole('table');
      expect(await within(details).findByText('3.12.0')).toBeInTheDocument();
      expect(within(details).queryByText('Security')).not.toBeInTheDocument();
      expect(within(details).queryByText('License')).not.toBeInTheDocument();
    });

    it('marks the signals unavailable and explains why when Repository Health Check is disabled', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ disabled: true }));
      renderTab(MAVEN, '3.19.0');

      expect(await screen.findByLabelText(/Age: The age and popularity data is only available/)).toBeInTheDocument();
      expect(screen.queryByText('Security')).not.toBeInTheDocument();
    });

    it('distinguishes an upstream failure from a component with no data', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ error: true }));
      renderTab(MAVEN, '3.19.0');
      expect(await screen.findByLabelText('Age: Error retrieving component data')).toBeInTheDocument();
    });

    it('explains the quota when the insight-service query limit has been reached', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ capped: true, mostPopularVersion: '3.12.0' }));
      renderTab(MAVEN, '3.19.0');

      // capped takes precedence over any value that happens to have been filled in, matching the
      // order of Classic's precondition ladder.
      expect(await screen.findByLabelText(/Popularity: The query limit for age and popularity data/)).toBeInTheDocument();
      expect(screen.queryByText('3.12.0')).not.toBeInTheDocument();
    });

    it('marks a signal unavailable when the component is simply unknown upstream', async () => {
      mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({}));
      renderTab(MAVEN, '3.19.0');
      expect(await screen.findByLabelText('Age: No data available for this component')).toBeInTheDocument();
    });

    it('reports an error rather than empty values when the Ext.Direct call fails', async () => {
      mockExtAPIRequest.mockResolvedValue({
        data: { type: 'exception', message: 'https://insight.internal.example.com timed out' },
      });
      renderTab(MAVEN, '3.19.0');

      expect(await screen.findByLabelText('Age: Error retrieving component data')).toBeInTheDocument();
      // The raw message can carry an upstream URL or a stack trace, so it must not reach the UI.
      expect(screen.queryByText(/insight\.internal\.example\.com/)).not.toBeInTheDocument();
    });

    it('requests nothing for a format the backend cannot answer for', () => {
      renderTab(detail({ gaId: 'raw:some/path', format: 'raw', displayName: 'path' }), '1.0.0');
      expect(mockExtAPIRequest).not.toHaveBeenCalled();
      expect(screen.queryByText('Most popular version')).not.toBeInTheDocument();
    });

    it('requests nothing and shows no rows without nexus:healthcheck:read', () => {
      // Classic removes the whole section on the same condition. The check here only avoids a
      // request that is certain to be rejected — @RequiresPermissions on the Ext.Direct method
      // remains the enforcement boundary.
      (global as unknown as { NX: { Permissions: { check: jest.Mock } } }).NX.Permissions.check.mockReturnValue(false);
      renderTab(MAVEN, '3.19.0');

      expect(mockExtAPIRequest).not.toHaveBeenCalled();
      expect(screen.queryByText('Most popular version')).not.toBeInTheDocument();
    });

    /*
     * The alert fields have their own conditions, checked by Classic's proui renderPreconditions
     * before it shows either of them: a non-EVAL edition with a valid license, then
     * nexus:healthcheckalerts:update, then the alert quota. nexus:healthcheck:read alone admits a
     * caller to age and popularity only.
     */
    describe('security and license entitlement', () => {
      const nx = () =>
        (global as unknown as { NX: { Permissions: { check: jest.Mock }; State: { getValue: jest.Mock; getEdition: jest.Mock } } }).NX;

      /** Grants every permission except the alert-specific one. */
      function denyAlertPermission() {
        nx().Permissions.check.mockImplementation((p: string) => p !== 'nexus:healthcheckalerts:update');
      }

      function licenseState(license: Record<string, unknown> | undefined) {
        nx().State.getValue.mockImplementation((key: string) => (key === 'license' ? license : undefined));
      }

      // clearMocks (and jest.clearAllMocks) clear recorded calls but leave implementations in
      // place, so an edition or license override here would otherwise leak into every later test.
      afterEach(() => {
        nx().State.getEdition.mockReset();
        nx().State.getValue.mockReset();
        nx().Permissions.check.mockReset();
        nx().Permissions.check.mockReturnValue(true);
      });

      it('hides security and license from a user without nexus:healthcheckalerts:update', async () => {
        denyAlertPermission();
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
        renderTab(MAVEN, '3.19.0');

        const details = screen.getByRole('table');
        // Age and popularity still resolve — the base permission is unaffected.
        expect(await within(details).findByText('2.0 yrs')).toBeInTheDocument();
        expect(within(details).getByText('6 %')).toBeInTheDocument();
        expect(within(details).queryByText('Security')).not.toBeInTheDocument();
        expect(within(details).queryByText('License')).not.toBeInTheDocument();
        expect(within(details).queryByText('Apache-2.0')).not.toBeInTheDocument();
      });

      it('hides security and license when the license is installed but not valid', async () => {
        licenseState({ installed: true, valid: false });
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
        renderTab(MAVEN, '3.19.0');

        const details = screen.getByRole('table');
        expect(await within(details).findByText('2.0 yrs')).toBeInTheDocument();
        expect(within(details).queryByText('Security')).not.toBeInTheDocument();
        expect(within(details).queryByText('License')).not.toBeInTheDocument();
      });

      // Fails closed: entitlement that has not resolved is not entitlement, and the values are
      // already in the response, so rendering them here would expose an unlicensed user's alerts.
      it('hides security and license while the license state is still unknown', async () => {
        licenseState(undefined);
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
        renderTab(MAVEN, '3.19.0');

        const details = screen.getByRole('table');
        expect(await within(details).findByText('2.0 yrs')).toBeInTheDocument();
        expect(within(details).queryByText('Security')).not.toBeInTheDocument();
        expect(within(details).queryByText('Apache-2.0')).not.toBeInTheDocument();
      });

      it('hides security and license on an EVAL edition', async () => {
        nx().State.getEdition.mockReturnValue('EVAL');
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
        renderTab(MAVEN, '3.19.0');

        const details = screen.getByRole('table');
        expect(await within(details).findByText('2.0 yrs')).toBeInTheDocument();
        expect(within(details).queryByText('Security')).not.toBeInTheDocument();
      });

      it('shows security and license to an entitled, permitted user', async () => {
        licenseState({ installed: true, valid: true });
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope(FULL_INFO));
        renderTab(MAVEN, '3.19.0');

        expect(await screen.findByText('Apache-2.0')).toBeInTheDocument();
        expect(screen.getByLabelText('1 severe')).toBeInTheDocument();
      });

      it('explains the quota on both alert rows when only the alert quota is exhausted', async () => {
        // cappedAlerts, unlike capped, leaves age and popularity valid — so the two alert rows
        // must say why they are empty instead of vanishing as if unentitled.
        mockExtAPIRequest.mockResolvedValue(healthCheckEnvelope({ ...FULL_INFO, cappedAlerts: true }));
        renderTab(MAVEN, '3.19.0');

        const details = screen.getByRole('table');
        expect(await within(details).findByText('2.0 yrs')).toBeInTheDocument();
        expect(within(details).getByText('6 %')).toBeInTheDocument();
        expect(within(details).getByLabelText(/^Security: The query limit/)).toBeInTheDocument();
        expect(within(details).getByLabelText(/^License: The query limit/)).toBeInTheDocument();
        // The values are in the response but must not be rendered.
        expect(within(details).queryByText('Apache-2.0')).not.toBeInTheDocument();
        expect(within(details).queryByLabelText('1 severe')).not.toBeInTheDocument();
      });
    });

    it('requests nothing for a versionless component', () => {
      // The insight service identifies a component by coordinates including a version, so '' has
      // nothing to match — even though '' is a real selected version elsewhere on this page.
      renderTab(detail({ gaId: 'maven2:org.example:lib', format: 'maven2', versions: [] as never }), '');
      expect(mockExtAPIRequest).not.toHaveBeenCalled();
    });
  });
});

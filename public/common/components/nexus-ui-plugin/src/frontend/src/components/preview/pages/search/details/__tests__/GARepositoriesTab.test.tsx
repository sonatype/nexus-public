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
import { Theme } from '@radix-ui/themes';

import GARepositoriesTab from '../GARepositoriesTab';
import type { RepoRow } from '../gaRepositoriesMachine';
import * as exportToCsvModule from '../../../../shared/utils/exportToCsv';

const ROWS: RepoRow[] = [
  { repositoryName: 'maven-central', type: 'proxy',  versionCount: 42 },
  { repositoryName: 'maven-releases', type: 'hosted', versionCount:  3 },
  { repositoryName: 'maven-public',   type: 'group',  versionCount:  0 },
];

function renderTab(props: Partial<React.ComponentProps<typeof GARepositoriesTab>> = {}) {
  return render(
    <Theme>
      <GARepositoriesTab
        rows={ROWS}
        loading={false}
        error={null}
        selectedVersion="1.2.3"
        {...props}
      />
    </Theme>,
  );
}

describe('GARepositoriesTab', () => {
  it('renders one row per repository with correct name, type, and version count', () => {
    renderTab();
    ROWS.forEach((r) => {
      const nameCell = screen.getByText(r.repositoryName);
      const row = nameCell.closest('tr') as HTMLTableRowElement;
      expect(within(row).getByText(r.type)).toBeInTheDocument();
      expect(within(row).getByText(new RegExp(`${r.versionCount}\\s+version`))).toBeInTheDocument();
    });
  });

  it('has no Format column, no Format sort option, no Format filter section', () => {
    renderTab();
    expect(screen.queryByText('Format', { selector: 'th, [role="columnheader"]' })).toBeNull();
    // Sort dropdown items — Radix Select renders on trigger; assert the option is not in the DOM.
    expect(screen.queryByRole('option', { name: 'Format' })).toBeNull();
  });

  it('type filter narrows visible rows', async () => {
    renderTab();
    // Checkbox aria-label depends on Radix internals; select by association with the "Hosted" label.
    const hostedCheckbox = screen.getByLabelText('Hosted');
    fireEvent.click(hostedCheckbox);

    expect(screen.getByText('maven-releases')).toBeInTheDocument();
    expect(screen.queryByText('maven-central')).toBeNull();
    expect(screen.queryByText('maven-public')).toBeNull();
  });

  it('text search matches repositoryName', async () => {
    renderTab();
    await userEvent.type(screen.getByPlaceholderText('Filter'), 'central');
    expect(screen.getByText('maven-central')).toBeInTheDocument();
    expect(screen.queryByText('maven-releases')).toBeNull();
  });

  it('renders spinner when loading', () => {
    const { container } = renderTab({ rows: [], loading: true });
    // Radix Spinner doesn't have role="progressbar", check for spinner class
    expect(container.querySelector('.rt-Spinner')).toBeInTheDocument();
  });

  it('renders error callout when error is set', () => {
    renderTab({ rows: [], error: 'boom' });
    expect(screen.getByText('boom')).toBeInTheDocument();
  });

  it('renders empty state when rows is empty and not loading', () => {
    renderTab({ rows: [] });
    expect(screen.getByText('No repositories found')).toBeInTheDocument();
  });

  describe('CSV export', () => {
    let exportToCsvSpy: jest.SpyInstance;

    beforeEach(() => {
      exportToCsvSpy = jest
        .spyOn(exportToCsvModule, 'exportToCsv')
        .mockImplementation(() => undefined);
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('calls exportToCsv with repositoryName, type, versionCount columns', async () => {
      renderTab();
      await userEvent.click(screen.getByLabelText('Export all filtered results as CSV'));
      expect(exportToCsvSpy).toHaveBeenCalledTimes(1);
      expect(exportToCsvSpy).toHaveBeenCalledWith(
        expect.any(Array),
        'repositories.csv',
        ['repositoryName', 'type', 'versionCount'],
      );
    });

    it('exports one row per visible repository', async () => {
      renderTab();
      await userEvent.click(screen.getByLabelText('Export all filtered results as CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(ROWS.length);
    });

    it('exports only rows matching the active text filter', async () => {
      renderTab();
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'central');
      await userEvent.click(screen.getByLabelText('Export all filtered results as CSV'));
      const [data] = exportToCsvSpy.mock.calls[0];
      expect(data).toHaveLength(1);
      expect(data[0].repositoryName).toBe('maven-central');
    });

    it('disables the Export CSV button when no rows match filters', async () => {
      renderTab();
      await userEvent.type(screen.getByPlaceholderText('Filter'), 'zzznomatch');
      expect(screen.getByLabelText('Export all filtered results as CSV')).toBeDisabled();
    });
  });
});

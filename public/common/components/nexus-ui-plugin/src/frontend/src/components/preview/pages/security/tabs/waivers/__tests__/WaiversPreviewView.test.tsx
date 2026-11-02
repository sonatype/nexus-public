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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { WaiversPreviewView } from '../WaiversPreviewView';
import { EXAMPLE_WAIVER, EXAMPLE_WAIVER_REQUEST } from '../waiverExamples';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

const renderView = () =>
  render(
    <Theme>
      <WaiversPreviewView />
    </Theme>
  );

describe('WaiversPreviewView', () => {
  it('shows the Existing and Requested tabs with Existing selected', () => {
    renderView();

    // Radix renders each label twice (a hidden copy reserves width), so match on the start of the name.
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent(/^Existing/);
    expect(tabs[1]).toHaveTextContent(/^Requested/);
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
  });

  it('shows the example-data callout and the single example waiver on the Existing tab', () => {
    renderView();

    expect(screen.getByTestId('example-data-callout')).toHaveTextContent(
      'Example data — nothing on this tab reflects your repositories.'
    );
    expect(screen.getByRole('heading', { name: /Existing Waivers/ })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Example waiver' })).toBeInTheDocument();
    expect(screen.getAllByTestId('example-badge').length).toBeGreaterThan(0);
    expect(screen.getByText(EXAMPLE_WAIVER.component)).toBeInTheDocument();
    expect(screen.getByText(EXAMPLE_WAIVER.upgradeVersion)).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(2);
  });

  it('shows a disabled Revoke button', () => {
    renderView();

    expect(screen.getByRole('button', { name: 'Revoke' })).toBeDisabled();
  });

  it('shows the permission note and the example request on the Requested tab', async () => {
    renderView();

    await userEvent.click(screen.getAllByRole('tab')[1]);

    expect(screen.getByTestId('waiver-permission-note')).toHaveTextContent('No role currently carries the Apply Waivers permission.');
    expect(screen.getByRole('link', { name: 'Firewall access' })).toHaveAttribute('href', '#preview/admin/security/roles');
    expect(screen.getByRole('link', { name: 'Manage users' })).toHaveAttribute('href', '#preview/admin/security/users');
    expect(screen.getByRole('region', { name: 'Example waiver request' })).toBeInTheDocument();
    expect(screen.getByText(EXAMPLE_WAIVER_REQUEST.component)).toBeInTheDocument();
    expect(screen.getByText(EXAMPLE_WAIVER_REQUEST.requestedBy)).toBeInTheDocument();
    expect(screen.getByText(EXAMPLE_WAIVER_REQUEST.reason)).toBeInTheDocument();
  });

  it('makes no restClient call', () => {
    const { restClient } = jest.requireMock('../../../../../../../interface/api');

    renderView();

    expect(restClient.get).not.toHaveBeenCalled();
    expect(restClient.post).not.toHaveBeenCalled();
  });
});

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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { QuarantinePreviewView } from '../QuarantinePreviewView';
import { ProUpsellContext } from '../../ProUpsellContext';
import { CACHED_RISK_FIXTURES, CONTAINER_FIXTURES, QUARANTINE_FIXTURES } from '../quarantineFixtures';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

const renderView = (proUpsell = true) =>
  render(
    <Theme>
      <ProUpsellContext.Provider value={proUpsell}>
        <QuarantinePreviewView />
      </ProUpsellContext.Provider>
    </Theme>
  );

describe('QuarantinePreviewView', () => {
  it('shows the Quarantine, Cached Risk and Containers tabs with Quarantine selected', () => {
    renderView();

    // Radix renders each label twice (a hidden copy reserves width), so match on the start of the name.
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(3);
    expect(tabs[0]).toHaveTextContent(/^Quarantine/);
    expect(tabs[1]).toHaveTextContent(/^Cached Risk/);
    expect(tabs[2]).toHaveTextContent(/^Containers/);
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
  });

  it('shows the example-data and upgrade callouts and the upgrade button on the Quarantine tab', () => {
    renderView();

    expect(screen.getByTestId('example-data-callout')).toHaveTextContent(
      'Example data — nothing on this tab reflects your repositories.'
    );
    expect(screen.getByTestId('upgrade-callout')).toHaveTextContent('Upgrade to Nexus Repository Pro Plus');
    expect(screen.getByRole('link', { name: 'Upgrade to Nexus Repository Pro +' })).toBeInTheDocument();
  });

  it('renders one row per quarantine fixture, each with an Example badge', () => {
    renderView();

    const table = screen.getByRole('table', { name: 'Example quarantined components' });
    expect(within(table).getAllByRole('row')).toHaveLength(QUARANTINE_FIXTURES.length + 1);
    QUARANTINE_FIXTURES.forEach((item) => {
      expect(within(table).getByText(item.component)).toBeInTheDocument();
    });
    expect(within(table).getAllByTestId('example-badge')).toHaveLength(QUARANTINE_FIXTURES.length);
    expect(screen.getByRole('region', { name: 'Example quarantine entry' })).toBeInTheDocument();
  });

  it('shows the cached component on the Cached Risk tab', async () => {
    renderView();

    await userEvent.click(screen.getByRole('tab', { name: /^Cached Risk/ }));

    const table = screen.getByRole('table', { name: 'Example cached components' });
    CACHED_RISK_FIXTURES.forEach((item) => {
      expect(within(table).getByText(item.component)).toBeInTheDocument();
      expect(within(table).getByText(item.cve)).toBeInTheDocument();
    });
    expect(within(table).getByText('9,378')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Example cached component' })).toBeInTheDocument();
  });

  it('shows the container and its expandable layer row on the Containers tab', async () => {
    renderView();

    await userEvent.click(screen.getByRole('tab', { name: /^Containers/ }));

    const [container] = CONTAINER_FIXTURES;
    const table = screen.getByRole('table', { name: 'Example quarantined containers' });
    expect(within(table).getByText(container.image)).toBeInTheDocument();
    expect(within(table).getByText('Layer 4 of 7')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: `Collapse ${container.image}` }));

    expect(within(table).queryByText('Layer 4 of 7')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: `Expand ${container.image}` })).toBeInTheDocument();
  });

  it('makes no restClient call', () => {
    const { restClient } = jest.requireMock('../../../../../../../interface/api');

    renderView();

    expect(restClient.get).not.toHaveBeenCalled();
    expect(restClient.post).not.toHaveBeenCalled();
  });
});

describe('QuarantinePreviewView without the Pro upsell', () => {
  it('shows no upgrade prompt or link', () => {
    renderView(false);

    expect(screen.queryByTestId('upgrade-callout')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Upgrade to Nexus Repository/ })).not.toBeInTheDocument();
    expect(screen.getByTestId('example-data-callout')).toBeInTheDocument();
  });
});

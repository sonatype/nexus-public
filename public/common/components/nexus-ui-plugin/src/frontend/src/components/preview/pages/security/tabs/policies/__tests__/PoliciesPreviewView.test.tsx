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
import { Theme } from '@radix-ui/themes';

import { PoliciesPreviewView } from '../PoliciesPreviewView';
import { ProUpsellContext } from '../../ProUpsellContext';
import { POLICY_FIXTURE } from '../policyFixtures';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

const renderView = (proUpsell = true) =>
  render(
    <Theme>
      <ProUpsellContext.Provider value={proUpsell}>
        <PoliciesPreviewView />
      </ProUpsellContext.Provider>
    </Theme>
  );

describe('PoliciesPreviewView', () => {
  it('shows the Pro + info callout and the example-data callout', () => {
    renderView();

    expect(screen.getByTestId('policies-info-callout')).toHaveTextContent(
      'Nexus Repository Pro + adds configurable security policies, age-based rules, and quarantine'
    );
    expect(screen.getByTestId('example-data-callout')).toHaveTextContent(
      'Example data — nothing on this tab reflects your repositories.'
    );
  });

  it('shows the heading and the example policy frame', () => {
    renderView();

    expect(screen.getByRole('heading', { name: /Policy/ })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Example policy' })).toBeInTheDocument();
  });

  it('renders a header row plus the single example policy with a disabled Block switch', () => {
    renderView();

    const table = screen.getByRole('table', { name: 'Example policies' });
    expect(within(table).getAllByRole('row')).toHaveLength(2);
    ['Block', 'Policy Name', 'Type', 'Criteria'].forEach((name) => {
      expect(within(table).getByRole('columnheader', { name })).toBeInTheDocument();
    });
    expect(within(table).getByText(POLICY_FIXTURE.name)).toBeInTheDocument();
    expect(within(table).getByText(POLICY_FIXTURE.criteria)).toBeInTheDocument();
    expect(within(table).getByRole('switch', { name: `Block ${POLICY_FIXTURE.name}` })).toBeDisabled();
    expect(within(table).getByTestId('example-badge')).toBeInTheDocument();
  });

  it('shows the upgrade text and button', () => {
    renderView();

    expect(screen.getByRole('link', { name: 'Upgrade to Nexus Repository Pro +' })).toBeInTheDocument();
    expect(
      screen.getByText('Upgrade to Nexus Repository Pro + to configure the full policy set and apply it to your own repositories.')
    ).toBeInTheDocument();
  });

  it('makes no restClient call', () => {
    const { restClient } = jest.requireMock('../../../../../../../interface/api');

    renderView();

    expect(restClient.get).not.toHaveBeenCalled();
    expect(restClient.post).not.toHaveBeenCalled();
  });
});

describe('PoliciesPreviewView without the Pro upsell', () => {
  it('shows no upgrade prompt or link', () => {
    renderView(false);

    expect(screen.queryByTestId('policies-info-callout')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Upgrade to Nexus Repository/ })).not.toBeInTheDocument();
    expect(screen.getByTestId('example-data-callout')).toBeInTheDocument();
  });
});

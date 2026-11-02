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

import { EvaluationsPreviewView } from '../EvaluationsPreviewView';
import { ProUpsellContext } from '../../ProUpsellContext';
import { EVALUATION_FIXTURE } from '../evaluationFixtures';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

const renderView = (proUpsell = true) =>
  render(
    <Theme>
      <ProUpsellContext.Provider value={proUpsell}>
        <EvaluationsPreviewView />
      </ProUpsellContext.Provider>
    </Theme>
  );

describe('EvaluationsPreviewView', () => {
  it('shows the example-data and upgrade callouts', () => {
    renderView();

    expect(screen.getByTestId('example-data-callout')).toHaveTextContent(
      'Example data — nothing on this tab reflects your repositories.'
    );
    expect(screen.getByTestId('upgrade-callout')).toHaveTextContent(
      'Upgrade to Nexus Repository Pro Plus to run security evaluations against your own repositories and see the findings.'
    );
  });

  it('shows the heading, a disabled Run Evaluation Now button and the example frame', () => {
    renderView();

    expect(screen.getByRole('heading', { name: /Security Evaluations/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Run Evaluation Now' })).toBeDisabled();
    expect(screen.getByRole('region', { name: 'Example evaluation' })).toBeInTheDocument();
  });

  it('renders the single example evaluation with its severity counts', () => {
    renderView();

    const table = screen.getByRole('table', { name: 'Example security evaluations' });
    expect(within(table).getAllByRole('row')).toHaveLength(2);
    [
      'Evaluation Run', 'Vulnerabilities', 'Summary', 'Type', 'Affected Packages', 'Affected Repositories',
    ].forEach((name) => {
      expect(within(table).getByRole('columnheader', { name })).toBeInTheDocument();
    });
    expect(within(table).getByText(EVALUATION_FIXTURE.summary)).toBeInTheDocument();
    expect(within(table).getByTitle('4 Critical CVEs')).toHaveTextContent('4');
    expect(within(table).getByTitle('8 High CVEs')).toHaveTextContent('8');
    expect(within(table).getByTestId('example-badge')).toBeInTheDocument();
  });

  it('shows inert pagination', () => {
    renderView();

    expect(screen.getByText('1-10 of 10')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  it('makes no restClient call', () => {
    const { restClient } = jest.requireMock('../../../../../../../interface/api');

    renderView();

    expect(restClient.get).not.toHaveBeenCalled();
    expect(restClient.post).not.toHaveBeenCalled();
  });
});

describe('EvaluationsPreviewView without the Pro upsell', () => {
  it('shows no upgrade prompt or link', () => {
    renderView(false);

    expect(screen.queryByTestId('upgrade-callout')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Upgrade to Nexus Repository/ })).not.toBeInTheDocument();
    expect(screen.getByTestId('example-data-callout')).toBeInTheDocument();
  });
});

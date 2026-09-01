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
import { Theme } from '@radix-ui/themes';
import { restClient } from '../../../../../../../interface/api';
import { PlanInformationWidget } from '../PlanInformationWidget';

jest.mock('../../../../../../../interface/api', () => ({ restClient: { get: jest.fn() } }));
const mockGet = restClient.get as jest.Mock;
const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('PlanInformationWidget', () => {
  beforeEach(() => jest.clearAllMocks());

  it('shows aggregated counts after a successful load', async () => {
    mockGet.mockResolvedValueOnce({
      items: [
        { state: 'PLANNED', blobStore: 'b1', repository: 'r1',
          configuration: { planStartDate: '2026-03-01', planEndDate: '2026-03-10' } },
        { state: 'EXECUTED', blobStore: 'b2', repository: 'r2',
          configuration: { planStartDate: '2026-01-01', planEndDate: '2026-05-01' } },
      ],
      continuationToken: null,
    });
    renderWithTheme(<PlanInformationWidget />);
    // Use testids: counts are all "2" here, so text matching would be ambiguous.
    expect(await screen.findByTestId('plan-info-plans')).toHaveTextContent('2');
    expect(screen.getByTestId('plan-info-blob-stores')).toHaveTextContent('2');
    expect(screen.getByTestId('plan-info-repositories')).toHaveTextContent('2');
    expect(screen.getByTestId('plan-info-start-date')).not.toHaveTextContent('N/A');
    expect(screen.getByTestId('plan-info-end-date')).not.toHaveTextContent('N/A');
    expect(screen.getByText('Plans')).toBeInTheDocument();
    expect(screen.getByText('Blob stores')).toBeInTheDocument();
    expect(screen.getByText('Repositories')).toBeInTheDocument();
    expect(screen.getByText('Start date')).toBeInTheDocument();
    expect(screen.getByText('End date')).toBeInTheDocument();
  });

  it('shows N/A for every value when there are no active plans', async () => {
    mockGet.mockResolvedValueOnce({ items: [], continuationToken: null });
    renderWithTheme(<PlanInformationWidget />);
    await waitFor(() => expect(screen.getAllByText('N/A').length).toBe(5));
  });

  it('shows an inline error when the fetch fails', async () => {
    mockGet.mockRejectedValueOnce(new Error('boom'));
    renderWithTheme(<PlanInformationWidget />);
    expect(await screen.findByText('boom')).toBeInTheDocument();
    expect(screen.queryByText('N/A')).not.toBeInTheDocument();
  });
});

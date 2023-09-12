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
import axios from 'axios';
import {render, screen, waitForElementToBeRemoved, within} from '@testing-library/react';
import {when} from "jest-when";

import UsageMetrics from './UsageMetrics';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {APIConstants, ExtJS} from '@sonatype/nexus-ui-plugin';

const {USAGE_METRICS} = APIConstants.REST.INTERNAL;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(false),
  },
}));

const data = {
  "usage": [{
    "component_total_count": 791,
    "unique_users_last_30d": 17,
    "request_rates": {
      "peak_requests_per_minute_1d": 42,
      "peak_requests_per_day_30d": 84
    }
  }]
}

const selectors = {
  ...TestUtils.selectors,
  getCard: (t) => screen.getByRole('region', {name: t}),
  getHeading: (t) => screen.getByRole('heading', {name: t}),
  getCardHeader: (c, t) => within(c).getByRole('heading', {name: t}),
  getCardContent: (c, t) => within(c).getByText(t),
};

describe('Usage Metrics', () => {
  it('renders data correctly', async () => {
    when(axios.get).calledWith(USAGE_METRICS).mockResolvedValue({
      data: data
    });

    render(<UsageMetrics />);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getHeading('Usage')).toBeInTheDocument();

    // card 1 of total components
    const card1 = selectors.getCard('total components'),
        card1Header = selectors.getCardHeader(card1, 'Total components'),
        totalComponents = selectors.getCardContent(card1, '791');
    expect(card1).toBeInTheDocument();
    expect(card1Header).toBeInTheDocument();
    expect(totalComponents).toBeInTheDocument();

    // card 2 of unique logins
    const card2 = selectors.getCard('unique logins'),
        card2Header = selectors.getCardHeader(card2,'Unique logins'),
        card2SubTitle = selectors.getCardContent(card2,'Past 30 days'),
        uniqueLogins = selectors.getCardContent(card2, '17');
    expect(card2).toBeInTheDocument();
    expect(card2Header).toBeInTheDocument();
    expect(card2SubTitle).toBeInTheDocument();
    expect(uniqueLogins).toBeInTheDocument();

    // card 3 of peak requests per minute
    const card3 = selectors.getCard('peak requests per minute'),
        card3Header = selectors.getCardHeader(card3, 'Peak requests per minute'),
        card3SubTitle = selectors.getCardContent(card3, 'Past 24 hours'),
        peakReqPerMin = selectors.getCardContent(card3, '42');
    expect(card3).toBeInTheDocument();
    expect(card3Header).toBeInTheDocument();
    expect(card3SubTitle).toBeInTheDocument();
    expect(peakReqPerMin).toBeInTheDocument();

    // card 4 of peak requests per day
    const card4 = selectors.getCard('peak requests per day'),
        card4Header = selectors.getCardHeader(card4, 'Peak requests per day'),
        card4SubTitle = selectors.getCardContent(card4, 'Past 30 days'),
        peakReqPerDay = selectors.getCardContent(card4, '84');
    expect(card4).toBeInTheDocument();
    expect(card4Header).toBeInTheDocument();
    expect(card4SubTitle).toBeInTheDocument();
    expect(peakReqPerDay).toBeInTheDocument();
  });

  it('does not render unique logins card when Pro edition', async () => {
    when(axios.get).calledWith(USAGE_METRICS).mockResolvedValue({
      data: data
    });
    ExtJS.isProEdition.mockReturnValue(true);

    render(<UsageMetrics />);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getHeading('Usage')).toBeInTheDocument();

    expect(selectors.getCard('total components')).toBeInTheDocument();
    expect(selectors.getCard('peak requests per minute')).toBeInTheDocument();
    expect(selectors.getCard('peak requests per day')).toBeInTheDocument();

    expect(() => selectors.getCard('unique logins')).toThrow();
  });
})

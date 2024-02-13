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
import {when} from 'jest-when';

import UsageMetrics from './UsageMetrics';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {APIConstants, ExtJS} from '@sonatype/nexus-ui-plugin';
import {act} from 'react-dom/test-utils';
import {
  METRICS_CONTENT,
  METRICS_CONTENT_WITH_CIRCUIT_BREAKER_OSS,
  METRICS_CONTENT_WITH_CIRCUIT_BREAKER_PRO,
  METRICS_CONTENT_WITH_CIRCUIT_BREAKER_PRO_POSTGRESQL} from './UsageMetrics.testdata';

const {USAGE_METRICS} = APIConstants.REST.INTERNAL;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(false),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
      getUser: jest.fn().mockReturnValue({ administrator: true }),
    }),
  },
}));

const selectors = {
  ...TestUtils.selectors,
  getAllCards: () => screen.getAllByRole('region'),
  queryAllCards: () => screen.queryAllByRole('region'),
  getCard: (t) => screen.getByRole('region', {name: t}),
  getHeading: (t) => screen.getByRole('heading', {name: t}),
  getCardHeader: (c, t) => within(c).getByRole('heading', {name: t}),
  getCardContent: (c, t) => within(c).getByText(t),
  getCardInfoIcon: (c) => c.querySelector('[data-icon="info-circle"]'),
  getCardMeter: (c) => within(c).getByTestId('meter'),
  getCardTextLink: (c) => within(c).queryByRole('link', {name: 'Understand your usage'})
};

describe('Usage Metrics', () => {
  async function renderView(usage = METRICS_CONTENT,
      usageWithCircuitBreaker = METRICS_CONTENT_WITH_CIRCUIT_BREAKER_OSS)
  {
    when(axios.get)
        .calledWith(USAGE_METRICS).mockResolvedValue({data: usage});

    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult')
        .mockReturnValue(usageWithCircuitBreaker);

    render(<UsageMetrics />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  it('renders data correctly', async () => {
    await renderView();

    expect(selectors.getHeading('Usage')).toBeInTheDocument();
    expect(selectors.getAllCards().length).toBe(4);

    // card 1 of total components
    const card1 = selectors.getCard('Total components'),
        card1Header = selectors.getCardHeader(card1, 'Total components'),
        totalComponents = selectors.getCardContent(card1, '7,911,247');

    expectCardToRender(card1, card1Header, totalComponents);

    // card 2 of unique logins
    const card2 = selectors.getCard('Unique logins'),
        card2Header = selectors.getCardHeader(card2,'Unique logins'),
        card2SubTitle = selectors.getCardContent(card2,'Past 30 days'),
        uniqueLogins = selectors.getCardContent(card2, '1,723');

    expectCardToRender(card2, card2Header, card2SubTitle, uniqueLogins);

    // card 3 of peak requests per minute
    const card3 = selectors.getCard('Peak requests per minute'),
        card3Header = selectors.getCardHeader(card3, 'Peak requests per minute'),
        card3SubTitle = selectors.getCardContent(card3, 'Past 24 hours'),
        peakReqPerMin = selectors.getCardContent(card3, '421');

    expectCardToRender(card3, card3Header, card3SubTitle, peakReqPerMin);


    // card 4 of peak requests per day
    const card4 = selectors.getCard('Peak requests per day'),
        card4Header = selectors.getCardHeader(card4, 'Peak requests per day'),
        card4SubTitle = selectors.getCardContent(card4, 'Past 30 days'),
        peakReqPerDay = selectors.getCardContent(card4, '84');

    expectCardToRender(card4, card4Header, card4SubTitle, peakReqPerDay);
  });

  it('does not render unique logins card when Pro edition', async () => {
    ExtJS.isProEdition.mockReturnValue(true);
    await renderView();

    expect(selectors.getHeading('Usage')).toBeInTheDocument();
    expect(selectors.getAllCards().length).toBe(3);

    expect(selectors.getCard('Total components')).toBeInTheDocument();
    expect(selectors.getCard('Peak requests per minute')).toBeInTheDocument();
    expect(selectors.getCard('Peak requests per day')).toBeInTheDocument();
  });

  it("does not render any card when HA mode on", async() =>{
    when(ExtJS.state().getValue)
        .calledWith('nexus.datastore.clustered.enabled')
        .mockReturnValue(true);

    await act(async() => {
      render(<UsageMetrics />);
    });

    expect(selectors.queryAllCards().length).toBe(0);
  });

  describe('Metrics with Circuit Breaker', () => {
    beforeEach( () => {
      when(ExtJS.state().getValue)
          .calledWith('nexus.circuitb.enabled')
          .mockReturnValue(true);

      when(ExtJS.state().getValue)
          .calledWith('nexus.datastore.clustered.enabled')
          .mockReturnValue(false);

      when(ExtJS.state().getValue)
          .calledWith('datastore.isPostgresql')
          .mockReturnValue(false);

      ExtJS.isProEdition.mockReturnValue(false);
    });

    it('renders data correctly when OSS edition', async () => {
      await renderView();

      expect(selectors.getHeading('Usage')).toBeInTheDocument();
      expect(selectors.getAllCards().length).toBe(3);

      // card 1 of total components
      const card1 = selectors.getCard('Total Components'),
          card1Header = selectors.getCardHeader(card1, 'Total Components'),
          card1SubTitle = selectors.getCardContent(card1,'Current'),
          card1LimitTitle = selectors.getCardContent(card1,'Threshold'),
          card1Meter = selectors.getCardMeter(card1),
          card1TextLink = selectors.getCardTextLink(card1),
          totalComponents = selectors.getCardContent(card1, '85,000'),
          componentsLimit = selectors.getCardContent(card1, '100,000'),
          card1HighestRecordedCountTitle = selectors.getCardContent(card1, 'Highest Recorded Count (30 days)'),
          componentsHighestRecordedCount = selectors.getCardContent(card1, '12,500');

      expectCardToRender(
          card1,
          card1Header,
          card1SubTitle,
          card1LimitTitle,
          card1Meter,
          card1TextLink,
          totalComponents,
          componentsLimit,
          card1HighestRecordedCountTitle,
          componentsHighestRecordedCount
      );

      // card 2 of unique logins - no meter and threshold
      const card2 = selectors.getCard('Unique Logins'),
          card2Header = selectors.getCardHeader(card2,'Unique Logins'),
          card2SubTitle = selectors.getCardContent(card2,'Last 24 hours'),
          uniqueLogins24H = selectors.getCardContent(card2, '26'),
          uniqueLogins30DTitle = selectors.getCardContent(card2, 'Last 30 days'),
          uniqueLogins30D = selectors.getCardContent(card2, '52');

      expectCardToRender(
          card2,
          card2Header,
          card2SubTitle,
          uniqueLogins24H,
          uniqueLogins30DTitle,
          uniqueLogins30D
      );

      // card 3 of requests per day
      const card3 = selectors.getCard('Requests Per Day'),
          card3Header = selectors.getCardHeader(card3, 'Requests Per Day'),
          card3SubTitle = selectors.getCardContent(card3, 'Last 24 hours'),
          card3LimitTitle = selectors.getCardContent(card3,'Threshold'),
          card3Meter = selectors.getCardMeter(card3),
          card3TextLink = selectors.getCardTextLink(card3),
          reqsPerDay = selectors.getCardContent(card3, '3,300'),
          reqsLimit = selectors.getCardContent(card3, '20,000'),
          card3HighestRecordedCountTitle = selectors.getCardContent(card3, 'Highest Recorded Count (30 days)'),
          reqsHighestRecordedCount = selectors.getCardContent(card3, '75,000');

      expectCardToRender(
          card3,
          card3Header,
          card3SubTitle,
          card3LimitTitle,
          card3Meter,
          reqsPerDay,
          reqsLimit,
          card3HighestRecordedCountTitle,
          reqsHighestRecordedCount
      );
      // only render link when reaching 75% or more of threshold
      expect(card3TextLink).not.toBeInTheDocument();
    });

    it('renders data correctly when PRO edition', async () => {
      ExtJS.isProEdition.mockReturnValue(true);
      await renderView(METRICS_CONTENT, METRICS_CONTENT_WITH_CIRCUIT_BREAKER_PRO);

      expect(selectors.getHeading('Usage')).toBeInTheDocument();
      expect(selectors.getAllCards().length).toBe(3);

      // card 1 of total components
      const card1 = selectors.getCard('Total Components'),
          card1Header = selectors.getCardHeader(card1, 'Total Components'),
          card1SubTitle = selectors.getCardContent(card1,'Current'),
          card1LimitTitle = selectors.getCardContent(card1,'Threshold'),
          card1Meter = selectors.getCardMeter(card1),
          card1TextLink = selectors.getCardTextLink(card1),
          totalComponents = selectors.getCardContent(card1, '75,000'),
          componentsLimit = selectors.getCardContent(card1, '100,000'),
          card1HighestRecordedCountTitle = selectors.getCardContent(card1, 'Highest Recorded Count (30 days)'),
          componentsHighestRecordedCount = selectors.getCardContent(card1, '66,666');

      expectCardToRender(
          card1,
          card1Header,
          card1SubTitle,
          card1LimitTitle,
          card1Meter,
          card1TextLink,
          totalComponents,
          componentsLimit,
          card1HighestRecordedCountTitle,
          componentsHighestRecordedCount
      );

      // card 2 of requests per minute - no meter and threshold
      const card2 = selectors.getCard('Requests Per Minute'),
          card2Header = selectors.getCardHeader(card2,'Requests Per Minute'),
          card2SubTitle = selectors.getCardContent(card2,'Peak minute in last 24 hours'),
          requestsPerMinute24H = selectors.getCardContent(card2, '1,200'),
          RequestsPerMinute30DTitle = selectors.getCardContent(card2, 'Peak minute in last 30 days'),
          requestsPerMinute30D = selectors.getCardContent(card2, '2,500');

      expectCardToRender(
          card2,
          card2Header,
          card2SubTitle,
          requestsPerMinute24H,
          RequestsPerMinute30DTitle,
          requestsPerMinute30D
      );

      // card 3 of requests per day
      const card3 = selectors.getCard('Requests Per Day'),
          card3Header = selectors.getCardHeader(card3, 'Requests Per Day'),
          card3SubTitle = selectors.getCardContent(card3, 'Last 24 hours'),
          card3LimitTitle = selectors.getCardContent(card3,'Threshold'),
          card3Meter = selectors.getCardMeter(card3),
          card3TextLink = selectors.getCardTextLink(card3),
          reqsPerDay = selectors.getCardContent(card3, '12,500'),
          reqsLimit = selectors.getCardContent(card3, '20,000'),
          card3HighestRecordedCountTitle = selectors.getCardContent(card3, 'Highest Recorded Count (30 days)'),
          reqsHighestRecordedCount = selectors.getCardContent(card3, '95,000');

      expectCardToRender(
          card3,
          card3Header,
          card3SubTitle,
          card3LimitTitle,
          card3Meter,
          reqsPerDay,
          reqsLimit,
          card3HighestRecordedCountTitle,
          reqsHighestRecordedCount
      );
      // only render link when reaching 75% or more of threshold
      expect(card3TextLink).not.toBeInTheDocument();
    });

    it('renders data correctly when Pro edition with Postgresql DB', async () => {
      ExtJS.isProEdition.mockReturnValue(true);
      when(ExtJS.state().getValue)
          .calledWith('datastore.isPostgresql')
          .mockReturnValue(true);
      await renderView(METRICS_CONTENT, METRICS_CONTENT_WITH_CIRCUIT_BREAKER_PRO_POSTGRESQL);

      expect(selectors.getHeading('Usage')).toBeInTheDocument();
      expect(selectors.getAllCards().length).toBe(3);

      // card 1 of total components
      const card1 = selectors.getCard('Total Components'),
          card1Header = selectors.getCardHeader(card1, 'Total Components'),
          totalComponents = selectors.getCardContent(card1, '4,758');

      expectCardToRender(
          card1,
          card1Header,
          totalComponents,
      );

      // card 2 of peak requests per minute
      const card2 = selectors.getCard('Peak Requests Per Minute'),
          card2Header = selectors.getCardHeader(card2, 'Peak Requests Per Minute'),
          card2SubTitle = selectors.getCardContent(card2, 'Past 24 hours'),
          highestReqsPerMinute = selectors.getCardContent(card2, '1,236');

      expectCardToRender(
          card2,
          card2Header,
          card2SubTitle,
          highestReqsPerMinute
      );

      // card 3 of peak requests per day
      const card3 = selectors.getCard('Peak Requests Per Day'),
          card3Header = selectors.getCardHeader(card3, 'Peak Requests Per Day'),
          card3SubTitle = selectors.getCardContent(card3, 'Past 30 days'),
          highestReqsPerDay = selectors.getCardContent(card3, '145,302');

      expectCardToRender(
          card3,
          card3Header,
          card3SubTitle,
          highestReqsPerDay
      );
    });

    it('renders tooltips when hovering on the info icon when OSS edition', async () => {
      await renderView();

      const totalComponentsCard = selectors.getCard('Total Components'),
          uniqueLoginsCard = selectors.getCard('Unique Logins'),
          reqsPerDayCard = selectors.getCard('Requests Per Day');

      let infoIcon = selectors.getCardInfoIcon(totalComponentsCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Sonatype Nexus Repository OSS performs best when your total component counts remain under the threshold.');

      infoIcon = selectors.getCardInfoIcon(uniqueLoginsCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Measures unique users who login over a period of time.');

      infoIcon = selectors.getCardInfoIcon(reqsPerDayCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Sonatype Nexus Repository OSS performs best when requests per day remain under the threshold.');
    });

    it('renders tooltips when hovering on the info icon when PRO edition', async () => {
      ExtJS.isProEdition.mockReturnValue(true);
      await renderView(METRICS_CONTENT, METRICS_CONTENT_WITH_CIRCUIT_BREAKER_PRO);

      const totalComponentsCard = selectors.getCard('Total Components'),
          reqsPerMinuteCard = selectors.getCard('Requests Per Minute'),
          reqsPerDayCard = selectors.getCard('Requests Per Day');

      let infoIcon = selectors.getCardInfoIcon(totalComponentsCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Sonatype Nexus Repository Pro using an embedded database performs best when your total component counts remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.');

      infoIcon = selectors.getCardInfoIcon(reqsPerMinuteCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Measures requests per minute to your Sonatype Nexus Repository Pro instance.');

      infoIcon = selectors.getCardInfoIcon(reqsPerDayCard);
      await TestUtils.expectToSeeTooltipOnHover(infoIcon,
          'Sonatype Nexus Repository Pro using an embedded database performs best when your requests per day remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.');
    });
  });
});

function expectCardToRender(card, ...cardItems) {
  expect(card).toBeInTheDocument();

  for (let i of cardItems) {
    expect(i).toBeInTheDocument();
  }
};

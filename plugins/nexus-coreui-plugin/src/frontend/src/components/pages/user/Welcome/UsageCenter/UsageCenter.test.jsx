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
import {render, screen, within} from '@testing-library/react';
import {when} from 'jest-when';

import UsageCenter from './UsageCenter';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {act} from 'react-dom/test-utils';
import {
  USAGE_CENTER_CONTENT_CE,
  USAGE_CENTER_CONTENT_PRO} from './UsageCenter.testdata';

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
      getEdition: jest.fn().mockReturnValue('COMMUNITY')
    }),
    useState: jest.fn()
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
  getStatusIndicator: () => screen.getByRole('status')
};

describe('Usage Center', () => {
  async function renderView(usage = USAGE_CENTER_CONTENT_CE)
  {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult', [])
        .mockReturnValue(usage);

    when(ExtJS.state().getValue)
        .calledWith('nexus.community.componentCountLimitDateLastExceeded')
        .mockReturnValue('2024-11-01T00:00:00.000');

    when(ExtJS.state().getValue)
        .calledWith('nexus.community.requestPer24HoursLimitDateLastExceeded')
        .mockReturnValue('2024-11-01T00:00:00.000');

    when(ExtJS.state().getValue)
        .calledWith('nexus.datastore.clustered.enabled')
        .mockReturnValue(false);

    return render(<UsageCenter />);
  }

  it("does not render any card when HA mode on", async() =>{
    when(ExtJS.state().getValue)
        .calledWith('nexus.datastore.clustered.enabled')
        .mockReturnValue(true);

    await act(async() => {
      render(<UsageCenter />);
    });

    expect(selectors.queryAllCards().length).toBe(0);
  });

  it('renders tooltips when hovering on the info icon when Community edition', async () => {
    ExtJS.isProEdition.mockReturnValue(false);
    ExtJS.state().getEdition.mockReturnValue('COMMUNITY');
    await renderView();

    const totalComponentsCard = selectors.getCard('Total Components'),
        uniqueLoginsCard = selectors.getCard('Unique Logins'),
        reqsPerDayCard = selectors.getCard('Requests Per Day');

    let infoIcon = selectors.getCardInfoIcon(totalComponentsCard);
    await TestUtils.expectToSeeTooltipOnHover(infoIcon,
        'Community Edition tracks the total components stored in this instance. If usage exceeds the 100,000 component limit, the date will be displayed, and write restrictions will apply until usage is reduced.');

    infoIcon = selectors.getCardInfoIcon(uniqueLoginsCard);
    await TestUtils.expectToSeeTooltipOnHover(infoIcon,
        'Unique successful logins to this Sonatype Nexus Repository instance in the last 30 days.');

    infoIcon = selectors.getCardInfoIcon(reqsPerDayCard);
    await TestUtils.expectToSeeTooltipOnHover(infoIcon,
        'Community Edition tracks the total daily requests to this instance. If usage exceeds the 200,000 request limit, the date will be displayed, and write restrictions will apply until usage is reduced.');
  });

  describe('Pro edition', () => {

    beforeEach(() => {
      ExtJS.isProEdition.mockReturnValue(true);
      ExtJS.state().getEdition.mockReturnValue('PRO');
    });

    it('renders data correctly', async () => {
      await renderView(USAGE_CENTER_CONTENT_PRO);

      expect(selectors.getHeading('Monitor this instance\'s usage to optimize your deployments. Usage Metrics Overview')).toBeInTheDocument();
      expect(selectors.getAllCards().length).toBe(3);

      // card 1 of total components
      const card1 = selectors.getCard('Total Components'),
          card1Header = selectors.getCardHeader(card1, 'Total Components'),
          totalComponents = selectors.getCardContent(card1, '4,758');

      expectCardToRender(card1, card1Header, totalComponents);

      // card 3 of peak requests per minute
      const card2 = selectors.getCard('Peak Requests Per Minute'),
          card2Header = selectors.getCardHeader(card2, 'Peak Requests Per Minute'),
          card2SubTitle = selectors.getCardContent(card2, 'Past 24 hours'),
          peakReqPerMin = selectors.getCardContent(card2, '1,236');

      expectCardToRender(card2, card2Header, card2SubTitle, peakReqPerMin);

      // card 3 of peak requests per day
      const card3 = selectors.getCard('Peak Requests Per Day'),
        card3Header = selectors.getCardHeader(card3, 'Peak Requests Per Day'),
        card3SubTitle = selectors.getCardContent(card3, 'Past 30 days'),
        peakReqPerDay = selectors.getCardContent(card3, '145,302');

      expectCardToRender(card3, card3Header, card3SubTitle, peakReqPerDay);
    });

    it('does not render unique logins card when Pro edition', async () => {
      await renderView(USAGE_CENTER_CONTENT_PRO);

      expect(selectors.getAllCards().length).toBe(3);

      expect(selectors.getCard('Total Components')).toBeInTheDocument();
      expect(selectors.getCard('Peak Requests Per Minute')).toBeInTheDocument();
      expect(selectors.getCard('Peak Requests Per Day')).toBeInTheDocument();
    });
  });

  describe('Community edition', () => {

    beforeEach(() => {
      ExtJS.isProEdition.mockReturnValue(false);
      ExtJS.state().getEdition.mockReturnValue('COMMUNITY');

      const date = new Date('2024-12-02');
      jest.useFakeTimers().setSystemTime(date);
    });

    it("renders text and status indicator when usage is under limits", async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue('Under limits');
  
      await renderView();
  
      expect(selectors.getHeading('Monitor this instance\'s usage to ensure your deployment is appropriate for your needs. Learn more about the usage center . Usage Metrics Overview')).toBeInTheDocument();
      expect(selectors.getStatusIndicator()).toHaveTextContent('Usage below limits');
    });

    it("renders text and status indicator when usage is nearing limits", async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue('75% usage');    
  
      await renderView();
  
      expect(selectors.getHeading('Monitor this instance\'s usage to ensure your deployment is appropriate for your needs. Learn more about the usage center . Usage Metrics Overview')).toBeInTheDocument();
      expect(selectors.getStatusIndicator()).toHaveTextContent('Usage nearing limits')
    });

    it("renders text and status indicator when usage is over limits", async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue('Over limits');
  
      await renderView();
  
      expect(selectors.getHeading('Monitor this instance\'s usage to ensure your deployment is appropriate for your needs. Learn more about the usage center . Usage Metrics Overview')).toBeInTheDocument();
      expect(selectors.getStatusIndicator()).toHaveTextContent('Usage over limits')
    });

    it('renders data correctly', async () => {
      await renderView();
  
      expect(selectors.getAllCards().length).toBe(3);
  
      // card 1 of total components
      const card1 = selectors.getCard('Total Components'),
          card1Header = selectors.getCardHeader(card1, 'Total Components'),
          card1SubTitle = selectors.getCardContent(card1,'Current'),
          card1ThresholdTitle = selectors.getCardContent(card1,'Usage Limit'),
          card1Meter = selectors.getCardMeter(card1),
          totalComponents = selectors.getCardContent(card1, '85,000'),
          componentsThreshold = selectors.getCardContent(card1, '100,000'),
          card1HighestRecordedCountTitle = selectors.getCardContent(card1, 'Highest Recorded Count (30 days)'),
          componentsHighestRecordedCount = selectors.getCardContent(card1, '12,500'),
          card1LastExceededDateLabel = selectors.getCardContent(card1, 'Last time over the usage limit'),
          card1LastExceededDate = selectors.getCardContent(card1, 'Nov 1, 2024, 12:00 AM');
  
      expectCardToRender(
          card1,
          card1Header,
          card1SubTitle,
          card1ThresholdTitle,
          card1Meter,
          totalComponents,
          componentsThreshold,
          card1HighestRecordedCountTitle,
          componentsHighestRecordedCount,
          card1LastExceededDateLabel,
          card1LastExceededDate
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
          card3ThresholdTitle = selectors.getCardContent(card3,'Usage Limit'),
          card3Meter = selectors.getCardMeter(card3),
          reqsPerDay = selectors.getCardContent(card3, '3,300'),
          reqsThreshold = selectors.getCardContent(card3, '200,000'),
          card3HighestRecordedCountTitle = selectors.getCardContent(card3, 'Highest Recorded Count (30 days)'),
          reqsHighestRecordedCount = selectors.getCardContent(card3, '75,000'),
          card3LastExceededDateLabel = selectors.getCardContent(card3, 'Last time over the usage limit'),
          card3LastExceededDate = selectors.getCardContent(card3, 'Nov 1, 2024, 12:00 AM');
  
      expectCardToRender(
          card3,
          card3Header,
          card3SubTitle,
          card3ThresholdTitle,
          card3Meter,
          reqsPerDay,
          reqsThreshold,
          card3HighestRecordedCountTitle,
          reqsHighestRecordedCount,
          card3LastExceededDateLabel,
          card3LastExceededDate
      );
    });
  });
});

function expectCardToRender(card, ...cardItems) {
  expect(card).toBeInTheDocument();

  for (let i of cardItems) {
    expect(i).toBeInTheDocument();
  }
};

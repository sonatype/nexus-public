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
import { within } from '@testing-library/react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import WelcomeStrings from '../../../constants/pages/user/WelcomeStrings';
import {
  assertCanDismissTheBanner,
  assertCommunityEditionLimitMessageShowing, assertHasLinkWithText,
  renderView
} from './SystemNotices.testutils';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

// Note: Using global mock from setup.js and configuring in beforeEach
describe('SystemNotices -- CEHardLimitAlert', () => {
  beforeEach(() => {
    const date = new Date('2024-12-02T00:00:00');
    jest.useFakeTimers().setSystemTime(date);
    
    // Configure ExtJS for Community Edition mode using spyOn
    jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(false);
    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: true });
    jest.spyOn(ExtJS, 'useState').mockImplementation(() => undefined);
    jest.spyOn(ExtJS, 'usePermission').mockReturnValue(true);
    
    // Configure state mock for Community Edition
    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getValue: jest.fn(),
      getEdition: jest.fn().mockReturnValue('COMMUNITY'),
    });
  });
  
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should render the nearing limits banner (NEAR_LIMITS_NEVER_IN_GRACE)', async () => {
    renderView('', '75% usage');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.NEAR_LIMITS;
    const expectedTitle = WelcomeStrings.WELCOME.USAGE.BANNERS.NEAR_LIMITS_TITLE;

    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage, expectedTitle)
    assertHasContactAndReviewUsageLinks(alert);
    await assertCanDismissTheBanner(alert);
  });

  it('should render the over limits banner inside grace period (OVER_LIMITS_IN_GRACE)', async () => {
    renderView('2024-12-15T00:00:00.000', 'Over limits');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.OVER_LIMIT_IN_GRACE('December 15, 2024');
    const expectedTitle = '13 Days Remaining';

    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage, expectedTitle);
    assertHasContactAndReviewUsageLinks(alert);
    await assertCanDismissTheBanner(alert);
  });

  it('should render the over limits banner outside of grace period (OVER_LIMITS_GRACE_PERIOD_ENDED)', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.OVER_LIMIT_END_GRACE;

    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage)
    assertHasContactAndReviewUsageLinks(alert);
    await assertCanDismissTheBanner(alert);
  });

  it('should render the near limits banner outside grace period (BELOW_LIMITS_GRACE_PERIOD_ENDED)', async () => {
    renderView('2024-10-15T00:00:00.000', '75% usage');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.BELOW_LIMIT_END_GRACE;

    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage)
    assertHasContactAndReviewUsageLinks(alert);
    await assertCanDismissTheBanner(alert);
  });

  it('should render the over limits banner outside of grace period as non-admin (NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED)', async () => {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

    renderView('2024-10-15T00:00:00.000', 'Over limits');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.THROTTLING_NON_ADMIN;
    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage)

    assertHasLinkWithText(
        alert,
        'Learn about Nexus Repository Community Edition',
        'http://links.sonatype.com/products/nxrm3/learn-about-community-edition?utm_medium=product&utm_source=nexus_repo_community&utm_campaign=repo_community_usage'
    );
    await assertCanDismissTheBanner(alert);
  });

  it('should render the nearing limits banner as non-admin (NEAR_LIMITS_NON_ADMIN)', async () => {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

    renderView('', '75% usage');

    const expectedMessage = WelcomeStrings.WELCOME.USAGE.BANNERS.NEARING_NON_ADMIN;
    const alert = assertCommunityEditionLimitMessageShowing(expectedMessage)

    assertHasLinkWithText(
        alert,
        'Learn about Nexus Repository Community Edition',
        'http://links.sonatype.com/products/nxrm3/learn-about-community-edition?utm_medium=product&utm_source=nexus_repo_community&utm_campaign=repo_community_usage'
    );
    await assertCanDismissTheBanner(alert);
  });

  function assertHasReviewUsageLink(alert) {
    expect(within(alert).getByRole('button', { name: 'Review your usage' }))
  }

  function assertHasContactAndReviewUsageLinks(alert) {
    assertHasLinkWithText(
        alert,
        'purchase a license to remove limits.',
        'http://links.sonatype.com/products/nxrm3/ce/purchase-license?nodeId=node-example-id&componentCountLimit=40000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=100000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&malwareCount=3'
    );
    assertHasReviewUsageLink(alert);
  }
});

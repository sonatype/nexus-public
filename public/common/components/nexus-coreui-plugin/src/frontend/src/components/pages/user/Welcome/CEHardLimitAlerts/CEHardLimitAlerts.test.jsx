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
import {render, screen} from '@testing-library/react';
import {when} from 'jest-when';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import CEHardLimitAlerts from './CEHardLimitAlerts';
import {USAGE_CENTER_CONTENT_CE} from '../UsageCenter/UsageCenter.testdata';

import {helperFunctions} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

const {
  useGracePeriodEndsDate,
  useThrottlingStatusValue,
  useEdition
} = helperFunctions;

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
      getEdition: jest.fn().mockReturnValue('COMMUNITY')
    }),
    useState: jest.fn(),
    useUser: jest.fn().mockReturnValue({ administrator: true })
  },
}));

describe('CEHardLimitAlerts', () => {
  async function renderView(gracePeriodEnd, throttlingStatus)
  {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult', [])
        .mockReturnValue(USAGE_CENTER_CONTENT_CE);

    when(ExtJS.state().getValue)
        .calledWith('nexus.node.id')
        .mockReturnValue('node-example-id');

    when(ExtJS.state().getValue)
        .calledWith('nexus.datastore.clustered.enabled')
        .mockReturnValue(false);

    when(ExtJS.state().getValue)
        .calledWith('nexus.malware.count')
        .mockReturnValue({totalCount: 3});

    // Use mockImplementation to handle all useState calls, including
    // useEdition which is passed as a function reference
    ExtJS.useState.mockImplementation((arg) => {
      if (arg === useThrottlingStatusValue) return throttlingStatus;
      if (arg === useGracePeriodEndsDate) return new Date(gracePeriodEnd);
      // For function arguments (like useEdition), call the function
      if (typeof arg === 'function') return arg();
      return arg;
    });

    return render(<CEHardLimitAlerts onClose={jest.fn()} />);
  }

  beforeEach(() => {
    // Default to a logged-in admin so each test starts from a known user state
    // (a prior test overriding useUser to null must not leak into the next).
    ExtJS.useUser.mockReturnValue({ administrator: true });
    const date = new Date('2024-12-02');
    jest.useFakeTimers().setSystemTime(date);
  });

  it('does not render any alert when there is no logged-in user (anonymous)', async () => {
    ExtJS.useUser.mockReturnValue(null);

    // Over limits + grace period ended: for a logged-in non-admin this resolves
    // to NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED and renders the error alert.
    // Anonymous users must see nothing at all.
    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.ce-alerts')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('should render the correct links', () => {
    renderView('', '75% usage');

    const learnMoreButton = screen.getByRole('link', {name: 'Learn More'});
    const uploadLicense = screen.getByRole('link', {name: 'upload it here'});

    expect(learnMoreButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/ce/learn-more?nodeId=node-example-id&componentCountLimit=40000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=100000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&malwareCount=3');
    expect(uploadLicense).toHaveAttribute('href', '#admin/system/licensing');
  });

  it('should render the nearing limits banner with warning styling and a tertiary Learn More CTA', async () => {
    const {container} = await renderView('', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Instance Trending Toward Usage LimitsOnce limits are reached, new components cannot be added. Purchase a license to remove limits, or if you have already purchased a license upload it here.Learn More');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();

    // 75% approaching limits state uses warning styling
    expect(container.querySelector('.nx-alert--warning')).toBeInTheDocument();
    expect(container.querySelector('.nx-alert--error')).not.toBeInTheDocument();

    // "Learn More" CTA uses surface (tertiary) styling
    expect(screen.getByRole('link', {name: 'Learn More'})).toHaveClass('nx-btn', 'nx-btn--tertiary');
  });

  it('should render the over limits banner outside of grace period with error styling and tertiary/primary CTAs', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');
    const getPurchaseNowButton = screen.getByRole('link', {name: 'Purchase Now'});
    const getLearnMoreLimitsEnforcedButton = screen.getByRole('link', {name: 'Learn More'});

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage Limits In EffectUsage limits came into effect on October 15, 2024. As usage levels are currently higher than the Nexus Repository Community Edition maximum, new components can no longer be added to this instance. Purchase a license to remove limits, or if you have already purchased a license upload it here.Learn MorePurchase Now');
    expect(getPurchaseNowButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/ce/purchase-license?nodeId=node-example-id&componentCountLimit=40000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=100000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&malwareCount=3');
    expect(getLearnMoreLimitsEnforcedButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/ce/learn-more-limits-enforced?nodeId=node-example-id&componentCountLimit=40000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=100000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&malwareCount=3');

    // Over limits, write restricted uses error styling
    expect(container.querySelector('.nx-alert--error')).toBeInTheDocument();
    expect(container.querySelector('.nx-alert--warning')).not.toBeInTheDocument();

    // "Learn More" is tertiary (surface); "Purchase Now" is primary (blue/white)
    expect(getLearnMoreLimitsEnforcedButton).toHaveClass('nx-btn', 'nx-btn--tertiary');
    expect(getPurchaseNowButton).toHaveClass('nx-btn', 'nx-btn--primary');
  });

  it('should render the over limits banner inside grace period with warning styling and a tertiary Learn More CTA', async () => {
    const {container} = await renderView('2024-12-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage Limits Will Be Enforced Starting December 15, 2024Starting December 15, 2024, new components cannot be added. Purchase a license to remove limits, or if you have already purchased a license upload it here.Learn More');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();

    // Over limits grace period uses warning styling, not error
    expect(container.querySelector('.nx-alert--warning')).toBeInTheDocument();
    expect(container.querySelector('.nx-alert--error')).not.toBeInTheDocument();

    // "Learn More" CTA uses surface (tertiary) styling
    expect(screen.getByRole('link', {name: 'Learn More'})).toHaveClass('nx-btn', 'nx-btn--tertiary');
  });

  it('should render the under limits banner inside grace period', async () => {
    const {container} = await renderView('2024-12-15T00:00:00.000', 'Under limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage limits take effect on December 15, 2024. When the usage exceeds the Nexus Repository Community Edition maximum, new components can no longer be added to this instance. Purchase a license to remove limits, or if you have already purchased a license upload it here.');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });

  it('should render the near limits banner outside grace period', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Instance Trending Toward Usage LimitsIf you exceed usage limits, you will not be able to add new components. Purchase a license to remove limits, or if you have already purchased a license upload it here.Learn More');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });
});

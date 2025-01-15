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

import {helperFunctions} from '../../../../widgets/CELimits/UsageHelper';

const {
  useGracePeriodEndsDate,
  useThrottlingStatusValue
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

    when(ExtJS.useState)
      .calledWith(useThrottlingStatusValue)
      .mockReturnValue(throttlingStatus);

    when(ExtJS.useState)
      .calledWith(useGracePeriodEndsDate)
      .mockReturnValue(new Date(gracePeriodEnd));

    return render(<CEHardLimitAlerts onClose={jest.fn()} />);
  }

  beforeEach(() => {
    const date = new Date('2024-12-02');
    jest.useFakeTimers().setSystemTime(date);
  });

  it('should render the correct links', () => {
    renderView('', '75% usage');

    const getStartedButton = screen.getByRole('link', {name: 'Get Started'});
    const uploadLicense = screen.getByRole('link', {name: 'upload it here'});

    expect(getStartedButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/pricing?nodeId=node-example-id&componentCountLimit=100000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=200000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&utm_medium=product&utm_source=nexus_repo_community&utm_campaign=repo_community_usage&malwareCount=3');
    expect(uploadLicense).toHaveAttribute('href', '#admin/system/licensing');
  });

  it('should render the nearing limits banner', async () => {
    const {container} = await renderView('', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Instance Trending Toward Usage LimitsOnce limits are reached, new components cannot be added. Purchase a license to remove limits, or if you have already purchased a license upload it here.Get Started');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });

  it('should render the over limits banner outside of grace period', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');
    const getPurchaseNowButton = screen.getByRole('link', {name: 'Purchase Now'});
    const getRestoreUsageButton = screen.getByRole('link', {name: 'How to Restore Usage'});

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage Limits In EffectUsage limits came into effect on October 15, 2024. As usage levels are currently higher than the Nexus Repository Community Edition maximum, new components can no longer be added to this instance. Purchase a license to remove limits, or if you have already purchased a license upload it here.How to Restore UsagePurchase Now');
    expect(getPurchaseNowButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/pricing?nodeId=node-example-id&componentCountLimit=100000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=200000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&utm_medium=product&utm_source=nexus_repo_community&utm_campaign=repo_community_usage&malwareCount=3');
    expect(getRestoreUsageButton).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/how-to-restore-usage?utm_medium=product&utm_source=nexus_repo_community&utm_campaign=repo_community_usage');
  });

  it('should render the over limits banner inside grace period', async () => {
    const {container} = await renderView('2024-12-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage Limits Will Be Enforced Starting December 15, 2024Starting December 15, 2024, new components cannot be added. Purchase a license to remove limits, or if you have already purchased a license upload it here.Get Started');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });

  it('should render the under limits banner inside grace period', async () => {
    const {container} = await renderView('2024-12-15T00:00:00.000', 'Under limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Usage limits take effect on December 15, 2024. When the usage exceeds the Nexus Repository Community Edition maximum, new components can no longer be added to this instance. Purchase a license to remove limits, or if you have already purchased a license upload it here.');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });

  it('should render the near limits banner outside grace period', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('Instance Trending Toward Usage LimitsIf you exceed usage limits, you will not be able to add new components. Purchase a license to remove limits, or if you have already purchased a license upload it here.Get Started');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });
});

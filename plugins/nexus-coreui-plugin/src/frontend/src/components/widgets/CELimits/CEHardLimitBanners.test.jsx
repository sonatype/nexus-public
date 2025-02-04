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

import CEHardLimitBanners from './CEHardLimitBanners';
import {USAGE_CENTER_CONTENT_CE} from '../../pages/user/Welcome/UsageCenter/UsageCenter.testdata';

import {helperFunctions} from './UsageHelper';

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
      getUser: jest.fn().mockReturnValue({ administrator: true }),
      getEdition: jest.fn().mockReturnValue('COMMUNITY')
    }),
    useState: jest.fn(),
    useUser: jest.fn().mockReturnValue({ administrator: true })
  },
}));


describe('CEHardLimitBanners', () => {
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

    return render(<CEHardLimitBanners onClose={jest.fn()} />);
  }

  beforeEach(() => {
    const date = new Date('2024-12-02T00:00:00');
    jest.useFakeTimers().setSystemTime(date);
  });

  it('should render the correct link', () => {
    renderView('', '75% usage');

    const viewPricingLink = screen.getByRole('link', {name: 'purchase a license to remove limits.'});

    expect(viewPricingLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/ce/purchase-license?nodeId=node-example-id&componentCountLimit=100000&componentCountMax=12500&componentCount=85000&requestsPer24HoursLimit=200000&requestsPer24HoursMax=75000&requestsPer24HoursCount=3300&malwareCount=3');
  });

  it('should render the nearing limits banner', async () => {
    const {container} = await renderView('', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('This instance of Nexus Repository Community Edition is trending toward its usage limit. Once limits are reached, new components cannot be added. Review your usage or purchase a license to remove limits.');
    expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
  });

  it('should render the over limits banner outside of grace period', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('This instance of Nexus Repository Community Edition has exceeded its usage limit. New components can no longer be added. Review your usage or purchase a license to remove limits.');
  });

  it('should render the over limits banner inside grace period', async () => {
    const {container} = await renderView('2024-12-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('13 Days RemainingThis instance of Nexus Repository Community Edition has exceeded its usage limit. Limits will be enforced starting December 15, 2024, when new components can no longer be added. Review your usage or purchase a license to remove limits.');
  });

  it('should render the near limits banner outside grace period', async () => {
    const {container} = await renderView('2024-10-15T00:00:00.000', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('If this instance of Nexus Repository Community Edition exceeds usage limits, you will not be able to add new components. Review your usage or purchase a license to remove limits.');
  });

  it('should render the over limits banner outside of grace period as non-admin', async () => {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

    const {container} = await renderView('2024-10-15T00:00:00.000', 'Over limits');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('This instance of Nexus Repository Community Edition has exceeded its usage limit. New components can no longer be added. Talk to your repository administrator. Learn about Nexus Repository Community Edition.');
  });

  it('should render the nearing limits banner as non-admin', async () => {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue({ administrator: false });

    const {container} = await renderView('', '75% usage');

    expect(container.querySelector('.nx-alert')).toHaveTextContent('This instance of Nexus Repository Community Edition is trending toward its usage limit. Once limits are reached, new components cannot be added. Talk to your repository administrator. Learn about Nexus Repository Community Edition.');
  });
});

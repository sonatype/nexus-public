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
import {render, screen, fireEvent} from '@testing-library/react';
import CELimitsAlert from '../CELimitsAlert';

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn(),
    state: jest.fn(),
    useState: jest.fn((getValue: () => unknown) => getValue()),
  },
}));
jest.mock('../../../../../interface/LocationUtils', () => ({
  scrollToUsageCenter: jest.fn(),
}));
jest.mock('../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper', () => ({
  helperFunctions: {
    useThrottlingStatus: jest.fn(),
    useGracePeriodEndDate: jest.fn(() => 'May 30, 2026'),
    useDaysUntilGracePeriodEnds: jest.fn(() => 5),
    useViewPurchaseALicenseUrl: jest.fn(() => 'http://example/purchase'),
    buildLearnMoreUrl: jest.fn(() => 'http://example/learn'),
  },
}));

import {ExtJS} from '../../../../../interface/ExtJS';
import {helperFunctions} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

const mockStatus = helperFunctions.useThrottlingStatus as jest.Mock;
const mockUser = ExtJS.useUser as jest.Mock;
const mockState = ExtJS.state as jest.Mock;
const stateValue = {
  getValue: jest.fn().mockReturnValue(false), // HA off
  getEdition: jest.fn().mockReturnValue('COMMUNITY'),
};

describe('CELimitsAlert', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUser.mockReturnValue({administrator: true});
    mockState.mockReturnValue(stateValue);
    stateValue.getValue.mockReturnValue(false);
    stateValue.getEdition.mockReturnValue('COMMUNITY');
    document.cookie = 'under_end_grace=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
  });

  it('renders warning tier with title for NEAR_LIMITS_NEVER_IN_GRACE', () => {
    mockStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
    render(<CELimitsAlert />);
    expect(screen.getByText('Approaching Usage Limits')).toBeInTheDocument();
    expect(screen.getByText(/trending toward its usage limit/)).toBeInTheDocument();
    expect(screen.getByText('Review your usage')).toBeInTheDocument();
    expect(screen.getByText('purchase a license to remove limits.')).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert--warning')).not.toBeNull();
  });

  it('renders error tier with days-remaining title for OVER_LIMITS_IN_GRACE', () => {
    mockStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
    render(<CELimitsAlert />);
    expect(screen.getByText('5 Days Remaining')).toBeInTheDocument();
    expect(screen.getByText(/Limits will be enforced starting May 30, 2026/)).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert--error')).not.toBeNull();
  });

  it('renders error tier "Usage Limits In Effect" for OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
    mockStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
    render(<CELimitsAlert />);
    expect(screen.getByText('Usage Limits In Effect')).toBeInTheDocument();
    expect(screen.getByText(/New components can no longer be added\./)).toBeInTheDocument();
  });

  it('renders dismissible warning (no title) for BELOW_LIMITS_GRACE_PERIOD_ENDED and hides + sets cookie on dismiss', () => {
    mockStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
    render(<CELimitsAlert />);
    expect(screen.getByText(/If this instance of Nexus Repository Community Edition exceeds usage limits/)).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert__title')).toBeNull();
    fireEvent.click(screen.getByLabelText('Dismiss alert'));
    expect(screen.queryByText(/If this instance of Nexus Repository/)).toBeNull();
    expect(document.cookie).toContain('under_end_grace=dismissed');
  });

  it('does not render BELOW_LIMITS banner when dismiss cookie is already set', () => {
    document.cookie = 'under_end_grace=dismissed; path=/';
    mockStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
    const {container} = render(<CELimitsAlert />);
    expect(container.querySelector('[data-testid="nxrm-system-alert"]')).toBeNull();
  });

  it('renders non-admin error banner with learn-more link for NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
    mockUser.mockReturnValue({administrator: false});
    mockStatus.mockReturnValue('NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED');
    render(<CELimitsAlert />);
    expect(screen.getByText(/Talk to your repository administrator/)).toBeInTheDocument();
    expect(screen.getByText('Learn about Nexus Repository Community Edition')).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert--error')).not.toBeNull();
  });

  it('renders non-admin warning banner for NEAR_LIMITS_NON_ADMIN', () => {
    mockUser.mockReturnValue({administrator: false});
    mockStatus.mockReturnValue('NEAR_LIMITS_NON_ADMIN');
    render(<CELimitsAlert />);
    expect(screen.getByText(/trending toward its usage limit/)).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert--warning')).not.toBeNull();
  });

  it('renders null when NO_THROTTLING', () => {
    mockStatus.mockReturnValue('NO_THROTTLING');
    const {container} = render(<CELimitsAlert />);
    expect(container.querySelector('[data-testid="nxrm-system-alert"]')).toBeNull();
  });

  it('renders null for BELOW_LIMITS_IN_GRACE (intentional no-op)', () => {
    mockStatus.mockReturnValue('BELOW_LIMITS_IN_GRACE');
    const {container} = render(<CELimitsAlert />);
    expect(container.querySelector('[data-testid="nxrm-system-alert"]')).toBeNull();
  });

  it('renders null when not Community Edition', () => {
    stateValue.getEdition.mockReturnValue('PRO');
    mockStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
    const {container} = render(<CELimitsAlert />);
    expect(container.querySelector('[data-testid="nxrm-system-alert"]')).toBeNull();
  });

  it('renders null in HA mode', () => {
    stateValue.getValue.mockReturnValue(true); // clustered.enabled = true
    mockStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
    const {container} = render(<CELimitsAlert />);
    expect(container.querySelector('[data-testid="nxrm-system-alert"]')).toBeNull();
  });
});

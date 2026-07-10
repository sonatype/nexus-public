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
import CELimitsAlerts from '../CELimitsAlerts';

// Mock ExtJS - relative path from __tests__ to interface (6 levels up)
jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn(),
    state: jest.fn(),
  },
}));

// Mock UsageHelper - relative path from __tests__ to widgets
jest.mock('../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper', () => ({
  helperFunctions: {
    useThrottlingStatus: jest.fn(),
    useGracePeriodEndDate: jest.fn(() => 'May 30, 2026'),
    useDaysUntilGracePeriodEnds: jest.fn(() => 5),
    useViewPurchaseALicenseUrl: jest.fn(() => 'http://links.sonatype.com/products/nxrm3/ce/purchase-license'),
    useViewLearnMoreUrl: jest.fn(() => 'http://links.sonatype.com/products/nxrm3/learn-about-community-edition'),
  },
}));

import {ExtJS} from '../../../../../interface/ExtJS';
import {helperFunctions} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

const mockUseThrottlingStatus = helperFunctions.useThrottlingStatus as jest.Mock;
const mockUseUser = ExtJS.useUser as jest.Mock;
const mockState = ExtJS.state as jest.Mock;

const mockStateValue = {
 getValue: jest.fn(),
  getEdition: jest.fn().mockReturnValue('COMMUNITY'),
};

describe('CELimitsAlerts', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseUser.mockReturnValue({administrator: true});
    mockState.mockReturnValue(mockStateValue);
    mockStateValue.getValue.mockReturnValue(false); // HA mode off
    document.cookie = '';
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('Admin Banners', () => {
    it('renders soft limit warning banner for NEAR_LIMITS_NEVER_IN_GRACE', () => {
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      render(<CELimitsAlerts />);
      expect(screen.getByText(/trending toward its usage limit/)).toBeInTheDocument();
      expect(screen.getByText('Review your usage')).toBeInTheDocument();
      expect(screen.getByText('purchase a license to remove limits.')).toBeInTheDocument();
    });

    it('renders hard limit in grace banner for OVER_LIMITS_IN_GRACE', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      render(<CELimitsAlerts />);
      expect(screen.getByText('5 Days Remaining')).toBeInTheDocument();
      expect(screen.getByText(/exceeded its usage limit/)).toBeInTheDocument();
    });

    it('renders hard limit grace ended banner for OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      expect(screen.getByText('Usage Limits In Effect')).toBeInTheDocument();
      expect(screen.getByText(/New components can no longer be added/)).toBeInTheDocument();
    });

    it('renders dismissible below limit banner for BELOW_LIMITS_GRACE_PERIOD_ENDED', () => {
      mockUseThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      expect(screen.getByText(/you will not be able to add new components/)).toBeInTheDocument();
      expect(screen.getByRole('button', {name: 'Dismiss'})).toBeInTheDocument();
    });

    it('dismisses below limit banner when dismiss button clicked', () => {
      mockUseThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      const dismissButton = screen.getByRole('button', {name: 'Dismiss'});
      fireEvent.click(dismissButton);
      expect(screen.queryByText(/you will not be able to add new components/)).not.toBeInTheDocument();
    });

    it('does not render below limit banner if dismissed', () => {
      document.cookie = 'under_end_grace=dismissed';
      mockUseThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      expect(screen.queryByText(/you will not be able to add new components/)).not.toBeInTheDocument();
    });
  });

  describe('Non-Admin Banners', () => {
    beforeEach(() => {
      mockUseUser.mockReturnValue({administrator: false});
    });

    it('renders non-admin over limit banner for NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
      mockUseThrottlingStatus.mockReturnValue('NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      expect(screen.getByText(/New components can no longer be added/)).toBeInTheDocument();
      expect(screen.getByText(/Talk to your repository administrator/)).toBeInTheDocument();
    });

    it('renders non-admin near limit banner for NEAR_LIMITS_NON_ADMIN', () => {
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NON_ADMIN');
      render(<CELimitsAlerts />);
      expect(screen.getByText(/trending toward its usage limit/)).toBeInTheDocument();
      expect(screen.getByText(/Talk to your repository administrator/)).toBeInTheDocument();
    });
  });

  describe('No Render Conditions', () => {
    it('does not render when NO_THROTTLING', () => {
      mockUseThrottlingStatus.mockReturnValue('NO_THROTTLING');
      const {container} = render(<CELimitsAlerts />);
      expect(container.firstChild).toBeNull();
    });

    it('does not render when not Community Edition', () => {
      mockStateValue.getEdition.mockReturnValue('PRO');
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      const {container} = render(<CELimitsAlerts />);
      expect(container.firstChild).toBeNull();
    });

    it('does not render when HA mode enabled', () => {
      mockStateValue.getValue.mockReturnValue(true); // HA mode on
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      const {container} = render(<CELimitsAlerts />);
      expect(container.firstChild).toBeNull();
    });
  });

  // Links rendering is already verified in the Admin Banners tests above
  // No additional tests needed
});

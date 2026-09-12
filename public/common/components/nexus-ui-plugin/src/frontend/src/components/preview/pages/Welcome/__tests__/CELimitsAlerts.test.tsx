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

// Mock LocationUtils so the "Review your usage" onClick can be asserted
jest.mock('../../../../../interface/LocationUtils', () => ({
  scrollToUsageCenter: jest.fn(),
}));

import {ExtJS} from '../../../../../interface/ExtJS';
import {helperFunctions} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';
import {scrollToUsageCenter} from '../../../../../interface/LocationUtils';

const mockUseThrottlingStatus = helperFunctions.useThrottlingStatus as jest.Mock;
const mockUseUser = ExtJS.useUser as jest.Mock;
const mockState = ExtJS.state as jest.Mock;
const mockScrollToUsageCenter = scrollToUsageCenter as jest.Mock;

const mockStateValue = {
  getValue: jest.fn(),
  getEdition: jest.fn().mockReturnValue('COMMUNITY'),
};

describe('CELimitsAlerts', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockScrollToUsageCenter.mockClear();
    mockUseUser.mockReturnValue({administrator: true});
    mockState.mockReturnValue(mockStateValue);
    mockStateValue.getValue.mockReturnValue(false); // HA mode off
    mockStateValue.getEdition.mockReturnValue('COMMUNITY');
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

  // NEXUS-53219: page-level Callout severity mapping + right-aligned CTA buttons.
  // Radix Themes render color via the data-accent-color attribute and variant via
  // the rt-variant-* class; Button asChild forwards these onto the rendered anchor.
  describe('NEXUS-53219 page-level styling', () => {
    const PURCHASE_URL = 'http://links.sonatype.com/products/nxrm3/ce/purchase-license';
    const LEARN_MORE_URL = 'http://links.sonatype.com/products/nxrm3/learn-about-community-edition';

    it('75% approaching limits uses warning styling with a single tertiary Learn More CTA', () => {
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      const {container} = render(<CELimitsAlerts />);

      // Warning (not error) styling
      expect(container.querySelector('[data-accent-color="yellow"]')).toBeInTheDocument();
      expect(container.querySelector('[data-accent-color="red"]')).not.toBeInTheDocument();

      // Learn More CTA: surface (tertiary), gray, external
      const learnMore = screen.getByRole('link', {name: 'Learn More'});
      expect(learnMore).toHaveClass('rt-variant-surface');
      expect(learnMore).toHaveAttribute('data-accent-color', 'gray');
      expect(learnMore).toHaveAttribute('href', LEARN_MORE_URL);

      // No Purchase Now on warning states
      expect(screen.queryByRole('link', {name: 'Purchase Now'})).not.toBeInTheDocument();
    });

    it('over limits in grace period uses error styling to match the global banner', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      const {container} = render(<CELimitsAlerts />);

      // NEXUS-54200: Design confirmed this is an over-usage state and must render as
      // error/red, matching the global banner (CELimitsAlert.tsx tier="error"), not
      // the warning/yellow styling previously used per NEXUS-53219.
      expect(container.querySelector('[data-accent-color="red"]')).toBeInTheDocument();
      expect(container.querySelector('[data-accent-color="yellow"]')).not.toBeInTheDocument();

      const learnMore = screen.getByRole('link', {name: 'Learn More'});
      expect(learnMore).toHaveClass('rt-variant-surface');
      expect(learnMore).toHaveAttribute('data-accent-color', 'gray');
      expect(screen.queryByRole('link', {name: 'Purchase Now'})).not.toBeInTheDocument();
    });

    it('over limits write restricted uses error styling with tertiary Learn More and primary Purchase Now CTAs', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      const {container} = render(<CELimitsAlerts />);

      // Error styling
      expect(container.querySelector('[data-accent-color="red"]')).toBeInTheDocument();

      // Learn More: surface (tertiary), gray
      const learnMore = screen.getByRole('link', {name: 'Learn More'});
      expect(learnMore).toHaveClass('rt-variant-surface');
      expect(learnMore).toHaveAttribute('data-accent-color', 'gray');
      expect(learnMore).toHaveAttribute('href', LEARN_MORE_URL);

      // Purchase Now: primary (solid), blue, external
      const purchaseNow = screen.getByRole('link', {name: 'Purchase Now'});
      expect(purchaseNow).toHaveClass('rt-variant-solid');
      expect(purchaseNow).toHaveAttribute('data-accent-color', 'blue');
      expect(purchaseNow).toHaveAttribute('href', PURCHASE_URL);

      // White (contrast) text on the blue background — guards against the global
      // anchor color leaking in and rendering blue-on-blue (illegible) text.
      expect(purchaseNow).toHaveStyle({color: 'var(--accent-contrast)'});
      // Learn More keeps the surface/tertiary accent text (not the global link blue)
      expect(learnMore).toHaveStyle({color: 'var(--accent-a11)'});
    });

    it('renders Learn More before Purchase Now in the write restricted state', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);

      const ctas = screen.getAllByRole('link', {name: /Learn More|Purchase Now/});
      expect(ctas.map((el) => el.textContent)).toEqual(['Learn More', 'Purchase Now']);
    });

    it('invokes scrollToUsageCenter when "Review your usage" is clicked', () => {
      mockUseThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      render(<CELimitsAlerts />);

      fireEvent.click(screen.getByText('Review your usage'));
      expect(mockScrollToUsageCenter).toHaveBeenCalledTimes(1);
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

  // NEXUS-53862: the "Review your usage" CTA must reach the shared navigation
  // helper in the remaining CE states too (NEAR_LIMITS is already covered above).
  describe('Review your usage CTA (NEXUS-53862)', () => {
    it('calls scrollToUsageCenter when clicked in OVER_LIMITS_IN_GRACE', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      render(<CELimitsAlerts />);
      fireEvent.click(screen.getByText('Review your usage'));
      expect(mockScrollToUsageCenter).toHaveBeenCalledTimes(1);
    });

    it('calls scrollToUsageCenter when clicked in OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
      mockUseThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      fireEvent.click(screen.getByText('Review your usage'));
      expect(mockScrollToUsageCenter).toHaveBeenCalledTimes(1);
    });

    it('calls scrollToUsageCenter when clicked in BELOW_LIMITS_GRACE_PERIOD_ENDED', () => {
      // Earlier tests set the `under_end_grace` dismissal cookie; clear it so the
      // dismissible below-limit banner (and its CTA) is actually rendered here.
      document.cookie = 'under_end_grace=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
      mockUseThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      render(<CELimitsAlerts />);
      fireEvent.click(screen.getByText('Review your usage'));
      expect(mockScrollToUsageCenter).toHaveBeenCalledTimes(1);
    });
  });
});

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
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import CELimitsAlerts from './CELimitsAlerts';

// Mock ExtJS
jest.mock('../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({
      getValue: jest.fn((key: string) => {
        if (key === 'nexus.datastore.clustered.enabled') return false;
        if (key === 'nexus.community.gracePeriodEnds') return new Date().toISOString();
        return undefined;
      }),
      getEdition: jest.fn(() => 'COMMUNITY'),
    })),
    useUser: jest.fn(() => ({administrator: true})),
    useState: jest.fn((fn: () => any) => fn()),
  },
}));

// Mock helper functions
jest.mock('../../../widgets/SystemStatusAlerts/CELimits/UsageHelper', () => ({
  helperFunctions: {
    useThrottlingStatus: jest.fn(),
    useGracePeriodEndDate: jest.fn(() => 'January 15, 2026'),
    useDaysUntilGracePeriodEnds: jest.fn(() => 7),
    useViewPurchaseALicenseUrl: jest.fn(() => 'http://example.com/purchase'),
    useViewLearnMoreUrl: jest.fn(() => 'http://example.com/learn'),
  },
}));

// Mock LocationUtils
jest.mock('../../../../interface/LocationUtils', () => ({
  scrollToUsageCenter: jest.fn(),
}));

const {ExtJS} = jest.requireMock('../../../../interface/ExtJS');
const {helperFunctions} = jest.requireMock('../../../widgets/SystemStatusAlerts/CELimits/UsageHelper');

const mockThrottlingStatus = helperFunctions.useThrottlingStatus as jest.Mock;

function renderAlerts() {
  return render(<CELimitsAlerts />);
}

describe('CELimitsAlerts', () => {
  let mockStateObj: {getValue: jest.Mock; getEdition: jest.Mock};

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset cookie
    document.cookie = 'under_end_grace=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    // Default to CE, admin, non-HA — use a shared object so component calls see the same mock
    mockStateObj = {
      getValue: jest.fn((key: string) => {
        if (key === 'nexus.datastore.clustered.enabled') return false;
        if (key === 'nexus.community.gracePeriodEnds') return new Date().toISOString();
        return undefined;
      }),
      getEdition: jest.fn(() => 'COMMUNITY'),
    };
    ExtJS.state.mockReturnValue(mockStateObj);
    ExtJS.useUser.mockReturnValue({administrator: true});
    mockThrottlingStatus.mockReturnValue('NO_THROTTLING');
  });

  describe('Render Conditions', () => {
    it('renders null for Pro edition', () => {
      mockStateObj.getEdition.mockReturnValue('PRO');
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      const {container} = renderAlerts();
      expect(container.firstChild).toBeNull();
    });

    it('renders null for HA mode', () => {
      mockStateObj.getValue.mockImplementation((key: string) => {
        if (key === 'nexus.datastore.clustered.enabled') return true;
        return undefined;
      });
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      const {container} = renderAlerts();
      expect(container.firstChild).toBeNull();
    });

    it('renders null when throttlingStatus is NO_THROTTLING', () => {
      mockThrottlingStatus.mockReturnValue('NO_THROTTLING');
      const {container} = renderAlerts();
      expect(container.firstChild).toBeNull();
    });
  });

  describe('Admin Banner States', () => {
    it('renders NEAR_LIMITS_NEVER_IN_GRACE warning banner', () => {
      mockThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      renderAlerts();
      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(screen.getByText(/trending toward its usage limit/i)).toBeInTheDocument();
    });

    it('renders OVER_LIMITS_IN_GRACE error banner with grace period info', () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      renderAlerts();
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/days remaining/i)).toBeInTheDocument();
    });

    it('renders OVER_LIMITS_GRACE_PERIOD_ENDED error banner', () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      renderAlerts();
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/exceeded its usage limit/i)).toBeInTheDocument();
    });

    it('renders BELOW_LIMITS_GRACE_PERIOD_ENDED dismissible warning', () => {
      mockThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      renderAlerts();
      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(screen.getByText(/exceeds usage limits, you will not be able to add new components/i)).toBeInTheDocument();
      expect(screen.getByRole('button', {name: /dismiss/i})).toBeInTheDocument();
    });

    it('no banner for BELOW_LIMITS_IN_GRACE (intentional)', () => {
      mockThrottlingStatus.mockReturnValue('BELOW_LIMITS_IN_GRACE');
      const {container} = renderAlerts();
      expect(container.querySelector('[role="status"]')).toBeNull();
      expect(container.querySelector('[role="alert"]')).toBeNull();
    });
  });

  describe('Non-Admin Banner States', () => {
    beforeEach(() => {
      ExtJS.useUser.mockReturnValue({administrator: false});
    });

    it('renders NEAR_LIMITS_NON_ADMIN warning banner', () => {
      mockThrottlingStatus.mockReturnValue('NEAR_LIMITS_NON_ADMIN');
      renderAlerts();
      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(screen.getByText(/Talk to your repository administrator/i)).toBeInTheDocument();
    });

    it('renders NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED error banner', () => {
      mockThrottlingStatus.mockReturnValue('NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED');
      renderAlerts();
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/New components can no longer be added/i)).toBeInTheDocument();
    });
  });

  describe('Dismiss Cookie Behavior', () => {
    it('hides BELOW_LIMITS_GRACE_PERIOD_ENDED when dismiss cookie is set', () => {
      document.cookie = 'under_end_grace=dismissed; path=/';
      mockThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      const {container} = renderAlerts();
      expect(container.querySelector('[role="status"]')).toBeNull();
    });

    it('sets cookie on dismiss click and hides banner on re-render', async () => {
      mockThrottlingStatus.mockReturnValue('BELOW_LIMITS_GRACE_PERIOD_ENDED');
      const {rerender, getByRole} = renderAlerts();

      const dismissButton = getByRole('button', {name: /dismiss/i});
      fireEvent.click(dismissButton);

      // Cookie should be set
      expect(document.cookie).toContain('under_end_grace=dismissed');

      // Re-render should hide banner
      rerender(<CELimitsAlerts />);
      await waitFor(() => {
        expect(screen.queryByRole('button', {name: /dismiss/i})).toBeNull();
      });
    });
  });

  describe('Call-to-Action Links', () => {
    it('renders Purchase Now button for OVER_LIMITS_GRACE_PERIOD_ENDED', () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      renderAlerts();
      expect(screen.getByText(/purchase now/i)).toBeInTheDocument();
    });

    it('does not render Purchase Now button for OVER_LIMITS_IN_GRACE', () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      renderAlerts();
      expect(screen.queryByText(/purchase now/i)).toBeNull();
    });

    it('renders Learn More link for all banner states', () => {
      mockThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      renderAlerts();
      expect(screen.getByText(/learn more/i)).toBeInTheDocument();
    });
  });

  describe('Storage Event Listener (Test Hub)', () => {
    it('re-renders when SONATYPE_TEST_CE_THROTTLING_STATUS changes', async () => {
      mockThrottlingStatus.mockReturnValue('NO_THROTTLING');
      const {container} = renderAlerts();
      expect(container.firstChild).toBeNull();

      // Simulate storage event
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      fireEvent(window, new StorageEvent('storage', {
        key: 'SONATYPE_TEST_CE_THROTTLING_STATUS',
        newValue: 'Over limits',
      }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('re-renders when SONATYPE_TEST_CE_GRACE_PERIOD_ENDS changes', async () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      renderAlerts();

      // Initial render
      expect(screen.getByRole('alert')).toBeInTheDocument();

      // Simulate grace period change
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_GRACE_PERIOD_ENDED');
      fireEvent(window, new StorageEvent('storage', {
        key: 'SONATYPE_TEST_CE_GRACE_PERIOD_ENDS',
        newValue: '2025-01-01T00:00:00Z',
      }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('re-renders when SONATYPE_TEST_CE_COMPONENTS changes', async () => {
      mockThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      renderAlerts();

      expect(screen.getByRole('status')).toBeInTheDocument();

      // Simulate component count change
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      fireEvent(window, new StorageEvent('storage', {
        key: 'SONATYPE_TEST_CE_COMPONENTS',
        newValue: '45000',
      }));

      await waitFor(() => {
        expect(screen.getByText(/days remaining/i)).toBeInTheDocument();
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('re-renders when SONATYPE_TEST_CE_REQUESTS changes', async () => {
      mockThrottlingStatus.mockReturnValue('UNDER_LIMITS');
      const {container} = renderAlerts();

      expect(container.querySelector('[role="status"]')).toBeNull();

      // Simulate request count change
      mockThrottlingStatus.mockReturnValue('NEAR_LIMITS_NEVER_IN_GRACE');
      fireEvent(window, new StorageEvent('storage', {
        key: 'SONATYPE_TEST_CE_REQUESTS',
        newValue: '76000',
      }));

      await waitFor(() => {
        expect(screen.getByRole('status')).toBeInTheDocument();
      });
    });
  });

  describe('Grace Period Date Formatting', () => {
    it('displays formatted grace period end date in OVER_LIMITS_IN_GRACE banner', () => {
      mockThrottlingStatus.mockReturnValue('OVER_LIMITS_IN_GRACE');
      renderAlerts();

      // Verify the grace period end date string appears
      expect(screen.getByText(/January 15, 2026/)).toBeInTheDocument();
    });
  });
});

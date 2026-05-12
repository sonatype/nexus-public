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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { GASecurityTab } from '../GASecurityTab';
import type { UseComponentSecurityResult } from '../useComponentSecurity';

// ---------------------------------------------------------------------------
// Mock the hook so each test fully controls state
// ---------------------------------------------------------------------------
const mockRefetch = jest.fn();

const defaultHookResult: UseComponentSecurityResult = {
  data: null,
  loading: false,
  error: null,
  iqConnected: null,
  refetch: mockRefetch,
};

let hookResult: UseComponentSecurityResult = { ...defaultHookResult };

jest.mock('../useComponentSecurity', () => ({
  useComponentSecurity: () => hookResult,
}));

// Mock SettingsFormSection to avoid SCSS import issues in tests
jest.mock('@/components/super/shared/form', () => ({
  SettingsFormSection: ({
    title,
    children,
  }: {
    title: string;
    children: React.ReactNode;
  }) => (
    <div data-testid={`settings-section-${title.replace(/\s+/g, '-').toLowerCase()}`}>
      <span>{title}</span>
      {children}
    </div>
  ),
}));

const renderTab = (gaId = 'maven:org.example:lib', selectedVersion: string | null = '1.0.0') =>
  render(
    <Theme>
      <GASecurityTab gaId={gaId} selectedVersion={selectedVersion} />
    </Theme>
  );

describe('GASecurityTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    hookResult = { ...defaultHookResult };
  });

  // -------------------------------------------------------------------------
  // State 0 — No version selected
  // -------------------------------------------------------------------------
  describe('State 0: no version selected', () => {
    it('renders amber callout when selectedVersion is null', () => {
      hookResult = { ...defaultHookResult, iqConnected: null };
      renderTab('maven:org.example:lib', null);

      expect(
        screen.getByText(/select a version/i)
      ).toBeInTheDocument();
    });

    it('does not render spinner or security content when no version', () => {
      renderTab('maven:org.example:lib', null);

      expect(screen.queryByText(/evaluating component security/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/no policy violations/i)).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 1 — Loading
  // -------------------------------------------------------------------------
  describe('State 1: loading', () => {
    it('renders spinner while loading', () => {
      hookResult = { ...defaultHookResult, loading: true, iqConnected: null };
      renderTab();

      expect(screen.getByText(/evaluating component security/i)).toBeInTheDocument();
    });

    it('does not show stale data while loading', () => {
      hookResult = { ...defaultHookResult, loading: true, iqConnected: true };
      renderTab();

      expect(screen.queryByText(/no policy violations/i)).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Vulnerabilities' })).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 2 — Violations found
  // -------------------------------------------------------------------------
  describe('State 2: violations found', () => {
    it('renders violation summary card with counts', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 3,
          severeCount: 7,
          moderateCount: 12,
          lowCount: 2,
          violations: [
            {
              policyName: 'Security-Critical',
              threatLevel: 10,
              constraintViolations: [
                { constraintName: 'CVE-2021-44228', reasons: ['Log4Shell RCE vulnerability'] },
              ],
            },
          ],
          reportUrl: 'https://iq.example.com/report/abc123',
        },
      };
      renderTab();

      expect(screen.getByRole('heading', { name: 'Vulnerabilities' })).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('Critical')).toBeInTheDocument();
      expect(screen.getByText('High')).toBeInTheDocument();
      expect(screen.getByText('Medium')).toBeInTheDocument();
      expect(screen.getByText('Low')).toBeInTheDocument();
    });

    it('renders the violations table when violations array is non-empty', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 1,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [
            {
              policyName: 'Security-Critical',
              threatLevel: 10,
              constraintViolations: [
                { constraintName: 'CVE-2021-44228', reasons: ['Log4Shell RCE vulnerability'] },
              ],
            },
          ],
        },
      };
      renderTab();

      expect(
        screen.getByTestId('settings-section-policy-violations')
      ).toBeInTheDocument();
      expect(screen.getByText('Security-Critical')).toBeInTheDocument();
    });

    it('renders "View Full Report in Lifecycle" button when reportUrl is present', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 2,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          reportUrl: 'https://iq.example.com/report/abc123',
        },
      };
      renderTab();

      expect(
        screen.getByRole('button', { name: /view full report in lifecycle/i })
      ).toBeInTheDocument();
    });

    it('does NOT render "View Full Report" button when reportUrl is absent', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 2,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          // no reportUrl
        },
      };
      renderTab();

      expect(
        screen.queryByRole('button', { name: /view full report in lifecycle/i })
      ).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 3 — Clean component (IQ connected, zero violations)
  // -------------------------------------------------------------------------
  describe('State 3: clean component (no violations)', () => {
    it('renders green callout for zero violations', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
        },
      };
      renderTab();

      expect(screen.getByText(/no policy violations found/i)).toBeInTheDocument();
      expect(
        screen.getByText(/this version passed all active policies/i)
      ).toBeInTheDocument();
    });

    it('shows evaluation date when provided', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          evaluationDate: '2026-03-04T12:00:00Z',
        },
      };
      renderTab();

      expect(screen.getByText(/2026-03-04T12:00:00Z/)).toBeInTheDocument();
    });

    it('does NOT show evaluation date when absent', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
        },
      };
      renderTab();

      expect(screen.queryByText(/evaluated:/i)).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 4 — IQ not connected
  // -------------------------------------------------------------------------
  describe('State 4: IQ not connected', () => {
    it('renders empty state with "IQ Server Not Connected" heading', () => {
      hookResult = { ...defaultHookResult, iqConnected: false };
      renderTab();

      expect(screen.getByText(/IQ Server Not Connected/i)).toBeInTheDocument();
      expect(
        screen.getByText(/connect IQ Server to see policy violations/i)
      ).toBeInTheDocument();
    });

    it('renders "Configure IQ Server" button', () => {
      hookResult = { ...defaultHookResult, iqConnected: false };
      renderTab();

      expect(
        screen.getByRole('button', { name: /configure IQ server/i })
      ).toBeInTheDocument();
    });

    it('clicking "Configure IQ Server" navigates to the IQ settings hash', async () => {
      hookResult = { ...defaultHookResult, iqConnected: false };
      renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /configure IQ server/i })
      );

      expect(window.location.hash).toBe('#preview/admin/iq');
    });
  });

  // -------------------------------------------------------------------------
  // State 5 — Error
  // -------------------------------------------------------------------------
  describe('State 5: error state', () => {
    it('renders red callout with error message on fetch failure', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        error: 'Network Error: connection refused',
      };
      renderTab();

      expect(screen.getByText(/failed to load security data/i)).toBeInTheDocument();
      expect(screen.getByText(/Network Error: connection refused/i)).toBeInTheDocument();
    });

    it('renders "Try again" button that calls refetch', async () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        error: 'Internal Server Error',
        refetch: mockRefetch,
      };
      renderTab();

      const retryBtn = screen.getByRole('button', { name: /try again/i });
      expect(retryBtn).toBeInTheDocument();

      await userEvent.click(retryBtn);

      expect(mockRefetch).toHaveBeenCalledTimes(1);
    });
  });

  // -------------------------------------------------------------------------
  // Boundary: reportUrl conditional rendering (clean state)
  // -------------------------------------------------------------------------
  describe('"View in Lifecycle" link — clean state', () => {
    it('shows "View in Lifecycle" link when reportUrl present in clean state', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          reportUrl: 'https://iq.example.com/report/clean',
        },
      };
      renderTab();

      expect(
        screen.getByRole('button', { name: /view in lifecycle/i })
      ).toBeInTheDocument();
    });

    it('does NOT show "View in Lifecycle" link when reportUrl absent in clean state', () => {
      hookResult = {
        ...defaultHookResult,
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
        },
      };
      renderTab();

      expect(
        screen.queryByRole('button', { name: /view in lifecycle/i })
      ).not.toBeInTheDocument();
    });
  });
});

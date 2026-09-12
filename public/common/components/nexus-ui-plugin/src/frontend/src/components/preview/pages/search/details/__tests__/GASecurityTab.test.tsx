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
import { render, screen, } from '@testing-library/react';
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
  status: 'idle',
  refetch: mockRefetch,
};

// Prefix with 'mock' to allow access in jest.mock scope (jest.mock is hoisted)
let mockHookResult: UseComponentSecurityResult = { ...defaultHookResult };

jest.mock('../useComponentSecurity', () => ({
  useComponentSecurity: () => mockHookResult,
}));

// The IQ CTA navigates by state name through the router, so the tab needs a router in context.
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

// Mock SettingsFormSection to avoid SCSS import issues in tests
jest.mock('../../../../shared/form', () => ({
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

/** The only failure string useComponentSecurity ever produces. */
const SANITIZED_ERROR =
  'Unable to determine the IQ Server connection status. Check your connection and try again.';

const renderTab = (gaId = 'maven:org.example:lib', selectedVersion: string | null = '1.0.0') =>
  render(
    <Theme>
      <GASecurityTab gaId={gaId} selectedVersion={selectedVersion} />
    </Theme>
  );

describe('GASecurityTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockHookResult = { ...defaultHookResult };
  });

  // -------------------------------------------------------------------------
  // State 0 — No version selected
  // -------------------------------------------------------------------------
  describe('State 0: no version selected', () => {
    it('renders amber callout when selectedVersion is null', () => {
      mockHookResult = { ...defaultHookResult, iqConnected: null };
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

    // '' is the valid selected version for versionless formats (raw) — distinct from null
    // ("nothing selected yet"). Plain truthiness treats them the same and would permanently show
    // the "select a version" prompt for raw components (NEXUS-54201). '' gets its own state
    // because IQ cannot evaluate a component without a version: useComponentSecurity makes no
    // request, so this must not be a spinner either.
    it('explains that a versionless component cannot be evaluated, rather than prompting for a version', () => {
      mockHookResult = { ...defaultHookResult, loading: false, iqConnected: null };
      renderTab('raw:/some/path:/some/path/file.txt', '');

      expect(screen.queryByText(/select a version/i)).not.toBeInTheDocument();
      expect(
        screen.getByText(/not available for components without a version/i),
      ).toBeInTheDocument();
      expect(screen.queryByText(/evaluating component security/i)).not.toBeInTheDocument();
    });

    // The blank-panel trap: iqConnected === null returns null further down the state chain, so
    // without an explicit '' branch a raw component's Security tab renders nothing at all.
    it('renders something rather than an empty panel for a versionless component', () => {
      mockHookResult = { ...defaultHookResult, loading: false, iqConnected: null, data: null };
      const { container } = renderTab('raw:/some/path:/some/path/file.txt', '');

      expect(container).not.toBeEmptyDOMElement();
    });
  });

  // -------------------------------------------------------------------------
  // State 1 — Loading
  // -------------------------------------------------------------------------
  describe('State 1: loading', () => {
    it('renders spinner while loading', () => {
      mockHookResult = { ...defaultHookResult, loading: true, iqConnected: null };
      renderTab();

      expect(screen.getByText(/evaluating component security/i)).toBeInTheDocument();
    });

    it('does not show stale data while loading', () => {
      mockHookResult = { ...defaultHookResult, loading: true, iqConnected: true };
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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

    /**
     * The report URL is supplied by IQ Server, so the opened tab is not first-party. Without
     * `noopener` it receives a live `window.opener` handle back into this document (reverse
     * tabnabbing). Matches FirewallCell and HealthCheckCell, which open their own
     * server-supplied report URLs the same way.
     */
    it('opens the full report with noopener,noreferrer', async () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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

      await userEvent.click(
        screen.getByRole('button', { name: /view full report in lifecycle/i })
      );

      expect(openSpy).toHaveBeenCalledWith(
        'https://iq.example.com/report/abc123',
        '_blank',
        'noopener,noreferrer'
      );
      openSpy.mockRestore();
    });

    /**
     * `reportUrl` is server-supplied, so the scheme cannot be assumed. Anything that is not an
     * absolute http(s) URL is discarded rather than passed to `window.open` — a `javascript:`
     * URL there would execute in this document's origin.
     */
    it.each([
      ['javascript:alert(document.cookie)'],
      ['data:text/html,<script>alert(1)</script>'],
      ['vbscript:msgbox(1)'],
      ['file:///etc/passwd'],
      ['/relative/report/path'],
      ['not a url at all'],
    ])('does not open a report URL with a disallowed scheme: %s', async (reportUrl) => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
        data: {
          criticalCount: 2,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          reportUrl,
        },
      };
      renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /view full report in lifecycle/i })
      );

      expect(openSpy).not.toHaveBeenCalled();
      openSpy.mockRestore();
    });

    it('still opens a plain http report URL', async () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
        data: {
          criticalCount: 2,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          reportUrl: 'http://iq.internal.example.com/report/abc123',
        },
      };
      renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /view full report in lifecycle/i })
      );

      expect(openSpy).toHaveBeenCalledWith(
        'http://iq.internal.example.com/report/abc123',
        '_blank',
        'noopener,noreferrer'
      );
      openSpy.mockRestore();
    });
  });

  // -------------------------------------------------------------------------
  // State 3 — Clean component (IQ connected, zero violations)
  // -------------------------------------------------------------------------
  describe('State 3: clean component (no violations)', () => {
    it('renders green callout for zero violations', () => {
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
      mockHookResult = { ...defaultHookResult, iqConnected: false, status: 'not-connected' };
      renderTab();

      expect(screen.getByText(/IQ Server Not Connected/i)).toBeInTheDocument();
      expect(
        screen.getByText(/connect IQ Server to see policy violations/i)
      ).toBeInTheDocument();
    });

    it('renders "Connect IQ Server" button', () => {
      mockHookResult = { ...defaultHookResult, iqConnected: false, status: 'not-connected' };
      renderTab();

      expect(
        screen.getByRole('button', { name: /connect IQ server/i })
      ).toBeInTheDocument();
    });

    it('clicking "Connect IQ Server" navigates by state name, not by a hardcoded hash', async () => {
      mockHookResult = { ...defaultHookResult, iqConnected: false, status: 'not-connected' };
      const hashBefore = window.location.hash;
      renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /connect IQ server/i })
      );

      // `preview.admin.iq` is declared with url '/iq-overview', so assembling
      // '#preview/admin/iq' from the state name 404s. Only the router knows the URL.
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iq');
      expect(window.location.hash).toBe(hashBefore);
    });
  });

  // -------------------------------------------------------------------------
  // State 5 — Error
  // -------------------------------------------------------------------------
  describe('State 5: error state', () => {
    it('renders red callout with the sanitized failure message', () => {
      mockHookResult = {
        ...defaultHookResult,
        status: 'unavailable',
        error: SANITIZED_ERROR,
      };
      renderTab();

      expect(screen.getByTestId('security-error')).toBeInTheDocument();
      expect(screen.getByText(SANITIZED_ERROR)).toBeInTheDocument();
    });

    it('renders "Try again" button that calls refetch', async () => {
      mockHookResult = {
        ...defaultHookResult,
        status: 'unavailable',
        error: SANITIZED_ERROR,
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
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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

    it('opens the clean-state report link with noopener,noreferrer', async () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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

      await userEvent.click(screen.getByRole('button', { name: /view in lifecycle/i }));

      expect(openSpy).toHaveBeenCalledWith(
        'https://iq.example.com/report/clean',
        '_blank',
        'noopener,noreferrer'
      );
      openSpy.mockRestore();
    });

    /** Same scheme guard as the violations-state button — this is the second call site. */
    it('does not open a disallowed clean-state report URL', async () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
          reportUrl: 'javascript:alert(1)',
        },
      };
      renderTab();

      await userEvent.click(screen.getByRole('button', { name: /view in lifecycle/i }));

      expect(openSpy).not.toHaveBeenCalled();
      openSpy.mockRestore();
    });

    it('does NOT show "View in Lifecycle" link when reportUrl absent in clean state', () => {
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'evaluated',
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
  // -------------------------------------------------------------------------
  // State 4b — IQ connected but not entitled
  // -------------------------------------------------------------------------
  describe('State 4b: IQ connected without entitlement', () => {
    it('renders a safe empty state naming the missing products', () => {
      mockHookResult = { ...defaultHookResult, iqConnected: true, status: 'not-entitled' };
      renderTab();

      expect(screen.getByTestId('security-not-entitled')).toBeInTheDocument();
      expect(screen.getByText(/security analysis not available/i)).toBeInTheDocument();
      expect(
        screen.getByText(/does not include Sonatype Lifecycle or Sonatype Repository Firewall/i)
      ).toBeInTheDocument();
    });

    it('offers the Connect IQ Server call to action', () => {
      mockHookResult = { ...defaultHookResult, iqConnected: true, status: 'not-entitled' };
      renderTab();

      expect(
        screen.getByRole('button', { name: /connect IQ server/i })
      ).toBeInTheDocument();
    });

    it('routes the call to action by state name too', async () => {
      mockHookResult = { ...defaultHookResult, iqConnected: true, status: 'not-entitled' };
      renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /connect IQ server/i })
      );

      expect(mockGo).toHaveBeenCalledWith('preview.admin.iq');
    });

    it('never claims the component is clean', () => {
      mockHookResult = { ...defaultHookResult, iqConnected: true, status: 'not-entitled' };
      renderTab();

      expect(screen.queryByText(/no policy violations found/i)).not.toBeInTheDocument();
      expect(
        screen.queryByText(/passed all active policies/i)
      ).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 6 — connected and entitled, but no evaluation data retrievable
  // -------------------------------------------------------------------------
  describe('State 6: evaluation data not available', () => {
    it('renders the explicit "Evaluation Data Not Available" state', () => {
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'no-evaluation-data',
      };
      renderTab();

      expect(screen.getByTestId('security-no-evaluation-data')).toBeInTheDocument();
      expect(screen.getByText(/evaluation data not available/i)).toBeInTheDocument();
    });

    it('does NOT present the component as having passed its policies', () => {
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'no-evaluation-data',
      };
      renderTab();

      expect(screen.queryByText(/no policy violations found/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/passed all active policies/i)).not.toBeInTheDocument();
      expect(screen.queryByTestId('security-clean')).not.toBeInTheDocument();
    });

    it('does NOT render the zero-count summary card', () => {
      mockHookResult = {
        ...defaultHookResult,
        iqConnected: true,
        status: 'no-evaluation-data',
      };
      renderTab();

      expect(
        screen.queryByRole('heading', { name: 'Vulnerabilities' })
      ).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 7 — capabilities endpoint absent from this deployment (404)
  // -------------------------------------------------------------------------
  describe('State 7: IQ API not present in this deployment', () => {
    it('renders a safe empty state without a retry or a connect CTA', () => {
      mockHookResult = { ...defaultHookResult, status: 'unsupported', iqConnected: null };
      renderTab();

      expect(screen.getByTestId('security-unsupported')).toBeInTheDocument();
      expect(screen.getByText(/security analysis not available/i)).toBeInTheDocument();
      expect(
        screen.getByText(/does not include IQ Server integration/i)
      ).toBeInTheDocument();
      // A permanent condition: retrying or opening IQ settings cannot resolve it.
      expect(screen.queryByRole('button', { name: /try again/i })).not.toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: /connect IQ server/i })
      ).not.toBeInTheDocument();
    });

    it('does not claim IQ is disconnected or the component is clean', () => {
      mockHookResult = { ...defaultHookResult, status: 'unsupported', iqConnected: null };
      renderTab();

      expect(screen.queryByText(/IQ Server Not Connected/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/no policy violations found/i)).not.toBeInTheDocument();
      expect(screen.queryByTestId('security-clean')).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // State 8 — caller lacks nexus:settings:read (403)
  // -------------------------------------------------------------------------
  describe('State 8: insufficient permission to read IQ status', () => {
    it('explains the permission gap without a retry or a connect CTA', () => {
      mockHookResult = { ...defaultHookResult, status: 'forbidden', iqConnected: null };
      renderTab();

      expect(screen.getByTestId('security-forbidden')).toBeInTheDocument();
      expect(
        screen.getByText(/does not have permission to view the IQ Server connection status/i)
      ).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /try again/i })).not.toBeInTheDocument();
      // The user cannot reach IQ settings either, so offering the CTA would be a dead end.
      expect(
        screen.queryByRole('button', { name: /connect IQ server/i })
      ).not.toBeInTheDocument();
    });

    it('does not claim IQ is disconnected or the component is clean', () => {
      mockHookResult = { ...defaultHookResult, status: 'forbidden', iqConnected: null };
      renderTab();

      expect(screen.queryByText(/IQ Server Not Connected/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/passed all active policies/i)).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // Initial idle state must not read as resolved
  // -------------------------------------------------------------------------
  describe('idle with a version selected (pre-check frame)', () => {
    it('renders the loading state, not a resolved one', () => {
      // `loading: false` here reproduces the pre-fix frame exactly: status still `idle`
      // because the effect had not run, yet the tab rendered resolved copy.
      mockHookResult = {
        ...defaultHookResult,
        status: 'idle',
        loading: false,
        iqConnected: null,
      };
      renderTab();

      expect(screen.getByText(/evaluating component security/i)).toBeInTheDocument();
    });

    it('never states that IQ Server is connected before any check has completed', () => {
      mockHookResult = {
        ...defaultHookResult,
        status: 'idle',
        loading: false,
        iqConnected: null,
      };
      const { container } = renderTab();

      expect(container.textContent).not.toMatch(/IQ Server is connected/i);
      expect(screen.queryByTestId('security-no-evaluation-data')).not.toBeInTheDocument();
      expect(screen.queryByTestId('security-clean')).not.toBeInTheDocument();
      expect(screen.queryByTestId('security-not-connected')).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // Never blank — the headline symptom of NEXUS-54431
  // -------------------------------------------------------------------------
  describe('the tab is never blank (NEXUS-54431)', () => {
    const ALL_STATES: UseComponentSecurityResult[] = [
      { ...defaultHookResult, status: 'idle' },
      { ...defaultHookResult, status: 'checking', loading: true },
      { ...defaultHookResult, status: 'not-connected', iqConnected: false },
      { ...defaultHookResult, status: 'not-entitled', iqConnected: true },
      { ...defaultHookResult, status: 'unavailable', error: SANITIZED_ERROR },
      { ...defaultHookResult, status: 'unsupported' },
      { ...defaultHookResult, status: 'forbidden' },
      { ...defaultHookResult, status: 'no-evaluation-data', iqConnected: true },
      {
        ...defaultHookResult,
        status: 'evaluated',
        iqConnected: true,
        data: {
          criticalCount: 0,
          severeCount: 0,
          moderateCount: 0,
          lowCount: 0,
          violations: [],
        },
      },
      {
        ...defaultHookResult,
        status: 'evaluated',
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
              constraintViolations: [{ constraintName: 'CVE-1', reasons: ['boom'] }],
            },
          ],
        },
      },
    ];

    it.each(ALL_STATES)('renders visible content for status "$status"', (state) => {
      mockHookResult = state;
      const { container } = renderTab();

      expect(container.textContent?.trim().length ?? 0).toBeGreaterThan(0);
    });

    it('renders content when iqConnected is still null and nothing is loading', () => {
      // Regression: this combination previously hit `return null`, leaving an empty panel.
      mockHookResult = { ...defaultHookResult, iqConnected: null, loading: false, status: 'idle' };
      const { container } = renderTab();

      expect(container.textContent?.trim().length ?? 0).toBeGreaterThan(0);
    });
  });

  // -------------------------------------------------------------------------
  // Raw API bodies must never reach the UI
  // -------------------------------------------------------------------------
  describe('raw API error bodies are not rendered', () => {
    it('renders only the message it was given, with no HTML body or status code', () => {
      mockHookResult = {
        ...defaultHookResult,
        status: 'unavailable',
        error: SANITIZED_ERROR,
      };
      const { container } = renderTab();

      expect(container.textContent).not.toMatch(/<html|Internal Server Error|status code|stack/i);
    });

    it('does not render an IQ Server URL or host from the failure path', () => {
      mockHookResult = {
        ...defaultHookResult,
        status: 'unavailable',
        error: SANITIZED_ERROR,
      };
      const { container } = renderTab();

      expect(container.textContent).not.toMatch(/https?:\/\//);
    });
  });
});

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
import React from "react";
import {when} from "jest-when";
import {render, screen} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {act} from "react-dom/test-utils";

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";

import {maliciousRiskOnDiskResponse, maliciousRiskOnDiskResponseWithCount0} from "./MaliciousRiskOnDisk.testdata";
import MaliciousRiskOnDisk from "./MaliciousRiskOnDisk";
import FeatureFlags from '../../../constants/FeatureFlags';
import MaliciousRiskStrings from '../../../../../../../nexus-ui-plugin/src/frontend/src/components/preview/constants/pages/maliciousrisk/MaliciousRiskStrings';
import { helperFunctions } from "../SystemStatusAlerts/CELimits/UsageHelper";

const {
  useThrottlingStatusValue,
  useGracePeriodEndsDate
} = helperFunctions;

const {
  CLM,
  MALWARE_RISK_ENABLED
} = FeatureFlags;
const {MALICIOUS_RISK: {RISK_ON_DISK}} = MaliciousRiskStrings;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn()
    }),
    useUser: jest.fn(),
    useState: jest.fn()
  },
}));

const selectors = {
  ...TestUtils.selectors,
  getHeading: (t) => screen.getByRole('heading', {name: t}),
  queryButton: (t) => screen.queryByRole('button', {name: t}),
  queryLink: (t) => screen.queryByRole('link', {name: t}),
  queryAlert: () => screen.queryByRole('alert'),
  queryText: (t) => screen.queryByText(t),
};

describe('MaliciousRiskOnDisk', () => {
  let mockSetCookie;

  beforeEach(() => {
    mockSetCookie = jest.fn();
    Object.defineProperty(document, 'cookie', {
      get: () => '',
      set: mockSetCookie,
      configurable: true,
    });

    ExtJS.useState.mockImplementation((fn) => {
      if (fn === useThrottlingStatusValue) return 'Under limits';
      if (fn === useGracePeriodEndsDate) return new Date('');
      try { return fn(); } catch { return undefined; }
    });
    when(ExtJS.state().getValue)
        .calledWith(MALWARE_RISK_ENABLED)
        .mockReturnValue(true);
    when(ExtJS.state().getValue)
        .calledWith('nexus.malware.count')
        .mockReturnValue(maliciousRiskOnDiskResponse);

    const date = new Date('2024-12-02');
    jest.useFakeTimers().setSystemTime(date);
  })

  async function renderView(isAdmin, isProEdition) {
    ExtJS.isProEdition.mockReturnValue(isProEdition);
    ExtJS.useUser.mockReturnValue({'administrator': isAdmin});
    const rerender = jest.fn();
    const toggle = jest.fn();

    await act(async () => {
      render(<MaliciousRiskOnDisk rerender={rerender} toggle={toggle} />);
    });
  }

  it('does not render if user is not logged', async () => {
    ExtJS.useUser.mockReturnValue(null);
    await act(async () => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it('expires the malware banner cookie if user is not logged', async () => {
    ExtJS.useUser.mockReturnValue(null);
    const setCookie = jest.fn();
    Object.defineProperty(document, 'cookie', {
      get: () => '',
      set: setCookie,
      configurable: true,
    });
    await act(async () => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(setCookie).toHaveBeenCalledWith('MALWARE_BANNER=; expires=Thu, 26 Feb 1950 00:00:00 UTC; path=/');
  });

  it('does not render if feature flag is false', async () => {
    when(ExtJS.state().getValue)
        .calledWith(MALWARE_RISK_ENABLED)
        .mockReturnValue(false);
    await act(async () => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it('does not render if malicious count is 0', async () => {
    when(ExtJS.state().getValue)
        .calledWith('nexus.malware.count')
        .mockReturnValue(maliciousRiskOnDiskResponseWithCount0);

    await act(async () => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it('renders correctly when admin user and pro edition', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender('1,234,567', isAdmin, isProEdition);
  });

  it('renders correctly when admin user and pro edition with IQ and FF enabled', async () => {
    when(ExtJS.state().getValue).calledWith(CLM).mockReturnValue({enabled: true, hasFirewall: true});
    when(ExtJS.state().getValue).calledWith(MALWARE_RISK_ENABLED).mockReturnValue(true);
    const isAdmin = true;
    const isProEdition = true;
    const isIqServerEnabled = true;
    const isMalwareRiskEnabled = true;

    await renderView(isAdmin, isProEdition);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender('1,234,567', isAdmin, isProEdition, isIqServerEnabled, isMalwareRiskEnabled);
  });

  it('renders correctly when admin user and community edition', async () => {
    const isAdmin = true;
    const isProEdition = false;

    await renderView(isAdmin, isProEdition);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender('1,234,567', isAdmin, isProEdition);
  });

  it('renders correctly when non-admin user and pro edition', async () => {
    const isAdmin = false;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender('1,234,567', isAdmin, isProEdition);
  });

  it('renders correctly when non-admin user and community edition', async () => {
    const isAdmin = false;
    const isProEdition = false;

    await renderView(isAdmin, isProEdition);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender('1,234,567', isAdmin, isProEdition);
  });

  it('renders a close button and set the cookie when button clicked', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    const closeButton = selectors.queryButton('Close');
    expect(closeButton).toBeInTheDocument();

    userEvent.click(closeButton);
    expect(mockSetCookie).toHaveBeenCalledWith('MALWARE_BANNER=close; path=/');
  });

  it('expands and collapses on toggle', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    const toggleButton = screen.getAllByRole('button')[0];

    expect(toggleButton).toHaveAttribute('aria-expanded', 'true');
    userEvent.click(toggleButton);
    expect(toggleButton).toHaveAttribute('aria-expanded', 'false');
  });

  it('renders the toggle button with proper structure (title first, caret icon last)', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    const toggleButton = screen.getAllByRole('button')[0];

    // Verify toggle button has the collapsible trigger class
    expect(toggleButton).toHaveClass('nx-collapsible-items__trigger');

    // Get the children elements
    const titleElement = toggleButton.querySelector('.risk-on-disk-alert-title');
    const toggleElement = toggleButton.querySelector('.risk-on-disk-toggle');

    // Verify both elements exist
    expect(titleElement).toBeInTheDocument();
    expect(toggleElement).toBeInTheDocument();

    // Verify the order: title should come before toggle icon
    // Compare the DOM positions using compareDocumentPosition
    // Node.DOCUMENT_POSITION_FOLLOWING = 4 means titleElement precedes toggleElement
    const position = titleElement.compareDocumentPosition(toggleElement);
    expect(position & Node.DOCUMENT_POSITION_FOLLOWING).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it('renders the caret icon inside the toggle element', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    const toggleElement = screen.getAllByRole('button')[0].querySelector('.risk-on-disk-toggle');
    const caretIcon = toggleElement.querySelector('.fa-caret-right');

    expect(caretIcon).toBeInTheDocument();
  });

  it('applies expanded class to caret icon when expanded', async () => {
    const isAdmin = true;
    const isProEdition = true;

    await renderView(isAdmin, isProEdition);

    const toggleButton = screen.getAllByRole('button')[0];
    const caretIcon = toggleButton.querySelector('.fa-caret-right');

    // Initially expanded
    expect(caretIcon).toHaveClass('expanded');

    // Click to collapse
    userEvent.click(toggleButton);
    expect(caretIcon).not.toHaveClass('expanded');
  });

  it('does not render the banner if cookie "MALWARE_BANNER" is close', async () => {
    Object.defineProperty(document, 'cookie', {
      get: () => 'MALWARE_BANNER=close',
      set: mockSetCookie,
      configurable: true,
    });

    await act(async () => {
      render(<MaliciousRiskOnDisk />);
    });
    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  async function expectAlertToRender(count, isAdmin) {
    expect(selectors.getHeading(count + ' ' + RISK_ON_DISK.TITLE_PLURAL)).toBeInTheDocument();
    expect(selectors.queryAlert()).toHaveTextContent(RISK_ON_DISK.DESCRIPTION.CONTENT);

    if (isAdmin) {
      expect(selectors.queryLink(RISK_ON_DISK.VIEW_MALWARE_RISK)).toBeInTheDocument();
      expect(selectors.queryLink(RISK_ON_DISK.VIEW_MALWARE_RISK)).toHaveAttribute('href', '#browse/malwarerisk');
    } else {
      expect(selectors.queryLink(RISK_ON_DISK.VIEW_MALWARE_RISK)).not.toBeInTheDocument();
      expect(selectors.queryText('Contact your instance administrator to resolve.')).toBeInTheDocument();
    }
  }
});

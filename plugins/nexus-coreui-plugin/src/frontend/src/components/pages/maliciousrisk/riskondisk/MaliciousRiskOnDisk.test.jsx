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
import axios from "axios";
import {render, screen, waitForElementToBeRemoved} from "@testing-library/react";
import {act} from "react-dom/test-utils";

import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";

import {maliciousRiskOnDiskResponse} from "./MaliciousRiskOnDisk.testdata";
import MaliciousRiskOnDisk from "./MaliciousRiskOnDisk";

const {MALICIOUS_RISK_ON_DISK} = APIConstants.REST.PUBLIC;

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
    useUser: jest.fn()
  },
}));

const selectors = {
  ...TestUtils.selectors,
  getHeading: (t) => screen.getByRole('heading', {name: t}),
  queryButton: (t) => screen.queryByRole('button', {name: t}),
  queryLink: (t) => screen.queryByRole('link', {name: t}),
  queryAlert: () => screen.queryByRole('alert')
};

describe('MaliciousRiskOnDisk', () => {
  beforeEach(() => {
    when(ExtJS.state().getValue)
        .calledWith('nexus.malicious.risk.on.disk.enabled')
        .mockReturnValue(true);
    when(ExtJS.state().getValue)
        .calledWith('MaliciousRiskDashboard')
        .mockReturnValue(true);
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockResolvedValue({
      data: maliciousRiskOnDiskResponse
    });
  })

  async function renderView(isAdmin, isProEdition, page) {
    window.location.hash = `#browse/${page}`;
    ExtJS.isProEdition.mockReturnValue(isProEdition);
    ExtJS.useUser.mockReturnValue({'administrator': isAdmin});

    render(<MaliciousRiskOnDisk/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('does not render if user is not logged', async (page) => {
    ExtJS.useUser.mockReturnValue(null);
    await act(async () => {
      render(<MaliciousRiskOnDisk/>);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('does not render if feature flag is false', async (page) => {
    when(ExtJS.state().getValue)
        .calledWith('nexus.malicious.risk.on.disk.enabled')
        .mockReturnValue(false);
    await act(async () => {
      render(<MaliciousRiskOnDisk/>);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders error message if data fetch fails', async (page) => {
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockRejectedValue(new Error('Failed to fetch data'));
    await renderView(true, true, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    expect(selectors.queryAlert()).toHaveTextContent('Failed to fetch data');
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when admin user and pro edition', async (page) => {
    const isAdmin = true;
    const isProEdition = true;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, 123, 'Malicious Components Found in Your Repository',
        'Protect your repositories from malware with Sonatype Malware Defense.', showContactSonatypeBtn, isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when admin user and oss edition', async (page) => {
    const isAdmin = true;
    const isProEdition = false;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, 123, 'Malicious Components Found in Your Repository',
        'Protect your repositories from malware with Sonatype Malware Defense.', showContactSonatypeBtn, isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when non-admin user and pro edition', async (page) => {
    const isAdmin = false;
    const isProEdition = true;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, 123, 'Malicious Components Found in Your Repository',
        'Contact Sonatype or your Nexus Repository administrator for more information.', showContactSonatypeBtn,
        isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when non-admin user and oss edition', async (page) => {
    const isAdmin = false;
    const isProEdition = false;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, 123, 'Malicious Components Found in Your Repository',
        'Sonatype Repository Firewall identifies and blocks malware. Contact your Nexus Repository Administrator to resolve.',
        showContactSonatypeBtn, isProEdition);
  });

  async function expectAlertToRender(page, count, title, description, showContactSonatypeBtn, isProEdition) {
    expect(selectors.getHeading(count)).toBeInTheDocument();
    expect(selectors.getHeading(title)).toBeInTheDocument();
    expect(selectors.queryAlert()).toHaveTextContent(description);

    if (page === 'malicious') {
      expect(selectors.queryButton('View OSS Malware Risk')).not.toBeInTheDocument();
    }
    else {
      expect(selectors.queryButton('View OSS Malware Risk')).toBeInTheDocument();
      await userEvent.click(selectors.queryButton('View OSS Malware Risk'));
      expect(window.location.hash).toBe('#browse/maliciousrisk');
    }

    if (showContactSonatypeBtn) {
      expect(selectors.queryLink('Contact Sonatype')).toBeInTheDocument();

      if (isProEdition) {
        expect(selectors.queryLink('Contact Sonatype'))
            .toHaveAttribute('href',
                'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/pro-admin-learn-more');
      }
      else {
        expect(selectors.queryLink('Contact Sonatype'))
            .toHaveAttribute('href',
                'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/oss-admin-learn-more');
      }
    }
    else {
      expect(selectors.queryButton('Contact Sonatype')).not.toBeInTheDocument();
    }
  }
});

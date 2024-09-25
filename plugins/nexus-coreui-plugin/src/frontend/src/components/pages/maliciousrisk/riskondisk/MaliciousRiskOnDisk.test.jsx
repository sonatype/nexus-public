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

import {APIConstants, ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";

import {maliciousRiskOnDiskResponse, maliciousRiskOnDiskResponseWithCount0} from "./MaliciousRiskOnDisk.testdata";
import MaliciousRiskOnDisk from "./MaliciousRiskOnDisk";
import FeatureFlags from '../../../../constants/FeatureFlags';
import MaliciousRiskStrings from "../../../../constants/pages/maliciousrisk/MaliciousRiskStrings";

const {MALICIOUS_RISK_ON_DISK} = APIConstants.REST.PUBLIC;
const {
  MALWARE_RISK_ENABLED,
  MALWARE_RISK_ON_DISK_ENABLED
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
        .calledWith(MALWARE_RISK_ON_DISK_ENABLED)
        .mockReturnValue(true);
    when(ExtJS.state().getValue)
        .calledWith(MALWARE_RISK_ENABLED)
        .mockReturnValue(true);
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockResolvedValue({
      data: maliciousRiskOnDiskResponse
    });
  })

  async function renderView(isAdmin, isProEdition, page) {
    window.location.hash = `#browse/${page}`;
    ExtJS.isProEdition.mockReturnValue(isProEdition);
    ExtJS.useUser.mockReturnValue({'administrator': isAdmin});
    const rerender = jest.fn();

    render(<MaliciousRiskOnDisk rerender={rerender}/>);
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
        .calledWith(MALWARE_RISK_ON_DISK_ENABLED)
        .mockReturnValue(false);
    await act(async () => {
      render(<MaliciousRiskOnDisk/>);
    });

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('does not render if malicious count is 0', async (page) => {
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockResolvedValue({
      data: maliciousRiskOnDiskResponseWithCount0
    });
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
    await expectAlertToRender(page, '1,234,567', RISK_ON_DISK.TITLE_PLURAL, RISK_ON_DISK.DESCRIPTION.CONTENT,
        showContactSonatypeBtn, isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when admin user and oss edition', async (page) => {
    const isAdmin = true;
    const isProEdition = false;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, '1,234,567', RISK_ON_DISK.TITLE_PLURAL, RISK_ON_DISK.DESCRIPTION.CONTENT,
        showContactSonatypeBtn, isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when non-admin user and pro edition', async (page) => {
    const isAdmin = false;
    const isProEdition = true;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, '1,234,567', RISK_ON_DISK.TITLE_PLURAL, RISK_ON_DISK.DESCRIPTION.CONTENT,
        showContactSonatypeBtn, isProEdition);
  });

  it.each(['maliciousRisk', 'welcome', 'browse', 'search'])
  ('renders correctly when non-admin user and oss edition', async (page) => {
    const isAdmin = false;
    const isProEdition = false;
    const showContactSonatypeBtn = isAdmin;

    await renderView(isAdmin, isProEdition, page);

    expect(selectors.queryAlert()).toBeInTheDocument();
    await expectAlertToRender(page, '1,234,567', RISK_ON_DISK.TITLE_PLURAL, RISK_ON_DISK.DESCRIPTION.CONTENT,
        showContactSonatypeBtn, isProEdition);
  });

  async function expectAlertToRender(page, count, title, description, showContactSonatypeBtn, isProEdition) {
    expect(selectors.getHeading(count)).toBeInTheDocument();
    expect(selectors.getHeading(title)).toBeInTheDocument();
    expect(selectors.queryAlert()).toHaveTextContent(description);

    if (showContactSonatypeBtn) {
      expect(selectors.queryLink('Contact Sonatype to Resolve')).toBeInTheDocument();

      if (isProEdition) {
        expect(selectors.queryLink('Contact Sonatype to Resolve'))
            .toHaveAttribute('href',
                'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/pro-admin-learn-more');
      }
      else {
        expect(selectors.queryLink('Contact Sonatype to Resolve'))
            .toHaveAttribute('href',
                'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/oss-admin-learn-more');
      }
    }
    else {
      expect(selectors.queryButton('Contact Sonatype')).not.toBeInTheDocument();
    }
  }
});

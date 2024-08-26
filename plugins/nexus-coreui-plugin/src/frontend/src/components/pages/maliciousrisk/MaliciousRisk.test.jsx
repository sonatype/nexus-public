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
import {when} from "jest-when";
import axios from "axios";
import {render, screen, waitForElementToBeRemoved, within} from "@testing-library/react";

import MaliciousRisk from "./MaliciousRisk";
import {
  maliciousRiskProxyFullyProtectedResponse,
  maliciousRiskProxyPartiallyProtectedResponse,
  maliciousRiskProxyUnprotectedResponse,
  maliciousRiskResponseWithHdsError
} from "./MaliciousRisk.testdata";
import {APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

const {MALICIOUS_RISK_SUMMARY} = APIConstants.REST.PUBLIC;

const selectors = {
  ...TestUtils.selectors,
  getHeading: (t) => screen.getByRole('heading', {name: t}),
  getTextLink: (t) => screen.getByRole('link', {name: t}),
  getText: (t) => screen.getByText(t),
  getEcoSystem: (id) => screen.getByTestId(id),
  getEcoSystemInfoIcon: (s) => s.querySelector('[data-icon="info-circle"]'),
  getEcoSystemIndicator: (s) => within(s).queryByRole('status'),
  containsText: (t) => screen.getByText(t, {exact: false}),
  getAllText: (t) => screen.getAllByText(t),
  getId: (t) => screen.getByTestId(t),
  queryAlert: () => screen.queryByRole('alert'),
};

const content = 'Open Source malware exploits the open source DevOps tool chain to introduce malware such as ' +
    'credential harvester, crypto-miner, a virus, ransomware, data corruption, malicious code injector, etc.'

describe('MaliciousRisk Fully Protected', () => {
  async function renderView() {
    when(axios.get).calledWith(MALICIOUS_RISK_SUMMARY).mockResolvedValue({
      data: maliciousRiskProxyFullyProtectedResponse
    });

    render(<MaliciousRisk/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  it('should render malicious events content widget', async () => {
    await renderView();

    expect(selectors.getHeading('Fully Protected from Malware')).toBeInTheDocument();
    expect(selectors.getHeading('Proxy Repository Protection')).toBeInTheDocument();
    expect(selectors.containsText(
        '16000 malicious events identified by Sonatype')).toBeInTheDocument();
    expect(selectors.getAllText('10 / 10 total').length).toBe(2);
    expect(selectors.getId('meter')).toBeInTheDocument();

    const moreLink = selectors.getTextLink('more');
    expect(moreLink).toBeInTheDocument();
    expect(moreLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/language-and-package-support');

    const howToProtectLink = selectors.getTextLink('How can I protect my repositories?');
    expect(howToProtectLink).toBeInTheDocument();
    expect(howToProtectLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall');
  });
});

describe('MaliciousRisk Partially Protected', () => {
  async function renderView() {
    when(axios.get).calledWith(MALICIOUS_RISK_SUMMARY).mockResolvedValue({
      data: maliciousRiskProxyPartiallyProtectedResponse
    });

    render(<MaliciousRisk/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  it('should render malicious components in high risk ecosystems without an indicator', async () => {
    await renderView();

    expect(selectors.getHeading('Open Source Malware in High Risk Ecosystems')).toBeInTheDocument();
    await expectEcoSystemToRender('npm', 10000, false);
    await expectEcoSystemToRender('pypi', 5000, false);
    await expectEcoSystemToRender('maven', 1000, false);
  });

  it('should render malicious events content widget', async () => {
    await renderView();

    expect(selectors.getHeading('Partially Protected from Malware')).toBeInTheDocument();
    expect(selectors.getHeading('Proxy Repository Protection')).toBeInTheDocument();
    expect(selectors.containsText(
        '16000 malicious events identified by Sonatype')).toBeInTheDocument();
    expect(selectors.getAllText('3 / 10 total').length).toBe(2);
    expect(selectors.getId('meter')).toBeInTheDocument();

    const moreLink = selectors.getTextLink('more');
    expect(moreLink).toBeInTheDocument();
    expect(moreLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/language-and-package-support');

    const howToProtectLink = selectors.getTextLink('How can I protect my repositories?');
    expect(howToProtectLink).toBeInTheDocument();
    expect(howToProtectLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall');
  });
});

describe('MaliciousRisk unprotected', () => {
  async function renderView(res = maliciousRiskProxyUnprotectedResponse) {
    when(axios.get).calledWith(MALICIOUS_RISK_SUMMARY).mockResolvedValue({
      data: res
    });

    render(<MaliciousRisk/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  it('renders error page', async () => {
    const message = 'Server Error';
    axios.get.mockRejectedValue({message});

    render(<MaliciousRisk/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryLoadError()).toBeInTheDocument();
  });

  it('should render malicious components contents', async () => {
    await renderView();

    expect(selectors.getHeading('Open Source Malware Risk')).toBeInTheDocument();
    expect(selectors.getHeading('What Is Open Source Malware?')).toBeInTheDocument();
    expect(selectors.getHeading('Average Cost to Remediate OSS Malware')).toBeInTheDocument();
    expect(selectors.getHeading('$5.12 million')).toBeInTheDocument();
    expect(selectors.getText(content)).toBeInTheDocument();

    const learnMoreLink = selectors.getTextLink('Learn More');
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/press-releases');
  });

  it('should render malicious components in high risk ecosystems', async () => {
    await renderView();

    expect(selectors.getHeading('Open Source Malware in High Risk Ecosystems')).toBeInTheDocument();
    await expectEcoSystemToRender('npm', 10000);
    await expectEcoSystemToRender('pypi', 5000);
    await expectEcoSystemToRender('maven', 1000);
  });

  it('should render malicious events content widget', async () => {
    await renderView();

    expect(selectors.getHeading('Unprotected from Malware')).toBeInTheDocument();
    expect(selectors.getHeading('Proxy Repository Protection')).toBeInTheDocument();
    expect(selectors.containsText(
        '16000 malicious events identified by Sonatype')).toBeInTheDocument();
    expect(selectors.getAllText('0 / 10 total').length).toBe(2);
    expect(selectors.getId('meter')).toBeInTheDocument();

    const moreLink = selectors.getTextLink('more');
    expect(moreLink).toBeInTheDocument();
    expect(moreLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/language-and-package-support');

    const howToProtectLink = selectors.getTextLink('How can I protect my repositories?');
    expect(howToProtectLink).toBeInTheDocument();
    expect(howToProtectLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall');
  });

  it('should not render warning alert when no hds connection error', async () => {
    await renderView();

    expect(selectors.queryAlert()).not.toBeInTheDocument();
  });

  it('should render warning alert when hds connection error', async () => {
    await renderView(maliciousRiskResponseWithHdsError);

    const hdsWarningAlert = selectors.queryAlert();
    expect(hdsWarningAlert).toBeInTheDocument();
    expect(hdsWarningAlert).toHaveTextContent('OSS Malware Risk data relies on backend services that are currently ' +
        'unreachable. To view malware risk, ensure the required URLs are accessible');
    expect(selectors.getTextLink('ensure the required URLs are accessible')).toBeInTheDocument();
  });
})

async function expectEcoSystemToRender(name, count, expectedIndicator = true) {
  const ecosystem = selectors.getEcoSystem(name);
  expect(ecosystem).toBeInTheDocument();

  const indicator = selectors.getEcoSystemIndicator(ecosystem);
  if (expectedIndicator) {
    expect(indicator).toBeInTheDocument();
    expect(indicator).toHaveTextContent('0 repositories protected');
  }
  else {
    expect(indicator).not.toBeInTheDocument();
  }

  const infoIcon = selectors.getEcoSystemInfoIcon(ecosystem);
  expect(infoIcon).toBeInTheDocument();
  await TestUtils.expectToSeeTooltipOnHover(infoIcon,
      'Total amount of malicious components found across this ecosystem’s public repositories');

  expect(selectors.getHeading(count)).toBeInTheDocument();
}

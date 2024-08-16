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
import {maliciousRiskResponse} from "./MaliciousRisk.testdata";
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
  getEcoSystemIndicator: (s) => within(s).getByRole('status'),
  containsText: (t) => screen.getByText(t, {exact: false}),
  getAllText: (t) => screen.getAllByText(t),
  getId: (t) => screen.getByTestId(t),
};

const content = 'Malicious components exploit the open source DevOps tool chain to introduce malware such as ' +
    'credential harvester, crypto-miner, a virus, ransomware, data corruption, malicious code injector, etc.'

describe('MaliciousRisk', () => {
  async function renderView() {
    when(axios.get).calledWith(MALICIOUS_RISK_SUMMARY).mockResolvedValue({
      data: maliciousRiskResponse
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
    expect(selectors.getHeading('Malicious Components are Malware')).toBeInTheDocument();
    expect(selectors.getHeading('Average Cost to Remediate a Malicious Attack')).toBeInTheDocument();
    expect(selectors.getHeading('$5.12 million')).toBeInTheDocument();
    expect(selectors.getText(content)).toBeInTheDocument();

    const learnMoreLink = selectors.getTextLink('Learn More');
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink).toHaveAttribute('href',
        'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/press-releases');
  });

  it('should render malicious components in high risk ecosystems', async () => {
    await renderView();

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
})

async function expectEcoSystemToRender(name, count) {
  const ecosystem = selectors.getEcoSystem(name);
  expect(ecosystem).toBeInTheDocument();

  const indicator = selectors.getEcoSystemIndicator(ecosystem);
  expect(indicator).toBeInTheDocument();
  expect(indicator).toHaveTextContent('0 repositories protected');

  const infoIcon = selectors.getEcoSystemInfoIcon(ecosystem);
  expect(infoIcon).toBeInTheDocument();
  await TestUtils.expectToSeeTooltipOnHover(infoIcon,
      'Total amount of malicious components found across this ecosystem’s public repositories');

  expect(selectors.getHeading(count)).toBeInTheDocument();
}

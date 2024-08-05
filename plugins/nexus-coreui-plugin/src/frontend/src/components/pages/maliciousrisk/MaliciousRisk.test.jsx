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
import {when} from "jest-when";
import axios from "axios";
import {render, waitForElementToBeRemoved} from "@testing-library/react";
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import React from "react";
import {maliciousRiskResponse} from "./MaliciousRisk.testdata";
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";
import MaliciousRisk from "./MaliciousRisk";

const {MALICIOUS_RISK_SUMMARY} = APIConstants.REST.PUBLIC;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

const selectors = {
  ...TestUtils.selectors,
};

describe('MaliciousRisk', () => {
  beforeEach(() => {
    jest.spyOn(ExtJS, 'state').mockReturnValue({getValue: () => false});
  });

  it('renders error page', async () => {
    const message = 'Server Error';
    axios.get.mockRejectedValue({message});

    render(<MaliciousRisk />);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryLoadError()).toBeInTheDocument();
  });

  it('renders correctly', async () => {
    when(axios.get).calledWith(MALICIOUS_RISK_SUMMARY).mockResolvedValue({
      data: maliciousRiskResponse
    });

    render(<MaliciousRisk />);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

  });
})

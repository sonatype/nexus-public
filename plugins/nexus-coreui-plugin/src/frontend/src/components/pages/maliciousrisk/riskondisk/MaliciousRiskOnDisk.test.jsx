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
import {render, screen} from "@testing-library/react";
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import React from "react";
import {maliciousRiskOnDiskResponse} from "./MaliciousRiskOnDisk.testdata";
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";
import MaliciousRiskOnDisk from "./MaliciousRiskOnDisk";
import {act} from "react-dom/test-utils";

const {MALICIOUS_RISK_ON_DISK} = APIConstants.REST.PUBLIC;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

const selectors = {
  ...TestUtils.selectors,
  getText: (t) => screen.getByText(t),
};

describe('MaliciousRiskOnDisk', () => {
  beforeEach(() => {
    const user = {
      'administrator': true
    };

    jest.spyOn(ExtJS, 'state').mockReturnValue({getValue: () => true});
    jest.spyOn(ExtJS, 'useUser').mockImplementation( () => user);
  });

  it('renders correctly', async () => {
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockResolvedValue({
      data: maliciousRiskOnDiskResponse
    });

    await act(async() => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(selectors.getText('Total count: 123')).toBeInTheDocument();
  });

  it('does not render if user is not logged', async () => {
    const user = null;

    jest.spyOn(ExtJS, 'useUser').mockImplementation( () => user);

    await act(async() => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(screen.queryByText('Total count: 123')).not.toBeInTheDocument();
  });

  it('does not render if feature flag is false', async () => {
    jest.spyOn(ExtJS, 'state').mockReturnValue({getValue: () => false});

    await act(async() => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(screen.queryByText('Total count: 123')).not.toBeInTheDocument();
  });

  it('renders error message if data fetch fails', async () => {
    when(axios.get).calledWith(MALICIOUS_RISK_ON_DISK).mockRejectedValue(new Error('Failed to fetch data'));

    await act(async() => {
      render(<MaliciousRiskOnDisk />);
    });

    expect(selectors.getText('An error occurred loading data. Error loading malicious risk on disk data')).toBeInTheDocument();
  });
})

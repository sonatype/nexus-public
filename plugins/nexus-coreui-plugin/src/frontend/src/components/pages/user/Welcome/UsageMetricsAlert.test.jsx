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
import {render, screen} from '@testing-library/react';
import userEvent from "@testing-library/user-event";
import {when} from "jest-when";

import UsageMetricsAlert from './UsageMetricsAlert';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {SOFT_LIMIT_REACHED} from './UsageMetrics.testdata';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
  }
}));

const selectors = {
  ...TestUtils.selectors,
  getAlert: () => screen.getByRole('alert'),
  getCloseButton: () => screen.getByRole('button', {name: 'Close'})
};

describe('Usage Metrics Alert', () => {
  async function renderView(usage, onClose = null) {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult')
        .mockReturnValue(usage);

    render(<UsageMetricsAlert onClose={onClose}/>);
  };

  it("renders the warning when at least one limit is reached", async () => {
    await renderView(SOFT_LIMIT_REACHED);

    const alert = selectors.getAlert();
    const alertMessage = 'This repository is approaching the maximum of 75,000 components. Users will not be ' +
      'able to upload to this repository once this limit is reached. Review your usage and consider removing ' +
      'unused components or consider upgrading to Pro for unlimited usage.'

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(alertMessage);

  });

  it("tests the close button in the alert", async () => {
    const onClose = jest.fn();
    await renderView(SOFT_LIMIT_REACHED, onClose);

    const alert = selectors.getAlert();
    expect(alert).toBeInTheDocument();
    userEvent.click(selectors.getCloseButton());
    expect(onClose).toBeCalled();
  });
});

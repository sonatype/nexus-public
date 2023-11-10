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
import {when} from "jest-when";
import TestUtils from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils";
import {SOFT_LIMIT_REACHED} from "./UsageMetrics.testdata";
import {act} from "react-dom/test-utils";
import UsageMetricsAlert from "./UsageMetricsAlert";
import {ExtJS} from "@sonatype/nexus-ui-plugin";
import userEvent from "@testing-library/user-event";

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
  },
}));

const selectors = {
  ...TestUtils.selectors,
  getWarningMessage: () => screen.getByRole('alert'),
  getCloseButton: () => screen.getByRole('button'),
};

describe('Usage Metrics Alert', () => {
  beforeEach(() => {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult')
        .mockReturnValue(SOFT_LIMIT_REACHED);
  });

  it("renders the warning when at least one limit is reached", async () => {

    await act(async () => {
      render(<UsageMetricsAlert/>);
    });

    const alert = selectors.getWarningMessage();
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent('This repository is approaching the maximum of 75,000 components');

  });

  it("tests the close button in the alert", async () => {

    const onClose = jest.fn();
    await act(async () => {
      render(<UsageMetricsAlert onClose={onClose}/>);
    });

    const alert = selectors.getWarningMessage();
    expect(alert).toBeInTheDocument();
    userEvent.click(selectors.getCloseButton());
    expect(onClose).toBeCalled();
  });
});

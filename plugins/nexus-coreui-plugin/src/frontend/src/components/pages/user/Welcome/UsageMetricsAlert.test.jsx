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
import {HARD_LIMIT_REACHED, WARNING_LIMIT_REACHED} from './UsageMetricsAlert.testdata';

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
  getCloseButton: () => screen.getByRole('button', {name: 'Close'}),
  getLinks: (t) => screen.getAllByRole('link', {name: t})
};

describe('Usage Metrics Alert', () => {
  async function renderView(usage, onClose = null) {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult')
        .mockReturnValue(usage);

    render(<UsageMetricsAlert onClose={onClose}/>);
  }

  it('renders hard limit alert when hard limit value reached', async () => {
    await renderView(HARD_LIMIT_REACHED);

    const alert = selectors.getAlert();
    const componentCountMessage = 'Users can not currently upload to this repository. This repository contains the ' +
        'maximum of 120,000 components. Review your usage and consider removing unused components or ' +
        'upgrading to Pro for unlimited usage.';
    const requestsPerDayMessage = 'Users can not currently upload to this repository. This repository has hit ' +
        'the maximum of 200,000 peak requests in the past 30 days. Review your usage and consider upgrading to Pro ' +
        'for unlimited usage.';

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(componentCountMessage);
    expect(alert).toHaveTextContent(requestsPerDayMessage);
    expectLinksToBeRendered('Learn about Pro', 'Review your usage', 'upgrading to Pro');
  });

  it('renders warning limit alert when warning limit value reached', async () => {
    await renderView(WARNING_LIMIT_REACHED);

    const alert = selectors.getAlert();
    const componentCountMessage = 'This repository is approaching the maximum of 120,000 components. ' +
        'Users will not be able to upload to this repository after reaching this limit. ' +
        'Review your usage and consider removing unused components or upgrading to Pro for unlimited usage.';
    const requestsPerDayMessage = 'This repository is approaching the maximum of 200,000 peak requests in the past 30 days. ' +
        'Users will not be able to upload to this repository after reaching this limit. ' +
        'Review your usage and consider upgrading to Pro for unlimited usage.';

    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(componentCountMessage);
    expect(alert).toHaveTextContent(requestsPerDayMessage);
    expectLinksToBeRendered('Review your usage', 'upgrading to Pro');
  });

  it("tests the close button in the alert", async () => {
    const onClose = jest.fn();
    await renderView(WARNING_LIMIT_REACHED, onClose);

    const alert = selectors.getAlert();

    expect(alert).toBeInTheDocument();
    userEvent.click(selectors.getCloseButton());
    expect(onClose).toBeCalled();
  });
});

function expectLinksToBeRendered(...links) {

  for (let l of links) {
    const links = selectors.getLinks(l);
    if (l === 'Learn about Pro') {
      expect(links.length).toBe(1);
      expect(links[0]).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/learn-about-pro');
    } else if (l === 'Review your usage') {
      expect(links.length).toBe(2);
      expect(links[0]).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/review-usage');
      expect(links[1]).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/review-usage');
    } else {
      expect(links.length).toBe(2);
      expect(links[0]).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/upgrade-to-pro');
      expect(links[1]).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/upgrade-to-pro');
    }
  }
}

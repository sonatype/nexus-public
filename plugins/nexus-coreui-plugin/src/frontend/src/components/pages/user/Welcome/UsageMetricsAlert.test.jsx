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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {
  INVALID_THRESHOLD_VALUE_DATA,
  NO_THRESHOLD_NAME_DATA,
  NO_THRESHOLD_VALUE_DATA,
  NO_THRESHOLDS_DATA,
  STARTER_THRESHOLD_REACHED,
  SOFT_THRESHOLD_REACHED,
  NO_USAGE_LEVEL_DATA
} from './UsageMetricsAlert.testdata';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
    isProStarterEdition: jest.fn().mockReturnValue(true),
  }
}));

const selectors = {
  getAlert: () => screen.getByRole('alert'),
  queryAlert: () => screen.queryByRole('alert'),
  getCloseButton: () => screen.getByRole('button', {name: 'Close'}),
  getLinks: (t) => screen.getAllByRole('link', {name: t})
};

describe('Usage Metrics Alert', () => {
  async function renderView(usage, onClose = null) {
    when(ExtJS.state().getValue)
        .calledWith('contentUsageEvaluationResult', [])
        .mockReturnValue(usage);

    render(<UsageMetricsAlert onClose={onClose}/>);
  }

  beforeEach(function() {
    global.NX = {
      I18n: {get: jest.fn().mockReturnValue('')},
    };
  })

  it('renders error alert when starter threshold value reached', async () => {
    await renderView(STARTER_THRESHOLD_REACHED);

    const alert = selectors.getAlert();

    expect(alert).toBeInTheDocument();
    expectLinksToBeRendered('Learn about Pro', 'Review your usage', 'upgrading to Pro');
  });

  it('renders warning alert when soft threshold value reached', async () => {
    await renderView(SOFT_THRESHOLD_REACHED);

    const alert = selectors.getAlert();

    expect(alert).toBeInTheDocument();
    expectLinksToBeRendered('Review your usage', 'upgrading to Pro');
  });

  it("tests the close button in the alert", async () => {
    const onClose = jest.fn();
    await renderView(SOFT_THRESHOLD_REACHED, onClose);

    const alert = selectors.getAlert();
    expect(alert).toBeInTheDocument();
    userEvent.click(selectors.getCloseButton());
    expect(onClose).toBeCalled();
  });

  it('should not render alert when there is no metrics', async () => {
    await renderView([]);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when there is no usage level', async () => {
    await renderView(NO_USAGE_LEVEL_DATA);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when there is no thresholds in metrics', async () => {
    await renderView(NO_THRESHOLDS_DATA);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when there is no threshold value in thresholds', async () => {
    await renderView(NO_THRESHOLD_NAME_DATA);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when there is no threshold name in thresholds', async () => {
    await renderView(NO_THRESHOLD_VALUE_DATA);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when invalid threshold value in thresholds', async () => {
    await renderView(INVALID_THRESHOLD_VALUE_DATA);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
  });

  it('should not render alert when edition is not pro-starter', async () => {
    ExtJS.isProStarterEdition.mockReturnValue(false);
    await renderView(SOFT_THRESHOLD_REACHED);
    const alert = selectors.queryAlert();
    expect(alert).not.toBeInTheDocument();
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

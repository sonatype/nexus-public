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

import UsageMetricsAlert from './UsageMetricsAlert';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {HARD_LIMIT_REACHED} from './UsageMetricsAlert.testdata';

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
  getLearnAboutLink: () => screen.getByRole('link', {name: 'Learn about Pro'})
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
    const link = selectors.getLearnAboutLink();
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(componentCountMessage);
    expect(alert).toHaveTextContent(requestsPerDayMessage);
    expect(link).toBeInTheDocument();
  });
});

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
import Axios from 'axios';
import {act} from 'react-dom/test-utils';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import HealthCheckEula from './HealthCheckEula';

jest.mock('axios', () => {
  return {
    put: jest.fn(() => Promise.resolve()),
  };
});

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      urlOf: jest.fn()
    }
  }
});

describe('HealthCheckEula', () => {
  const render = () => TestUtils.render(<HealthCheckEula onAccept={() => {}} onDecline={() => {}}/>, ({getByText}) => ({
    acceptButton: () => getByText(UIStrings.HEALTHCHECK_EULA.BUTTONS.ACCEPT),
    declineButton: () => getByText(UIStrings.HEALTHCHECK_EULA.BUTTONS.DECLINE),
  }));

  it('sends the accept eula message', async () => {
    let {acceptButton} = render();

    await act(async () => userEvent.click(acceptButton()));

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(`service/rest/internal/ui/ahc`);
  });

  it('does not send accept eula message', async () => {
    let {declineButton} = render();

    await act(async () => userEvent.click(declineButton()));

    expect(Axios.put).toHaveBeenCalledTimes(0);
  });
});

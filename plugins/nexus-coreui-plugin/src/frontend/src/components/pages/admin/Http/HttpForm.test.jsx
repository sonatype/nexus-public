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
import {
  render,
  screen,
  waitForElementToBeRemoved,
  act,
} from '@testing-library/react';
import {when} from 'jest-when';
import Axios from 'axios';
import HttpForm from './HttpForm';
import {
  TestUtils,
  APIConstants,
  ExtJS,
  ExtAPIUtils,
} from '@sonatype/nexus-ui-plugin';
import userEvent from '@testing-library/user-event';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
  },
}));

const {
  EXT: {
    URL,
    HTTP: {ACTION, METHODS},
  },
} = APIConstants;

const {
  HTTP: {CONFIGURATION: LABELS},
  ERROR,
} = UIStrings;

const REQUEST = ExtAPIUtils.createRequestBody(ACTION, METHODS.READ);
const UPDATE = ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE);

const selectors = {
  ...TestUtils.selectors,
  userAgent: () => screen.getByText(LABELS.USER_AGENT.LABEL),
  userAgentInput: () => screen.getByLabelText(LABELS.USER_AGENT.LABEL),
  timeout: () => screen.getByText(LABELS.TIMEOUT.LABEL),
  timeoutInput: () => screen.getByLabelText(LABELS.TIMEOUT.LABEL),
  retries: () => screen.getByText(LABELS.ATTEMPTS.LABEL),
  retriesInput: () => screen.getByLabelText(LABELS.ATTEMPTS.LABEL),
  saveButton: () => screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
};

global.NX = {
  Messages: {
    success: jest.fn(),
    error: jest.fn(),
  },
};

const mock = {
  userAgentSuffix: null,
  timeout: null,
  retries: null,
  httpEnabled: null,
  httpHost: null,
  httpPort: null,
  httpAuthEnabled: null,
  httpAuthUsername: null,
  httpAuthPassword: null,
  httpAuthNtlmHost: null,
  httpAuthNtlmDomain: null,
  httpsEnabled: null,
  httpsHost: null,
  httpsPort: null,
  httpsAuthEnabled: null,
  httpsAuthUsername: null,
  httpsAuthPassword: null,
  httpsAuthNtlmHost: null,
  httpsAuthNtlmDomain: null,
  nonProxyHosts: null,
};

describe('HttpForm', () => {
  const renderAndWaitForLoad = async () => {
    render(<HttpForm />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  beforeEach(() => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult(mock),
      });

    when(Axios.post)
      .calledWith(URL, UPDATE)
      .mockResolvedValue({
        data: TestUtils.makeExtResult({}),
      });
  });

  it('renders correctly', async () => {
    const {retries, userAgent, timeout} = selectors;
    await renderAndWaitForLoad();

    expect(userAgent()).toBeInTheDocument();
    expect(timeout()).toBeInTheDocument();
    expect(retries()).toBeInTheDocument();
  });

  it('connection timeout should be greater than 0, less than or equal to 3600', async () => {
    const {timeoutInput, saveButton} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.changeField(timeoutInput, 'text');
    expect(screen.getByText(ERROR.NAN)).toBeInTheDocument();

    await TestUtils.changeField(timeoutInput, '0');
    expect(saveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(saveButton()).toHaveAttribute(
      'aria-label',
      'Submit disabled: Validation errors are present'
    );

    await TestUtils.changeField(timeoutInput, '3601');
    expect(saveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(saveButton()).toHaveAttribute(
      'aria-label',
      'Submit disabled: Validation errors are present'
    );

    await TestUtils.changeField(timeoutInput, '1');
    expect(timeoutInput()).toHaveValue('1');
  });

  it('connection attempts should be greater or equal to 0, less than or equal to 10', async () => {
    const {retriesInput, saveButton} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.changeField(retriesInput, 'text');
    expect(screen.getByText(ERROR.NAN)).toBeInTheDocument();

    await TestUtils.changeField(retriesInput, '11');
    expect(saveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(saveButton()).toHaveAttribute(
      'aria-label',
      'Submit disabled: Validation errors are present'
    );

    await TestUtils.changeField(retriesInput, '1');

    expect(retriesInput()).toHaveValue('1');
  });

  it('saves the changes', async () => {
    const userAgentSuffix = 'agent';
    const timeout = '1000';
    const retries = '0';
    const {retriesInput, userAgentInput, timeoutInput, saveButton} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.changeField(userAgentInput, userAgentSuffix);
    await TestUtils.changeField(retriesInput, retries);
    await TestUtils.changeField(timeoutInput, timeout);

    Axios.post = jest
      .fn()
      .mockReturnValue(Promise.resolve({data: TestUtils.makeExtResult({})}));

    await act(async () => await userEvent.click(saveButton()));

    const expected = {
      data: [
        {
          ...mock,
          userAgentSuffix,
          timeout,
          retries,
        },
      ],
    };

    expect(Axios.post).toHaveBeenCalledWith(
      URL,
      ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, expected)
    );
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });
});

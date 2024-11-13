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
import {act} from 'react-dom/test-utils';
import {interpret} from 'xstate';
import {waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import axios from "axios";

import {APIConstants, ExtJS, DateUtils} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import {UserTokenForm} from './UserToken';
import UserTokenMachine from './UserTokenMachine';

const mockToken = 'fakeToken'
const mockTokenB64 = 'ZmFrZVRva2Vu'
const API_URL = APIConstants.REST.USER_TOKEN_TIMESTAMP;

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      requestAuthenticationToken: jest.fn(() => Promise.resolve(mockToken))
    },
    DateUtils: {
      prettyDateTimeLong: jest.fn()
    }
  }
});

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn((url) => {
      if (url === `/service/rest/internal/current-user/user-token?authToken=${mockTokenB64}`) {
        return Promise.resolve({
          data: {
            nameCode: 'fWlnS_RJ',
            passCode: 'EAQvjLMhKd0JYV_05Ig7yIg8mwrOyBO61AdsPS7bsJma',
            created: '2020-04-23T21:49:40.112+01:00'
          }
        });
      }
      if (url === 'service/rest/internal/current-user/user-token/attributes') {
        return Promise.resolve({
          data: {
            "expirationTimeTimestamp": "1713498443006",
            "expirationEnabled": "true"
          }
        });
      }
    }),
    post: jest.fn((url) => {
      if (url === `/service/rest/internal/current-user/user-token?authToken=${mockTokenB64}`) {
        return Promise.resolve({
          data: {
            nameCode: 'EjGJcwfm',
            passCode: 'WE9UAaEQJQzh5NrmNJ22I44Eh80_h3oORkkO0EaoB4UR',
            created: '2024-04-23T21:49:40.112+01:00'
          }
        });
      }
    }),
    delete: jest.fn((url) => {
      if (url === `/service/rest/internal/current-user/user-token?authToken=${mockTokenB64}`) {
        return Promise.resolve();
      }
    }),
  };
});

describe('UserToken', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  function renderView(view) {
    return TestUtils.render(view, ({getByLabelText, getByText, queryByRole}) => ({
      accessTokenButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.ACCESS),
      resetTokenButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.RESET),
      userTokenNameCodeField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.USER_TOKEN_NAME_CODE),
      userTokenPassCodeField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.USER_TOKEN_PASS_CODE),
      mavenUsageField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.MAVEN_USAGE),
      b64UserTokenField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.BASE64_USER_TOKEN),
      closeButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.CLOSE),
      userTokenStatusText: () => getByText(UIStrings.USER_TOKEN.USER_TOKEN_STATUS.TEXT),
      userTokenStatusDescription: () => getByText(UIStrings.USER_TOKEN.USER_TOKEN_STATUS.DESCRIPTION),
      timestamp: (t) => getByText(t),
      expiredAlert: () => queryByRole('alert'),
      alertCloseButton: (c) => within(c).queryByRole('button', {name: 'Close'}),
      generateTokenButton: () => queryByRole('button', {name: 'Generate User Token'})
    }));
  }

  it('renders without crashing', async () => {
    const service = interpret(UserTokenMachine).start();
    const {
      loadingMask,
      accessTokenButton,
      resetTokenButton
    } = renderView(<UserTokenForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));
    expect(loadingMask()).not.toBeInTheDocument();
    expect(accessTokenButton()).toBeEnabled();
    expect(resetTokenButton()).toBeEnabled();
  });

  it('Resets user token requires authentication and displays success message', async () => {
    const service = interpret(UserTokenMachine).start();
    const {resetTokenButton} = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await act(async () => userEvent.click(resetTokenButton()));

    expect(ExtJS.requestAuthenticationToken).toHaveBeenCalled();
  });

  it('Accesses user token requires authentication and displays the token', async () => {
    const service = interpret(UserTokenMachine).start();
    const {
      accessTokenButton,
      userTokenNameCodeField,
      userTokenPassCodeField,
      b64UserTokenField,
      mavenUsageField,
      closeButton
    } = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await act(async () => userEvent.click(accessTokenButton()));

    expect(ExtJS.requestAuthenticationToken).toHaveBeenCalled();
    await waitFor(() => expect(service.state.value).toBe('showToken'));
    expect(userTokenNameCodeField()).toHaveValue('fWlnS_RJ');
    expect(userTokenPassCodeField()).toHaveValue('EAQvjLMhKd0JYV_05Ig7yIg8mwrOyBO61AdsPS7bsJma');
    expect(b64UserTokenField()).toHaveValue('ZldsblNfUko6RUFRdmpMTWhLZDBKWVZfMDVJZzd5SWc4bXdyT3lCTzYxQWRzUFM3YnNKbWE=');
    expect(mavenUsageField()).toHaveValue(
        '<server>\n\t<id>${server}</id>\n\t<username>fWlnS_RJ</username>\n\t<password>EAQvjLMhKd0JYV_05Ig7yIg8mwrOyBO61AdsPS7bsJma</password>\n</server>');

    await act(async () => userEvent.click(closeButton()));
    await waitFor(() => expect(service.state.value).toBe('loaded'));
  });

  it('Renders user token status with correct data', async () => {
    when(DateUtils.prettyDateTimeLong).calledWith(expect.any(Date)).mockReturnValue('Sunday, July 21, 2024 at 12:32:53 GMT+03:00');
    const service = interpret(UserTokenMachine).start();
    const {
      userTokenStatusText,
      userTokenStatusDescription,
      timestamp
    } = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    expect(userTokenStatusText()).toBeInTheDocument();
    expect(userTokenStatusDescription()).toBeInTheDocument();
    expect(timestamp('Expires: Sunday, July 21, 2024 at 12:32:53 GMT+03:00')).toBeInTheDocument();
  });

  it('Renders user token alert with close button when expired', async () => {
    when(axios.get).calledWith(API_URL).mockRejectedValue({response: {data: 'User Token expired'}});
    const service = interpret(UserTokenMachine).start();
    const {
      userTokenStatusText,
      userTokenStatusDescription,
      expiredAlert,
      alertCloseButton
    } = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    const closeButton = alertCloseButton(expiredAlert());

    expect(userTokenStatusText()).toBeInTheDocument();
    expect(userTokenStatusDescription()).toBeInTheDocument();
    expect(expiredAlert()).toHaveTextContent('Your user token has expired. Select generate user token to create a new one.');
    expect(closeButton).toBeInTheDocument();

    userEvent.click(closeButton);
    expect(expiredAlert()).not.toBeInTheDocument();
  });

  it('Renders generate user token button when expired', async () => {
    when(axios.get).calledWith(API_URL).mockRejectedValue({response: {data: 'User Token expired'}});
    const service = interpret(UserTokenMachine).start();
    const {generateTokenButton} = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    expect(generateTokenButton()).toBeInTheDocument();
  });

  it('Generates user token requires authentication and displays the token', async () => {
    when(axios.get).calledWith(API_URL).mockRejectedValue({response: {data: 'User Token expired'}});
    const service = interpret(UserTokenMachine).start();
    const {
      generateTokenButton,
      userTokenNameCodeField,
      userTokenPassCodeField,
      b64UserTokenField,
      mavenUsageField,
      closeButton
    } = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await act(async () => userEvent.click(generateTokenButton()));

    expect(ExtJS.requestAuthenticationToken).toHaveBeenCalled();
    await waitFor(() => expect(service.state.value).toBe('showToken'));
    expect(userTokenNameCodeField()).toHaveValue('EjGJcwfm');
    expect(userTokenPassCodeField()).toHaveValue('WE9UAaEQJQzh5NrmNJ22I44Eh80_h3oORkkO0EaoB4UR');
    expect(b64UserTokenField()).toHaveValue('RWpHSmN3Zm06V0U5VUFhRVFKUXpoNU5ybU5KMjJJNDRFaDgwX2gzb09Sa2tPMEVhb0I0VVI=');
    expect(mavenUsageField()).toHaveValue(
        '<server>\n\t<id>${server}</id>\n\t<username>EjGJcwfm</username>\n\t<password>WE9UAaEQJQzh5NrmNJ22I44Eh80_h3oORkkO0EaoB4UR</password>\n</server>');

    await act(async () => userEvent.click(closeButton()));
    await waitFor(() => expect(service.state.value).toBe('loaded'));
  });
});

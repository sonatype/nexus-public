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
import {waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import {UserTokenForm} from './UserToken';
import UserTokenMachine from './UserTokenMachine';

const mockToken = 'fakeToken'
const mockTokenB64 = 'ZmFrZVRva2Vu'

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      requestAuthenticationToken: jest.fn(() => Promise.resolve(mockToken))
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
    return TestUtils.render(view, ({getByLabelText, getByText}) => ({
      accessTokenButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.ACCESS),
      resetTokenButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.RESET),
      userTokenNameCodeField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.USER_TOKEN_NAME_CODE),
      userTokenPassCodeField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.USER_TOKEN_PASS_CODE),
      mavenUsageField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.MAVEN_USAGE),
      b64UserTokenField: () => getByLabelText(UIStrings.USER_TOKEN.LABELS.BASE64_USER_TOKEN),
      closeButton: () => getByText(UIStrings.USER_TOKEN.BUTTONS.CLOSE),
    }));
  }

  it('renders without crashing', async () => {
    const service = interpret(UserTokenMachine).start();
    const {
      loadingMask,
      accessTokenButton,
      resetTokenButton
    } = renderView(<UserTokenForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('idle'));
    expect(loadingMask()).not.toBeInTheDocument();
    expect(accessTokenButton()).toBeEnabled();
    expect(resetTokenButton()).toBeEnabled();
  });

  it('Reset user token requires authentication and displays success message', async () => {
    const service = interpret(UserTokenMachine).start();
    const {resetTokenButton} = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('idle'));

    await act(async () => userEvent.click(resetTokenButton()));

    expect(ExtJS.requestAuthenticationToken).toHaveBeenCalled();
  });

  it('Access user token requires authentication and displays the token', async () => {
    const service = interpret(UserTokenMachine).start();
    const {
      accessTokenButton,
      userTokenNameCodeField,
      userTokenPassCodeField,
      b64UserTokenField,
      mavenUsageField,
      closeButton
    } = renderView(<UserTokenForm service={service}/>);
    await waitFor(() => expect(service.state.value).toBe('idle'));

    await act(async () => userEvent.click(accessTokenButton()));

    expect(ExtJS.requestAuthenticationToken).toHaveBeenCalled();
    await waitFor(() => expect(service.state.value).toBe('showToken'));
    expect(userTokenNameCodeField()).toHaveValue('fWlnS_RJ');
    expect(userTokenPassCodeField()).toHaveValue('EAQvjLMhKd0JYV_05Ig7yIg8mwrOyBO61AdsPS7bsJma');
    expect(b64UserTokenField()).toHaveValue('ZldsblNfUko6RUFRdmpMTWhLZDBKWVZfMDVJZzd5SWc4bXdyT3lCTzYxQWRzUFM3YnNKbWE=');
    expect(mavenUsageField()).toHaveValue(
        '<server>\n\t<id>${server}</id>\n\t<username>fWlnS_RJ</username>\n\t<password>EAQvjLMhKd0JYV_05Ig7yIg8mwrOyBO61AdsPS7bsJma</password>\n</server>');

    await act(async () => userEvent.click(closeButton()));
    await waitFor(() => expect(service.state.value).toBe('idle'));
  });
});

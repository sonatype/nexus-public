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
import {waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import NuGetApiToken from './NuGetApiToken';
import UIStrings from '../../../../constants/UIStrings';

const mockToken = 'fakeToken'
const mockTokenB64 = 'ZmFrZVRva2Vu'

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      requestAuthenticationToken: jest.fn(() => Promise.resolve(mockToken)),
      absolutePath: jest.fn(() => 'http://localhost:4242/repository/fakeUrl'),
      showSuccessMessage: jest.fn()
    }
  };
});

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn((url) => {
      if (url === `/service/rest/internal/nuget-api-key?authToken=${mockTokenB64}`) {
        return Promise.resolve({data: {apiKey: 'testApiKey'}});
      }
    }),
    delete: jest.fn((url) => {
      if (url === `/service/rest/internal/nuget-api-key?authToken=${mockTokenB64}`) {
        return Promise.resolve({data: {apiKey: 'newTestApiKey'}});
      }
  }),
  };
});

describe('NuGetApiToken', () => {
  function renderView(view) {
    return TestUtils.render(view, ({queryByText, getByText}) => ({
      accessButton: () => getByText(UIStrings.NUGET_API_KEY.ACCESS.BUTTON),
      resetButton: () => getByText(UIStrings.NUGET_API_KEY.RESET.BUTTON),
      nugetKey: () => queryByText('testApiKey'),
      newNugetKey: () => queryByText('newTestApiKey')
    }));
  }

  it('renders correctly', async () => {
    let {accessButton, nugetKey} = renderView(<NuGetApiToken/>);

    await waitFor(() =>  expect(nugetKey()).not.toBeInTheDocument());

    await act(async () => userEvent.click(accessButton()));

    await waitFor(() =>  expect(nugetKey()).toBeInTheDocument());
  });

  it('uses the get call when the access button is pressed',  async () => {
    let { accessButton, nugetKey } = renderView(<NuGetApiToken/>);

    await act(async () => userEvent.click(accessButton()));

    await waitFor(() =>  expect(nugetKey()).toBeInTheDocument());

    expect(Axios.get).toHaveBeenCalledTimes(1);
    expect(Axios.get).toHaveBeenCalledWith(
        `/service/rest/internal/nuget-api-key?authToken=${mockTokenB64}`
    );
  });

  it('uses the delete call when the reset button is pressed',  async () => {
    let { resetButton } =  renderView(<NuGetApiToken/>);

    await act(async () => userEvent.click(resetButton()));

    expect(Axios.delete).toHaveBeenCalledTimes(1);
    expect(Axios.delete).toHaveBeenCalledWith(
        `/service/rest/internal/nuget-api-key?authToken=${mockTokenB64}`
    );
  });
});

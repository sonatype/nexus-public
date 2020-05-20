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
import {fireEvent, render, wait} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import NuGetApiToken from './NuGetApiToken';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('nexus-ui-plugin'),
    ExtJS: {
      requestAuthenticationToken: jest.fn(() => Promise.resolve({data: 'fakeToken'})),
      urlOf: jest.fn(() => 'http://localhost:4242/repository/fakeUrl'),
    }
  };
});


jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn((url) => {
      if (url === '/service/rest/internal/nugetApiKey?authToken=ZmFrZVRva2VuCg==') {
        return Promise.resolve({data: {data: {apiKey: 'testApiKey'}}});
      }
    }),
    delete: jest.fn((url) => {
      if (url === '/service/rest/internal/nugetApiKey?authToken=ZmFrZVRva2VuCg==') {
        return Promise.resolve({data: {data: {apiKey: 'newTestApiKey'}}});
      }
  }),
  };
});

describe('NuGetApiToken', () => {
  const renderView = async (view) => {
    let selectors = {};
    await act(async () => {
      let {container, getByText, queryByText} = render(view);
      selectors = {
        container,
        accessButton: () => getByText(UIStrings.NUGET_API_KEY.ACCESS.BUTTON),
        resetButton: () => getByText(UIStrings.NUGET_API_KEY.RESET.BUTTON),
        nugetKey: () => queryByText('testApiKey'),
        newNugetKey: () => queryByText('newTestApiKey')
      };
    });
    return selectors;
  };

  it('renders correctly', async () => {
    let { container, accessButton, resetButton, nugetKey } =
        await renderView(<NuGetApiToken/>);

    expect(container).toMatchSnapshot('baseline');
    await wait(() =>  expect(nugetKey()).not.toBeInTheDocument());

    await act(async () => fireEvent.click(accessButton()));

    await wait(() =>  expect(nugetKey()).toBeInTheDocument());

    expect(container).toMatchSnapshot('nugetKeyPresent');
  });

  it('uses the get call when the access button is pressed',  async () => {
    let {_, accessButton, resetButton, nugetKey  } =
        await renderView(<NuGetApiToken/>);

    await act(async () => fireEvent.click(accessButton()));

    await wait(() =>  expect(nugetKey()).toBeInTheDocument());

    expect(Axios.get).toHaveBeenCalledTimes(1);
    expect(Axios.get).toHaveBeenCalledWith(
        `/service/rest/internal/nugetApiKey?authToken=ZmFrZVRva2VuCg==`
    );
  });

  it('uses the delete call when the reset button is pressed',  async () => {
    let {_, accessButton, resetButton, nugetKey  } =
        await renderView(<NuGetApiToken/>);

    await act(async () => fireEvent.click(resetButton()));

    expect(Axios.delete).toHaveBeenCalledTimes(1);
    expect(Axios.delete).toHaveBeenCalledWith(
        `/service/rest/internal/nugetApiKey?authToken=ZmFrZVRva2VuCg==`
    );
  });
});

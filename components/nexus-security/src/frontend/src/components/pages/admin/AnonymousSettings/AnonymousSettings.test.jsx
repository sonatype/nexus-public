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
import {mount, shallow} from 'enzyme';
import React from 'react';

import Axios from 'axios';
import AnonymousSettings from './AnonymousSettings';
import Button from '../../../../components/widgets/Button/Button';
import Checkbox from '../../../../components/widgets/Checkbox/Checkbox';
import Select from '../../../../components/widgets/Select/Select';
import Textfield from '../../../../components/widgets/Textfield/Textfield';
import UIStrings from '../../../../constants/UIStrings';

const mockRealmTypes = [
  {id: 'r1', name: 'Realm One'},
  {id: 'r2', name: 'Realm Two'}
];
const mockAnonymousSettings = {
  enabled: true,
  userId: 'testUser',
  realmName: 'r2'
};

jest.mock('axios', () => {  // Mock out parts of axios, has to be done in same scope as import statements
  return {
    ...jest.requireActual('axios'), // Use most functions from actual axios
    get: jest.fn((url) => {
      switch (url) {
        case '/service/rest/internal/ui/realms/types':
          return Promise.resolve({data: mockRealmTypes});
        case '/service/rest/internal/ui/anonymous-settings':
          return Promise.resolve({data: mockAnonymousSettings});
      }
    }),
    put: jest.fn(() => Promise.resolve())
  };
});

jest.mock('../../../../interface/ExtJS');

describe('AnonymousSettings', () => {
  // see https://github.com/airbnb/enzyme/issues/1587
  const waitForDataFromApi = (wrapper) => Promise.resolve(wrapper)
      .then(() => wrapper.update())
      .then(() => wrapper.update());

  const assertDefaultValues = (wrapper) => {
    expect(wrapper.find(Checkbox)).toHaveProp('isChecked', mockAnonymousSettings.enabled);
    expect(wrapper.find(Textfield)).toHaveProp('value', mockAnonymousSettings.userId);
    expect(wrapper.find(Select)).toHaveProp('value', mockAnonymousSettings.realmName);
  };

  it('renders correctly', () => {
    expect(shallow(<AnonymousSettings/>)).toMatchSnapshot();
  });

  it('fetches the values of fields from the API and updates them as expected', async () => {
    // using mount because of bug preventing useEffect hook from being called when component is shallow rendered
    // See https://github.com/airbnb/enzyme/issues/2086
    const wrapper = mount(<AnonymousSettings/>);

    await waitForDataFromApi(wrapper);

    expect(Axios.get).toHaveBeenCalledTimes(2);
    assertDefaultValues(wrapper);
  });

  it('Sends changes to the API on save', async () => {
    // using mount because of bug preventing useEffect hook from being called when component is shallow rendered
    // See https://github.com/airbnb/enzyme/issues/2086
    const wrapper = mount(<AnonymousSettings/>);

    await waitForDataFromApi(wrapper);

    expect(Axios.get).toHaveBeenCalledTimes(2);
    expect(Axios.put).toHaveBeenCalledTimes(0);

    wrapper.find(Textfield).simulate('change', {
      target: {
        type: 'text',
        name: 'userId',
        value: 'changed-username'
      }
    });

    wrapper.findWhere(node =>
        node.is(Button) && node.text() === UIStrings.SETTINGS.SAVE_BUTTON_LABEL
    ).simulate('click');

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        '/service/rest/internal/ui/anonymous-settings',
        {
          ...mockAnonymousSettings,
          userId: 'changed-username'
        }
    );
  });

  it('Sends nothing to the API on discard', async () => {
    // using mount because of bug preventing useEffect hook from being called when component is shallow rendered
    // See https://github.com/airbnb/enzyme/issues/2086
    const wrapper = mount(<AnonymousSettings/>);

    await waitForDataFromApi(wrapper);

    wrapper.find(Select).simulate('change', {
      target: {
        name: 'realmName',
        value: 'r2'
      }
    });

    wrapper.findWhere(node =>
        node.is(Button) && node.text() === UIStrings.SETTINGS.DISCARD_BUTTON_LABEL
    ).simulate('click');

    expect(Axios.put).toHaveBeenCalledTimes(0);
    assertDefaultValues(wrapper);
  });

  it('Does not allow the user to save if the required username is empty', async () => {
    // using mount because of bug preventing useEffect hook from being called when component is shallow rendered
    // See https://github.com/airbnb/enzyme/issues/2086
    const wrapper = mount(<AnonymousSettings/>);

    await waitForDataFromApi(wrapper);

    wrapper.find(Select).simulate('change', {
      target: {
        name: 'userId',
        value: ''
      }
    });

    const saveButton = wrapper.findWhere(node =>
        node.is(Button) && node.text() === UIStrings.SETTINGS.SAVE_BUTTON_LABEL
    );

    expect(saveButton).toBeDisabled();
  });
});

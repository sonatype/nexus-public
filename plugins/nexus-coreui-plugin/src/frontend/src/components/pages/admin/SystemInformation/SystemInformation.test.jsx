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
import { act } from 'react-dom/test-utils';

import Axios from 'axios';
import SystemInformation from './SystemInformation';
import { Button, Checkbox, Textfield, Select } from 'nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => {  // Mock response from axios
  return {
    ...jest.requireActual('axios'), // Use most functions from actual axios
    get: jest.fn((url) => Promise.resolve({
      data: {
        'nexus-status': {
          'status': 'value'
        },
        'nexus-node': {
          'node': 0
        },
        'nexus-configuration': {
          'enabled': true
        },
        'nexus-properties': {
          'property': false
        },
        'nexus-license': {
          'fingerprint': 'hash'
        },
        'nexus-bundles': {
          '0': {
            'bundleId': 0,
            'enabled': true,
            'config': 'value'
          }
        },
        'system-time': {
          'time': 'value'
        },
        'system-properties': {
          'property': 'value'
        },
        'system-environment': {
          'property': 'value'
        },
        'system-runtime': {
          'property': 'value'
        },
        'system-network': {
          'en0': {
            'enabled': true
          }
        },
        'system-filestores': {
          '/dev/null': {
            'path': '/dev/null'
          }
        }
      }
    }))
  };
});

describe('SystemInformation', () => {
  // see https://github.com/airbnb/enzyme/issues/1587
  const waitForDataFromApi = (wrapper) => act(() => Promise.resolve(wrapper)
      .then(() => wrapper.update())
      .then(() => wrapper.update())
  );

  it('renders correctly', async () => {
    const wrapper = mount(<SystemInformation/>);

    await waitForDataFromApi(wrapper);

    expect(wrapper).toMatchSnapshot();
  });
});

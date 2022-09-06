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
import {render, waitForElementToBeRemoved} from '@testing-library/react';
import axios from 'axios';
import {TestUtils} from '@sonatype/nexus-ui-plugin';

import MetricHealth from './MetricHealth';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn()
}));

describe('MetricHealth', function() {
  it('renders the resolved data', async function() {
    const metrics = {
      "Available CPUs" : {
        "healthy" : true,
        "message" : "The host system is allocating a maximum of 6 cores to the application.",
        "error" : null,
        "details" : null,
        "time" : 1588629343436,
        "duration" : 0,
        "timestamp" : "2020-05-04T15:55:43.436-06:00"
      },
      "testError" : {
        "healthy" : false,
        "message" : "TestError",
        "error" : {
          "cause" : null,
          "stackTrace" : [ {
            "methodName" : "getSystemStatusChecks",
            "fileName" : "HealthCheckResource.java",
            "lineNumber" : 61,
            "className" : "org.sonatype.nexus.internal.rest.HealthCheckResource",
            "nativeMethod" : false
          }, {
            "methodName" : "CGLIB$getSystemStatusChecks$0",
            "fileName" : "<generated>",
            "lineNumber" : -1,
            "className" : "org.sonatype.nexus.internal.rest.HealthCheckResource$$EnhancerByGuice$$1b6f529d",
            "nativeMethod" : false
          } ],
          "message" : "TestError",
          "localizedMessage" : "TestError",
          "suppressed" : [ ]
        }
      }
    };

    axios.get.mockReturnValue(Promise.resolve({
      data: metrics
    }));

    const {container} = render(<MetricHealth/>);

    await waitForElementToBeRemoved(TestUtils.selectors.queryLoadingMask());

    expect(container.querySelector(`tbody tr:nth-child(1) td:nth-child(2)`)).toHaveTextContent("Available CPUs");
    expect(container.querySelector(`tbody tr:nth-child(1) td:nth-child(3)`)).toHaveTextContent("The host system is allocating a maximum of 6 cores to the application.");
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container} = render(<MetricHealth/>);

    await waitForElementToBeRemoved(TestUtils.selectors.queryLoadingMask());

    expect(container.querySelector('.nx-cell--meta-info')).toHaveTextContent('Error');
  });
});

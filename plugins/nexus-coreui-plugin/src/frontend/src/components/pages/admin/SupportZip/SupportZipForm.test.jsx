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
import { act } from 'react-dom/test-utils';
import {fireEvent, render, wait} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import SupportZipForm from './SupportZipForm';

describe('SupportZipForm', function() {
  const renderView = async (params, setParams, submit, clustered, hazips) => {
    var selectors;

    await act(async () => {
      selectors = render(<SupportZipForm params={params} setParams={setParams} submit={submit} clustered={clustered} hazips={hazips} />);
    });

    return selectors;
  };

  it('renders support zip form', async function() {
    const params = {
      systemInformation: true,
      threadDump: false,
      configuration: true,
      security: false,
      log: true,
      taskLog: false,
      auditLog: true,
      metrics: false,
      jmx: true,
      limitFileSizes: false,
      limitZipSize: true
    };

    const {container} = await renderView(params);

    expect(container.querySelector('input#systemInformation')).toBeChecked();
    expect(container.querySelector('input#threadDump')).not.toBeChecked();
    expect(container.querySelector('input#configuration')).toBeChecked();
    expect(container.querySelector('input#security')).not.toBeChecked();
    expect(container.querySelector('input#log')).toBeChecked();
    expect(container.querySelector('input#taskLog')).not.toBeChecked();
    expect(container.querySelector('input#auditLog')).toBeChecked();
    expect(container.querySelector('input#metrics')).not.toBeChecked();
    expect(container.querySelector('input#jmx')).toBeChecked();
    expect(container.querySelector('input#limitFileSizes')).not.toBeChecked();
    expect(container.querySelector('input#limitZipSize')).toBeChecked();  

    expect(container).toMatchSnapshot();
  });

  it('updates the parameters when clicked', async function() {
    const params = {
      systemInformation: true,
      threadDump: false,
      configuration: true,
      security: false,
      log: true,
      taskLog: false,
      auditLog: true,
      metrics: false,
      jmx: true,
      limitFileSizes: false,
      limitZipSize: true
    };
    const setParams = jest.fn((event) => event.persist());
    const {container} = await renderView(params, setParams);

    Object.entries(params).forEach(([name, value]) => {
      fireEvent.click(container.querySelector(`input#${name}`));
      expect(setParams.mock.calls.slice(-1)[0][0].target.id).toBe(name);
    });
    expect(setParams).toHaveBeenCalledTimes(Object.entries(params).length);
  });

  it('renders button for all nodes only when clustered', async function() {
    const params = {
      systemInformation: true,
      threadDump: false,
      configuration: true,
      security: false,
      log: true,
      taskLog: false,
      auditLog: true,
      metrics: false,
      jmx: true,
      limitFileSizes: false,
      limitZipSize: true
    };

    const nonHa = (await renderView(params, undefined, undefined, false, undefined)).container;
    expect(nonHa.querySelectorAll('button')).toHaveLength(1);

    const ha = (await renderView(params, undefined, undefined, true, undefined)).container;
    expect(ha.querySelectorAll('button')).toHaveLength(2);
  });
});

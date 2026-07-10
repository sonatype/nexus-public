/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {interpret} from 'xstate';
import welcomeMachine from '../WelcomeMachine';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtAPIUtils: {
    extAPIBulkRequest: jest.fn(),
    checkForError: jest.fn(),
    extractResult: jest.fn(),
  },
  ExtJS: {
    state: jest.fn(() => ({
      getUser: jest.fn(() => null),
      getValue: jest.fn(() => null),
    })),
  },
  APIConstants: {
    EXT: {
      OUTREACH: {
        ACTION: 'outreach',
        METHODS: {
          READ_STATUS: 'readStatus',
          GET_PROXY_DOWNLOAD_NUMBERS: 'getProxyDownloadNumbers',
        },
      },
    },
  },
}));

jest.mock('../../../../../constants/UIStrings', () => ({
  default: {ERROR: {UNKNOWN: 'An unknown error occurred'}},
}));

describe('WelcomeMachine', () => {
  it('is defined', () => {
    expect(welcomeMachine).toBeDefined();
  });

  it('has the id WelcomeMachine', () => {
    expect(welcomeMachine.id).toBe('WelcomeMachine');
  });

  it('starts in the loaded state', () => {
    const service = interpret(welcomeMachine).start();
    expect(service.state.value).toBe('loaded');
    service.stop();
  });

  it('transitions to loading when LOAD event is sent', () => {
    const service = interpret(welcomeMachine).start();
    service.send('LOAD');
    expect(service.state.value).toBe('loading');
    service.stop();
  });

  it('has setData and setError actions defined', () => {
    expect(welcomeMachine.options.actions).toHaveProperty('setData');
    expect(welcomeMachine.options.actions).toHaveProperty('setError');
  });

  it('has a fetch service defined', () => {
    expect(welcomeMachine.options.services).toHaveProperty('fetch');
    expect(typeof welcomeMachine.options.services.fetch).toBe('function');
  });

  it('fetch service calls extAPIBulkRequest for non-CORE editions', async () => {
    const {ExtAPIUtils, ExtJS} = jest.requireMock('@sonatype/nexus-ui-plugin');
    ExtJS.state.mockReturnValue({
      getUser: jest.fn(() => ({id: 'user1'})),
      getValue: jest.fn(() => 'COMMUNITY'),
    });
    ExtAPIUtils.extAPIBulkRequest.mockResolvedValue({
      data: [
        {method: 'readStatus', result: {success: true, data: 'abc'}},
        {method: 'getProxyDownloadNumbers', result: {data: {}}},
      ],
    });
    ExtAPIUtils.checkForError.mockImplementation(() => {});
    ExtAPIUtils.extractResult.mockReturnValue({count: 5});

    const fetchFn = welcomeMachine.options.services.fetch;
    await fetchFn({}, {});
    expect(ExtAPIUtils.extAPIBulkRequest).toHaveBeenCalled();
  });

  it('fetch service returns early for CORE edition without calling bulk request', async () => {
    const {ExtAPIUtils, ExtJS} = jest.requireMock('@sonatype/nexus-ui-plugin');
    ExtJS.state.mockReturnValue({
      getUser: jest.fn(() => null),
      getValue: jest.fn((key) => key === 'status' ? {edition: 'CORE'} : null),
    });

    const fetchFn = welcomeMachine.options.services.fetch;
    const result = await fetchFn({}, {});
    expect(ExtAPIUtils.extAPIBulkRequest).not.toHaveBeenCalled();
    expect(result).toHaveProperty('proxyDownloadNumberParams');
  });

  it('fetch service propagates errors from extAPIBulkRequest', async () => {
    const {ExtAPIUtils, ExtJS} = jest.requireMock('@sonatype/nexus-ui-plugin');
    ExtJS.state.mockReturnValue({
      getUser: jest.fn(() => ({id: 'user1'})),
      getValue: jest.fn(() => 'COMMUNITY'),
    });
    ExtAPIUtils.extAPIBulkRequest.mockRejectedValue(new Error('network error'));

    const fetchFn = welcomeMachine.options.services.fetch;
    await expect(fetchFn({}, {})).rejects.toThrow('network error');
  });
});

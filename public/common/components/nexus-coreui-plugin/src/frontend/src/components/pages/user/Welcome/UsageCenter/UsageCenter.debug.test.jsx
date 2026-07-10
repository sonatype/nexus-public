import React from 'react';
import {render, screen} from '@testing-library/react';
import {when} from 'jest-when';

import UsageCenter from './UsageCenter';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {act} from 'react-dom/test-utils';
import {
  USAGE_CENTER_CONTENT_CE} from './UsageCenter.testdata';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(false),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
      getUser: jest.fn().mockReturnValue({ administrator: true }),
      getEdition: jest.fn().mockReturnValue('COMMUNITY')
    }),
    useState: jest.fn()
  },
}));

describe('Debug test', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('debug - check what useState functions return', async () => {
    ExtJS.isProEdition.mockReturnValue(false);
    ExtJS.state().getEdition.mockReturnValue('COMMUNITY');

    // Track all useState calls
    const useStateCalls = [];
    ExtJS.useState.mockImplementation((arg) => {
      if (typeof arg === 'function') {
        const result = arg();
        useStateCalls.push({ arg: 'function', result });
        console.log('useState function returned:', result);
        return result;
      }
      useStateCalls.push({ arg: 'value', result: arg });
      return arg;
    });

    when(ExtJS.state().getValue)
      .calledWith('nexus.community.throttlingStatus')
      .mockReturnValue('Under limits');

    when(ExtJS.state().getValue)
      .calledWith('contentUsageEvaluationResult', [])
      .mockReturnValue(USAGE_CENTER_CONTENT_CE);

    when(ExtJS.state().getValue)
      .calledWith('nexus.community.componentCountLimitDateLastExceeded')
      .mockReturnValue('2024-11-01T00:00:00.000');

    when(ExtJS.state().getValue)
      .calledWith('nexus.community.requestPer24HoursLimitDateLastExceeded')
      .mockReturnValue('2024-11-01T00:00:00.000');

    when(ExtJS.state().getValue)
      .calledWith('nexus.datastore.clustered.enabled')
      .mockReturnValue(false);

    await act(async () => {
      render(<UsageCenter />);
    });

    console.log('All useState calls:', useStateCalls);
    console.log('DOM:', document.body.innerHTML);
  });
});

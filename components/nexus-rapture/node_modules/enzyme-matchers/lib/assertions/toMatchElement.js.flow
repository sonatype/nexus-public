/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toMatchElementAssertion
 * @flow
 */

import { shallow, mount } from 'enzyme';
import type { EnzymeObject, Matcher, ToMatchElementOptions } from '../types';
import isShallowWrapper from '../utils/isShallowWrapper';
import single from '../utils/single';

function toMatchElement(
  actualEnzymeWrapper: EnzymeObject,
  reactInstance: Object,
  options: ToMatchElementOptions = { ignoreProps: true }
): Matcher {
  let expectedWrapper: EnzymeObject;
  if (!isShallowWrapper(actualEnzymeWrapper)) {
    expectedWrapper = mount(reactInstance);
  } else {
    expectedWrapper = shallow(reactInstance);
  }

  const actual = actualEnzymeWrapper.debug({ verbose: true, ...options });
  const expected = expectedWrapper.debug({ verbose: true, ...options });
  const pass = actual === expected;

  return {
    pass,
    message: 'Expected actual value to match the expected value.',
    negatedMessage: 'Did not expect actual value to match the expected value.',
    contextualInformation: {
      actual: `Actual:\n ${actual}`,
      expected: `Expected:\n ${expected}`,
    },
  };
}

export default single(toMatchElement);

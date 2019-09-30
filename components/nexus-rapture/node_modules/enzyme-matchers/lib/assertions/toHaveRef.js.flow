/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveRefAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import single from '../utils/single';

function toHaveRef(enzymeWrapper: EnzymeObject, refName: string): Matcher {
  if (typeof enzymeWrapper.ref !== 'function') {
    throw new Error(
      'EnzymeMatchers::toHaveRef can not be called on a shallow wrapper'
    );
  }

  const node = enzymeWrapper.ref(refName);
  const pass = !!node;

  return {
    pass,
    message: `Expected to find a ref named "${refName}" on <${name(
      enzymeWrapper
    )}>, but didn't.`,
    negatedMessage: `Expected not to find a ref named "${refName}" on <${name(
      enzymeWrapper
    )}>, but did.`,
    contextualInformation: {},
  };
}

export default single(toHaveRef);

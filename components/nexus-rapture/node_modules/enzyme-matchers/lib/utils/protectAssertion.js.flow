/**
 * @function protectAssertion
 *
 * This should wrap every assertion this library outputs.
 * It is intended to help with developers to understand errors
 * when an enzyme-matchers assertion is used with a non-enzyme object.
 *
 * @flow
 */

import type { Assertion, EnzymeObject, Matcher } from '../types';

function heuristicCheck(arg: any): boolean {
  try {
    const shouldBeEmptyEnzyme = arg.find('asjdfsaf');
    return shouldBeEmptyEnzyme.length === 0;
  } catch (e) {
    return false;
  }
}

const ERROR_MESSAGE = assertion =>
  `The test assertion ${assertion.name} is part of the enzyme-matcher suite.
It appears you tried calling this matcher with a non-enzyme object.
This assertion must be called against a shallow, mount, or render-ed react component.
`;

const protectAssertion = (assertion: Assertion): Assertion =>
  function assertionWrapper(enzymeWrapper: EnzymeObject, ...args): Matcher {
    if (heuristicCheck(enzymeWrapper) === false) {
      throw new Error(ERROR_MESSAGE(assertion));
    }

    // Using `.call` to make sure we bind the runtime environment into the Matcher
    // so we can use asymmetric equalities.
    return assertion.call(this, enzymeWrapper, ...args);
  };

export default protectAssertion;

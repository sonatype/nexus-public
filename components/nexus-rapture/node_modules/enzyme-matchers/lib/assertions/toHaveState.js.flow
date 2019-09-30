/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveStateAssertion
 * @flow
 */

import type { EnzymeObject, Matcher, ObjectReductionResponse } from '../types';
import name from '../utils/name';
import reduceAssertionObject from '../utils/reduceAssertionObject';
import stringify from '../utils/stringify';
import single from '../utils/single';

function toHaveState(
  enzymeWrapper: EnzymeObject,
  stateKey: Object | string,
  stateValue?: any
): Matcher {
  const state = enzymeWrapper.state();
  const wrapperName = name(enzymeWrapper);

  // The API allows checking if a component has a value for a given key by dropping the third
  // argument.
  if (
    stateValue === undefined &&
    arguments.length === 2 &&
    typeof stateKey !== 'object' &&
    Array.isArray(stateKey) === false
  ) {
    return {
      pass: state.hasOwnProperty(stateKey),
      message: `Expected the state for <${wrapperName}> to contain the key "${stateKey}", but it did not.`,
      negatedMessage: `Expected the state for <${wrapperName}> to not contain the key "${stateKey}", but it did.`,
      contextualInformation: {
        actual: `Actual state: ${stringify({ [stateKey]: state[stateKey] })}`,
      },
    };
  }

  const results: ObjectReductionResponse = reduceAssertionObject.call(
    this,
    state,
    stateKey,
    stateValue
  );
  const contextualInformation = {
    actual: `Actual state: ${stringify(results.actual)}`,
    expected: `Expected state: ${stringify(results.expected)}`,
  };

  // error if some state doesn't exist
  if (results.missingKeys.length) {
    const missingKeys = results.missingKeys.join(', ');
    const _key_ = results.missingKeys.length === 1 ? 'key' : 'keys';
    return {
      pass: false,
      message: `Expected the state for <${wrapperName}> to contain the ${_key_} "${missingKeys}", but it did not.`,
      negatedMessage: `Expected the state for <${wrapperName}> to not contain the ${_key_} "${missingKeys}", but it did.`,
      contextualInformation,
    };
  }

  const unmatchedKeys = results.unmatchedKeys.join(', ');
  const _key_ = results.unmatchedKeys.length === 1 ? 'key' : 'keys';
  return {
    pass: results.pass,
    message: `Expected the state for <${wrapperName}> to match for ${_key_} "${unmatchedKeys}", but it did not.`,
    negatedMessage: `Expected the state for <${wrapperName}> to not match for ${_key_} "${unmatchedKeys}", but it did.`,
    contextualInformation,
  };
}

export default single(toHaveState);

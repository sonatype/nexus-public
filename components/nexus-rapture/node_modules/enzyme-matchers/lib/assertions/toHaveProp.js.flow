/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHavePropAssertion
 * @flow
 */

import type { EnzymeObject, Matcher, ObjectReductionResponse } from '../types';
import name from '../utils/name';
import reduceAssertionObject from '../utils/reduceAssertionObject';
import stringify from '../utils/stringify';
import single from '../utils/single';

function toHaveProp(
  enzymeWrapper: EnzymeObject,
  propKey: string | Object,
  propValue?: any
): Matcher {
  const props = enzymeWrapper.props();
  const wrapperName = name(enzymeWrapper);

  // The API allows to check if a component has a prop in general by dropping the third
  // argument.
  if (
    propValue === undefined &&
    arguments.length === 2 &&
    typeof propKey !== 'object' &&
    Array.isArray(propKey) === false
  ) {
    return {
      pass: props.hasOwnProperty(propKey),
      message: `Expected <${wrapperName}> to have received the prop "${propKey}", but it did not.`,
      negatedMessage: `Expected <${wrapperName}> to not have received the prop "${propKey}", but it did.`,
      contextualInformation: {
        actual: `Actual props: ${stringify({ [propKey]: props[propKey] })}`,
      },
    };
  }

  const results: ObjectReductionResponse = reduceAssertionObject.call(
    this,
    props,
    propKey,
    propValue
  );
  const unmatchedKeys = results.unmatchedKeys.join(', ');
  const contextualInformation = {
    actual: `Actual props: ${stringify(results.actual)}`,
    expected: `Expected props: ${stringify(results.expected)}`,
  };

  // error if some prop doesn't exist
  if (results.missingKeys.length) {
    const missingKeys = results.missingKeys.join(', ');
    const _prop_ = results.missingKeys.length === 1 ? 'prop' : 'props';
    return {
      pass: false,
      message: `Expected <${wrapperName}}> to have ${_prop_} "${missingKeys}", but it did not.`,
      negatedMessage: `Expected <${wrapperName}> to not have ${_prop_} "${missingKeys}", but it did.`,
      contextualInformation,
    };
  }

  const _prop_ = results.unmatchedKeys.length === 1 ? 'prop' : 'props';
  return {
    pass: results.pass,
    message: `Expected <${wrapperName}> to match for ${_prop_} "${unmatchedKeys}", but it did not.`,
    negatedMessage: `Expected <${wrapperName}> to not match for ${_prop_} "${unmatchedKeys}", but it did.`,
    contextualInformation,
  };
}

export default single(toHaveProp);

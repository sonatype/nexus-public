/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveStyleAssertion
 * @flow
 */

import type { EnzymeObject, Matcher, ObjectReductionResponse } from '../types';
import name from '../utils/name';
import stringify from '../utils/stringify';
import reduceAssertionObject from '../utils/reduceAssertionObject';
import html from '../utils/html';
import single from '../utils/single';

function flattenStyle(style: ?any): ?Object {
  if (!style) {
    return undefined;
  }

  if (!Array.isArray(style)) {
    return style;
  }

  return style.reduce(
    (computedStyle, currentStyle) => ({
      ...computedStyle,
      ...flattenStyle(currentStyle),
    }),
    undefined
  );
}

function toHaveStyle(
  enzymeWrapper: EnzymeObject,
  styleKey: string | Object,
  styleValue?: any
): Matcher {
  const style = flattenStyle(enzymeWrapper.prop('style'));
  const wrapperName = name(enzymeWrapper);

  // 1. If the component doesn't have a style prop in general. That's an immediate failure.
  if (!style) {
    return {
      pass: false,
      message: `Expected <${wrapperName}> component to have a style prop but it did not.`,
      negatedMessage: `Expected <${wrapperName}> component not to have a style prop but it did.`,
      contextualInformation: {
        actual: html(enzymeWrapper),
      },
    };
  }

  // 2. If the assertion is to check if the component has a style in general. We need to make sure
  // that its not an object and intended for the object assertion API.
  // Then we have to make sure the style has that key.
  if (
    styleKey === undefined &&
    arguments.length === 2 &&
    typeof styleKey !== 'object' &&
    Array.isArray(styleKey) === false
  ) {
    return {
      pass: style.hasOwnProperty(styleKey),
      message: `Expected <${wrapperName}> to have any value for the prop "${styleKey}"`,
      negatedMessage: `Expected <${wrapperName}> not to receive the prop "${styleKey}"`,
      contextualInformation: {
        actual: `Actual props: ${stringify({ [styleKey]: style[styleKey] })}`,
        expected: `Expected props: ${stringify({ [styleKey]: styleValue })}`,
      },
    };
  }

  const results: ObjectReductionResponse = reduceAssertionObject.call(
    this,
    style,
    styleKey,
    styleValue
  );
  const unmatchedKeys = results.unmatchedKeys.join(', ');
  const contextualInformation = {
    actual: `Actual style: ${stringify(results.actual)}`,
    expected: `Expected style: ${stringify(results.expected)}`,
  };

  if (results.missingKeys.length) {
    const missingKeys = results.missingKeys.join(', ');
    return {
      pass: false,
      message: `Expected <${wrapperName}> component to have a style keys of "${missingKeys}" but it did not.`,
      negatedMessage: `Expected <${wrapperName}> component not to have a style key of "${missingKeys}" but it did.`,
      contextualInformation: {
        actual: html(enzymeWrapper),
      },
    };
  }

  return {
    pass: results.pass,
    message: `Expected <${wrapperName}> component style values to match for key "${unmatchedKeys}", but they didn't`,
    negatedMessage: `Expected <${wrapperName}> component style values to be different for key "${unmatchedKeys}", but they weren't`,
    contextualInformation,
  };
}

export default single(toHaveStyle);

/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveValueAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import html from '../utils/html';
import single from '../utils/single';

function toHaveValue(enzymeWrapper: EnzymeObject, expectedValue: any): Matcher {
  let pass = false;

  const props = enzymeWrapper.props();
  const wrapperName = `<${name(enzymeWrapper)}>`;
  const wrapperHtml = html(enzymeWrapper);

  // set to the default checked
  if (props.hasOwnProperty('defaultValue')) {
    pass = props.defaultValue === expectedValue;
  }

  // if it has the `value` property, CHECK that
  if (props.hasOwnProperty('value')) {
    pass = props.value === expectedValue;
  }

  return {
    pass,
    message: `Expected ${wrapperName} component to have the value of "${expectedValue}" (using ===), but it didn't.`,
    negatedMessage: `Expected ${wrapperName} component not to have the value of "${expectedValue}" (using ===), but it did.`,
    contextualInformation: {
      actual: wrapperHtml,
    },
  };
}

export default single(toHaveValue);

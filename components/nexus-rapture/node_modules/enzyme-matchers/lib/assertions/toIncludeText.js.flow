/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toIncludeTextAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import single from '../utils/single';

function toIncludeText(enzymeWrapper: EnzymeObject, text: string): Matcher {
  const actualText = enzymeWrapper.text();

  if (text === undefined) {
    const message = `Expected ".toIncludeText(null)" to be given some text.
      If you are trying to assert this component has _some_ text, use the ".toHaveText()" matcher`;
    return {
      pass: false,
      message,
      negatedMessage: message,
      contextualInformation: {},
    };
  }

  const pass = actualText.includes(text);
  const wrapperName = `<${name(enzymeWrapper)}>`;

  return {
    pass,
    message: `Expected ${wrapperName} to contain "${text}" but it did not.`,
    negatedMessage: `Expected ${wrapperName} not to contain "${text}" but it did.`,
    contextualInformation: {
      expected: `Expected HTML: "${text}"`,
      actual: `Actual HTML: "${actualText}"`,
    },
  };
}

export default single(toIncludeText);

/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveHTMLAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import single from '../utils/single';

function toHaveHTML(enzymeWrapper: EnzymeObject, html: string): Matcher {
  const wrapperHTML = enzymeWrapper.html();

  // normalize quotes
  const useSingleQuotes = html.search("'") !== -1;

  const actualHTML = wrapperHTML.replace(/("|')/g, useSingleQuotes ? "'" : '"');
  const expectedHTML = html
    .replace(/("|')/g, useSingleQuotes ? "'" : '"')
    .replace(/>[\n\t ]+</g, '><');

  const pass = actualHTML === expectedHTML;

  return {
    pass,
    message: `Expected <${name(
      enzymeWrapper
    )}> html to match the expected, but it didn't.`,
    negatedMessage: `Expected <${name(
      enzymeWrapper
    )}> html not to match the expected, but it did.`,
    contextualInformation: {
      actual: `Actual HTML: ${actualHTML}`,
      expected: `Expected HTML: ${expectedHTML}`,
    },
  };
}

export default single(toHaveHTML);

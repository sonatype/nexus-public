/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toBeEmptyRenderAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import html from '../utils/html';
import single from '../utils/single';

function toBeEmptyRender(enzymeWrapper: EnzymeObject): Matcher {
  const pass = enzymeWrapper.isEmptyRender();

  return {
    pass,
    message: `Expected <${name(
      enzymeWrapper
    )}> to be empty render (false or null), but it was not`,
    negatedMessage: `Expected <${name(
      enzymeWrapper
    )}> not to be empty render (false or null), but it was`,
    contextualInformation: {
      actual: `Found Nodes HTML output: ${html(enzymeWrapper)}`,
    },
  };
}

export default single(toBeEmptyRender);

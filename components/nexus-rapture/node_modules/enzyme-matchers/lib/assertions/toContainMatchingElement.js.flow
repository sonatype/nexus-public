/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toContainMatchingElement
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import html from '../utils/html';
import getNodeName from '../utils/name';
import single from '../utils/single';

function toContainMatchingElement(
  enzymeWrapper: EnzymeObject,
  selector: string
): Matcher {
  const matches = enzymeWrapper.find(selector);
  const pass = matches.length > 0;
  const nodeName = getNodeName(enzymeWrapper);

  return {
    pass,
    message:
      `Expected <${nodeName}> to contain at least one element matching ` +
      `${selector} but none were found.`,
    negatedMessage:
      `Expected <${nodeName}> to not contain an element matching ` +
      `${selector} but it did.`,
    contextualInformation: {
      actual: `HTML Output of <${nodeName}>:\n ${html(enzymeWrapper)}`,
    },
  };
}

export default single(toContainMatchingElement);

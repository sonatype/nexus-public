/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveTagNameAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import html from '../utils/html';
import single from '../utils/single';

function toHaveDisplayName(enzymeWrapper: EnzymeObject, tag: string): Matcher {
  const wrapperHtml = html(enzymeWrapper);
  const actualTag = enzymeWrapper.name();
  const pass = actualTag === tag;

  const wrapperName = `<${name(enzymeWrapper)}>`;

  return {
    pass,
    message: `Expected ${wrapperName} to have display name "${tag}" but it had display name "${actualTag}".`,
    negatedMessage: `Expected ${wrapperName} to not have display name "${tag}" but it did.`,
    contextualInformation: {
      actual: wrapperHtml,
    },
  };
}

export default single(toHaveDisplayName);

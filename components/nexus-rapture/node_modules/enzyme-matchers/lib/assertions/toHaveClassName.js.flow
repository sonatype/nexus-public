/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveClassNameAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import name from '../utils/name';
import html from '../utils/html';

export default function toHaveClassName(
  enzymeWrapper: EnzymeObject,
  className: string
): Matcher {
  let normalizedClassName = className.split(' ').join('.');
  let actualClassName = '(none)';
  let pass = false;

  if (normalizedClassName[0] !== '.') {
    normalizedClassName = `.${normalizedClassName}`;
  }

  // handle different lengths of enzymeWrappers
  switch (enzymeWrapper.getElements().length) {
    case 0:
      break; // this will and should fail the test
    case 1:
      pass = enzymeWrapper.is(normalizedClassName);
      actualClassName = enzymeWrapper.prop('className');
      break;
    default:
      let allMatch = true;

      enzymeWrapper.forEach(node => {
        if (!node.is(normalizedClassName)) {
          allMatch = false;
        }
        actualClassName = node.prop('className');
      });

      pass = allMatch;
  }

  return {
    pass,
    message: `Expected <${name(
      enzymeWrapper
    )}> to have className of "${normalizedClassName}" but instead found "${actualClassName}"`, // eslint-disable-line max-len
    negatedMessage: `Expected <${name(
      enzymeWrapper
    )}> not to contain "${normalizedClassName}" in its className`,
    contextualInformation: {
      actual: `Found node output: ${html(enzymeWrapper)}`,
    },
  };
}

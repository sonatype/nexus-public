/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toContainReactAssertion
 * @flow
 */

import { shallow } from 'enzyme';
import type { EnzymeObject, Matcher } from '../types';
import html from '../utils/html';
import getNodeName from '../utils/name';
import single from '../utils/single';

function toContainReact(
  enzymeWrapper: EnzymeObject,
  reactInstance: Object
): Matcher {
  const wrappedInstance: EnzymeObject = shallow(reactInstance);
  const pass = enzymeWrapper.contains(reactInstance);

  return {
    pass,
    message: `Expected <${getNodeName(enzymeWrapper)}> to contain ${html(
      wrappedInstance
    )} but it was not found.`,
    negatedMessage: `Expected <${getNodeName(
      enzymeWrapper
    )}> not to contain ${html(wrappedInstance)} but it does.`,
    contextualInformation: {
      actual: `HTML Output of <${getNodeName(enzymeWrapper)}>:\n ${html(
        enzymeWrapper
      )}`,
    },
  };
}

export default single(toContainReact);

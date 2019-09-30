/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toBePresentAssertion
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import html from '../utils/html';
import getNodeName from '../utils/name';

export default function toExist(enzymeWrapper: EnzymeObject): Matcher {
  const pass = enzymeWrapper.exists();

  const contextualInformation = {};

  if (enzymeWrapper.getElements().length) {
    contextualInformation.actual = `Found Nodes: ${html(enzymeWrapper)}`;
  }

  const nodeName = getNodeName(enzymeWrapper);

  return {
    pass,
    message: `Expected "${nodeName}" to exist.`,
    negatedMessage: `Expected "${nodeName}" not to exist. Instead found ${enzymeWrapper.getElements()
      .length} nodes.`,
    contextualInformation,
  };
}

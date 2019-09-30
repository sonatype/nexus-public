/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toContainExactlyOneMatchingElement
 * @flow
 */

import type { EnzymeObject, Matcher } from '../types';
import toContainMatchingElements from './toContainMatchingElements';

function toContainExactlyOneMatchingElement(
  enzymeWrapper: EnzymeObject,
  selector: string
): Matcher {
  return toContainMatchingElements(enzymeWrapper, 1, selector);
}

export default toContainExactlyOneMatchingElement;

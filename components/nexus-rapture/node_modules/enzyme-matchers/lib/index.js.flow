/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule enzyme-matchrs
 * @flow
 */

import toBeChecked from './assertions/toBeChecked';
import toBeDisabled from './assertions/toBeDisabled';
import toBeEmptyRender from './assertions/toBeEmptyRender';
import toContainMatchingElement from './assertions/toContainMatchingElement';
import toContainMatchingElements from './assertions/toContainMatchingElements';
import toContainExactlyOneMatchingElement from './assertions/toContainExactlyOneMatchingElement';
import toContainReact from './assertions/toContainReact';
import toExist from './assertions/toExist';
import toHaveClassName from './assertions/toHaveClassName';
import toHaveDisplayName from './assertions/toHaveDisplayName';
import toHaveHTML from './assertions/toHaveHTML';
import toHaveProp from './assertions/toHaveProp';
import toHaveRef from './assertions/toHaveRef';
import toHaveState from './assertions/toHaveState';
import toHaveStyle from './assertions/toHaveStyle';
import toHaveTagName from './assertions/toHaveTagName';
import toHaveText from './assertions/toHaveText';
import toIncludeText from './assertions/toIncludeText';
import toHaveValue from './assertions/toHaveValue';
import toMatchSelector from './assertions/toMatchSelector';
import toMatchElement from './assertions/toMatchElement';

import protectAssertion from './utils/protectAssertion';

const assertions = {
  toBeChecked,
  toBeDisabled,
  toBeEmptyRender,
  toContainMatchingElement,
  toContainMatchingElements,
  toContainExactlyOneMatchingElement,
  toContainReact,
  toExist,
  toHaveClassName,
  toHaveDisplayName,
  toHaveHTML,
  toHaveProp,
  toHaveRef,
  toHaveState,
  toHaveStyle,
  toHaveTagName,
  toHaveText,
  toIncludeText,
  toHaveValue,
  toMatchSelector,
  toMatchElement,
};

// Protect all assertions from being called with not-enzyme-wrappers.
Object.keys(assertions).forEach(key => {
  assertions[key] = protectAssertion(assertions[key]);
});

export default assertions;

export type { EnzymeObject, Matcher, MatcherMethods } from './types';

// @flow

import type { EnzymeObject } from '../types';
import colors from './colors';

export default function deprecate(
  matcherFn: Function,
  message: string
): Function {
  let shouldWarn = true;
  return function deprecateWrapper(
    enzymeWrapper: EnzymeObject,
    ...args: Array<any>
  ) {
    if (shouldWarn) {
      // eslint-disable-next-line no-console
      console.warn(colors.yellow(message));
      shouldWarn = false;
    }

    return matcherFn.call(this, enzymeWrapper, ...args);
  };
}

// @flow

import type { EnzymeObject } from '../types';

const SHALLOW_WRAPPER_CONSTRUCTOR = 'ShallowWrapper';

export default function isShallowWrapper(wrapper: EnzymeObject): boolean {
  let isShallow;
  if (wrapper.constructor.name !== undefined) {
    isShallow = wrapper.constructor.name === SHALLOW_WRAPPER_CONSTRUCTOR;
  } else {
    isShallow = !!`${wrapper.constructor}`.match(/^function ShallowWrapper\(/);
  }
  return isShallow;
}

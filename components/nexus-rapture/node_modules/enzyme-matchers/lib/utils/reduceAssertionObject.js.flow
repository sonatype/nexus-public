/*
 * @flow
 */

import deepEqualIdent from 'deep-equal-ident';
import type { ObjectReductionResponse } from '../types';

type ObjectKey = Object | string;

export default function reduceAssertionObject(
  componentDetails: Object,
  objectOrKey: ObjectKey,
  potentialValue?: any
): ObjectReductionResponse {
  const matcherDetails =
    typeof objectOrKey === 'object' && !Array.isArray(objectOrKey)
      ? objectOrKey
      : { [objectOrKey]: potentialValue };

  const equals = this && this.equals ? this.equals : deepEqualIdent;

  return Object.keys(matcherDetails).reduce(
    (prevVal: ObjectReductionResponse, key: string) => {
      const retVal = { ...prevVal };
      const match = equals(componentDetails[key], matcherDetails[key]);
      retVal.actual[key] = componentDetails[key];
      retVal.expected[key] = matcherDetails[key];

      /*
       * This check helps us give better error messages when the componentDetails doesnt
       * include a specific key at all.
       */
      if (!componentDetails.hasOwnProperty(key)) {
        retVal.missingKeys.push(key);
        retVal.pass = false;
        return retVal;
      }

      /*
       * This is just a list of anything that fails to match.
       */
      if (!match) {
        retVal.unmatchedKeys.push(key);
      }

      /*
       * We only want to update if it was previous pass.
       * If one fails, its all a fail
       */
      if (retVal.pass) {
        retVal.pass = match;
      }

      return retVal;
    },
    {
      actual: {},
      expected: {},
      pass: true,
      missingKeys: [],
      unmatchedKeys: [],
    }
  );
}

'use strict';

var isEqual = require('lodash.isequal');
var _comparator = require('./_comparator');

var stackA = [];
var stackB = [];

function _stack(a, b) {
  var index = stackA.length;
  while (index-- > 0) {
    if (stackA[index] === a) {
      if (stackB[index] !== b) {
        return false;
        // stackA[index] === a && stackB[index] === b only tells us that the
        // objects are "identical" but not whether a and b are equal, so we
        // leave this to isEqual
      }
      break;
    } else if (stackB[index] === b) {
      return false;
    }
  }
  if (stackA[index] !== a) { // no match, found add to stack
    stackA.push(a);
    stackB.push(b);
  }
}

/**
 * @see deepEqualIdentTag
 */
function deepEqualIdentStack(a, b, callback, thisArg) {
  var result = isEqual(
      a,
      b,
      _comparator.bind(null, _stack, callback, thisArg)
  );

  // clear stacks
  stackA.length = 0;
  stackB.length = 0;

  return result;
}

module.exports = deepEqualIdentStack;

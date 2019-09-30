'use strict';

var isEqual = require('lodash.isequal');
var _comparator = require('./_comparator');

/**
 * @see deepEqualIdentTag
 */
function deepEqualIdentMap(a, b, callback, thisArg) {
  var map = new Map();

  // Defined in here so that this module can be loaded even if maps are not
  // supported.
  function _map(a, b) { // eslint-disable-line no-shadow
    if (!map.has(a) && !map.has(b)) {
      map.set(a, b);
      map.set(b, a);
    } else if (map.get(a) !== b || map.get(b) !== a) {
      return false;
    }
    // map.get(a) === b && map.get(b) === a only tells us that the objects are
    // "identical" but not whether a and b are equal, so we leave this to
    // isEqual
  }

  var result = isEqual(
      a,
      b,
      _comparator.bind(null, _map, callback, thisArg)
  );

  return result;
}

module.exports = deepEqualIdentMap;

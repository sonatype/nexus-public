'use strict';

var isEqual = require('lodash.isequal');
var _comparator = require('./_comparator');

var KEY = '__ident' + Date.now() + '__';
var objs = [];
var hasOwn = Object.prototype.hasOwnProperty;

function _tag(a, b) {
  if (!hasOwn.call(a, KEY) && !hasOwn.call(b, KEY)) {
    Object.defineProperty(a, KEY, {value: b, configurable: true});
    Object.defineProperty(b, KEY, {value: a, configurable: true});
    objs.push(a, b);
  }
  else if(a[KEY] !== b || b[KEY] !== a) {
    return false;
  }
  // a[KEY] === b && b[KEY] === a only tells us that the objects are "identical"
  // but not whether a and b are equal, so we leave this to isEqual
}

/**
 * Performs a deep comparison between the two values a and b. It has the
 * same signature and functionality as lodash's isEqual function
 * (http://lodash.com/docs#isEqual), with one difference: It also considers
 * the identity of nested objects.
 *
 * Example:
 * Most deep equality tests (including _.isEqual) consider the following
 * structures as equal:
 *
 *   var a = [1,2,3];
 *   var foo = [a, a];
 *   var bar = [[1,2,3], [1,2,3]]
 *   _.isEqual(foo, bar): // => true
 *
 * However, it should be obvious that `foo` contains two reference to the same
 * objects whereas `bar` contains two different (not identical) objects.
 * `deepEqualIdent` will consider these values to be different:
 *
 *   deepEqualIdent(foo, bar); // => false
 *
 * The following slightly different structures would be considered equal:
 *
 *   var a = [1,2,3];
 *   var b = [1,2,3];
 *   var foo = [a, a];
 *   var bar = [b, b];
 *   deepEqualIdent(foo, bar); // => true
 *
 * @param {*} a The value to compare
 * @param {*} b The other value to compare
 * @param {function} callback A function to customize comparing values
 * @param {*} thisArg The `this` value of the callback
 * @return {bool}
 */
function isEqualIdentTag(a, b, callback, thisArg) {
  var result = isEqual(
      a,
      b,
      _comparator.bind(null, _tag, callback, thisArg)
  );
  //
  // clear tags
  objs.forEach(function(obj) {
    delete obj[KEY];
  });
  objs.length = 0;

  return result;
}

module.exports = isEqualIdentTag;

'use strict';

var toString = Object.prototype.toString;
var objectClass = '[object Object]';
var arrayClass = '[object Array]';

function comparator(implementation, callback, thisArg, a, b) {
  if (typeof callback !== 'undefined') {
    return callback.call(thisArg, a, b);
  }
  var aClass = toString.call(a);
  var bClass = toString.call(b);

  if (!(aClass === objectClass || aClass === arrayClass) ||
      !(bClass === objectClass || bClass === arrayClass)) {
    return; // eslint-disable-line consistent-return
  }
  return implementation(a, b);
}

module.exports = comparator;

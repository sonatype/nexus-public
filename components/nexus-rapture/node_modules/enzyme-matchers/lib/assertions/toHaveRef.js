'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _name = require('../utils/name');

var _name2 = _interopRequireDefault(_name);

var _single = require('../utils/single');

var _single2 = _interopRequireDefault(_single);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function toHaveRef(enzymeWrapper, refName) {
  if (typeof enzymeWrapper.ref !== 'function') {
    throw new Error('EnzymeMatchers::toHaveRef can not be called on a shallow wrapper');
  }

  var node = enzymeWrapper.ref(refName);
  var pass = !!node;

  return {
    pass: pass,
    message: 'Expected to find a ref named "' + refName + '" on <' + (0, _name2.default)(enzymeWrapper) + '>, but didn\'t.',
    negatedMessage: 'Expected not to find a ref named "' + refName + '" on <' + (0, _name2.default)(enzymeWrapper) + '>, but did.',
    contextualInformation: {}
  };
} /**
   * This source code is licensed under the MIT-style license found in the
   * LICENSE file in the root directory of this source tree. *
   *
   * @providesModule toHaveRefAssertion
   * 
   */

exports.default = (0, _single2.default)(toHaveRef);
module.exports = exports['default'];
'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _name = require('../utils/name');

var _name2 = _interopRequireDefault(_name);

var _html = require('../utils/html');

var _html2 = _interopRequireDefault(_html);

var _single = require('../utils/single');

var _single2 = _interopRequireDefault(_single);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toHaveValueAssertion
 * 
 */

function toHaveValue(enzymeWrapper, expectedValue) {
  var pass = false;

  var props = enzymeWrapper.props();
  var wrapperName = '<' + (0, _name2.default)(enzymeWrapper) + '>';
  var wrapperHtml = (0, _html2.default)(enzymeWrapper);

  // set to the default checked
  if (props.hasOwnProperty('defaultValue')) {
    pass = props.defaultValue === expectedValue;
  }

  // if it has the `value` property, CHECK that
  if (props.hasOwnProperty('value')) {
    pass = props.value === expectedValue;
  }

  return {
    pass: pass,
    message: 'Expected ' + wrapperName + ' component to have the value of "' + expectedValue + '" (using ===), but it didn\'t.',
    negatedMessage: 'Expected ' + wrapperName + ' component not to have the value of "' + expectedValue + '" (using ===), but it did.',
    contextualInformation: {
      actual: wrapperHtml
    }
  };
}

exports.default = (0, _single2.default)(toHaveValue);
module.exports = exports['default'];
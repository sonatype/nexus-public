'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _name = require('../utils/name');

var _name2 = _interopRequireDefault(_name);

var _single = require('../utils/single');

var _single2 = _interopRequireDefault(_single);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function toIncludeText(enzymeWrapper, text) {
  var actualText = enzymeWrapper.text();

  if (text === undefined) {
    var message = 'Expected ".toIncludeText(null)" to be given some text.\n      If you are trying to assert this component has _some_ text, use the ".toHaveText()" matcher';
    return {
      pass: false,
      message: message,
      negatedMessage: message,
      contextualInformation: {}
    };
  }

  var pass = actualText.includes(text);
  var wrapperName = '<' + (0, _name2.default)(enzymeWrapper) + '>';

  return {
    pass: pass,
    message: 'Expected ' + wrapperName + ' to contain "' + text + '" but it did not.',
    negatedMessage: 'Expected ' + wrapperName + ' not to contain "' + text + '" but it did.',
    contextualInformation: {
      expected: 'Expected HTML: "' + text + '"',
      actual: 'Actual HTML: "' + actualText + '"'
    }
  };
} /**
   * This source code is licensed under the MIT-style license found in the
   * LICENSE file in the root directory of this source tree. *
   *
   * @providesModule toIncludeTextAssertion
   * 
   */

exports.default = (0, _single2.default)(toIncludeText);
module.exports = exports['default'];
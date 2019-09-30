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
 * @providesModule toHaveTextAssertion
 * 
 */

function toHaveText(enzymeWrapper, text) {
  var actualText = enzymeWrapper.text();
  var wrapperName = '<' + (0, _name2.default)(enzymeWrapper) + '>';
  var wrapperHtml = (0, _html2.default)(enzymeWrapper);
  var pass = void 0;

  if (text === undefined) {
    pass = actualText.length > 0;
    return {
      pass: pass,
      message: 'Expected ' + wrapperName + ' node to have text, but it did not.',
      negatedMessage: 'Expected ' + wrapperName + ' node not to have text, but it did',
      contextualInformation: {
        actual: 'Actual HTML: "' + wrapperHtml + '"'
      }
    };
  }

  pass = actualText === text;

  return {
    pass: pass,
    message: 'Expected ' + wrapperName + ' components text to match (using ===), but it did not.',
    negatedMessage: 'Expected ' + wrapperName + ' components text not to match (using ===), but it did.',
    contextualInformation: {
      actual: 'Actual HTML: "' + actualText + '"',
      expected: 'Expected HTML: "' + text + '"'
    }
  };
}

exports.default = (0, _single2.default)(toHaveText);
module.exports = exports['default'];
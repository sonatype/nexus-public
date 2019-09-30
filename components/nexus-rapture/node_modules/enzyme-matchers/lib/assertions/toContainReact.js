'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _enzyme = require('enzyme');

var _html = require('../utils/html');

var _html2 = _interopRequireDefault(_html);

var _name = require('../utils/name');

var _name2 = _interopRequireDefault(_name);

var _single = require('../utils/single');

var _single2 = _interopRequireDefault(_single);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree. *
 *
 * @providesModule toContainReactAssertion
 * 
 */

function toContainReact(enzymeWrapper, reactInstance) {
  var wrappedInstance = (0, _enzyme.shallow)(reactInstance);
  var pass = enzymeWrapper.contains(reactInstance);

  return {
    pass: pass,
    message: 'Expected <' + (0, _name2.default)(enzymeWrapper) + '> to contain ' + (0, _html2.default)(wrappedInstance) + ' but it was not found.',
    negatedMessage: 'Expected <' + (0, _name2.default)(enzymeWrapper) + '> not to contain ' + (0, _html2.default)(wrappedInstance) + ' but it does.',
    contextualInformation: {
      actual: 'HTML Output of <' + (0, _name2.default)(enzymeWrapper) + '>:\n ' + (0, _html2.default)(enzymeWrapper)
    }
  };
}

exports.default = (0, _single2.default)(toContainReact);
module.exports = exports['default'];
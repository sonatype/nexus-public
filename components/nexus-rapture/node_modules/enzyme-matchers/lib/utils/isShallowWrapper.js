'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = isShallowWrapper;


var SHALLOW_WRAPPER_CONSTRUCTOR = 'ShallowWrapper';

function isShallowWrapper(wrapper) {
  var isShallow = void 0;
  if (wrapper.constructor.name !== undefined) {
    isShallow = wrapper.constructor.name === SHALLOW_WRAPPER_CONSTRUCTOR;
  } else {
    isShallow = !!('' + wrapper.constructor).match(/^function ShallowWrapper\(/);
  }
  return isShallow;
}
module.exports = exports['default'];
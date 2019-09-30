"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = getComponentName;

var _functionPrototype = _interopRequireDefault(require("function.prototype.name"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function getComponentName(Component) {
  if (typeof Component === 'string') {
    return Component;
  }

  if (typeof Component === 'function') {
    return Component.displayName || (0, _functionPrototype["default"])(Component);
  }

  return null;
}
//# sourceMappingURL=getComponentName.js.map
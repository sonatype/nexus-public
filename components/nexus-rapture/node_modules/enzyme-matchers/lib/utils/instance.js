"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = internalInstance;
function internalInstanceKey(node) {
  return Object.keys(Object(node)).filter(function (key) {
    return key.match(/^__reactInternalInstance\$/);
  })[0];
}

function internalInstance(inst) {
  return inst._reactInternalInstance || inst[internalInstanceKey(inst)];
}
module.exports = exports["default"];
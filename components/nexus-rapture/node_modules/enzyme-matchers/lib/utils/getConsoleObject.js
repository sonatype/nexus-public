"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getConsoleObject;

/* eslint-disable no-console */

function getConsoleObject() {
  try {
    return console;
  } catch (e) {
    // If no global console object is available, set consoleObject to a dummy object.
    return {};
  }
}
module.exports = exports["default"];
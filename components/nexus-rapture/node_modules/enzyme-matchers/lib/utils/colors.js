'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
var colorValues = {
  blue: [34, 39],
  gray: [90, 39],
  green: [32, 39],
  red: [31, 39],
  yellow: [33, 39]
};

var colorFns = {};

var matchOperatorsRe = /[|\\{}()[\]^$+*?.]/g;

Object.keys(colorValues).forEach(function (color) {
  var colorValue = colorValues[color];
  colorFns[color] = function (str) {
    var open = '\x1B[' + colorValue[0] + 'm';
    var close = '\x1B[' + colorValue[1] + 'm';
    var regex = new RegExp(close.replace(matchOperatorsRe, '\\$&'), 'g');
    var regStr = ('' + str).replace(regex, open);
    return '' + open + regStr + close;
  };
});

exports.default = colorFns;
module.exports = exports['default'];
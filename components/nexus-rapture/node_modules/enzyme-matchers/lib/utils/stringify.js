'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

exports.default = stringify;

var _circularJsonEs = require('circular-json-es6');

var _circularJsonEs2 = _interopRequireDefault(_circularJsonEs);

var _colors = require('./colors');

var _colors2 = _interopRequireDefault(_colors);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function stringifySingle(key, value) {
  var stringifyingValue = value;
  var skipCircularCheck = false;
  if (Array.isArray(value)) {
    var values = value.map(function (v) {
      return stringifySingle('', v)[1];
    });

    // Skip circular check because we have already safely dealt with it above
    skipCircularCheck = true;

    var joined = values.join(' ');
    var initialBracket = _colors2.default.gray('[');
    var endingBracket = _colors2.default.gray(']');

    if (joined.length > 20) {
      var pad = '\n  ';
      joined = values.join(pad) + '\n';
      initialBracket += pad;
    }

    stringifyingValue = _colors2.default.gray('' + initialBracket + joined + endingBracket);
  } else if (value === null) {
    stringifyingValue = _colors2.default.gray(value);
  } else if ((typeof value === 'undefined' ? 'undefined' : _typeof(value)) === 'object') {
    stringifyingValue = _colors2.default.gray(_circularJsonEs2.default.stringify(value));
  } else if (typeof value === 'string') {
    stringifyingValue = _colors2.default.gray('"' + value + '"');
  } else if (typeof value === 'number') {
    stringifyingValue = _colors2.default.blue(value);
  } else if (value) {
    stringifyingValue = _colors2.default.green(value);
  } else if (!value) {
    stringifyingValue = _colors2.default.red(value);
  }

  try {
    // circular if you cant stringify
    if (!skipCircularCheck) {
      JSON.stringify(_defineProperty({}, key, value));
    }

    return [key, stringifyingValue];
  } catch (e) {
    return [key, _colors2.default.gray('[Circular]')];
  }
}

function color(_ref) {
  var _ref2 = _slicedToArray(_ref, 2),
      key = _ref2[0],
      value = _ref2[1];

  return '' + _colors2.default.yellow(key) + _colors2.default.gray(':') + ' ' + _colors2.default.yellow(value);
}

function stringify(object) {
  var keys = Object.keys(object);

  return keys.map(function (key) {
    return color(stringifySingle(key, object[key]));
  }).join('\n');
}
module.exports = exports['default'];
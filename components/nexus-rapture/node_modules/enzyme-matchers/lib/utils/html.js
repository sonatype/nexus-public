'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = printHTMLForWrapper;

var _instance = require('./instance');

var _instance2 = _interopRequireDefault(_instance);

var _isShallowWrapper = require('./isShallowWrapper');

var _isShallowWrapper2 = _interopRequireDefault(_isShallowWrapper);

var _getConsoleObject = require('./getConsoleObject');

var _getConsoleObject2 = _interopRequireDefault(_getConsoleObject);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var consoleObject = (0, _getConsoleObject2.default)();
var noop = function noop() {};

function mapWrappersHTML(wrapper) {
  return wrapper.getElements().map(function (node) {
    var inst = (0, _instance2.default)(node);
    var type = node.type || inst._tag;

    var error = consoleObject.error;

    consoleObject.error = noop;

    var _ref = node.props ? node.props : inst._currentElement.props,
        children = _ref.children,
        props = _objectWithoutProperties(_ref, ['children']);

    consoleObject.error = error;

    var transformedProps = Object.keys(props).map(function (key) {
      try {
        return key + '="' + props[key].toString() + '"';
      } catch (e) {
        return key + '="[object Object]"';
      }
    });
    var stringifiedNode = '<' + type + ' ' + transformedProps.join(' ');

    if (children) {
      stringifiedNode += '>[..children..]</' + node.type;
    } else {
      stringifiedNode += '/>';
    }

    return stringifiedNode;
  });
}

function printHTMLForWrapper(wrapper) {
  switch (wrapper.getElements().length) {
    case 0:
      {
        return '[empty set]';
      }
    case 1:
      {
        if ((0, _isShallowWrapper2.default)(wrapper)) {
          // This is used to clean up in any awkward spacing in the debug output.
          // <div>  <Foo /></div> => <div><Foo /></div>
          return wrapper.debug().replace(/\n(\s*)/g, '');
        }

        return wrapper.html();
      }
    default:
      {
        var nodes = mapWrappersHTML(wrapper).reduce(function (acc, curr, index) {
          return '' + acc + index + ': ' + curr + '\n';
        }, '');

        return 'Multiple nodes found:\n' + nodes;
      }
  }
}
module.exports = exports['default'];
'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.RootFinder = exports.wrap = exports.createRenderWrapper = exports.createMountWrapper = undefined;

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

exports.mapNativeEventNames = mapNativeEventNames;
exports.propFromEvent = propFromEvent;
exports.withSetStateAllowed = withSetStateAllowed;
exports.assertDomAvailable = assertDomAvailable;
exports.displayNameOfNode = displayNameOfNode;
exports.nodeTypeFromType = nodeTypeFromType;
exports.isArrayLike = isArrayLike;
exports.flatten = flatten;
exports.ensureKeyOrUndefined = ensureKeyOrUndefined;
exports.elementToTree = elementToTree;
exports.findElement = findElement;
exports.propsWithKeysAndRef = propsWithKeysAndRef;
exports.getComponentStack = getComponentStack;
exports.simulateError = simulateError;
exports.getMaskedContext = getMaskedContext;
exports.getNodeFromRootFinder = getNodeFromRootFinder;
exports.wrapWithWrappingComponent = wrapWithWrappingComponent;
exports.getWrappingComponentMountRenderer = getWrappingComponentMountRenderer;
exports.fakeDynamicImport = fakeDynamicImport;

var _object = require('object.assign');

var _object2 = _interopRequireDefault(_object);

var _functionPrototype = require('function.prototype.name');

var _functionPrototype2 = _interopRequireDefault(_functionPrototype);

var _object3 = require('object.fromentries');

var _object4 = _interopRequireDefault(_object3);

var _createMountWrapper = require('./createMountWrapper');

var _createMountWrapper2 = _interopRequireDefault(_createMountWrapper);

var _createRenderWrapper = require('./createRenderWrapper');

var _createRenderWrapper2 = _interopRequireDefault(_createRenderWrapper);

var _wrapWithSimpleWrapper = require('./wrapWithSimpleWrapper');

var _wrapWithSimpleWrapper2 = _interopRequireDefault(_wrapWithSimpleWrapper);

var _RootFinder = require('./RootFinder');

var _RootFinder2 = _interopRequireDefault(_RootFinder);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

exports.createMountWrapper = _createMountWrapper2['default'];
exports.createRenderWrapper = _createRenderWrapper2['default'];
exports.wrap = _wrapWithSimpleWrapper2['default'];
exports.RootFinder = _RootFinder2['default'];
function mapNativeEventNames(event) {
  var _ref = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
      _ref$animation = _ref.animation,
      animation = _ref$animation === undefined ? false : _ref$animation,
      _ref$pointerEvents = _ref.pointerEvents,
      pointerEvents = _ref$pointerEvents === undefined ? false : _ref$pointerEvents,
      _ref$auxClick = _ref.auxClick,
      auxClick = _ref$auxClick === undefined ? false : _ref$auxClick;

  var nativeToReactEventMap = (0, _object2['default'])({
    compositionend: 'compositionEnd',
    compositionstart: 'compositionStart',
    compositionupdate: 'compositionUpdate',
    keydown: 'keyDown',
    keyup: 'keyUp',
    keypress: 'keyPress',
    contextmenu: 'contextMenu',
    dblclick: 'doubleClick',
    doubleclick: 'doubleClick', // kept for legacy. TODO: remove with next major.
    dragend: 'dragEnd',
    dragenter: 'dragEnter',
    dragexist: 'dragExit',
    dragleave: 'dragLeave',
    dragover: 'dragOver',
    dragstart: 'dragStart',
    mousedown: 'mouseDown',
    mousemove: 'mouseMove',
    mouseout: 'mouseOut',
    mouseover: 'mouseOver',
    mouseup: 'mouseUp',
    touchcancel: 'touchCancel',
    touchend: 'touchEnd',
    touchmove: 'touchMove',
    touchstart: 'touchStart',
    canplay: 'canPlay',
    canplaythrough: 'canPlayThrough',
    durationchange: 'durationChange',
    loadeddata: 'loadedData',
    loadedmetadata: 'loadedMetadata',
    loadstart: 'loadStart',
    ratechange: 'rateChange',
    timeupdate: 'timeUpdate',
    volumechange: 'volumeChange',
    beforeinput: 'beforeInput',
    mouseenter: 'mouseEnter',
    mouseleave: 'mouseLeave',
    transitionend: 'transitionEnd'
  }, animation && {
    animationstart: 'animationStart',
    animationiteration: 'animationIteration',
    animationend: 'animationEnd'
  }, pointerEvents && {
    pointerdown: 'pointerDown',
    pointermove: 'pointerMove',
    pointerup: 'pointerUp',
    pointercancel: 'pointerCancel',
    gotpointercapture: 'gotPointerCapture',
    lostpointercapture: 'lostPointerCapture',
    pointerenter: 'pointerEnter',
    pointerleave: 'pointerLeave',
    pointerover: 'pointerOver',
    pointerout: 'pointerOut'
  }, auxClick && {
    auxclick: 'auxClick'
  });

  return nativeToReactEventMap[event] || event;
}

// 'click' => 'onClick'
// 'mouseEnter' => 'onMouseEnter'
function propFromEvent(event) {
  var eventOptions = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

  var nativeEvent = mapNativeEventNames(event, eventOptions);
  return 'on' + String(nativeEvent[0].toUpperCase()) + String(nativeEvent.slice(1));
}

function withSetStateAllowed(fn) {
  // NOTE(lmr):
  // this is currently here to circumvent a React bug where `setState()` is
  // not allowed without global being defined.
  var cleanup = false;
  if (typeof global.document === 'undefined') {
    cleanup = true;
    global.document = {};
  }
  var result = fn();
  if (cleanup) {
    // This works around a bug in node/jest in that developers aren't able to
    // delete things from global when running in a node vm.
    global.document = undefined;
    delete global.document;
  }
  return result;
}

function assertDomAvailable(feature) {
  if (!global || !global.document || !global.document.createElement) {
    throw new Error('Enzyme\'s ' + String(feature) + ' expects a DOM environment to be loaded, but found none');
  }
}

function displayNameOfNode(node) {
  if (!node) return null;

  var type = node.type;


  if (!type) return null;

  return type.displayName || (typeof type === 'function' ? (0, _functionPrototype2['default'])(type) : type.name || type);
}

function nodeTypeFromType(type) {
  if (typeof type === 'string') {
    return 'host';
  }
  if (type && type.prototype && type.prototype.isReactComponent) {
    return 'class';
  }
  return 'function';
}

function getIteratorFn(obj) {
  var iteratorFn = obj && (typeof Symbol === 'function' && _typeof(Symbol.iterator) === 'symbol' && obj[Symbol.iterator] || obj['@@iterator']);

  if (typeof iteratorFn === 'function') {
    return iteratorFn;
  }

  return undefined;
}

function isIterable(obj) {
  return !!getIteratorFn(obj);
}

function isArrayLike(obj) {
  return Array.isArray(obj) || typeof obj !== 'string' && isIterable(obj);
}

function flatten(arrs) {
  // optimize for the most common case
  if (Array.isArray(arrs)) {
    return arrs.reduce(function (flatArrs, item) {
      return flatArrs.concat(isArrayLike(item) ? flatten(item) : item);
    }, []);
  }

  // fallback for arbitrary iterable children
  var flatArrs = [];

  var iteratorFn = getIteratorFn(arrs);
  var iterator = iteratorFn.call(arrs);

  var step = iterator.next();

  while (!step.done) {
    var item = step.value;
    var flatItem = void 0;

    if (isArrayLike(item)) {
      flatItem = flatten(item);
    } else {
      flatItem = item;
    }

    flatArrs = flatArrs.concat(flatItem);

    step = iterator.next();
  }

  return flatArrs;
}

function ensureKeyOrUndefined(key) {
  return key || (key === '' ? '' : undefined);
}

function elementToTree(el) {
  var recurse = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : elementToTree;

  if (typeof recurse !== 'function' && arguments.length === 3) {
    // special case for backwards compat for `.map(elementToTree)`
    recurse = elementToTree; // eslint-disable-line no-param-reassign
  }
  if (el === null || (typeof el === 'undefined' ? 'undefined' : _typeof(el)) !== 'object' || !('type' in el)) {
    return el;
  }
  var type = el.type,
      props = el.props,
      key = el.key,
      ref = el.ref;
  var children = props.children;

  var rendered = null;
  if (isArrayLike(children)) {
    rendered = flatten(children).map(function (x) {
      return recurse(x);
    });
  } else if (typeof children !== 'undefined') {
    rendered = recurse(children);
  }

  var nodeType = nodeTypeFromType(type);

  if (nodeType === 'host' && props.dangerouslySetInnerHTML) {
    if (props.children != null) {
      var error = new Error('Can only set one of `children` or `props.dangerouslySetInnerHTML`.');
      error.name = 'Invariant Violation';
      throw error;
    }
  }

  return {
    nodeType: nodeType,
    type: type,
    props: props,
    key: ensureKeyOrUndefined(key),
    ref: ref,
    instance: null,
    rendered: rendered
  };
}

function mapFind(arraylike, mapper, finder) {
  var found = void 0;
  var isFound = Array.prototype.find.call(arraylike, function (item) {
    found = mapper(item);
    return finder(found);
  });
  return isFound ? found : undefined;
}

function findElement(el, predicate) {
  if (el === null || (typeof el === 'undefined' ? 'undefined' : _typeof(el)) !== 'object' || !('type' in el)) {
    return undefined;
  }
  if (predicate(el)) {
    return el;
  }
  var rendered = el.rendered;

  if (isArrayLike(rendered)) {
    return mapFind(rendered, function (x) {
      return findElement(x, predicate);
    }, function (x) {
      return typeof x !== 'undefined';
    });
  }
  return findElement(rendered, predicate);
}

function propsWithKeysAndRef(node) {
  if (node.ref !== null || node.key !== null) {
    return (0, _object2['default'])({}, node.props, {
      key: node.key,
      ref: node.ref
    });
  }
  return node.props;
}

function getComponentStack(hierarchy) {
  var getNodeType = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : nodeTypeFromType;
  var getDisplayName = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : displayNameOfNode;

  var tuples = hierarchy.filter(function (node) {
    return node.type !== _RootFinder2['default'];
  }).map(function (x) {
    return [getNodeType(x.type), getDisplayName(x)];
  }).concat([['class', 'WrapperComponent']]);

  return tuples.map(function (_ref2, i, arr) {
    var _ref3 = _slicedToArray(_ref2, 2),
        name = _ref3[1];

    var _ref4 = arr.slice(i + 1).find(function (_ref6) {
      var _ref7 = _slicedToArray(_ref6, 1),
          nodeType = _ref7[0];

      return nodeType !== 'host';
    }) || [],
        _ref5 = _slicedToArray(_ref4, 2),
        closestComponent = _ref5[1];

    return '\n    in ' + String(name) + (closestComponent ? ' (created by ' + String(closestComponent) + ')' : '');
  }).join('');
}

function simulateError(error, catchingInstance, rootNode, // TODO: remove `rootNode` next semver-major
hierarchy) {
  var getNodeType = arguments.length > 4 && arguments[4] !== undefined ? arguments[4] : nodeTypeFromType;
  var getDisplayName = arguments.length > 5 && arguments[5] !== undefined ? arguments[5] : displayNameOfNode;
  var catchingType = arguments.length > 6 && arguments[6] !== undefined ? arguments[6] : {};

  var instance = catchingInstance || {};

  var componentDidCatch = instance.componentDidCatch;
  var getDerivedStateFromError = catchingType.getDerivedStateFromError;


  if (!componentDidCatch && !getDerivedStateFromError) {
    throw error;
  }

  if (getDerivedStateFromError) {
    var stateUpdate = getDerivedStateFromError.call(catchingType, error);
    instance.setState(stateUpdate);
  }

  if (componentDidCatch) {
    var componentStack = getComponentStack(hierarchy, getNodeType, getDisplayName);
    componentDidCatch.call(instance, error, { componentStack: componentStack });
  }
}

function getMaskedContext(contextTypes, unmaskedContext) {
  if (!contextTypes || !unmaskedContext) {
    return {};
  }
  return (0, _object4['default'])(Object.keys(contextTypes).map(function (key) {
    return [key, unmaskedContext[key]];
  }));
}

function getNodeFromRootFinder(isCustomComponent, tree, options) {
  if (!isCustomComponent(options.wrappingComponent)) {
    return tree.rendered;
  }
  var rootFinder = findElement(tree, function (node) {
    return node.type === _RootFinder2['default'];
  });
  if (!rootFinder) {
    throw new Error('`wrappingComponent` must render its children!');
  }
  return rootFinder.rendered;
}

function wrapWithWrappingComponent(createElement, node, options) {
  var wrappingComponent = options.wrappingComponent,
      wrappingComponentProps = options.wrappingComponentProps;

  if (!wrappingComponent) {
    return node;
  }
  return createElement(wrappingComponent, wrappingComponentProps, createElement(_RootFinder2['default'], null, node));
}

function getWrappingComponentMountRenderer(_ref8) {
  var toTree = _ref8.toTree,
      getMountWrapperInstance = _ref8.getMountWrapperInstance;

  return {
    getNode: function () {
      function getNode() {
        var instance = getMountWrapperInstance();
        return instance ? toTree(instance).rendered : null;
      }

      return getNode;
    }(),
    render: function () {
      function render(el, context, callback) {
        var instance = getMountWrapperInstance();
        if (!instance) {
          throw new Error('The wrapping component may not be updated if the root is unmounted.');
        }
        return instance.setWrappingComponentProps(el.props, callback);
      }

      return render;
    }()
  };
}

function fakeDynamicImport(moduleToImport) {
  return Promise.resolve({ 'default': moduleToImport });
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uL3NyYy9VdGlscy5qcyJdLCJuYW1lcyI6WyJtYXBOYXRpdmVFdmVudE5hbWVzIiwicHJvcEZyb21FdmVudCIsIndpdGhTZXRTdGF0ZUFsbG93ZWQiLCJhc3NlcnREb21BdmFpbGFibGUiLCJkaXNwbGF5TmFtZU9mTm9kZSIsIm5vZGVUeXBlRnJvbVR5cGUiLCJpc0FycmF5TGlrZSIsImZsYXR0ZW4iLCJlbnN1cmVLZXlPclVuZGVmaW5lZCIsImVsZW1lbnRUb1RyZWUiLCJmaW5kRWxlbWVudCIsInByb3BzV2l0aEtleXNBbmRSZWYiLCJnZXRDb21wb25lbnRTdGFjayIsInNpbXVsYXRlRXJyb3IiLCJnZXRNYXNrZWRDb250ZXh0IiwiZ2V0Tm9kZUZyb21Sb290RmluZGVyIiwid3JhcFdpdGhXcmFwcGluZ0NvbXBvbmVudCIsImdldFdyYXBwaW5nQ29tcG9uZW50TW91bnRSZW5kZXJlciIsImZha2VEeW5hbWljSW1wb3J0IiwiY3JlYXRlTW91bnRXcmFwcGVyIiwiY3JlYXRlUmVuZGVyV3JhcHBlciIsIndyYXAiLCJSb290RmluZGVyIiwiZXZlbnQiLCJhbmltYXRpb24iLCJwb2ludGVyRXZlbnRzIiwiYXV4Q2xpY2siLCJuYXRpdmVUb1JlYWN0RXZlbnRNYXAiLCJjb21wb3NpdGlvbmVuZCIsImNvbXBvc2l0aW9uc3RhcnQiLCJjb21wb3NpdGlvbnVwZGF0ZSIsImtleWRvd24iLCJrZXl1cCIsImtleXByZXNzIiwiY29udGV4dG1lbnUiLCJkYmxjbGljayIsImRvdWJsZWNsaWNrIiwiZHJhZ2VuZCIsImRyYWdlbnRlciIsImRyYWdleGlzdCIsImRyYWdsZWF2ZSIsImRyYWdvdmVyIiwiZHJhZ3N0YXJ0IiwibW91c2Vkb3duIiwibW91c2Vtb3ZlIiwibW91c2VvdXQiLCJtb3VzZW92ZXIiLCJtb3VzZXVwIiwidG91Y2hjYW5jZWwiLCJ0b3VjaGVuZCIsInRvdWNobW92ZSIsInRvdWNoc3RhcnQiLCJjYW5wbGF5IiwiY2FucGxheXRocm91Z2giLCJkdXJhdGlvbmNoYW5nZSIsImxvYWRlZGRhdGEiLCJsb2FkZWRtZXRhZGF0YSIsImxvYWRzdGFydCIsInJhdGVjaGFuZ2UiLCJ0aW1ldXBkYXRlIiwidm9sdW1lY2hhbmdlIiwiYmVmb3JlaW5wdXQiLCJtb3VzZWVudGVyIiwibW91c2VsZWF2ZSIsInRyYW5zaXRpb25lbmQiLCJhbmltYXRpb25zdGFydCIsImFuaW1hdGlvbml0ZXJhdGlvbiIsImFuaW1hdGlvbmVuZCIsInBvaW50ZXJkb3duIiwicG9pbnRlcm1vdmUiLCJwb2ludGVydXAiLCJwb2ludGVyY2FuY2VsIiwiZ290cG9pbnRlcmNhcHR1cmUiLCJsb3N0cG9pbnRlcmNhcHR1cmUiLCJwb2ludGVyZW50ZXIiLCJwb2ludGVybGVhdmUiLCJwb2ludGVyb3ZlciIsInBvaW50ZXJvdXQiLCJhdXhjbGljayIsImV2ZW50T3B0aW9ucyIsIm5hdGl2ZUV2ZW50IiwidG9VcHBlckNhc2UiLCJzbGljZSIsImZuIiwiY2xlYW51cCIsImdsb2JhbCIsImRvY3VtZW50IiwicmVzdWx0IiwidW5kZWZpbmVkIiwiZmVhdHVyZSIsImNyZWF0ZUVsZW1lbnQiLCJFcnJvciIsIm5vZGUiLCJ0eXBlIiwiZGlzcGxheU5hbWUiLCJuYW1lIiwicHJvdG90eXBlIiwiaXNSZWFjdENvbXBvbmVudCIsImdldEl0ZXJhdG9yRm4iLCJvYmoiLCJpdGVyYXRvckZuIiwiU3ltYm9sIiwiaXRlcmF0b3IiLCJpc0l0ZXJhYmxlIiwiQXJyYXkiLCJpc0FycmF5IiwiYXJycyIsInJlZHVjZSIsImZsYXRBcnJzIiwiaXRlbSIsImNvbmNhdCIsImNhbGwiLCJzdGVwIiwibmV4dCIsImRvbmUiLCJ2YWx1ZSIsImZsYXRJdGVtIiwia2V5IiwiZWwiLCJyZWN1cnNlIiwiYXJndW1lbnRzIiwibGVuZ3RoIiwicHJvcHMiLCJyZWYiLCJjaGlsZHJlbiIsInJlbmRlcmVkIiwibWFwIiwieCIsIm5vZGVUeXBlIiwiZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUwiLCJlcnJvciIsImluc3RhbmNlIiwibWFwRmluZCIsImFycmF5bGlrZSIsIm1hcHBlciIsImZpbmRlciIsImZvdW5kIiwiaXNGb3VuZCIsImZpbmQiLCJwcmVkaWNhdGUiLCJoaWVyYXJjaHkiLCJnZXROb2RlVHlwZSIsImdldERpc3BsYXlOYW1lIiwidHVwbGVzIiwiZmlsdGVyIiwiaSIsImFyciIsImNsb3Nlc3RDb21wb25lbnQiLCJqb2luIiwiY2F0Y2hpbmdJbnN0YW5jZSIsInJvb3ROb2RlIiwiY2F0Y2hpbmdUeXBlIiwiY29tcG9uZW50RGlkQ2F0Y2giLCJnZXREZXJpdmVkU3RhdGVGcm9tRXJyb3IiLCJzdGF0ZVVwZGF0ZSIsInNldFN0YXRlIiwiY29tcG9uZW50U3RhY2siLCJjb250ZXh0VHlwZXMiLCJ1bm1hc2tlZENvbnRleHQiLCJPYmplY3QiLCJrZXlzIiwiaXNDdXN0b21Db21wb25lbnQiLCJ0cmVlIiwib3B0aW9ucyIsIndyYXBwaW5nQ29tcG9uZW50Iiwicm9vdEZpbmRlciIsIndyYXBwaW5nQ29tcG9uZW50UHJvcHMiLCJ0b1RyZWUiLCJnZXRNb3VudFdyYXBwZXJJbnN0YW5jZSIsImdldE5vZGUiLCJyZW5kZXIiLCJjb250ZXh0IiwiY2FsbGJhY2siLCJzZXRXcmFwcGluZ0NvbXBvbmVudFByb3BzIiwibW9kdWxlVG9JbXBvcnQiLCJQcm9taXNlIiwicmVzb2x2ZSJdLCJtYXBwaW5ncyI6Ijs7Ozs7Ozs7Ozs7UUFjZ0JBLG1CLEdBQUFBLG1CO1FBc0VBQyxhLEdBQUFBLGE7UUFLQUMsbUIsR0FBQUEsbUI7UUFtQkFDLGtCLEdBQUFBLGtCO1FBTUFDLGlCLEdBQUFBLGlCO1FBVUFDLGdCLEdBQUFBLGdCO1FBMkJBQyxXLEdBQUFBLFc7UUFJQUMsTyxHQUFBQSxPO1FBbUNBQyxvQixHQUFBQSxvQjtRQUlBQyxhLEdBQUFBLGE7UUFvREFDLFcsR0FBQUEsVztRQWNBQyxtQixHQUFBQSxtQjtRQVdBQyxpQixHQUFBQSxpQjtRQW1CQUMsYSxHQUFBQSxhO1FBOEJBQyxnQixHQUFBQSxnQjtRQU9BQyxxQixHQUFBQSxxQjtRQVdBQyx5QixHQUFBQSx5QjtRQVlBQyxpQyxHQUFBQSxpQztRQWdCQUMsaUIsR0FBQUEsaUI7Ozs7OztBQTlXaEI7Ozs7QUFDQTs7OztBQUNBOzs7O0FBQ0E7Ozs7QUFDQTs7OztBQUNBOzs7Ozs7UUFHRUMsa0IsR0FBQUEsK0I7UUFDQUMsbUIsR0FBQUEsZ0M7UUFDQUMsSSxHQUFBQSxrQztRQUNBQyxVLEdBQUFBLHVCO0FBR0ssU0FBU3RCLG1CQUFULENBQTZCdUIsS0FBN0IsRUFJQztBQUFBLGlGQUFKLEVBQUk7QUFBQSw0QkFITkMsU0FHTTtBQUFBLE1BSE5BLFNBR00sa0NBSE0sS0FHTjtBQUFBLGdDQUZOQyxhQUVNO0FBQUEsTUFGTkEsYUFFTSxzQ0FGVSxLQUVWO0FBQUEsMkJBRE5DLFFBQ007QUFBQSxNQUROQSxRQUNNLGlDQURLLEtBQ0w7O0FBQ04sTUFBTUM7QUFDSkMsb0JBQWdCLGdCQURaO0FBRUpDLHNCQUFrQixrQkFGZDtBQUdKQyx1QkFBbUIsbUJBSGY7QUFJSkMsYUFBUyxTQUpMO0FBS0pDLFdBQU8sT0FMSDtBQU1KQyxjQUFVLFVBTk47QUFPSkMsaUJBQWEsYUFQVDtBQVFKQyxjQUFVLGFBUk47QUFTSkMsaUJBQWEsYUFUVCxFQVN3QjtBQUM1QkMsYUFBUyxTQVZMO0FBV0pDLGVBQVcsV0FYUDtBQVlKQyxlQUFXLFVBWlA7QUFhSkMsZUFBVyxXQWJQO0FBY0pDLGNBQVUsVUFkTjtBQWVKQyxlQUFXLFdBZlA7QUFnQkpDLGVBQVcsV0FoQlA7QUFpQkpDLGVBQVcsV0FqQlA7QUFrQkpDLGNBQVUsVUFsQk47QUFtQkpDLGVBQVcsV0FuQlA7QUFvQkpDLGFBQVMsU0FwQkw7QUFxQkpDLGlCQUFhLGFBckJUO0FBc0JKQyxjQUFVLFVBdEJOO0FBdUJKQyxlQUFXLFdBdkJQO0FBd0JKQyxnQkFBWSxZQXhCUjtBQXlCSkMsYUFBUyxTQXpCTDtBQTBCSkMsb0JBQWdCLGdCQTFCWjtBQTJCSkMsb0JBQWdCLGdCQTNCWjtBQTRCSkMsZ0JBQVksWUE1QlI7QUE2QkpDLG9CQUFnQixnQkE3Qlo7QUE4QkpDLGVBQVcsV0E5QlA7QUErQkpDLGdCQUFZLFlBL0JSO0FBZ0NKQyxnQkFBWSxZQWhDUjtBQWlDSkMsa0JBQWMsY0FqQ1Y7QUFrQ0pDLGlCQUFhLGFBbENUO0FBbUNKQyxnQkFBWSxZQW5DUjtBQW9DSkMsZ0JBQVksWUFwQ1I7QUFxQ0pDLG1CQUFlO0FBckNYLEtBc0NBeEMsYUFBYTtBQUNmeUMsb0JBQWdCLGdCQUREO0FBRWZDLHdCQUFvQixvQkFGTDtBQUdmQyxrQkFBYztBQUhDLEdBdENiLEVBMkNBMUMsaUJBQWlCO0FBQ25CMkMsaUJBQWEsYUFETTtBQUVuQkMsaUJBQWEsYUFGTTtBQUduQkMsZUFBVyxXQUhRO0FBSW5CQyxtQkFBZSxlQUpJO0FBS25CQyx1QkFBbUIsbUJBTEE7QUFNbkJDLHdCQUFvQixvQkFORDtBQU9uQkMsa0JBQWMsY0FQSztBQVFuQkMsa0JBQWMsY0FSSztBQVNuQkMsaUJBQWEsYUFUTTtBQVVuQkMsZ0JBQVk7QUFWTyxHQTNDakIsRUF1REFuRCxZQUFZO0FBQ2RvRCxjQUFVO0FBREksR0F2RFosQ0FBTjs7QUE0REEsU0FBT25ELHNCQUFzQkosS0FBdEIsS0FBZ0NBLEtBQXZDO0FBQ0Q7O0FBRUQ7QUFDQTtBQUNPLFNBQVN0QixhQUFULENBQXVCc0IsS0FBdkIsRUFBaUQ7QUFBQSxNQUFuQndELFlBQW1CLHVFQUFKLEVBQUk7O0FBQ3RELE1BQU1DLGNBQWNoRixvQkFBb0J1QixLQUFwQixFQUEyQndELFlBQTNCLENBQXBCO0FBQ0EsdUJBQVlDLFlBQVksQ0FBWixFQUFlQyxXQUFmLEVBQVosV0FBMkNELFlBQVlFLEtBQVosQ0FBa0IsQ0FBbEIsQ0FBM0M7QUFDRDs7QUFFTSxTQUFTaEYsbUJBQVQsQ0FBNkJpRixFQUE3QixFQUFpQztBQUN0QztBQUNBO0FBQ0E7QUFDQSxNQUFJQyxVQUFVLEtBQWQ7QUFDQSxNQUFJLE9BQU9DLE9BQU9DLFFBQWQsS0FBMkIsV0FBL0IsRUFBNEM7QUFDMUNGLGNBQVUsSUFBVjtBQUNBQyxXQUFPQyxRQUFQLEdBQWtCLEVBQWxCO0FBQ0Q7QUFDRCxNQUFNQyxTQUFTSixJQUFmO0FBQ0EsTUFBSUMsT0FBSixFQUFhO0FBQ1g7QUFDQTtBQUNBQyxXQUFPQyxRQUFQLEdBQWtCRSxTQUFsQjtBQUNBLFdBQU9ILE9BQU9DLFFBQWQ7QUFDRDtBQUNELFNBQU9DLE1BQVA7QUFDRDs7QUFFTSxTQUFTcEYsa0JBQVQsQ0FBNEJzRixPQUE1QixFQUFxQztBQUMxQyxNQUFJLENBQUNKLE1BQUQsSUFBVyxDQUFDQSxPQUFPQyxRQUFuQixJQUErQixDQUFDRCxPQUFPQyxRQUFQLENBQWdCSSxhQUFwRCxFQUFtRTtBQUNqRSxVQUFNLElBQUlDLEtBQUosdUJBQXNCRixPQUF0Qiw4REFBTjtBQUNEO0FBQ0Y7O0FBRU0sU0FBU3JGLGlCQUFULENBQTJCd0YsSUFBM0IsRUFBaUM7QUFDdEMsTUFBSSxDQUFDQSxJQUFMLEVBQVcsT0FBTyxJQUFQOztBQUQyQixNQUc5QkMsSUFIOEIsR0FHckJELElBSHFCLENBRzlCQyxJQUg4Qjs7O0FBS3RDLE1BQUksQ0FBQ0EsSUFBTCxFQUFXLE9BQU8sSUFBUDs7QUFFWCxTQUFPQSxLQUFLQyxXQUFMLEtBQXFCLE9BQU9ELElBQVAsS0FBZ0IsVUFBaEIsR0FBNkIsb0NBQWFBLElBQWIsQ0FBN0IsR0FBa0RBLEtBQUtFLElBQUwsSUFBYUYsSUFBcEYsQ0FBUDtBQUNEOztBQUVNLFNBQVN4RixnQkFBVCxDQUEwQndGLElBQTFCLEVBQWdDO0FBQ3JDLE1BQUksT0FBT0EsSUFBUCxLQUFnQixRQUFwQixFQUE4QjtBQUM1QixXQUFPLE1BQVA7QUFDRDtBQUNELE1BQUlBLFFBQVFBLEtBQUtHLFNBQWIsSUFBMEJILEtBQUtHLFNBQUwsQ0FBZUMsZ0JBQTdDLEVBQStEO0FBQzdELFdBQU8sT0FBUDtBQUNEO0FBQ0QsU0FBTyxVQUFQO0FBQ0Q7O0FBRUQsU0FBU0MsYUFBVCxDQUF1QkMsR0FBdkIsRUFBNEI7QUFDMUIsTUFBTUMsYUFBYUQsUUFDaEIsT0FBT0UsTUFBUCxLQUFrQixVQUFsQixJQUFnQyxRQUFPQSxPQUFPQyxRQUFkLE1BQTJCLFFBQTNELElBQXVFSCxJQUFJRSxPQUFPQyxRQUFYLENBQXhFLElBQ0dILElBQUksWUFBSixDQUZjLENBQW5COztBQUtBLE1BQUksT0FBT0MsVUFBUCxLQUFzQixVQUExQixFQUFzQztBQUNwQyxXQUFPQSxVQUFQO0FBQ0Q7O0FBRUQsU0FBT1osU0FBUDtBQUNEOztBQUVELFNBQVNlLFVBQVQsQ0FBb0JKLEdBQXBCLEVBQXlCO0FBQ3ZCLFNBQU8sQ0FBQyxDQUFDRCxjQUFjQyxHQUFkLENBQVQ7QUFDRDs7QUFFTSxTQUFTN0YsV0FBVCxDQUFxQjZGLEdBQXJCLEVBQTBCO0FBQy9CLFNBQU9LLE1BQU1DLE9BQU4sQ0FBY04sR0FBZCxLQUF1QixPQUFPQSxHQUFQLEtBQWUsUUFBZixJQUEyQkksV0FBV0osR0FBWCxDQUF6RDtBQUNEOztBQUVNLFNBQVM1RixPQUFULENBQWlCbUcsSUFBakIsRUFBdUI7QUFDNUI7QUFDQSxNQUFJRixNQUFNQyxPQUFOLENBQWNDLElBQWQsQ0FBSixFQUF5QjtBQUN2QixXQUFPQSxLQUFLQyxNQUFMLENBQ0wsVUFBQ0MsUUFBRCxFQUFXQyxJQUFYO0FBQUEsYUFBb0JELFNBQVNFLE1BQVQsQ0FBZ0J4RyxZQUFZdUcsSUFBWixJQUFvQnRHLFFBQVFzRyxJQUFSLENBQXBCLEdBQW9DQSxJQUFwRCxDQUFwQjtBQUFBLEtBREssRUFFTCxFQUZLLENBQVA7QUFJRDs7QUFFRDtBQUNBLE1BQUlELFdBQVcsRUFBZjs7QUFFQSxNQUFNUixhQUFhRixjQUFjUSxJQUFkLENBQW5CO0FBQ0EsTUFBTUosV0FBV0YsV0FBV1csSUFBWCxDQUFnQkwsSUFBaEIsQ0FBakI7O0FBRUEsTUFBSU0sT0FBT1YsU0FBU1csSUFBVCxFQUFYOztBQUVBLFNBQU8sQ0FBQ0QsS0FBS0UsSUFBYixFQUFtQjtBQUNqQixRQUFNTCxPQUFPRyxLQUFLRyxLQUFsQjtBQUNBLFFBQUlDLGlCQUFKOztBQUVBLFFBQUk5RyxZQUFZdUcsSUFBWixDQUFKLEVBQXVCO0FBQ3JCTyxpQkFBVzdHLFFBQVFzRyxJQUFSLENBQVg7QUFDRCxLQUZELE1BRU87QUFDTE8saUJBQVdQLElBQVg7QUFDRDs7QUFFREQsZUFBV0EsU0FBU0UsTUFBVCxDQUFnQk0sUUFBaEIsQ0FBWDs7QUFFQUosV0FBT1YsU0FBU1csSUFBVCxFQUFQO0FBQ0Q7O0FBRUQsU0FBT0wsUUFBUDtBQUNEOztBQUVNLFNBQVNwRyxvQkFBVCxDQUE4QjZHLEdBQTlCLEVBQW1DO0FBQ3hDLFNBQU9BLFFBQVFBLFFBQVEsRUFBUixHQUFhLEVBQWIsR0FBa0I3QixTQUExQixDQUFQO0FBQ0Q7O0FBRU0sU0FBUy9FLGFBQVQsQ0FBdUI2RyxFQUF2QixFQUFvRDtBQUFBLE1BQXpCQyxPQUF5Qix1RUFBZjlHLGFBQWU7O0FBQ3pELE1BQUksT0FBTzhHLE9BQVAsS0FBbUIsVUFBbkIsSUFBaUNDLFVBQVVDLE1BQVYsS0FBcUIsQ0FBMUQsRUFBNkQ7QUFDM0Q7QUFDQUYsY0FBVTlHLGFBQVYsQ0FGMkQsQ0FFbEM7QUFDMUI7QUFDRCxNQUFJNkcsT0FBTyxJQUFQLElBQWUsUUFBT0EsRUFBUCx5Q0FBT0EsRUFBUCxPQUFjLFFBQTdCLElBQXlDLEVBQUUsVUFBVUEsRUFBWixDQUE3QyxFQUE4RDtBQUM1RCxXQUFPQSxFQUFQO0FBQ0Q7QUFQd0QsTUFTdkR6QixJQVR1RCxHQWFyRHlCLEVBYnFELENBU3ZEekIsSUFUdUQ7QUFBQSxNQVV2RDZCLEtBVnVELEdBYXJESixFQWJxRCxDQVV2REksS0FWdUQ7QUFBQSxNQVd2REwsR0FYdUQsR0FhckRDLEVBYnFELENBV3ZERCxHQVh1RDtBQUFBLE1BWXZETSxHQVp1RCxHQWFyREwsRUFicUQsQ0FZdkRLLEdBWnVEO0FBQUEsTUFjakRDLFFBZGlELEdBY3BDRixLQWRvQyxDQWNqREUsUUFkaUQ7O0FBZXpELE1BQUlDLFdBQVcsSUFBZjtBQUNBLE1BQUl2SCxZQUFZc0gsUUFBWixDQUFKLEVBQTJCO0FBQ3pCQyxlQUFXdEgsUUFBUXFILFFBQVIsRUFBa0JFLEdBQWxCLENBQXNCO0FBQUEsYUFBS1AsUUFBUVEsQ0FBUixDQUFMO0FBQUEsS0FBdEIsQ0FBWDtBQUNELEdBRkQsTUFFTyxJQUFJLE9BQU9ILFFBQVAsS0FBb0IsV0FBeEIsRUFBcUM7QUFDMUNDLGVBQVdOLFFBQVFLLFFBQVIsQ0FBWDtBQUNEOztBQUVELE1BQU1JLFdBQVczSCxpQkFBaUJ3RixJQUFqQixDQUFqQjs7QUFFQSxNQUFJbUMsYUFBYSxNQUFiLElBQXVCTixNQUFNTyx1QkFBakMsRUFBMEQ7QUFDeEQsUUFBSVAsTUFBTUUsUUFBTixJQUFrQixJQUF0QixFQUE0QjtBQUMxQixVQUFNTSxRQUFRLElBQUl2QyxLQUFKLENBQVUsb0VBQVYsQ0FBZDtBQUNBdUMsWUFBTW5DLElBQU4sR0FBYSxxQkFBYjtBQUNBLFlBQU1tQyxLQUFOO0FBQ0Q7QUFDRjs7QUFFRCxTQUFPO0FBQ0xGLHNCQURLO0FBRUxuQyxjQUZLO0FBR0w2QixnQkFISztBQUlMTCxTQUFLN0cscUJBQXFCNkcsR0FBckIsQ0FKQTtBQUtMTSxZQUxLO0FBTUxRLGNBQVUsSUFOTDtBQU9MTjtBQVBLLEdBQVA7QUFTRDs7QUFFRCxTQUFTTyxPQUFULENBQWlCQyxTQUFqQixFQUE0QkMsTUFBNUIsRUFBb0NDLE1BQXBDLEVBQTRDO0FBQzFDLE1BQUlDLGNBQUo7QUFDQSxNQUFNQyxVQUFVakMsTUFBTVIsU0FBTixDQUFnQjBDLElBQWhCLENBQXFCM0IsSUFBckIsQ0FBMEJzQixTQUExQixFQUFxQyxVQUFDeEIsSUFBRCxFQUFVO0FBQzdEMkIsWUFBUUYsT0FBT3pCLElBQVAsQ0FBUjtBQUNBLFdBQU8wQixPQUFPQyxLQUFQLENBQVA7QUFDRCxHQUhlLENBQWhCO0FBSUEsU0FBT0MsVUFBVUQsS0FBVixHQUFrQmhELFNBQXpCO0FBQ0Q7O0FBRU0sU0FBUzlFLFdBQVQsQ0FBcUI0RyxFQUFyQixFQUF5QnFCLFNBQXpCLEVBQW9DO0FBQ3pDLE1BQUlyQixPQUFPLElBQVAsSUFBZSxRQUFPQSxFQUFQLHlDQUFPQSxFQUFQLE9BQWMsUUFBN0IsSUFBeUMsRUFBRSxVQUFVQSxFQUFaLENBQTdDLEVBQThEO0FBQzVELFdBQU85QixTQUFQO0FBQ0Q7QUFDRCxNQUFJbUQsVUFBVXJCLEVBQVYsQ0FBSixFQUFtQjtBQUNqQixXQUFPQSxFQUFQO0FBQ0Q7QUFOd0MsTUFPakNPLFFBUGlDLEdBT3BCUCxFQVBvQixDQU9qQ08sUUFQaUM7O0FBUXpDLE1BQUl2SCxZQUFZdUgsUUFBWixDQUFKLEVBQTJCO0FBQ3pCLFdBQU9PLFFBQVFQLFFBQVIsRUFBa0I7QUFBQSxhQUFLbkgsWUFBWXFILENBQVosRUFBZVksU0FBZixDQUFMO0FBQUEsS0FBbEIsRUFBa0Q7QUFBQSxhQUFLLE9BQU9aLENBQVAsS0FBYSxXQUFsQjtBQUFBLEtBQWxELENBQVA7QUFDRDtBQUNELFNBQU9ySCxZQUFZbUgsUUFBWixFQUFzQmMsU0FBdEIsQ0FBUDtBQUNEOztBQUVNLFNBQVNoSSxtQkFBVCxDQUE2QmlGLElBQTdCLEVBQW1DO0FBQ3hDLE1BQUlBLEtBQUsrQixHQUFMLEtBQWEsSUFBYixJQUFxQi9CLEtBQUt5QixHQUFMLEtBQWEsSUFBdEMsRUFBNEM7QUFDMUMsd0NBQ0t6QixLQUFLOEIsS0FEVjtBQUVFTCxXQUFLekIsS0FBS3lCLEdBRlo7QUFHRU0sV0FBSy9CLEtBQUsrQjtBQUhaO0FBS0Q7QUFDRCxTQUFPL0IsS0FBSzhCLEtBQVo7QUFDRDs7QUFFTSxTQUFTOUcsaUJBQVQsQ0FDTGdJLFNBREssRUFJTDtBQUFBLE1BRkFDLFdBRUEsdUVBRmN4SSxnQkFFZDtBQUFBLE1BREF5SSxjQUNBLHVFQURpQjFJLGlCQUNqQjs7QUFDQSxNQUFNMkksU0FBU0gsVUFBVUksTUFBVixDQUFpQjtBQUFBLFdBQVFwRCxLQUFLQyxJQUFMLEtBQWN2RSx1QkFBdEI7QUFBQSxHQUFqQixFQUFtRHdHLEdBQW5ELENBQXVEO0FBQUEsV0FBSyxDQUN6RWUsWUFBWWQsRUFBRWxDLElBQWQsQ0FEeUUsRUFFekVpRCxlQUFlZixDQUFmLENBRnlFLENBQUw7QUFBQSxHQUF2RCxFQUdaakIsTUFIWSxDQUdMLENBQUMsQ0FDVCxPQURTLEVBRVQsa0JBRlMsQ0FBRCxDQUhLLENBQWY7O0FBUUEsU0FBT2lDLE9BQU9qQixHQUFQLENBQVcsaUJBQVdtQixDQUFYLEVBQWNDLEdBQWQsRUFBc0I7QUFBQTtBQUFBLFFBQWxCbkQsSUFBa0I7O0FBQUEsZ0JBQ1RtRCxJQUFJaEUsS0FBSixDQUFVK0QsSUFBSSxDQUFkLEVBQWlCUCxJQUFqQixDQUFzQjtBQUFBO0FBQUEsVUFBRVYsUUFBRjs7QUFBQSxhQUFnQkEsYUFBYSxNQUE3QjtBQUFBLEtBQXRCLEtBQThELEVBRHJEO0FBQUE7QUFBQSxRQUM3Qm1CLGdCQUQ2Qjs7QUFFdEMsZ0NBQW1CcEQsSUFBbkIsS0FBMEJvRCw0Q0FBbUNBLGdCQUFuQyxVQUF5RCxFQUFuRjtBQUNELEdBSE0sRUFHSkMsSUFISSxDQUdDLEVBSEQsQ0FBUDtBQUlEOztBQUVNLFNBQVN2SSxhQUFULENBQ0xxSCxLQURLLEVBRUxtQixnQkFGSyxFQUdMQyxRQUhLLEVBR0s7QUFDVlYsU0FKSyxFQVFMO0FBQUEsTUFIQUMsV0FHQSx1RUFIY3hJLGdCQUdkO0FBQUEsTUFGQXlJLGNBRUEsdUVBRmlCMUksaUJBRWpCO0FBQUEsTUFEQW1KLFlBQ0EsdUVBRGUsRUFDZjs7QUFDQSxNQUFNcEIsV0FBV2tCLG9CQUFvQixFQUFyQzs7QUFEQSxNQUdRRyxpQkFIUixHQUc4QnJCLFFBSDlCLENBR1FxQixpQkFIUjtBQUFBLE1BS1FDLHdCQUxSLEdBS3FDRixZQUxyQyxDQUtRRSx3QkFMUjs7O0FBT0EsTUFBSSxDQUFDRCxpQkFBRCxJQUFzQixDQUFDQyx3QkFBM0IsRUFBcUQ7QUFDbkQsVUFBTXZCLEtBQU47QUFDRDs7QUFFRCxNQUFJdUIsd0JBQUosRUFBOEI7QUFDNUIsUUFBTUMsY0FBY0QseUJBQXlCMUMsSUFBekIsQ0FBOEJ3QyxZQUE5QixFQUE0Q3JCLEtBQTVDLENBQXBCO0FBQ0FDLGFBQVN3QixRQUFULENBQWtCRCxXQUFsQjtBQUNEOztBQUVELE1BQUlGLGlCQUFKLEVBQXVCO0FBQ3JCLFFBQU1JLGlCQUFpQmhKLGtCQUFrQmdJLFNBQWxCLEVBQTZCQyxXQUE3QixFQUEwQ0MsY0FBMUMsQ0FBdkI7QUFDQVUsc0JBQWtCekMsSUFBbEIsQ0FBdUJvQixRQUF2QixFQUFpQ0QsS0FBakMsRUFBd0MsRUFBRTBCLDhCQUFGLEVBQXhDO0FBQ0Q7QUFDRjs7QUFFTSxTQUFTOUksZ0JBQVQsQ0FBMEIrSSxZQUExQixFQUF3Q0MsZUFBeEMsRUFBeUQ7QUFDOUQsTUFBSSxDQUFDRCxZQUFELElBQWlCLENBQUNDLGVBQXRCLEVBQXVDO0FBQ3JDLFdBQU8sRUFBUDtBQUNEO0FBQ0QsU0FBTyx5QkFBWUMsT0FBT0MsSUFBUCxDQUFZSCxZQUFaLEVBQTBCL0IsR0FBMUIsQ0FBOEI7QUFBQSxXQUFPLENBQUNULEdBQUQsRUFBTXlDLGdCQUFnQnpDLEdBQWhCLENBQU4sQ0FBUDtBQUFBLEdBQTlCLENBQVosQ0FBUDtBQUNEOztBQUVNLFNBQVN0RyxxQkFBVCxDQUErQmtKLGlCQUEvQixFQUFrREMsSUFBbEQsRUFBd0RDLE9BQXhELEVBQWlFO0FBQ3RFLE1BQUksQ0FBQ0Ysa0JBQWtCRSxRQUFRQyxpQkFBMUIsQ0FBTCxFQUFtRDtBQUNqRCxXQUFPRixLQUFLckMsUUFBWjtBQUNEO0FBQ0QsTUFBTXdDLGFBQWEzSixZQUFZd0osSUFBWixFQUFrQjtBQUFBLFdBQVF0RSxLQUFLQyxJQUFMLEtBQWN2RSx1QkFBdEI7QUFBQSxHQUFsQixDQUFuQjtBQUNBLE1BQUksQ0FBQytJLFVBQUwsRUFBaUI7QUFDZixVQUFNLElBQUkxRSxLQUFKLENBQVUsK0NBQVYsQ0FBTjtBQUNEO0FBQ0QsU0FBTzBFLFdBQVd4QyxRQUFsQjtBQUNEOztBQUVNLFNBQVM3Ryx5QkFBVCxDQUFtQzBFLGFBQW5DLEVBQWtERSxJQUFsRCxFQUF3RHVFLE9BQXhELEVBQWlFO0FBQUEsTUFDOURDLGlCQUQ4RCxHQUNoQkQsT0FEZ0IsQ0FDOURDLGlCQUQ4RDtBQUFBLE1BQzNDRSxzQkFEMkMsR0FDaEJILE9BRGdCLENBQzNDRyxzQkFEMkM7O0FBRXRFLE1BQUksQ0FBQ0YsaUJBQUwsRUFBd0I7QUFDdEIsV0FBT3hFLElBQVA7QUFDRDtBQUNELFNBQU9GLGNBQ0wwRSxpQkFESyxFQUVMRSxzQkFGSyxFQUdMNUUsY0FBY3BFLHVCQUFkLEVBQTBCLElBQTFCLEVBQWdDc0UsSUFBaEMsQ0FISyxDQUFQO0FBS0Q7O0FBRU0sU0FBUzNFLGlDQUFULFFBQWdGO0FBQUEsTUFBbkNzSixNQUFtQyxTQUFuQ0EsTUFBbUM7QUFBQSxNQUEzQkMsdUJBQTJCLFNBQTNCQSx1QkFBMkI7O0FBQ3JGLFNBQU87QUFDTEMsV0FESztBQUFBLHlCQUNLO0FBQ1IsWUFBTXRDLFdBQVdxQyx5QkFBakI7QUFDQSxlQUFPckMsV0FBV29DLE9BQU9wQyxRQUFQLEVBQWlCTixRQUE1QixHQUF1QyxJQUE5QztBQUNEOztBQUpJO0FBQUE7QUFLTDZDLFVBTEs7QUFBQSxzQkFLRXBELEVBTEYsRUFLTXFELE9BTE4sRUFLZUMsUUFMZixFQUt5QjtBQUM1QixZQUFNekMsV0FBV3FDLHlCQUFqQjtBQUNBLFlBQUksQ0FBQ3JDLFFBQUwsRUFBZTtBQUNiLGdCQUFNLElBQUl4QyxLQUFKLENBQVUscUVBQVYsQ0FBTjtBQUNEO0FBQ0QsZUFBT3dDLFNBQVMwQyx5QkFBVCxDQUFtQ3ZELEdBQUdJLEtBQXRDLEVBQTZDa0QsUUFBN0MsQ0FBUDtBQUNEOztBQVhJO0FBQUE7QUFBQSxHQUFQO0FBYUQ7O0FBRU0sU0FBUzFKLGlCQUFULENBQTJCNEosY0FBM0IsRUFBMkM7QUFDaEQsU0FBT0MsUUFBUUMsT0FBUixDQUFnQixFQUFFLFdBQVNGLGNBQVgsRUFBaEIsQ0FBUDtBQUNEIiwiZmlsZSI6IlV0aWxzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGZ1bmN0aW9uTmFtZSBmcm9tICdmdW5jdGlvbi5wcm90b3R5cGUubmFtZSc7XG5pbXBvcnQgZnJvbUVudHJpZXMgZnJvbSAnb2JqZWN0LmZyb21lbnRyaWVzJztcbmltcG9ydCBjcmVhdGVNb3VudFdyYXBwZXIgZnJvbSAnLi9jcmVhdGVNb3VudFdyYXBwZXInO1xuaW1wb3J0IGNyZWF0ZVJlbmRlcldyYXBwZXIgZnJvbSAnLi9jcmVhdGVSZW5kZXJXcmFwcGVyJztcbmltcG9ydCB3cmFwIGZyb20gJy4vd3JhcFdpdGhTaW1wbGVXcmFwcGVyJztcbmltcG9ydCBSb290RmluZGVyIGZyb20gJy4vUm9vdEZpbmRlcic7XG5cbmV4cG9ydCB7XG4gIGNyZWF0ZU1vdW50V3JhcHBlcixcbiAgY3JlYXRlUmVuZGVyV3JhcHBlcixcbiAgd3JhcCxcbiAgUm9vdEZpbmRlcixcbn07XG5cbmV4cG9ydCBmdW5jdGlvbiBtYXBOYXRpdmVFdmVudE5hbWVzKGV2ZW50LCB7XG4gIGFuaW1hdGlvbiA9IGZhbHNlLCAvLyBzaG91bGQgYmUgdHJ1ZSBmb3IgUmVhY3QgMTUrXG4gIHBvaW50ZXJFdmVudHMgPSBmYWxzZSwgLy8gc2hvdWxkIGJlIHRydWUgZm9yIFJlYWN0IDE2LjQrXG4gIGF1eENsaWNrID0gZmFsc2UsIC8vIHNob3VsZCBiZSB0cnVlIGZvciBSZWFjdCAxNi41K1xufSA9IHt9KSB7XG4gIGNvbnN0IG5hdGl2ZVRvUmVhY3RFdmVudE1hcCA9IHtcbiAgICBjb21wb3NpdGlvbmVuZDogJ2NvbXBvc2l0aW9uRW5kJyxcbiAgICBjb21wb3NpdGlvbnN0YXJ0OiAnY29tcG9zaXRpb25TdGFydCcsXG4gICAgY29tcG9zaXRpb251cGRhdGU6ICdjb21wb3NpdGlvblVwZGF0ZScsXG4gICAga2V5ZG93bjogJ2tleURvd24nLFxuICAgIGtleXVwOiAna2V5VXAnLFxuICAgIGtleXByZXNzOiAna2V5UHJlc3MnLFxuICAgIGNvbnRleHRtZW51OiAnY29udGV4dE1lbnUnLFxuICAgIGRibGNsaWNrOiAnZG91YmxlQ2xpY2snLFxuICAgIGRvdWJsZWNsaWNrOiAnZG91YmxlQ2xpY2snLCAvLyBrZXB0IGZvciBsZWdhY3kuIFRPRE86IHJlbW92ZSB3aXRoIG5leHQgbWFqb3IuXG4gICAgZHJhZ2VuZDogJ2RyYWdFbmQnLFxuICAgIGRyYWdlbnRlcjogJ2RyYWdFbnRlcicsXG4gICAgZHJhZ2V4aXN0OiAnZHJhZ0V4aXQnLFxuICAgIGRyYWdsZWF2ZTogJ2RyYWdMZWF2ZScsXG4gICAgZHJhZ292ZXI6ICdkcmFnT3ZlcicsXG4gICAgZHJhZ3N0YXJ0OiAnZHJhZ1N0YXJ0JyxcbiAgICBtb3VzZWRvd246ICdtb3VzZURvd24nLFxuICAgIG1vdXNlbW92ZTogJ21vdXNlTW92ZScsXG4gICAgbW91c2VvdXQ6ICdtb3VzZU91dCcsXG4gICAgbW91c2VvdmVyOiAnbW91c2VPdmVyJyxcbiAgICBtb3VzZXVwOiAnbW91c2VVcCcsXG4gICAgdG91Y2hjYW5jZWw6ICd0b3VjaENhbmNlbCcsXG4gICAgdG91Y2hlbmQ6ICd0b3VjaEVuZCcsXG4gICAgdG91Y2htb3ZlOiAndG91Y2hNb3ZlJyxcbiAgICB0b3VjaHN0YXJ0OiAndG91Y2hTdGFydCcsXG4gICAgY2FucGxheTogJ2NhblBsYXknLFxuICAgIGNhbnBsYXl0aHJvdWdoOiAnY2FuUGxheVRocm91Z2gnLFxuICAgIGR1cmF0aW9uY2hhbmdlOiAnZHVyYXRpb25DaGFuZ2UnLFxuICAgIGxvYWRlZGRhdGE6ICdsb2FkZWREYXRhJyxcbiAgICBsb2FkZWRtZXRhZGF0YTogJ2xvYWRlZE1ldGFkYXRhJyxcbiAgICBsb2Fkc3RhcnQ6ICdsb2FkU3RhcnQnLFxuICAgIHJhdGVjaGFuZ2U6ICdyYXRlQ2hhbmdlJyxcbiAgICB0aW1ldXBkYXRlOiAndGltZVVwZGF0ZScsXG4gICAgdm9sdW1lY2hhbmdlOiAndm9sdW1lQ2hhbmdlJyxcbiAgICBiZWZvcmVpbnB1dDogJ2JlZm9yZUlucHV0JyxcbiAgICBtb3VzZWVudGVyOiAnbW91c2VFbnRlcicsXG4gICAgbW91c2VsZWF2ZTogJ21vdXNlTGVhdmUnLFxuICAgIHRyYW5zaXRpb25lbmQ6ICd0cmFuc2l0aW9uRW5kJyxcbiAgICAuLi4oYW5pbWF0aW9uICYmIHtcbiAgICAgIGFuaW1hdGlvbnN0YXJ0OiAnYW5pbWF0aW9uU3RhcnQnLFxuICAgICAgYW5pbWF0aW9uaXRlcmF0aW9uOiAnYW5pbWF0aW9uSXRlcmF0aW9uJyxcbiAgICAgIGFuaW1hdGlvbmVuZDogJ2FuaW1hdGlvbkVuZCcsXG4gICAgfSksXG4gICAgLi4uKHBvaW50ZXJFdmVudHMgJiYge1xuICAgICAgcG9pbnRlcmRvd246ICdwb2ludGVyRG93bicsXG4gICAgICBwb2ludGVybW92ZTogJ3BvaW50ZXJNb3ZlJyxcbiAgICAgIHBvaW50ZXJ1cDogJ3BvaW50ZXJVcCcsXG4gICAgICBwb2ludGVyY2FuY2VsOiAncG9pbnRlckNhbmNlbCcsXG4gICAgICBnb3Rwb2ludGVyY2FwdHVyZTogJ2dvdFBvaW50ZXJDYXB0dXJlJyxcbiAgICAgIGxvc3Rwb2ludGVyY2FwdHVyZTogJ2xvc3RQb2ludGVyQ2FwdHVyZScsXG4gICAgICBwb2ludGVyZW50ZXI6ICdwb2ludGVyRW50ZXInLFxuICAgICAgcG9pbnRlcmxlYXZlOiAncG9pbnRlckxlYXZlJyxcbiAgICAgIHBvaW50ZXJvdmVyOiAncG9pbnRlck92ZXInLFxuICAgICAgcG9pbnRlcm91dDogJ3BvaW50ZXJPdXQnLFxuICAgIH0pLFxuICAgIC4uLihhdXhDbGljayAmJiB7XG4gICAgICBhdXhjbGljazogJ2F1eENsaWNrJyxcbiAgICB9KSxcbiAgfTtcblxuICByZXR1cm4gbmF0aXZlVG9SZWFjdEV2ZW50TWFwW2V2ZW50XSB8fCBldmVudDtcbn1cblxuLy8gJ2NsaWNrJyA9PiAnb25DbGljaydcbi8vICdtb3VzZUVudGVyJyA9PiAnb25Nb3VzZUVudGVyJ1xuZXhwb3J0IGZ1bmN0aW9uIHByb3BGcm9tRXZlbnQoZXZlbnQsIGV2ZW50T3B0aW9ucyA9IHt9KSB7XG4gIGNvbnN0IG5hdGl2ZUV2ZW50ID0gbWFwTmF0aXZlRXZlbnROYW1lcyhldmVudCwgZXZlbnRPcHRpb25zKTtcbiAgcmV0dXJuIGBvbiR7bmF0aXZlRXZlbnRbMF0udG9VcHBlckNhc2UoKX0ke25hdGl2ZUV2ZW50LnNsaWNlKDEpfWA7XG59XG5cbmV4cG9ydCBmdW5jdGlvbiB3aXRoU2V0U3RhdGVBbGxvd2VkKGZuKSB7XG4gIC8vIE5PVEUobG1yKTpcbiAgLy8gdGhpcyBpcyBjdXJyZW50bHkgaGVyZSB0byBjaXJjdW12ZW50IGEgUmVhY3QgYnVnIHdoZXJlIGBzZXRTdGF0ZSgpYCBpc1xuICAvLyBub3QgYWxsb3dlZCB3aXRob3V0IGdsb2JhbCBiZWluZyBkZWZpbmVkLlxuICBsZXQgY2xlYW51cCA9IGZhbHNlO1xuICBpZiAodHlwZW9mIGdsb2JhbC5kb2N1bWVudCA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBjbGVhbnVwID0gdHJ1ZTtcbiAgICBnbG9iYWwuZG9jdW1lbnQgPSB7fTtcbiAgfVxuICBjb25zdCByZXN1bHQgPSBmbigpO1xuICBpZiAoY2xlYW51cCkge1xuICAgIC8vIFRoaXMgd29ya3MgYXJvdW5kIGEgYnVnIGluIG5vZGUvamVzdCBpbiB0aGF0IGRldmVsb3BlcnMgYXJlbid0IGFibGUgdG9cbiAgICAvLyBkZWxldGUgdGhpbmdzIGZyb20gZ2xvYmFsIHdoZW4gcnVubmluZyBpbiBhIG5vZGUgdm0uXG4gICAgZ2xvYmFsLmRvY3VtZW50ID0gdW5kZWZpbmVkO1xuICAgIGRlbGV0ZSBnbG9iYWwuZG9jdW1lbnQ7XG4gIH1cbiAgcmV0dXJuIHJlc3VsdDtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIGFzc2VydERvbUF2YWlsYWJsZShmZWF0dXJlKSB7XG4gIGlmICghZ2xvYmFsIHx8ICFnbG9iYWwuZG9jdW1lbnQgfHwgIWdsb2JhbC5kb2N1bWVudC5jcmVhdGVFbGVtZW50KSB7XG4gICAgdGhyb3cgbmV3IEVycm9yKGBFbnp5bWUncyAke2ZlYXR1cmV9IGV4cGVjdHMgYSBET00gZW52aXJvbm1lbnQgdG8gYmUgbG9hZGVkLCBidXQgZm91bmQgbm9uZWApO1xuICB9XG59XG5cbmV4cG9ydCBmdW5jdGlvbiBkaXNwbGF5TmFtZU9mTm9kZShub2RlKSB7XG4gIGlmICghbm9kZSkgcmV0dXJuIG51bGw7XG5cbiAgY29uc3QgeyB0eXBlIH0gPSBub2RlO1xuXG4gIGlmICghdHlwZSkgcmV0dXJuIG51bGw7XG5cbiAgcmV0dXJuIHR5cGUuZGlzcGxheU5hbWUgfHwgKHR5cGVvZiB0eXBlID09PSAnZnVuY3Rpb24nID8gZnVuY3Rpb25OYW1lKHR5cGUpIDogdHlwZS5uYW1lIHx8IHR5cGUpO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gbm9kZVR5cGVGcm9tVHlwZSh0eXBlKSB7XG4gIGlmICh0eXBlb2YgdHlwZSA9PT0gJ3N0cmluZycpIHtcbiAgICByZXR1cm4gJ2hvc3QnO1xuICB9XG4gIGlmICh0eXBlICYmIHR5cGUucHJvdG90eXBlICYmIHR5cGUucHJvdG90eXBlLmlzUmVhY3RDb21wb25lbnQpIHtcbiAgICByZXR1cm4gJ2NsYXNzJztcbiAgfVxuICByZXR1cm4gJ2Z1bmN0aW9uJztcbn1cblxuZnVuY3Rpb24gZ2V0SXRlcmF0b3JGbihvYmopIHtcbiAgY29uc3QgaXRlcmF0b3JGbiA9IG9iaiAmJiAoXG4gICAgKHR5cGVvZiBTeW1ib2wgPT09ICdmdW5jdGlvbicgJiYgdHlwZW9mIFN5bWJvbC5pdGVyYXRvciA9PT0gJ3N5bWJvbCcgJiYgb2JqW1N5bWJvbC5pdGVyYXRvcl0pXG4gICAgfHwgb2JqWydAQGl0ZXJhdG9yJ11cbiAgKTtcblxuICBpZiAodHlwZW9mIGl0ZXJhdG9yRm4gPT09ICdmdW5jdGlvbicpIHtcbiAgICByZXR1cm4gaXRlcmF0b3JGbjtcbiAgfVxuXG4gIHJldHVybiB1bmRlZmluZWQ7XG59XG5cbmZ1bmN0aW9uIGlzSXRlcmFibGUob2JqKSB7XG4gIHJldHVybiAhIWdldEl0ZXJhdG9yRm4ob2JqKTtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIGlzQXJyYXlMaWtlKG9iaikge1xuICByZXR1cm4gQXJyYXkuaXNBcnJheShvYmopIHx8ICh0eXBlb2Ygb2JqICE9PSAnc3RyaW5nJyAmJiBpc0l0ZXJhYmxlKG9iaikpO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gZmxhdHRlbihhcnJzKSB7XG4gIC8vIG9wdGltaXplIGZvciB0aGUgbW9zdCBjb21tb24gY2FzZVxuICBpZiAoQXJyYXkuaXNBcnJheShhcnJzKSkge1xuICAgIHJldHVybiBhcnJzLnJlZHVjZShcbiAgICAgIChmbGF0QXJycywgaXRlbSkgPT4gZmxhdEFycnMuY29uY2F0KGlzQXJyYXlMaWtlKGl0ZW0pID8gZmxhdHRlbihpdGVtKSA6IGl0ZW0pLFxuICAgICAgW10sXG4gICAgKTtcbiAgfVxuXG4gIC8vIGZhbGxiYWNrIGZvciBhcmJpdHJhcnkgaXRlcmFibGUgY2hpbGRyZW5cbiAgbGV0IGZsYXRBcnJzID0gW107XG5cbiAgY29uc3QgaXRlcmF0b3JGbiA9IGdldEl0ZXJhdG9yRm4oYXJycyk7XG4gIGNvbnN0IGl0ZXJhdG9yID0gaXRlcmF0b3JGbi5jYWxsKGFycnMpO1xuXG4gIGxldCBzdGVwID0gaXRlcmF0b3IubmV4dCgpO1xuXG4gIHdoaWxlICghc3RlcC5kb25lKSB7XG4gICAgY29uc3QgaXRlbSA9IHN0ZXAudmFsdWU7XG4gICAgbGV0IGZsYXRJdGVtO1xuXG4gICAgaWYgKGlzQXJyYXlMaWtlKGl0ZW0pKSB7XG4gICAgICBmbGF0SXRlbSA9IGZsYXR0ZW4oaXRlbSk7XG4gICAgfSBlbHNlIHtcbiAgICAgIGZsYXRJdGVtID0gaXRlbTtcbiAgICB9XG5cbiAgICBmbGF0QXJycyA9IGZsYXRBcnJzLmNvbmNhdChmbGF0SXRlbSk7XG5cbiAgICBzdGVwID0gaXRlcmF0b3IubmV4dCgpO1xuICB9XG5cbiAgcmV0dXJuIGZsYXRBcnJzO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gZW5zdXJlS2V5T3JVbmRlZmluZWQoa2V5KSB7XG4gIHJldHVybiBrZXkgfHwgKGtleSA9PT0gJycgPyAnJyA6IHVuZGVmaW5lZCk7XG59XG5cbmV4cG9ydCBmdW5jdGlvbiBlbGVtZW50VG9UcmVlKGVsLCByZWN1cnNlID0gZWxlbWVudFRvVHJlZSkge1xuICBpZiAodHlwZW9mIHJlY3Vyc2UgIT09ICdmdW5jdGlvbicgJiYgYXJndW1lbnRzLmxlbmd0aCA9PT0gMykge1xuICAgIC8vIHNwZWNpYWwgY2FzZSBmb3IgYmFja3dhcmRzIGNvbXBhdCBmb3IgYC5tYXAoZWxlbWVudFRvVHJlZSlgXG4gICAgcmVjdXJzZSA9IGVsZW1lbnRUb1RyZWU7IC8vIGVzbGludC1kaXNhYmxlLWxpbmUgbm8tcGFyYW0tcmVhc3NpZ25cbiAgfVxuICBpZiAoZWwgPT09IG51bGwgfHwgdHlwZW9mIGVsICE9PSAnb2JqZWN0JyB8fCAhKCd0eXBlJyBpbiBlbCkpIHtcbiAgICByZXR1cm4gZWw7XG4gIH1cbiAgY29uc3Qge1xuICAgIHR5cGUsXG4gICAgcHJvcHMsXG4gICAga2V5LFxuICAgIHJlZixcbiAgfSA9IGVsO1xuICBjb25zdCB7IGNoaWxkcmVuIH0gPSBwcm9wcztcbiAgbGV0IHJlbmRlcmVkID0gbnVsbDtcbiAgaWYgKGlzQXJyYXlMaWtlKGNoaWxkcmVuKSkge1xuICAgIHJlbmRlcmVkID0gZmxhdHRlbihjaGlsZHJlbikubWFwKHggPT4gcmVjdXJzZSh4KSk7XG4gIH0gZWxzZSBpZiAodHlwZW9mIGNoaWxkcmVuICE9PSAndW5kZWZpbmVkJykge1xuICAgIHJlbmRlcmVkID0gcmVjdXJzZShjaGlsZHJlbik7XG4gIH1cblxuICBjb25zdCBub2RlVHlwZSA9IG5vZGVUeXBlRnJvbVR5cGUodHlwZSk7XG5cbiAgaWYgKG5vZGVUeXBlID09PSAnaG9zdCcgJiYgcHJvcHMuZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUwpIHtcbiAgICBpZiAocHJvcHMuY2hpbGRyZW4gIT0gbnVsbCkge1xuICAgICAgY29uc3QgZXJyb3IgPSBuZXcgRXJyb3IoJ0NhbiBvbmx5IHNldCBvbmUgb2YgYGNoaWxkcmVuYCBvciBgcHJvcHMuZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUxgLicpO1xuICAgICAgZXJyb3IubmFtZSA9ICdJbnZhcmlhbnQgVmlvbGF0aW9uJztcbiAgICAgIHRocm93IGVycm9yO1xuICAgIH1cbiAgfVxuXG4gIHJldHVybiB7XG4gICAgbm9kZVR5cGUsXG4gICAgdHlwZSxcbiAgICBwcm9wcyxcbiAgICBrZXk6IGVuc3VyZUtleU9yVW5kZWZpbmVkKGtleSksXG4gICAgcmVmLFxuICAgIGluc3RhbmNlOiBudWxsLFxuICAgIHJlbmRlcmVkLFxuICB9O1xufVxuXG5mdW5jdGlvbiBtYXBGaW5kKGFycmF5bGlrZSwgbWFwcGVyLCBmaW5kZXIpIHtcbiAgbGV0IGZvdW5kO1xuICBjb25zdCBpc0ZvdW5kID0gQXJyYXkucHJvdG90eXBlLmZpbmQuY2FsbChhcnJheWxpa2UsIChpdGVtKSA9PiB7XG4gICAgZm91bmQgPSBtYXBwZXIoaXRlbSk7XG4gICAgcmV0dXJuIGZpbmRlcihmb3VuZCk7XG4gIH0pO1xuICByZXR1cm4gaXNGb3VuZCA/IGZvdW5kIDogdW5kZWZpbmVkO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gZmluZEVsZW1lbnQoZWwsIHByZWRpY2F0ZSkge1xuICBpZiAoZWwgPT09IG51bGwgfHwgdHlwZW9mIGVsICE9PSAnb2JqZWN0JyB8fCAhKCd0eXBlJyBpbiBlbCkpIHtcbiAgICByZXR1cm4gdW5kZWZpbmVkO1xuICB9XG4gIGlmIChwcmVkaWNhdGUoZWwpKSB7XG4gICAgcmV0dXJuIGVsO1xuICB9XG4gIGNvbnN0IHsgcmVuZGVyZWQgfSA9IGVsO1xuICBpZiAoaXNBcnJheUxpa2UocmVuZGVyZWQpKSB7XG4gICAgcmV0dXJuIG1hcEZpbmQocmVuZGVyZWQsIHggPT4gZmluZEVsZW1lbnQoeCwgcHJlZGljYXRlKSwgeCA9PiB0eXBlb2YgeCAhPT0gJ3VuZGVmaW5lZCcpO1xuICB9XG4gIHJldHVybiBmaW5kRWxlbWVudChyZW5kZXJlZCwgcHJlZGljYXRlKTtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIHByb3BzV2l0aEtleXNBbmRSZWYobm9kZSkge1xuICBpZiAobm9kZS5yZWYgIT09IG51bGwgfHwgbm9kZS5rZXkgIT09IG51bGwpIHtcbiAgICByZXR1cm4ge1xuICAgICAgLi4ubm9kZS5wcm9wcyxcbiAgICAgIGtleTogbm9kZS5rZXksXG4gICAgICByZWY6IG5vZGUucmVmLFxuICAgIH07XG4gIH1cbiAgcmV0dXJuIG5vZGUucHJvcHM7XG59XG5cbmV4cG9ydCBmdW5jdGlvbiBnZXRDb21wb25lbnRTdGFjayhcbiAgaGllcmFyY2h5LFxuICBnZXROb2RlVHlwZSA9IG5vZGVUeXBlRnJvbVR5cGUsXG4gIGdldERpc3BsYXlOYW1lID0gZGlzcGxheU5hbWVPZk5vZGUsXG4pIHtcbiAgY29uc3QgdHVwbGVzID0gaGllcmFyY2h5LmZpbHRlcihub2RlID0+IG5vZGUudHlwZSAhPT0gUm9vdEZpbmRlcikubWFwKHggPT4gW1xuICAgIGdldE5vZGVUeXBlKHgudHlwZSksXG4gICAgZ2V0RGlzcGxheU5hbWUoeCksXG4gIF0pLmNvbmNhdChbW1xuICAgICdjbGFzcycsXG4gICAgJ1dyYXBwZXJDb21wb25lbnQnLFxuICBdXSk7XG5cbiAgcmV0dXJuIHR1cGxlcy5tYXAoKFssIG5hbWVdLCBpLCBhcnIpID0+IHtcbiAgICBjb25zdCBbLCBjbG9zZXN0Q29tcG9uZW50XSA9IGFyci5zbGljZShpICsgMSkuZmluZCgoW25vZGVUeXBlXSkgPT4gbm9kZVR5cGUgIT09ICdob3N0JykgfHwgW107XG4gICAgcmV0dXJuIGBcXG4gICAgaW4gJHtuYW1lfSR7Y2xvc2VzdENvbXBvbmVudCA/IGAgKGNyZWF0ZWQgYnkgJHtjbG9zZXN0Q29tcG9uZW50fSlgIDogJyd9YDtcbiAgfSkuam9pbignJyk7XG59XG5cbmV4cG9ydCBmdW5jdGlvbiBzaW11bGF0ZUVycm9yKFxuICBlcnJvcixcbiAgY2F0Y2hpbmdJbnN0YW5jZSxcbiAgcm9vdE5vZGUsIC8vIFRPRE86IHJlbW92ZSBgcm9vdE5vZGVgIG5leHQgc2VtdmVyLW1ham9yXG4gIGhpZXJhcmNoeSxcbiAgZ2V0Tm9kZVR5cGUgPSBub2RlVHlwZUZyb21UeXBlLFxuICBnZXREaXNwbGF5TmFtZSA9IGRpc3BsYXlOYW1lT2ZOb2RlLFxuICBjYXRjaGluZ1R5cGUgPSB7fSxcbikge1xuICBjb25zdCBpbnN0YW5jZSA9IGNhdGNoaW5nSW5zdGFuY2UgfHwge307XG5cbiAgY29uc3QgeyBjb21wb25lbnREaWRDYXRjaCB9ID0gaW5zdGFuY2U7XG5cbiAgY29uc3QgeyBnZXREZXJpdmVkU3RhdGVGcm9tRXJyb3IgfSA9IGNhdGNoaW5nVHlwZTtcblxuICBpZiAoIWNvbXBvbmVudERpZENhdGNoICYmICFnZXREZXJpdmVkU3RhdGVGcm9tRXJyb3IpIHtcbiAgICB0aHJvdyBlcnJvcjtcbiAgfVxuXG4gIGlmIChnZXREZXJpdmVkU3RhdGVGcm9tRXJyb3IpIHtcbiAgICBjb25zdCBzdGF0ZVVwZGF0ZSA9IGdldERlcml2ZWRTdGF0ZUZyb21FcnJvci5jYWxsKGNhdGNoaW5nVHlwZSwgZXJyb3IpO1xuICAgIGluc3RhbmNlLnNldFN0YXRlKHN0YXRlVXBkYXRlKTtcbiAgfVxuXG4gIGlmIChjb21wb25lbnREaWRDYXRjaCkge1xuICAgIGNvbnN0IGNvbXBvbmVudFN0YWNrID0gZ2V0Q29tcG9uZW50U3RhY2soaGllcmFyY2h5LCBnZXROb2RlVHlwZSwgZ2V0RGlzcGxheU5hbWUpO1xuICAgIGNvbXBvbmVudERpZENhdGNoLmNhbGwoaW5zdGFuY2UsIGVycm9yLCB7IGNvbXBvbmVudFN0YWNrIH0pO1xuICB9XG59XG5cbmV4cG9ydCBmdW5jdGlvbiBnZXRNYXNrZWRDb250ZXh0KGNvbnRleHRUeXBlcywgdW5tYXNrZWRDb250ZXh0KSB7XG4gIGlmICghY29udGV4dFR5cGVzIHx8ICF1bm1hc2tlZENvbnRleHQpIHtcbiAgICByZXR1cm4ge307XG4gIH1cbiAgcmV0dXJuIGZyb21FbnRyaWVzKE9iamVjdC5rZXlzKGNvbnRleHRUeXBlcykubWFwKGtleSA9PiBba2V5LCB1bm1hc2tlZENvbnRleHRba2V5XV0pKTtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIGdldE5vZGVGcm9tUm9vdEZpbmRlcihpc0N1c3RvbUNvbXBvbmVudCwgdHJlZSwgb3B0aW9ucykge1xuICBpZiAoIWlzQ3VzdG9tQ29tcG9uZW50KG9wdGlvbnMud3JhcHBpbmdDb21wb25lbnQpKSB7XG4gICAgcmV0dXJuIHRyZWUucmVuZGVyZWQ7XG4gIH1cbiAgY29uc3Qgcm9vdEZpbmRlciA9IGZpbmRFbGVtZW50KHRyZWUsIG5vZGUgPT4gbm9kZS50eXBlID09PSBSb290RmluZGVyKTtcbiAgaWYgKCFyb290RmluZGVyKSB7XG4gICAgdGhyb3cgbmV3IEVycm9yKCdgd3JhcHBpbmdDb21wb25lbnRgIG11c3QgcmVuZGVyIGl0cyBjaGlsZHJlbiEnKTtcbiAgfVxuICByZXR1cm4gcm9vdEZpbmRlci5yZW5kZXJlZDtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIHdyYXBXaXRoV3JhcHBpbmdDb21wb25lbnQoY3JlYXRlRWxlbWVudCwgbm9kZSwgb3B0aW9ucykge1xuICBjb25zdCB7IHdyYXBwaW5nQ29tcG9uZW50LCB3cmFwcGluZ0NvbXBvbmVudFByb3BzIH0gPSBvcHRpb25zO1xuICBpZiAoIXdyYXBwaW5nQ29tcG9uZW50KSB7XG4gICAgcmV0dXJuIG5vZGU7XG4gIH1cbiAgcmV0dXJuIGNyZWF0ZUVsZW1lbnQoXG4gICAgd3JhcHBpbmdDb21wb25lbnQsXG4gICAgd3JhcHBpbmdDb21wb25lbnRQcm9wcyxcbiAgICBjcmVhdGVFbGVtZW50KFJvb3RGaW5kZXIsIG51bGwsIG5vZGUpLFxuICApO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gZ2V0V3JhcHBpbmdDb21wb25lbnRNb3VudFJlbmRlcmVyKHsgdG9UcmVlLCBnZXRNb3VudFdyYXBwZXJJbnN0YW5jZSB9KSB7XG4gIHJldHVybiB7XG4gICAgZ2V0Tm9kZSgpIHtcbiAgICAgIGNvbnN0IGluc3RhbmNlID0gZ2V0TW91bnRXcmFwcGVySW5zdGFuY2UoKTtcbiAgICAgIHJldHVybiBpbnN0YW5jZSA/IHRvVHJlZShpbnN0YW5jZSkucmVuZGVyZWQgOiBudWxsO1xuICAgIH0sXG4gICAgcmVuZGVyKGVsLCBjb250ZXh0LCBjYWxsYmFjaykge1xuICAgICAgY29uc3QgaW5zdGFuY2UgPSBnZXRNb3VudFdyYXBwZXJJbnN0YW5jZSgpO1xuICAgICAgaWYgKCFpbnN0YW5jZSkge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1RoZSB3cmFwcGluZyBjb21wb25lbnQgbWF5IG5vdCBiZSB1cGRhdGVkIGlmIHRoZSByb290IGlzIHVubW91bnRlZC4nKTtcbiAgICAgIH1cbiAgICAgIHJldHVybiBpbnN0YW5jZS5zZXRXcmFwcGluZ0NvbXBvbmVudFByb3BzKGVsLnByb3BzLCBjYWxsYmFjayk7XG4gICAgfSxcbiAgfTtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIGZha2VEeW5hbWljSW1wb3J0KG1vZHVsZVRvSW1wb3J0KSB7XG4gIHJldHVybiBQcm9taXNlLnJlc29sdmUoeyBkZWZhdWx0OiBtb2R1bGVUb0ltcG9ydCB9KTtcbn1cbiJdfQ==
//# sourceMappingURL=Utils.js.map
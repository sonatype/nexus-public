# circular-json-es6

A replacement for `JSON.stringify` and `JSON.parse` that can handle circular references (persists reference structure).

**This implementation requires environments with native ES6 Map support,** but is decently faster than [circular-json](https://github.com/WebReflection/circular-json) (see benchmark with `npm run bench`).

``` js
var CircularJSON = require('circular-json-es6')

var obj = {}
obj.a = obj

var clone = CircularJSON.parse(CircularJSON.stringify(obj))

clone.a === clone // -> true
```

### NOTE

The default `stringify` method optimizes for cases where no circular reference is present by trying a plain `JSON.stringify` first. This means if no circular references are found in the data then it will not persist multiple (but non-circular) references to the same object.

If you want to enforce reference persistence, use `CircularJSON.stringifyStrict` instead.

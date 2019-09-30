## Deep comparison with object identity checks  [![Build Status](https://travis-ci.org/fkling/deep-equal-ident.svg?branch=master)](https://travis-ci.org/fkling/deep-equal-ident)


This function performs a deep comparison between the two values `a` and `b`. It
has the same signature and functionality as [lodash's isEqual function](http://lodash.com/docs#isEqual),
with one difference: It also tracks the identity of nested objects.

This function is intended to be used for unit tests (see below how to use it
with chai.js).

### Installation

    npm install -S deep-equal-ident

and use it as

```javascript
var deepEqualIdent = require('deep-equal-ident');
// ...
if (deepEqualIdent(foo, bar)) {
 // deep equal
}
```

### Use with chai.js

This module provides integration with the [chai.js assertion framework](http://chaijs.com/)
(for Node.js at least).
Enable the extensions with

    chai.use(require('deep-equal-ident/chai'));

Then you can use either the `expect` or `assert` interface:

```javascript
// expect
expect(foo).to.deep.identically.equal(bar);
expect(foo).to.identically.equal(bar);

// assert
assert.deepEqualIdent(foo, bar);
assert.NotDeepEqualIdent(foo, bar);
```

---

### So, what is this really about?

Most deep equality tests (including `_.isEqual`) consider the following
structures as equal:

```javascript
var a = [1,2,3];
var b = [1,2,3];
var foo = [a, a];
var bar = [a, b];
_.isEqual(foo , bar): // => true
```

Here, `foo` contains two reference to the same object, but `bar` contains
references to two different (not identical) objects. `a` and `b` might be itself
considered as equal (they do after all contain the same values), but the
*structures* of `foo` and `bar` are different.

`deepEqualIdent` will consider these values as not equal:

```javascript
deepEqualIdent(foo, bar); // => false
```

The following slightly different structures would be considered equal:

```javascript
var a = [1,2,3];
var b = [1,2,3];
var foo = [a, a];
var bar = [b, b];
deepEqualIdent(foo, bar); // => true
```

### Why does it matter?

Let's have a look at another procedure to answer that question: *deep cloning*.
Given

```javascript
var a = [1,2,3];
var foo = [a, a];
```

a *good* deep cloning algorithm would recognize that both elements in `foo`
refer to the same object and thus would create a single copy of `a`:

```javascript
var a_copy = [1,2,3];
var foo_copy = [a_copy, a_copy];
```

This is desired because we want `foo_copy` behave *exactly* like `foo` when we
process it. I.e. if the first element is mutated, the second element should
mutate as well:

```javascript
foo_copy[0][0] += 1;
console.log(foo_copy); // => [[2,2,3], [2,2,3]]
```

If the deep copy algorithm would produce separate copies for each element in `foo`
instead

```javascript
var a_copy_1 = [1,2,3];
var a_copy_2 = [1,2,3];
var foo_copy = [a_copy_1, a_copy_2];
```

then mutating the first element of `foo_copy` would not produce the same result
as mutation the first element of `foo`, and thus it would not be an exact copy
of `foo`.

---

I hope this makes it clearer why considering the identity of objects during
comparison is important: To preserve the structural integrity. If two nested
structures are said to be equal, they should *behave* exactly the same for all
intends and purposes.

Another way to look at it is to visualize the relationship between the values as
graphs. Let's change the structure a bit:

```javascript
var a = [1,2,3];
var b = [1,2,3];
var foo = [a, {x: a}];
var bar = [a, {x: b}];
```

Graph representations:

```
   - foo -        - bar -
  |       |      |       |
  v       v      v       v
  a <--- { }     a      { }
                         |
                         v
                         b
```

I think this makes it very obvious that the structure of `foo` and `bar` are
different and thus would produce different results when processed.

### OK, so how did you implement it?

It's really straightforward. Just like with deep cloning, we have to keep
track of which objects we already encountered in `a` and associate it with the
corresponding value in `b`. Interestingly, deep cloning methods that can handle
cycles are already doing this, but only vertically, not horizontally. It shouldn't
be too much effort to modify them to support this out of the box.

There are a couple of ways to do it, each with its advantages and disadvantages.
I implemented two of them and choose to build them on top of lodash's `isEqual`
function, since it allows me to pass a callback and utilize all of the other
comparison logic that `isEqual` provides.

#### Tags

One way is to "tag" objects we have already seen and associate them with the
corresponding other object (creating some kind of bijective relationship). For
this I just added a new, not enumerable property to the object and setting the other
object as value, e.g.

```javascript
Object.defineProperty(a, '__<random prop>__', {value: b});
Object.defineProperty(b, '__<random prop>__', {value: a});
```

Now whenever we encounter an object (`a1`) that already has the property, we check
whether it has a reference to `b1`. We also have to check the other direction,
i.e. if `b1` refers to `a1`. Overall this allows for the following outcomes:

- `a1` and `b1` not tagged: Not seen before => tag
- Either `a1` or `b1` not tagged: not equal
- `a1` tagged but does not refer to `b1`: not equal
- `b1` tagged but does not refer to `a1`: not equal

It's important to note that we can't detect equality. While the overall structure
might be the same, e.g. we have `[a, a]` and `[b, b]`, `a` and `b` might still be
different. So we have to let the actual comparison algorithm determine equality
of these two values.

#### Stack

The previous solution has the advantage that determining the "not equality" is
quick, but it doesn't work for *immutable* objects. As alternative, we can push
each of the objects onto a stack and whenever we encounter another object, we
iterate over the stack and check whether it is already contained in the stack.
The result is the same as with tags.

The disadvantage is that performance decreases the more objects have to be
compared.

#### Maps

The solution to the immutability and performance problems could be ES6 `Map`s,
assuming they are supported they are supported by the environment this code
runs in.

An implementation using Maps is included and is used if `global.Map` is
available.

### Caveats

`deep-equal-ident` **incorrectly** assumes the following structures to be
equal:

```javascript
var a = [[]];
var foo = [a, a[0]];
var bar = [a, []];
deepEqualIdent(foo, bar); // true
```

That's because lodash doesn't traverse deeper into the first element (because
`foo[0] === bar[0]`, so the algorithm doesn't know about the objects inside
`foo[0]` (and `bar[0]`) and therefore cannot detect whether they repeat
elsewhere in the data structure.

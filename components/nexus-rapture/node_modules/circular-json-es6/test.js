var c = require('./index')
var assert = require('assert')

describe('circular-json', function () {

  var o = {}
  o.a = o
  o.c = {}
  o.d = {
    a: 123,
    b: o
  }
  o.c.e = o
  o.c.f = o.d
  o.b = o.c
  o.arr = [o.a, o.c, o.d]
  o.d.arr = o.arr

  o = c.parse(c.stringify(o))

  it('raw value', function () {
    assert.ok(o.d.a === 123)
  })

  it('self reference', function () {
    assert.ok(o.a === o)
  })

  it('nested self reference', function () {
    assert.ok(o.c.e === o)
    assert.ok(o.d.b === o)
  })

  it('nested cross reference', function () {
    assert.ok(o.c.f === o.d)
    assert.ok(o.b = o.c)
    assert.ok(o.arr === o.d.arr)
  })

  it('array reference', function () {
    assert.ok(o.arr[0] === o.a)
    assert.ok(o.arr[1] === o.c)
    assert.ok(o.arr[2] === o.d)
  })
})

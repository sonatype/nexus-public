'use strict';

var deepEqualIdent = require('./deepEqualIdentStack');

/**
 * Extending chai.js assertions to check for identically deep equality.
 *
 * Add: `chai.use(require('deepEqualIdent/chai'));`
 *
 * Use:
 *
 *   expect(foo).to.deep.identically.equal(bar);
 *   assert.deepEqualNotIdent(foo, bar);
 *
 */
function chai(_chai, utils) {
  var assert = _chai.assert;
  var Assertion = _chai.Assertion;

  // expect interface
  Assertion.addProperty('identically', function() {
    utils.flag(this, 'deepEqualIdent.identity', true);
  });

  function assertDeepEqualIdent(_super) {
    return function(exp, msg) {
      /*jshint validthis:true*/
      if (msg) {
        utils.flag(this, 'message', msg);
      }
      if (utils.flag(this, 'deepEqualIdent.identity')) {
        this.assert(
            deepEqualIdent(this._obj, exp),
            'expected #{this} to be identically deep equal to #{exp}',
            'expected #[this} not to be identically deep equal to #{exp}',
            exp,
            this._obj,
            true
            );
      }
      else {
        return _super.apply(this, arguments);
      }
    };
  }

  Assertion.overwriteMethod('eql', assertDeepEqualIdent);
  Assertion.overwriteMethod('eqls', assertDeepEqualIdent);

  // assert interface
  assert.deepEqualIdent = function(act, exp, msg) {
    new Assertion(act, msg).to.deep.identically.eql(exp);
  };

  assert.notDeepEqualIdent = function(act, exp, msg) {
    new Assertion(act, msg).to.not.deep.identically.eql(exp);
  };
}

module.exports = chai;

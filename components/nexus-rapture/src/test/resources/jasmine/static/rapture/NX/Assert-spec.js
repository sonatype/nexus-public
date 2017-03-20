describe('Assert', function() {
  beforeAll(function(done) {
    Ext.onReady(done);
  });

  describe('assert', function() {
    beforeEach(function() {
      spyOn(NX.Console, 'error');
    });

    it('does nothing when disabled', function() {
      NX.Assert.disable = true;
      NX.Assert.assert(1 !== 1, '1 is not 1!?');
      NX.Assert.disable = false;
      expect(NX.Console.error).not.toHaveBeenCalled();
    });

    it('logs an error when the expression is false', function() {
      NX.Assert.assert(1 !== 1, '1 is not 1!?');
      expect(NX.Console.error).toHaveBeenCalledWith('Assertion failure:', '1 is not 1!?');
    });
  });
});

describe('NX.controller.ExtDirect', function() {
  var controller;

  beforeAll(function(done) {
    Ext.onReady(function(){
      controller = Ext.create('NX.controller.ExtDirect');
      spyOn(NX.Messages, 'error');
      done();
    });
  });

  describe('checkResponse', function() {
    it('handles failure result', function() {
      controller.checkResponse({},{result: {success: false, message: 'you dun screwed up there fella'}});
      expect(NX.Messages.error).toHaveBeenCalledWith('you dun screwed up there fella');
    });
    it('handles failure result with server exception', function() {
      controller.checkResponse({serverException: {exception: {message: 'you dun screwed up there fella'}}},{result: {success: true}});
      expect(NX.Messages.error).toHaveBeenCalledWith('you dun screwed up there fella');
    });
    it('handles null result', function() {
      controller.checkResponse({},{result: undefined});
      expect(NX.Messages.error).toHaveBeenCalledWith('Operation failed as server could not be contacted');
    });
  });
});

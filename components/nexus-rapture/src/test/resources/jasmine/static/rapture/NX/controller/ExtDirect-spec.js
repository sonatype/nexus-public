describe('NX.controller.ExtDirect', function() {
  var controller;

  beforeAll(function(done) {
    Ext.onReady(function(){
      controller = Ext.create('NX.controller.ExtDirect');
      spyOn(NX.Messages, 'add');
      done();
    });
  });

  describe('checkResponse', function() {
    it('handles failure result', function() {
      controller.checkResponse({},{result: {success: false, message: 'you dun screwed up there fella'}});
      expect(NX.Messages.add).toHaveBeenCalledWith({text: 'you dun screwed up there fella', type: 'danger'});
    });
    it('handles failure result with server exception', function() {
      controller.checkResponse({serverException: {exception: {message: 'you dun screwed up there fella'}}},{result: {success: true}});
      expect(NX.Messages.add).toHaveBeenCalledWith({text: 'you dun screwed up there fella', type: 'danger'});
    });
    it('handles null result', function() {
      controller.checkResponse({},{result: undefined});
      expect(NX.Messages.add).toHaveBeenCalledWith({text: 'Operation failed as server could not be contacted', type: 'danger'});
    });
  });
});

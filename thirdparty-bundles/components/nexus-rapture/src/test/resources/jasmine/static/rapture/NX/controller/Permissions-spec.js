describe('NX.controller.Permissions', function() {
  var data = [
    {
      id: 'permission1',
      permitted: 'false'
    }
  ];

  beforeAll(function(done) {
    Ext.onReady(function() {
      NX.store.Permission.addMembers({
        proxy: {
          type: 'memory',
          data: data,
          reader: {
            type: 'json'
          }
       }
      });

      done();
    });
  });

  describe('fetchPermissions', function() {
    it('loads data from the memory proxy', function() {
      var permissions = Ext.create('NX.controller.Permissions');
      permissions.fetchPermissions();
      expect(permissions.getStore('Permission').getById('permission1').get('permitted')).toBe(false);
    });
  });
});

describe('NX.controller.Permissions', function() {
  var data = [
    {
      id: 'permission1',
      permitted: 'false'
    },
    {
      id: 'permission2',
      permitted: 'true'
    }
  ];

  beforeAll(function(done) {
    Ext.define('NX.store.PermissionWithMemoryProxy', {
      override : 'NX.store.Permission',
      getProxy: function(){
        return Ext.Factory.proxy({
          type: 'memory',
          model: 'NX.model.Permission',
          data: data,
          reader: {
            type: 'json'
          }
        });
      }
    });

    Ext.onReady(function() {
      done();
    });
  });

  describe('fetchPermissions', function() {
    it('loads data from the memory proxy', function() {
      var permissions = Ext.create('NX.controller.Permissions');
      permissions.fetchPermissions();
      expect(permissions.getStore('Permission').getById('permission1').get('permitted')).toBe(false);
      expect(permissions.getStore('Permission').getById('permission2').get('permitted')).toBe(true);
    });
  });
});

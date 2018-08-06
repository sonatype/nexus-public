Ext.ns('NX');

NX.global = (function() {
  if (window !== undefined) {
    return window;
  }
  if (global !== undefined) {
    return global;
  }
  Ext.Error.raise('Unable to determine global object');
}());

Ext.Loader.setConfig({
  enabled: true,
  paths: {
    NX: './src/NX'
  }
});

(function() {
  var method = Ext.Loader.getPath;
  Ext.Loader.getPath = function() {
    var path = method.apply(this, arguments);
    if (path.indexOf('static/rapture/NX') === 0) {
      return path.replace('static/rapture/NX', './src/NX');
    }
    else {
      return path;
    }
  };
})();

// https://www.sencha.com/forum/showthread.php?291229-Ext-data-schema-Schema-addEntity()-Duplicate-entity-name
Ext.define('ExtEnding.data.schema.Schema', {
  override : 'Ext.data.schema.Schema',
  privates : {
    addEntity: function (entityType) {
      var me = this,
          entities = me.entities,
          entityName = entityType.entityName,
          entry = entities[entityName],
          fields = entityType.fields,
          associations, field, i, length, name;

      if (!entry) {
        entities[entityName] = entry = {
          name: entityName,
          associations: {}
        };
      } else {
        associations = entry.associations;
        for (name in associations) {
          // the associations collection describes the types to which this entity is
          // related, but the inverse descriptors need this entityType:
          associations[name].inverse.cls = entityType;

          me.associationEntityMap[entityName] = true;

          // We already have an entry, which means other associations have likely been added
          // for us, so go ahead and do the inverse decoration
          me.decorateModel(associations[name].association);
        }
      }

      entry.cls = entityType;
      entityType.prototype.associations = entityType.associations = entry.associations;
      me.entityClasses[entityType.$className] = entry;

      ++me.entityCount;

      for (i = 0, length = fields.length; i < length; ++i) {
        field = fields[i];
        if (field.reference) {
          me.addReferenceDescr(entityType, field);
        }
      }
    }
  }
});

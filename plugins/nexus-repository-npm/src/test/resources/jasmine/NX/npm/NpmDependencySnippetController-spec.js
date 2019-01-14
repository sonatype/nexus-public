describe('NpmDependencySnippetController#snippetGenerator', function() {
  var NPM = 0, YARN = 1, PACKAGE = 2;
  var NpmDependencySnippetController = TestClasses['NX.npm.controller.NpmDependencySnippetController'];
  it('uses the group for scoped dependencies', function() {
    var componentModel = {
      get: function(field) {
        if (field === 'group') {
          return 'sonatype';
        }

        if (field === 'name') {
          return 'nexus';
        }

        if (field === 'version') {
          return '1.0.0';
        }
      }
    };

    var result = NpmDependencySnippetController.snippetGenerator(componentModel);

    expect(result[NPM].snippetText).toEqual(jasmine.stringMatching(/@sonatype\/nexus@1\.0\.0$/));
  });

  it('ignores the group for non-scoped dependencies', function() {
    var componentModel = {
      get: function(field) {
        if (field === 'name') {
          return 'nexus';
        }

        if (field === 'version') {
          return '1.0.0';
        }
      }
    };

    var result = NpmDependencySnippetController.snippetGenerator(componentModel);

    expect(result[YARN].snippetText).toEqual(jasmine.stringMatching(/nexus@1\.0\.0$/));
  });

  it('correctly displays the package name and version for package.json', function() {
    var componentModel = {
      get: function(field) {
        if (field === 'group') {
          return 'sonatype';
        }

        if (field === 'name') {
          return 'nexus';
        }

        if (field === 'version') {
          return '1.0.0';
        }
      }
    };

    var result = NpmDependencySnippetController.snippetGenerator(componentModel);

    expect(result[PACKAGE].snippetText).toEqual(jasmine.stringMatching(/"@sonatype\/nexus"/));
    expect(result[PACKAGE].snippetText).toEqual(jasmine.stringMatching(/"1\.0\.0"/));
  })
});

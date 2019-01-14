describe('NX.maven.controller.MavenDependencySnippetController', function() {
  var MavenDependencySnippetController;
  beforeAll(function() {
    MavenDependencySnippetController = TestClasses['NX.maven.controller.MavenDependencySnippetController'];
  });

  describe('snippetGenerator', function() {
    it('returns the correct values for a component', function() {
      var component = new MockComponent({
          group: 'aop',
          name: 'aopalliance',
          version: '3.1'
        }),
        expectedSnippets = [
          {
            displayName: 'Apache Maven',
            description: 'Insert this snippet into your pom.xml',
            snippetText:
                '<dependency>\n' +
                '  <groupId>aop</groupId>\n' +
                '  <artifactId>aopalliance</artifactId>\n' +
                '  <version>3.1</version>\n' +
                '</dependency>'
          }, {
            displayName: 'Gradle Groovy DSL',
            snippetText: 'implementation \'aop:aopalliance:3.1\''
          }, {
            displayName: 'Gradle Kotlin DSL',
            snippetText: 'implementation("aop:aopalliance:3.1")'
          }, {
            displayName: 'Scala SBT',
            snippetText: 'libraryDependencies += "aop" % "aopalliance" % "3.1"'
          }, {
            displayName: 'Apache Ivy',
            snippetText: '<dependency org="aop" name="aopalliance" rev="3.1"></dependency>'
          }, {
            displayName: 'Groovy Grape',
            snippetText:
                '@Grapes(\n' +
                '  @Grab(group=\'aop\', module=\'aopalliance\', version=\'3.1\')\n' +
                ')'
          }, {
            displayName: 'Leiningen',
            snippetText: '[aop/aopalliance "3.1"]'
          }, {
            displayName: 'Apache Buildr',
            snippetText: '\'aop:aopalliance:jar:3.1\''
          }, {
            displayName: 'Maven Central Badge',
            snippetText:
                '[![Maven Central](https://img.shields.io/maven-central/v/aop/aopalliance.svg?label=Maven%20Central)]' +
                '(https://search.maven.org/search?q=g:%22aop%22%20AND%20a:%22aopalliance%22)'
          }, {
            displayName: 'PURL',
            snippetText: 'pkg:maven/aop/aopalliance@3.1'
          }
        ],
        snippets = MavenDependencySnippetController.snippetGenerator(component),
        i;

      expect(snippets.length).toEqual(expectedSnippets.length);

      // comparing elements one by one for more specific error message if test fails
      for (i = 0 ; i < snippets.length ; i++) {
        expect(snippets[i]).toEqual(expectedSnippets[i]);
      }
    });

    it('returns the correct values for a jar asset without a classifier', function() {
      var component = new MockComponent({
            group: 'aop2',
            name: 'alliance',
            version: '5.1'
          }),
          asset = new MockComponent({
            attributes: {
              maven2: {
                extension: 'jar'
              }
            }
          }),
          expectedSnippets = [
            {
              displayName: 'Apache Maven',
              description: 'Insert this snippet into your pom.xml',
              snippetText:
                  '<dependency>\n' +
                  '  <groupId>aop2</groupId>\n' +
                  '  <artifactId>alliance</artifactId>\n' +
                  '  <version>5.1</version>\n' +
                  '</dependency>'
            }, {
              displayName: 'Gradle Groovy DSL',
              snippetText: 'implementation \'aop2:alliance:5.1@jar\''
            }, {
              displayName: 'Gradle Kotlin DSL',
              snippetText: 'implementation("aop2:alliance:5.1@jar")'
            }, {
              displayName: 'Scala SBT',
              snippetText: 'libraryDependencies += "aop2" % "alliance" % "5.1"'
            }, {
              displayName: 'Apache Ivy',
              snippetText:
                  '<dependency org="aop2" name="alliance" rev="5.1">\n' +
                  '  <artifact name="alliance" ext="jar" />\n' +
                  '</dependency>'
            }, {
              displayName: 'Groovy Grape',
              snippetText:
                  '@Grapes(\n' +
                  '  @Grab(group=\'aop2\', module=\'alliance\', version=\'5.1\')\n' +
                  ')'
            }, {
              displayName: 'Leiningen',
              snippetText: '[aop2/alliance "5.1" :extension "jar"]'
            }, {
              displayName: 'Apache Buildr',
              snippetText: '\'aop2:alliance:jar:5.1\''
            }, {
              displayName: 'Maven Central Badge',
              snippetText:
                  '[![Maven Central](https://img.shields.io/maven-central/v/aop2/alliance.svg?label=Maven%20Central)]' +
                  '(https://search.maven.org/search?q=g:%22aop2%22%20AND%20a:%22alliance%22)'
            }, {
              displayName: 'PURL',
              snippetText: 'pkg:maven/aop2/alliance@5.1?extension=jar'
            }
          ],
          snippets = MavenDependencySnippetController.snippetGenerator(component, asset),
          i;

      expect(snippets.length).toEqual(expectedSnippets.length);

      // comparing elements one by one for more specific error message if test fails
      for (i = 0 ; i < snippets.length ; i++) {
        expect(snippets[i]).toEqual(expectedSnippets[i]);
      }
    });

    it('returns the correct values for a pom asset with a classifier', function() {
      var component = new MockComponent({
            group: 'aop3',
            name: 'axis',
            version: '8.0.8'
          }),
          asset = new MockComponent({
            attributes: {
              maven2: {
                classifier: 'dev',
                extension: 'pom'
              }
            }
          }),
          expectedSnippets = [
            {
              displayName: 'Apache Maven',
              description: 'Insert this snippet into your pom.xml',
              snippetText:
                  '<dependency>\n' +
                  '  <groupId>aop3</groupId>\n' +
                  '  <artifactId>axis</artifactId>\n' +
                  '  <version>8.0.8</version>\n' +
                  '  <classifier>dev</classifier>\n' +
                  '  <type>pom</type>\n' +
                  '</dependency>'
            }, {
              displayName: 'Gradle Groovy DSL',
              snippetText: 'implementation \'aop3:axis:8.0.8:dev@pom\''
            }, {
              displayName: 'Gradle Kotlin DSL',
              snippetText: 'implementation("aop3:axis:8.0.8:dev@pom")'
            }, {
              displayName: 'Scala SBT',
              snippetText: 'libraryDependencies += "aop3" % "axis" % "8.0.8" classifier "dev"'
            }, {
              displayName: 'Apache Ivy',
              snippetText:
                  '<dependency org="aop3" name="axis" rev="8.0.8">\n' +
                  '  <artifact name="axis" ext="pom" m:classifier="dev" />\n' +
                  '</dependency>'
            }, {
              displayName: 'Groovy Grape',
              snippetText:
                  '@Grapes(\n' +
                  '  @Grab(group=\'aop3\', module=\'axis\', version=\'8.0.8\', classifier=\'dev\')\n' +
                  ')'
            }, {
              displayName: 'Leiningen',
              snippetText: '[aop3/axis "8.0.8" :classifier "dev" :extension "pom"]'
            }, {
              displayName: 'Apache Buildr',
              snippetText: '\'aop3:axis:pom:dev:8.0.8\''
            }, {
              displayName: 'Maven Central Badge',
              snippetText:
                  '[![Maven Central](https://img.shields.io/maven-central/v/aop3/axis.svg?label=Maven%20Central)]' +
                  '(https://search.maven.org/search?q=g:%22aop3%22%20AND%20a:%22axis%22%20AND%20l:%22dev%22)'
            }, {
              displayName: 'PURL',
              snippetText: 'pkg:maven/aop3/axis@8.0.8?classifier=dev&extension=pom'
            }
          ],
          snippets = MavenDependencySnippetController.snippetGenerator(component, asset),
          i;

      expect(snippets.length).toEqual(expectedSnippets.length);

      // comparing elements one by one for more specific error message if test fails
      for (i = 0 ; i < snippets.length ; i++) {
        expect(snippets[i]).toEqual(expectedSnippets[i]);
      }
    });
  });
});

/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * @since 3.15
 */
Ext.define('NX.maven.controller.MavenDependencySnippetController', {
  extend: 'NX.app.Controller',

  /**
   * @override
   */
  init: function() {
    NX.getApplication().getDependencySnippetController()
        .addDependencySnippetGenerator('maven2', this.snippetGenerator);
  },

  snippetGenerator: function(componentModel, assetModel) {
    var group = componentModel.get('group'),
        name = componentModel.get('name'),
        version = componentModel.get('version'),
        attributes = assetModel && assetModel.get('attributes'),
        maven2 = attributes && attributes.maven2,
        classifier = maven2 && maven2.classifier,
        extension = maven2 && maven2.extension,
        gradleCoordinates;

    gradleCoordinates = group + ':' + name + ':' + version +
        (classifier ? ':' + classifier : '') +
        (extension ? '@' + extension : '');

    return [
      {
        displayName: 'Apache Maven',
        description: 'Insert this snippet into your pom.xml',
        snippetText:
            '<dependency>\n' +
              '  <groupId>' + group + '</groupId>\n' +
              '  <artifactId>' + name + '</artifactId>\n' +
              '  <version>' + version + '</version>\n' +
              (classifier ? '  <classifier>' + classifier + '</classifier>\n' : '') +
              ((extension && 'jar' !== extension) ? '  <type>' + extension + '</type>\n' : '') +
            '</dependency>'
      }, {
        displayName: 'Gradle Groovy DSL',
        snippetText:
            'implementation \'' + gradleCoordinates + '\''
      }, {
        displayName: 'Gradle Kotlin DSL',
        snippetText:
            'implementation("' + gradleCoordinates + '")'
      }, {
        displayName: 'Scala SBT',
        snippetText:
            'libraryDependencies += "' + group + '" % "' + name + '" % "' + version + '"' +
            (classifier ? ' classifier "' + classifier + '"' : '')
      }, {
        displayName: 'Apache Ivy',
        snippetText:
            '<dependency org="' + group + '" name="' + name + '" rev="' + version +'">' +
            (
                (classifier || extension) ?
                    '\n  <artifact name="' + name + '"' +
                    (extension ? ' ext="' + extension + '"' : '') +
                    (classifier ? ' m:classifier="' + classifier + '"' : '') +
                    ' />\n'
                    :
                    ''
            ) +
            '</dependency>'
      }, {
        displayName: 'Groovy Grape',
        snippetText:
            '@Grapes(\n' +
            '  @Grab(group=\'' + group + '\', module=\'' + name + '\', version=\'' + version + '\'' +
            (classifier ? ', classifier=\'' + classifier + '\'' : '' ) + ')\n' +
            ')'
      }, {
        displayName: 'Leiningen',
        snippetText:
            '[' + group + '/' + name + ' "' + version + '"' +
            (classifier ? ' :classifier "' + classifier + '"' : '') +
            (extension ? ' :extension "' + extension + '"' : '') +
            ']'
      }, {
        displayName: 'Apache Buildr',
        snippetText:
            '\'' + group + ':' + name +
            (extension ? ':' + extension : ':jar') +
            (classifier ? ':' + classifier : '') +
            ':' + version + '\''
      }, {
        displayName: 'Maven Central Badge',
        snippetText:
            '[![Maven Central](https://img.shields.io/maven-central/v/' + group + '/' + name + '.svg?label=Maven%20Central)]' +
            '(https://search.maven.org/search?q=g:%22' + group + '%22%20AND%20a:%22' + name + '%22' +
              (classifier ? '%20AND%20l:%22' + classifier + '%22' : '') +
            ')'
      }, {
        displayName: 'PURL',
        snippetText:
            'pkg:maven/' + group + '/' + name + '@' + version +
            ( (classifier || extension) ?
                '?' + (classifier ? 'classifier=' + classifier + (extension ? '&' :'') : '') +
                (extension ? 'extension=' + extension : '')
                :
                ''
            )
      }
    ];
  }
});

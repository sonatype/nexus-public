/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import { generate } from '../maven2';
import type { SnippetComponentModel } from '../types';

const component: SnippetComponentModel = {
  format: 'maven2',
  group: 'org.apache.commons',
  name: 'commons-lang3',
  version: '3.14.0',
};

function byName(component: SnippetComponentModel, asset?: Parameters<typeof generate>[1]) {
  const map: Record<string, string> = {};
  for (const s of generate(component, asset)) {
    map[s.displayName] = s.snippetText;
  }
  return map;
}

describe('maven2 dependency snippet generator', () => {
  it('emits the full Classic tool set in order', () => {
    const names = generate(component).map((s) => s.displayName);
    expect(names).toEqual([
      'Apache Maven',
      'Gradle Groovy DSL',
      'Gradle Kotlin DSL',
      'Scala SBT',
      'Apache Ivy',
      'Groovy Grape',
      'Leiningen',
      'Apache Buildr',
      'Maven Central Badge',
      'PURL',
    ]);
  });

  it('produces component-level snippet text matching Classic (no classifier/extension)', () => {
    const s = byName(component);
    expect(s['Apache Maven']).toBe(
      '<dependency>\n' +
        '  <groupId>org.apache.commons</groupId>\n' +
        '  <artifactId>commons-lang3</artifactId>\n' +
        '  <version>3.14.0</version>\n' +
        '</dependency>',
    );
    expect(s['Gradle Groovy DSL']).toBe("implementation 'org.apache.commons:commons-lang3:3.14.0'");
    expect(s['Gradle Kotlin DSL']).toBe('implementation("org.apache.commons:commons-lang3:3.14.0")');
    expect(s['Scala SBT']).toBe('libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.14.0"');
    expect(s['Apache Ivy']).toBe(
      '<dependency org="org.apache.commons" name="commons-lang3" rev="3.14.0"></dependency>',
    );
    expect(s['Groovy Grape']).toBe(
      '@Grapes(\n' +
        "  @Grab(group='org.apache.commons', module='commons-lang3', version='3.14.0')\n" +
        ')',
    );
    expect(s['Leiningen']).toBe('[org.apache.commons/commons-lang3 "3.14.0"]');
    expect(s['Apache Buildr']).toBe("'org.apache.commons:commons-lang3:jar:3.14.0'");
    expect(s['Maven Central Badge']).toBe(
      '[![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-lang3.svg?label=Maven%20Central)]' +
        '(https://search.maven.org/search?q=g:%22org.apache.commons%22%20AND%20a:%22commons-lang3%22)',
    );
    expect(s['PURL']).toBe('pkg:maven/org.apache.commons/commons-lang3@3.14.0');
  });

  it('includes classifier and non-jar extension from asset maven2 attributes', () => {
    const asset = { attributes: { maven2: { classifier: 'sources', extension: 'zip' } } };
    const s = byName(component, asset);
    expect(s['Apache Maven']).toBe(
      '<dependency>\n' +
        '  <groupId>org.apache.commons</groupId>\n' +
        '  <artifactId>commons-lang3</artifactId>\n' +
        '  <version>3.14.0</version>\n' +
        '  <classifier>sources</classifier>\n' +
        '  <type>zip</type>\n' +
        '</dependency>',
    );
    expect(s['Gradle Groovy DSL']).toBe(
      "implementation 'org.apache.commons:commons-lang3:3.14.0:sources@zip'",
    );
    expect(s['Apache Ivy']).toBe(
      '<dependency org="org.apache.commons" name="commons-lang3" rev="3.14.0">\n' +
        '  <artifact name="commons-lang3" ext="zip" m:classifier="sources" />\n' +
        '</dependency>',
    );
    expect(s['Apache Buildr']).toBe("'org.apache.commons:commons-lang3:zip:sources:3.14.0'");
    expect(s['PURL']).toBe('pkg:maven/org.apache.commons/commons-lang3@3.14.0?classifier=sources&extension=zip');
  });

  it('omits the <type> element when the extension is jar', () => {
    const asset = { attributes: { maven2: { extension: 'jar' } } };
    const s = byName(component, asset);
    expect(s['Apache Maven']).toBe(
      '<dependency>\n' +
        '  <groupId>org.apache.commons</groupId>\n' +
        '  <artifactId>commons-lang3</artifactId>\n' +
        '  <version>3.14.0</version>\n' +
        '</dependency>',
    );
  });

  it('XML-encodes special characters in component coordinates', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example&co',
      name: 'lib<test>',
      version: '1.0"beta',
    };
    const s = byName(special);
    expect(s['Apache Maven']).toBe(
      '<dependency>\n' +
        '  <groupId>com.example&amp;co</groupId>\n' +
        '  <artifactId>lib&lt;test&gt;</artifactId>\n' +
        '  <version>1.0&quot;beta</version>\n' +
        '</dependency>',
    );
    expect(s['Apache Ivy']).toBe(
      '<dependency org="com.example&amp;co" name="lib&lt;test&gt;" rev="1.0&quot;beta"></dependency>',
    );
  });

  it('leaves the Scala SBT coordinates raw (build.sbt is Scala, not XML)', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example&co',
      name: 'lib<test>',
      version: '1.0"beta',
    };
    const s = byName(special);
    // SBT build files are Scala source, not XML — the coordinates must stay raw (matching
    // Classic) rather than carry the &amp;/&lt;/&quot; entities used in the pom.xml/Ivy snippets.
    expect(s['Scala SBT']).toBe('libraryDependencies += "com.example&co" % "lib<test>" % "1.0"beta"');
  });

  it('leaves the Groovy Grape, Leiningen and Apache Buildr coordinates raw (non-XML source)', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example&co',
      name: 'lib<test>',
      version: '1.0"beta',
    };
    const s = byName(special);
    // Grape is Groovy, Leiningen is Clojure/EDN, Buildr is Ruby — none are XML, so the
    // coordinates must stay raw rather than carry &amp;/&lt;/&quot; entities.
    expect(s['Groovy Grape']).toBe(
      '@Grapes(\n' +
        "  @Grab(group='com.example&co', module='lib<test>', version='1.0\"beta')\n" +
        ')',
    );
    expect(s['Leiningen']).toBe('[com.example&co/lib<test> "1.0"beta"]');
    expect(s['Apache Buildr']).toBe("'com.example&co:lib<test>:jar:1.0\"beta'");
  });

  it('percent-encodes the Maven Central Badge coordinates (URL/Markdown context)', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example&co',
      name: 'lib<test>',
      version: '1.0',
    };
    const s = byName(special);
    // The badge is a shields.io/search URL, so coordinates need percent-encoding — not XML
    // entities and not raw characters that would break the query string.
    expect(s['Maven Central Badge']).toBe(
      '[![Maven Central](https://img.shields.io/maven-central/v/com.example%26co/lib%3Ctest%3E.svg?label=Maven%20Central)]' +
        '(https://search.maven.org/search?q=g:%22com.example%26co%22%20AND%20a:%22lib%3Ctest%3E%22)',
    );
  });

  it('leaves the PURL coordinates raw (no XML entity encoding)', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example&co',
      name: 'lib<test>',
      version: '1.0"beta',
    };
    const s = byName(special);
    // The PURL is not XML content, so it must carry the raw coordinates — matching Classic —
    // rather than the &amp;/&lt;/&quot; entities used inside the pom.xml/Ivy snippets.
    expect(s['PURL']).toBe('pkg:maven/com.example&co/lib<test>@1.0"beta');
  });

  it('leaves classifier and extension raw in the PURL query string', () => {
    const asset = { attributes: { maven2: { classifier: 'sources&test', extension: "zip'alpha" } } };
    const s = byName(component, asset);
    expect(s['PURL']).toBe(
      "pkg:maven/org.apache.commons/commons-lang3@3.14.0?classifier=sources&test&extension=zip'alpha",
    );
  });

  it('XML-encodes classifier and extension with special characters', () => {
    const special: SnippetComponentModel = {
      format: 'maven2',
      group: 'com.example',
      name: 'lib',
      version: '1.0',
    };
    const asset = { attributes: { maven2: { classifier: 'sources&test', extension: 'zip\'alpha' } } };
    const s = byName(special, asset);
    expect(s['Apache Maven']).toBe(
      '<dependency>\n' +
        '  <groupId>com.example</groupId>\n' +
        '  <artifactId>lib</artifactId>\n' +
        '  <version>1.0</version>\n' +
        '  <classifier>sources&amp;test</classifier>\n' +
        '  <type>zip&apos;alpha</type>\n' +
        '</dependency>',
    );
  });
});

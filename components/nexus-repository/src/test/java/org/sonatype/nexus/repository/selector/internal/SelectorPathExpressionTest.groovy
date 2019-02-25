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
package org.sonatype.nexus.repository.selector.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.repository.security.VariableResolverAdapter
import org.sonatype.nexus.repository.security.internal.SimpleVariableResolverAdapter
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetEntityAdapter
import org.sonatype.nexus.repository.storage.Bucket
import org.sonatype.nexus.repository.storage.BucketEntityAdapter
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter
import org.sonatype.nexus.repository.storage.ComponentFactory
import org.sonatype.nexus.repository.storage.DefaultComponent
import org.sonatype.nexus.selector.JexlSelector
import org.sonatype.nexus.selector.SelectorFactory
import org.sonatype.nexus.selector.SelectorSqlBuilder
import org.sonatype.nexus.validation.ConstraintViolationFactory

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static java.util.Collections.emptySet
import static org.hamcrest.CoreMatchers.is
import static org.junit.Assert.assertThat
import static org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule.inFilesystem
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES

class SelectorPathExpressionTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = 'test-repo'

  private static final String FORMAT_NAME = 'test-format'

  @Rule
  public DatabaseInstanceRule database = inFilesystem('test')

  @Mock
  private ConstraintViolationFactory violationFactory

  private SelectorFactory selectorFactory

  private VariableResolverAdapter variableResolverAdapter = new SimpleVariableResolverAdapter()

  private BucketEntityAdapter bucketEntityAdapter

  private ComponentEntityAdapter componentEntityAdapter

  private AssetEntityAdapter assetEntityAdapter

  private Bucket bucket

  @Before
  void setUp() throws Exception {
    selectorFactory = new SelectorFactory(violationFactory)

    ComponentFactory componentFactory = new ComponentFactory(emptySet())

    bucketEntityAdapter = new BucketEntityAdapter()
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet())
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter)

    database.getInstance().connect().withCloseable { db ->
      bucketEntityAdapter.register(db)
      componentEntityAdapter.register(db)
      assetEntityAdapter.register(db)
    }

    populateTestData()
  }


  @Test
  void 'SQL and JEXL selectors should select the same results'() {
    assertSameSelection('.*')
    assertSameSelection('/org/.*')
    assertSameSelection('.*/foo/.*')
    assertSameSelection('.?/foo/.*')
    assertSameSelection('/+foo/.*')
    assertSameSelection('/foo/.*|/bar/.*')
    assertSameSelection('.*/.*-[1-9].*')
    assertSameSelection('(?!.*(-sources|-javadoc).*).*/com/.*\\.jar')
    assertSameSelection('(?!^/com/foo/bar/.*).*')
    assertSameSelection('^/com/foo/bar/.*')
    assertSameSelection('/com/foo/bar/.*')
    assertSameSelection('/?com/foo/bar/.*')
  }

  private void assertSameSelection(final String pathRegex) {
    String expression = "path =~ \"$pathRegex\""
    Iterable<String> expectedPaths = evaluateAsJEXL(expression)
    log('{} selects {} paths', pathRegex, expectedPaths.size)
    assertThat(evaluateAsSQL(expression), is(expectedPaths))
  }

  private Iterable<String> evaluateAsSQL(final String expression) {
    SelectorSqlBuilder builder = new SelectorSqlBuilder()

    builder.propertyAlias('format', 'format')
    builder.propertyAlias('path', 'name')
    builder.propertyPrefix('attributes.' + FORMAT_NAME + '.')
    builder.parameterPrefix('p')

    selectorFactory.createSelector('csel', expression).toSql(builder)

    database.getInstance().acquire().withCloseable { db ->
      db.query(new OSQLSynchQuery("select from asset where ${builder.queryString}"), builder.queryParameters)
          .collect { it.field('name') }
    }
  }

  private Iterable<String> evaluateAsJEXL(final String expression) {
    JexlSelector jexl = selectorFactory.createSelector('jexl', expression)
    database.getInstance().acquire().withCloseable { db ->
      db.browseClass('asset').findAll { jexl.evaluate(variableResolverAdapter.fromDocument(it)) }
          .collect { it.field('name') }
    }
  }

  private void populateTestData() {
    database.getInstance().acquire().withCloseable { db ->
      bucket = new Bucket()
      bucket.setRepositoryName(REPOSITORY_NAME)
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()))
      bucketEntityAdapter.addEntity(db, bucket)

      populateTestVersion('0.1.0')
      populateTestVersion('1.0')
      populateTestVersion('1.1.0')
    }
  }

  private void populateTestVersion(final String version) {
    mavenProject('org', 'foo', version)
    mavenProject('org', 'foobar', version)
    mavenProject('org.foo', 'bar', version)
    mavenProject('org.foo.bar', 'org.foo.bar', version)

    mavenProject('foo', 'foo', version)
    mavenProject('foo', 'bar', version)
    mavenProject('bar', 'bar', version)
    mavenProject('bar', 'foo', version)

    mavenProject('com', 'foo', version)
    mavenProject('com', 'foobar', version)
    mavenProject('com.foo', 'bar', version)
    mavenProject('com.foo.bar', 'org.foo.bar', version)
  }

  private void mavenProject(final String group, final String name, final String version) {
    Component component = component(group, name, version)
    String gav = mavenMetadata(component)
    mavenPom(component, gav)
    mavenJar(component, gav)
  }

  private String mavenMetadata(final Component component) {
    String ga = "${component.group.replace('.', '/')}/${component.name()}"
    artifact(component, "${ga}/maven-metadata.xml")
    String gav = "${ga}/${component.version}"
    artifact(component, "${gav}/maven-metadata.xml")
    return gav
  }

  private void mavenPom(final Component component, final String gav) {
    String pom = "${gav}/${component.name()}-${component.version}.pom"
    artifact(component, "${pom}")
    asset(component, "${pom}.asc")
  }

  private void mavenJar(final Component component, final String gav) {
    String jar = "${gav}/${component.name()}-${component.version}.jar"
    artifact(component, "${jar}")
    asset(component, "${jar}.asc")
    String src = "${jar.replaceFirst('.jar', '-sources.jar')}"
    artifact(component, "${src}")
    asset(component, "${src}.asc")
    String doc = "${jar.replaceFirst('.jar', '-javadoc.jar')}"
    artifact(component, "${doc}")
    asset(component, "${doc}.asc")
  }

  private void artifact(final Component component, final String path) {
    asset(component, "${path}")
    asset(component, "${path}.md5")
    asset(component, "${path}.sha1")
  }

    private Component component(final String group, final String name, final String version) {
    Component component = new DefaultComponent()
    component.bucketId(EntityHelper.id(bucket))
    component.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()))
    component.format(FORMAT_NAME)
    component.group(group).name(name).version(version)
    componentEntityAdapter.addEntity(currentDb(), component)
    return component
  }

  private Asset asset(final Component component, final String name) {
    Asset asset = new Asset()
    asset.bucketId(EntityHelper.id(bucket))
    asset.componentId(EntityHelper.id(component))
    asset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()))
    asset.format(FORMAT_NAME)
    asset.name(name)
    assetEntityAdapter.addEntity(currentDb(), asset)
    return asset
  }

  private static ODatabaseDocumentTx currentDb() {
    return (ODatabaseDocumentTx) ODatabaseRecordThreadLocal.instance().get()
  }
}

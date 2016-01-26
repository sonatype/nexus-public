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
package org.sonatype.nexus.selector;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectorTest
{
  VariableSource source;

  @Before
  public void setUp() {
    source = new VariableSourceBuilder()
        .addResolver(new PropertiesResolver<>("component", of("format", "maven2")))
        .addResolver(new PropertiesResolver<>("asset", of("name", "junit", "group", "Jjunit", "path", "/org/apache/maven/foo/bar/moo.jar")))
        .addResolver(new ConstantVariableResolver(true, "X"))
        .addResolver(new ConstantVariableResolver(false, "Y"))
        .addResolver(new ConstantVariableResolver("foobar", "someString"))
        .addResolver(new ConstantVariableResolver(of("a", "alfa", "b", "bravo"), "someMap"))
        .build();
  }

  /*
   * component.format=maven2
   * asset.name=junit
   */
  @Test
  public void testBasicSelectorHappy() {
    Selector selector = new BasicSelector(of("component.format", "maven2", "asset.name", "junit"));

    assertTrue(selector.evaluate(source));
  }

  /*
   * component.format=maven2
   * asset.name=junit
   * asset.group=junit
   */
  @Test
  public void testBasicSelectorSad() {
    Selector selector = new BasicSelector(of("component.format", "maven2", "asset.name", "junit","asset.group", "junit"));

    assertFalse(selector.evaluate(source));
  }

  /*
   * component.format=maven2
   * asset.name=junit
   * asset.group=/^J/
   */
  @Test
  public void testBasicSelectorRegexHappy() {
    Selector selector = new BasicSelector(of("component.format", "maven2", "asset.name", "junit", "asset.group", "^J.+"));

    assertTrue(selector.evaluate(source));
  }

  private static VariableSource assetSourceWithPath(String path) {
    return new VariableSourceBuilder().addResolver(new PropertiesResolver<>("asset", of("path", path))).build();
  }

  /*
   * asset.path=/org/apache/maven/((?!sources\.).)*
   */
  @Test
  public void testAssetPathMavenPublic() {
    Selector selector = new BasicSelector(of("asset.path", "/org/apache/maven/((?!sources\\.).)*"));

    assertTrue(selector.evaluate(source));
    assertFalse(selector.evaluate(assetSourceWithPath("/org/apache/maven/foo/bar/moo-sources.jar")));
  }

  /*
   * asset.path=/org/apache/maven/.*
   */
  @Test
  public void testAssetPathMaven2WithSources() {
    Selector selector = new BasicSelector(of("asset.path", "/org/apache/maven/.*"));

    assertTrue(selector.evaluate(source));
    assertTrue(selector.evaluate(assetSourceWithPath("/org/apache/maven/foo/bar/moo-sources.jar")));
    assertFalse(selector.evaluate(assetSourceWithPath("/org.apache.maven.foo.bar.moo-sources.jar")));
  }

  /*
   * asset.path=/org\.apache\.maven.*
   */
  @Test
  public void testAssetPathMaven1() {
    Selector selector = new BasicSelector(of("asset.path", "/org\\.apache\\.maven.*"));

    assertFalse(selector.evaluate(source));
    assertFalse(selector.evaluate(assetSourceWithPath("/org/apache/maven/foo/bar/moo-sources.jar")));
    assertTrue(selector.evaluate(assetSourceWithPath("/org.apache.maven.foo.bar.moo-sources.jar")));
  }

  @Test
  public void testComponentFormatHappy() {
    Selector selector = new JexlSelector("component.format == 'maven2'");

    assertTrue(selector.evaluate(source));
  }

  @Test
  public void testComponentFormatSad() {
    Selector selector = new JexlSelector("component.format == 'nuget'");

    assertFalse(selector.evaluate(source));
  }

  @Test
  public void testAssetNameHappy() {
    Selector selector = new JexlSelector("asset.name =~ '^jun.+'");

    assertTrue(selector.evaluate(source));
  }

  @Test
  public void testAssetNameSad() {
    Selector selector = new JexlSelector("asset.name =~ '^jun.+' and asset.group =~ '^jun.+'");

    assertFalse(selector.evaluate(source));
  }

  @Test
  public void testXHappy() {
    Selector selector = new JexlSelector("X == true and Y == false");

    assertTrue(selector.evaluate(source));
  }

  @Test
  public void testStringToUppercase() {
    Selector selector = new JexlSelector("someString.toUpperCase() == 'FOOBAR'");

    assertTrue(selector.evaluate(source));
  }

  @Test
  public void testMap() {
    Selector selector = new JexlSelector("someMap['a'] == 'alfa'");

    assertTrue(selector.evaluate(source));
  }
}

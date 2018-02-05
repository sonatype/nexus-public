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

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class JexlSelectorTest
    extends TestSupport
{
  private VariableSource source;

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

  @Test
  public void testPrettyExceptionMsgOneLine() {
    String expression = "&&INVALID";
    testPrettyExceptionMsg(1, 1, "&&", expression);
  }

  @Test
  public void testPrettyExceptionMsgMultiLine() {
    String expression = "true\n #INVALID";
    // For some reason JEXL thinks # is at column 3 in line 2
    testPrettyExceptionMsg(2, 3, "#", expression);
  }

  private void testPrettyExceptionMsg(int line, int column, String detail, String expression) {
    String expected = String.format("Invalid JEXL at line '%s' column '%s'. Error parsing string: '%s'.", line, column,
        detail);
    String returned = null;
    try {
      new JexlSelector(expression);
    }
    catch (JexlException e) {
      returned = JexlSelector.prettyExceptionMsg(e);
    }
    assertNotNull("Returned string was not set.", returned);
    assertEquals(expected, returned);
  }

  @Test
  public void testPrettyExceptionMsgNoDetail() {
    // Setup
    String expected = "Invalid JEXL at line '2' column '4'.";

    JexlInfo info = new JexlInfo("", 2, 4);
    // Mocked because JexlException modifies msg internally after construction
    JexlException ex = mock(JexlException.class);
    doReturn(info).when(ex).getInfo();
    doReturn("").when(ex).getMessage();

    // Execute
    String returned = JexlSelector.prettyExceptionMsg(ex);

    // Verify
    assertNotNull("Returned string was not set.", returned);
    assertEquals(expected, returned);
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

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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.json.CurrentPathJsonParser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class NpmFieldUnmatcherTest
    extends TestSupport
{
  private static final String FIELD_NAME = "test";

  private static final String FIELD_PATH = "/test";

  @Mock
  private NpmFieldDeserializer fieldDeserializer;

  @Mock
  private CurrentPathJsonParser parser;

  private NpmFieldUnmatcher underTest;

  @Before
  public void setUp() throws IOException {
    when(parser.getCurrentName()).thenReturn(FIELD_NAME);
    when(parser.currentPath()).thenReturn(FIELD_PATH);
  }

  @Test
  public void should_Match_Current_Path() throws IOException {
    underTest = new NpmFieldUnmatcher(FIELD_NAME, FIELD_PATH, fieldDeserializer);

    assertTrue(underTest.matches(parser));
    assertFalse(underTest.wasNeverMatched());
  }

  @Test
  public void should_Match_By_Field_And_Regex_Path() throws IOException {
    underTest = new NpmFieldUnmatcher(FIELD_NAME, "/t(.*)t", fieldDeserializer);

    assertTrue(underTest.matches(parser));
    assertFalse(underTest.wasNeverMatched());
  }

  @Test
  public void should_NotMatch_Current_Path() throws IOException {
    underTest = new NpmFieldUnmatcher("", "", fieldDeserializer);

    assertFalse(underTest.matches(parser));
    assertTrue(underTest.wasNeverMatched());
  }

  @Test
  public void should_NotMatch_By_Name() throws IOException {
    underTest = new NpmFieldUnmatcher("", FIELD_PATH, fieldDeserializer);

    assertFalse(underTest.matches(parser));
    assertTrue(underTest.wasNeverMatched());
  }

  @Test
  public void should_NotMatch_By_Path() throws IOException {
    underTest = new NpmFieldUnmatcher(FIELD_NAME, "", fieldDeserializer);

    assertFalse(underTest.matches(parser));
    assertTrue(underTest.wasNeverMatched());
  }

  @Test
  public void should_Match_By_Field_But_Not_By_Regex_Path() throws IOException {
    underTest = new NpmFieldUnmatcher(FIELD_NAME, "/b(.*)b", fieldDeserializer);

    assertFalse(underTest.matches(parser));
    assertTrue(underTest.wasNeverMatched());
  }

  @Test
  public void should_PreventDeserializationOnMatched() {
    assertFalse(new NpmFieldUnmatcher(FIELD_NAME, "/b(.*)b", fieldDeserializer).allowDeserializationOnMatched());
  }
}

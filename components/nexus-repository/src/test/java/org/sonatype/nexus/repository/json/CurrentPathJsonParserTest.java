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
package org.sonatype.nexus.repository.json;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;

import com.fasterxml.jackson.core.JsonFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CurrentPathJsonParserTest
    extends TestSupport
{
  private final static String SIMPLE_JSON = "{\"_id\":\"simple\",\"user\":{\"description\":\"simplestuff\"}}";

  private CurrentPathJsonParser underTest;

  @Before
  public void setUp() throws IOException {
    underTest = new CurrentPathJsonParser(new JsonFactory().createParser(SIMPLE_JSON));
  }

  @Test
  public void should_Return_CurrentPath_For_Parser() throws IOException {
    assertThat(underTest.currentPath(), equalTo("/"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/_id"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/user"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/user/description"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/user"));

    underTest.nextValue();
    assertThat(underTest.currentPath(), equalTo("/"));
  }

  @Test
  public void should_Return_CurrentPath_InParts_For_Parser() throws IOException {
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo(""));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo(""));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo("_id"));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo("user"));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(2));
    assertThat(underTest.currentPathInParts()[0], equalTo("user"));
    assertThat(underTest.currentPathInParts()[1], equalTo("description"));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo("user"));

    underTest.nextValue();
    assertThat(underTest.currentPathInParts().length, equalTo(1));
    assertThat(underTest.currentPathInParts()[0], equalTo(""));
  }

  @Test
  public void should_Return_CurrentPointer() {
    assertThat(underTest.currentPointer(), notNullValue());
  }
}

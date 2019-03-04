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
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NestedAttributesMapJsonParserTest
    extends TestSupport
{
  @Mock
  private JsonParser jsonParser;

  private NestedAttributesMap nestedAttributesMap = new NestedAttributesMap("testMap", newHashMap());

  private NestedAttributesMapJsonParser underTest;

  @Before
  public void setUp() {
    underTest = new NestedAttributesMapJsonParser(jsonParser, nestedAttributesMap);
  }

  @Test
  public void default_Mapping_Disabled_By_Default() {
    assertFalse(underTest.isDefaultMapping());
  }

  @Test
  public void change_defaultMapping() {
    underTest.enableDefaultMapping();
    assertTrue(underTest.isDefaultMapping());

    underTest.disableDefaultMapping();
    assertFalse(underTest.isDefaultMapping());
  }

  @Test
  public void mapping_Inside_Array_Disabled_By_Default() {
    assertFalse(underTest.isMappingInsideArray());
  }

  @Test
  public void mark_MappingInsideArray() {
    underTest.markMappingInsideArray();
    assertTrue(underTest.isMappingInsideArray());

    underTest.unMarkMappingInsideArray();
    assertFalse(underTest.isMappingInsideArray());
  }

  @Test
  public void assure_Same_RootMap() {
    assertEquals(underTest.getRoot(), nestedAttributesMap);
  }

  @Test
  public void no_ChildMap_From_RootMap_On_MappingInsideArray() {
    underTest.markMappingInsideArray();
    assertThat(underTest.getChildFromRoot(), nullValue());
  }

  @Test
  public void retrieve_ChildMap_From_RootMap() throws IOException {
    String simpleJson = "{\"user\":{\"description\":\"simplestuff\"}}";
    underTest = new NestedAttributesMapJsonParser(new JsonFactory().createParser(simpleJson), nestedAttributesMap);

    // for the test fast forward to first chid
    underTest.nextValue();
    underTest.nextValue();
    NestedAttributesMap child = underTest.getChildFromRoot();
    assertThat(child.getKey(), equalTo("user"));

    underTest.nextValue();
    child = underTest.getChildFromRoot();
    assertThat(child.getKey(), equalTo("description"));
  }
}

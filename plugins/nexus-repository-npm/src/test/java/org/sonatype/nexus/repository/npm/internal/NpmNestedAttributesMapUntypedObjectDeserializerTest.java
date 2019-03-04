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
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.json.NestedAttributesMapJsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.fasterxml.jackson.core.JsonTokenId.ID_START_OBJECT;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmNestedAttributesMapUntypedObjectDeserializerTest
    extends TestSupport
{
  private final static String AUTHOR_FIELD_NAME = "author";

  private final static String AUTHOR_FIELD_VALUE = "shakespeare";

  private final static String AUTHOR_EMAIL_NAME = "email";

  private final static String AUTHOR_EMAIL_VALUE = "a@a.com";

  private final static String MAINTAINER_FIELD_NAME = "maintainer";

  private final static String VERSIONS_FIELD_NAME = "versions";

  private NestedAttributesMap recessive;

  @Mock
  private DeserializationContext context;

  @Mock
  private NestedAttributesMapJsonParser parser;

  private NpmNestedAttributesMapUntypedObjectDeserializer underTest;

  @Before
  public void setUp() {
    recessive = new NestedAttributesMap("recessive", newHashMap());

    when(parser.getCurrentTokenId()).thenReturn(ID_START_OBJECT);
    when(parser.getChildFromRoot()).thenReturn(recessive);

    underTest = spy(new NpmNestedAttributesMapUntypedObjectDeserializer(parser));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void default_Npm_Overlaying() throws IOException {
    recessive.child(MAINTAINER_FIELD_NAME).set(AUTHOR_FIELD_NAME, "");
    recessive.child(MAINTAINER_FIELD_NAME).set(AUTHOR_EMAIL_NAME, AUTHOR_EMAIL_VALUE);

    NestedAttributesMap dominant = new NestedAttributesMap("dominant", Maps.newHashMap());
    dominant.child(MAINTAINER_FIELD_NAME).set(AUTHOR_FIELD_NAME, AUTHOR_FIELD_VALUE);
    whenSuperParseObject(MAINTAINER_FIELD_NAME, dominant.backing());

    Map<String, Map> deserializedValue = (Map<String, Map>) underTest.deserialize(parser, context);
    Map maintainer = deserializedValue.get(MAINTAINER_FIELD_NAME);
    assertThat(maintainer.get(AUTHOR_FIELD_NAME), equalTo(AUTHOR_FIELD_VALUE));
    assertThat(maintainer.get(AUTHOR_EMAIL_NAME), equalTo(AUTHOR_EMAIL_VALUE));

    verify(parser).isDefaultMapping();
    verify(parser).getChildFromRoot();
    verify(parser).currentPath();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void recessiveVersion_Overwritten_By_DominantVersion() throws IOException {
    recessive.child(VERSIONS_FIELD_NAME).child("1.0").set("name", "recessive");
    recessive.child(VERSIONS_FIELD_NAME).child("2.0").set("name", "recessive");

    NestedAttributesMap dominant = new NestedAttributesMap("dominant", Maps.newHashMap());
    dominant.child(VERSIONS_FIELD_NAME).child("1.0").set("name", "dominant");
    whenSuperParseObject(VERSIONS_FIELD_NAME, dominant.backing());

    Map deserializedValue = (Map) underTest.deserialize(parser, context);
    Map<String, Map> versions = (Map<String, Map>) deserializedValue.get(VERSIONS_FIELD_NAME);
    assertThat(versions.size(), equalTo(1));
    assertThat(versions.get("1.0").get("name"), equalTo("dominant"));

    verify(parser).isDefaultMapping();
    verify(parser).getChildFromRoot();
    verify(parser).currentPath();
  }

  private void whenSuperParseObject(final String fieldName, final Object returnObject) throws IOException {
    when(parser.currentPath()).thenReturn(fieldName);

    // simple forcing without having to override lots of methods
    when(context.handleUnexpectedToken(eq(Object.class), eq(parser))).thenReturn(returnObject);
  }
}

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
package org.sonatype.nexus.repository.p2.internal.util;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.p2.internal.exception.AttributeParsingException;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class P2TempBlobUtilsTest
    extends TestSupport
{
  private static final String EXTENSION = "jar";

  private static final String FAKE_VERSION = "1.2.100.v20170912-1859";

  private static final String JAR_NAME = "org.eclipse.core.runtime.feature_1.2.100.v20170912-1859.jar";

  private P2TempBlobUtils p2TempBlobUtils;

  @Mock
  private TempBlobConverter tempBlobConverter;

  @Mock
  private PropertyParser propertyParser;

  @Spy
  @InjectMocks
  private AttributesParserFeatureXml xmlParser;

  @Spy
  @InjectMocks
  private AttributesParserManifest manifestParser;

  @Mock
  private TempBlob tempBlob;

  @Before
  public void setUp() {
    p2TempBlobUtils = new P2TempBlobUtils(xmlParser, manifestParser);
  }

  @Test
  public void getVersion() throws Exception {
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(JAR_NAME));
    doReturn(buildWithVersionAndExtension()).when(manifestParser).getAttributesFromBlob(any(), any());

    P2Attributes p2Attributes = p2TempBlobUtils
        .mergeAttributesFromTempBlob(tempBlob, buildWithVersionAndExtension());

    assertThat(p2Attributes.getComponentVersion(), is(equalTo(FAKE_VERSION)));
  }

  @Test
  public void getUnknownVersion() throws IOException, AttributeParsingException {
    P2Attributes p2Attributes = buildWithExtension();
    doReturn(p2Attributes).when(manifestParser).getAttributesFromBlob(any(), anyString());
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(JAR_NAME));

    P2Attributes actual = p2TempBlobUtils.mergeAttributesFromTempBlob(tempBlob, p2Attributes);
    Assert.assertEquals(buildWithVersionAndExtension(), actual);
  }

  @Test
  public void getJarWithJarFile() throws Exception {
    when(tempBlob.get()).thenAnswer((a) -> getClass().getResourceAsStream(JAR_NAME));
    assertThat(xmlParser.getAttributesFromBlob(tempBlob, EXTENSION).isEmpty(), is(false));
  }

  @Test
  public void getJarWithPackGz() throws Exception {
    when(tempBlobConverter.getJarFromPackGz(tempBlob)).thenReturn(getClass().getResourceAsStream(JAR_NAME));
    when(tempBlob.get()).thenAnswer((a) -> getClass().getResourceAsStream(JAR_NAME));

    assertThat(xmlParser.getAttributesFromBlob(tempBlob, EXTENSION).isEmpty(), is(false));
  }

  private P2Attributes buildWithVersionAndExtension() {
    return P2Attributes.builder()
        .componentVersion(FAKE_VERSION)
        .extension(EXTENSION).build();
  }

  private P2Attributes buildWithExtension() {
    return P2Attributes.builder()
        .extension(EXTENSION).build();
  }
}

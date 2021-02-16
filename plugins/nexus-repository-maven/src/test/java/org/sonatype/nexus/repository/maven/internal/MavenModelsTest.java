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
package org.sonatype.nexus.repository.maven.internal;

import java.io.ByteArrayInputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class MavenModelsTest
    extends TestSupport
{
  String notXml = "not xml";

  @Test
  public void testReadModel_emptyInputStreamIsNull() throws Exception {
    Model model = MavenModels.readModel(new ByteArrayInputStream(new byte[0]));
    assertThat(model, nullValue());
  }

  @Test
  public void testReadModel_NotXmlIsNull() throws Exception {
    Model model = MavenModels.readModel(new ByteArrayInputStream(notXml.getBytes()));
    assertThat(model, nullValue());
  }

  @Test
  public void testReadModel_WithoutClosingTagsIsNull() throws Exception {
    Metadata metadata = MavenModels.readMetadata(
        getClass().getResourceAsStream("/org/sonatype/nexus/repository/maven/metadataWithoutClosingTags.xml"));
    assertThat(metadata, nullValue());
  }
}

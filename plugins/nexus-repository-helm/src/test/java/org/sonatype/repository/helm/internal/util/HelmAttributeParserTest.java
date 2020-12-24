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
package org.sonatype.repository.helm.internal.util;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HelmAttributeParserTest
    extends TestSupport
{
  private YamlParser yamlParser;

  private TgzParser tgzParser;

  private ProvenanceParser provenanceParser;

  private HelmAttributeParser underTest;

  @Before
  public void setUp() {
    yamlParser = new YamlParser();
    tgzParser = new TgzParser();
    provenanceParser = new ProvenanceParser();
    underTest = new HelmAttributeParser(tgzParser, yamlParser, provenanceParser);
  }

  @Test
  public void testGetAttributesFromChart() throws Exception {
    String name = "mongodb-0.4.9.tgz";
    InputStream chart = getClass().getResourceAsStream(name);
    AssetKind assetKind = AssetKind.getAssetKindByFileName(name);
    HelmAttributes result = underTest.getAttributes(assetKind, chart);

    assertThat(result.getName(), is("mongodb"));
    assertThat(result.getVersion(), is("0.4.9"));
    assertThat(result.getDescription(),
        is("NoSQL document-oriented database that stores JSON-like documents with dynamic schemas, simplifying the integration of data in content-driven applications."));
    assertThat(result.getIcon(), is("https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png"));

    Map<String, String> maintainers = new LinkedHashMap<String, String>()
    {{
      put("email", "containers@bitnami.com");
      put("name", "Bitnami");
    }};
    assertThat(result.getMaintainers(), is(Collections.singletonList(maintainers)));
    assertThat(result.getSources(), is(Collections.singletonList("https://github.com/bitnami/bitnami-docker-mongodb")));
  }
}

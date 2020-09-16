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
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.repository.helm.internal.HelmListTestHelper;
import org.sonatype.repository.helm.internal.metadata.ChartEntry;
import org.sonatype.repository.helm.internal.metadata.ChartIndex;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class YamlParserTest
    extends TestSupport
{
  private YamlParser underTest;

  @Before
  public void setUp() throws Exception {
    this.underTest = new YamlParser();
  }

  @Test
  public void testParseChartYaml() throws Exception {
    InputStream is = getClass().getResourceAsStream("Chart.yaml");
    Map<String, Object> helmYaml = underTest.load(is);

    assertThat(helmYaml.get("version").toString(), is(equalTo("0.4.9")));
    assertThat(helmYaml.get("name").toString(), is(equalTo("mongodb")));
    assertThat(helmYaml.get("home").toString(), is(equalTo("https://mongodb.org")));
    assertThat(helmYaml.get("description").toString(), is(equalTo("NoSQL document-oriented database that stores JSON-like documents with" +
        " dynamic schemas, simplifying the integration of data in content-driven applications.")));
    assertThat(helmYaml.get("engine").toString(), is(equalTo("gotpl")));
    assertThat(helmYaml.get("icon").toString(), is(equalTo("https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png")));
    assertThat(helmYaml.get("keywords"), is(equalTo(getKeywords())));
    assertThat(helmYaml.get("maintainers"), is(equalTo(getMaintainers())));
    assertThat(helmYaml.get("sources"), is(equalTo(getSources())));
  }

  @Test
  public void testWriteIndexYaml() throws Exception {
    InputStream expected = getClass().getResourceAsStream("indexresult.yaml");
    StringWriter writer = new StringWriter();
    IOUtils.copy(expected, writer);
    String expectedResult = writer.toString();
    OutputStream os = new ByteArrayOutputStream();
    underTest.write(os, createChartIndex());

    assertThat(os, is(notNullValue()));
    assertEquals(StringUtils.normalizeSpace(os.toString()), StringUtils.normalizeSpace(expectedResult));
  }

  private List<String> getKeywords() {
    List<String> list = new ArrayList<>();
    list.add("mongodb");
    list.add("database");
    list.add("nosql");
    return list;
  }

  private List<Map<String, Object>> getMaintainers() {
    List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
    Map<String, Object> map = new HashMap<String, Object>() {
      {
        put("email", "containers@bitnami.com");
        put("name", "Bitnami");
      }
    };
    listMap.add(map);
    return listMap;
  }

  private List<String> getSources() {
    List<String> list = new ArrayList<>();
    list.add("https://github.com/bitnami/bitnami-docker-mongodb");
    return list;
  }

  private ChartIndex createChartIndex() {
    ChartIndex chartIndex = new ChartIndex();
    chartIndex.setApiVersion("1.0");

    chartIndex.addEntry(createChartEntry(
        "NoSQL document-oriented database that stores JSON-like documents with\n" +
            "  dynamic schemas, simplifying the integration of data in content-driven applications.",
        "mongodb",
        "0.4.9",
        DateTime.parse("2018-08-13T22:05:33.023Z"),
        "0.0.1",
        "12345",
        "https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png",
        HelmListTestHelper.getUrlList(),
        HelmListTestHelper.getSourcesList(),
        HelmListTestHelper.getMaintainersList()
    ));

    chartIndex.addEntry(createChartEntry(
        "NoSQL document-oriented database that stores JSON-like documents with\n" +
            "  dynamic schemas, simplifying the integration of data in content-driven applications.",
        "mongodb",
        "0.4.8",
        DateTime.parse("2018-08-13T22:05:33.023Z"),
        "0.0.2",
        "12345",
        "https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png",
        HelmListTestHelper.getUrlList(),
        HelmListTestHelper.getSourcesList(),
        HelmListTestHelper.getMaintainersList()
    ));

    chartIndex.addEntry(createChartEntry(
        "NoSQL document-oriented database that stores JSON-like documents with\n" +
            "  dynamic schemas, simplifying the integration of data in content-driven applications.",
        "notmongdb",
        "1.0.0",
        DateTime.parse("2018-08-13T22:05:33.023Z"),
        "0.0.1",
        "12345",
        "https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png",
        HelmListTestHelper.getUrlList(),
        HelmListTestHelper.getSourcesList(),
        HelmListTestHelper.getMaintainersList()
    ));

    return chartIndex;
  }

  private ChartEntry createChartEntry(final String description,
                                      final String name,
                                      final String version,
                                      final DateTime created,
                                      final String appVersion,
                                      final String digest,
                                      final String icon,
                                      final List<String> urls,
                                      final List<String> sources,
                                      final List<Map<String, String>> maintainers)
  {
    ChartEntry chartEntry = new ChartEntry();

    chartEntry.setDescription(description);
    chartEntry.setName(name);
    chartEntry.setCreated(created);
    chartEntry.setVersion(version);
    chartEntry.setAppVersion(appVersion);
    chartEntry.setDigest(digest);
    chartEntry.setIcon(icon);
    chartEntry.setUrls(urls);
    chartEntry.setSources(sources);
    chartEntry.setMaintainers(maintainers);

    return chartEntry;
  }
}

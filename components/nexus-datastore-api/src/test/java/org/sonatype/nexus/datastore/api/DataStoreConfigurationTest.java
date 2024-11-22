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
package org.sonatype.nexus.datastore.api;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sonatype.nexus.datastore.api.DataStoreConfiguration.REDACTED;

public class DataStoreConfigurationTest
    extends TestSupport
{
  private DataStoreConfiguration configurationA;

  private DataStoreConfiguration configurationB;

  private DataStoreConfiguration configurationC;

  @Before
  public void setup() {
    Map<String, String> configA = new HashMap<>();
    configA.put("entry-1", "value1");
    configA.put("entry-2", "value2");
    configA.put("entry-3", "value3");
    configurationA = new DataStoreConfiguration();
    configurationA.setName("configA");
    configurationA.setSource("sourceA");
    configurationA.setType("mainType");
    configurationA.setAttributes(configA);

    Map<String, String> configB = new HashMap<>();
    configB.put("entry-1", "value1");
    configB.put("entry-2", "value2");
    configB.put("entry-3", "value3");
    configurationB = new DataStoreConfiguration();
    configurationB.setName("configB");
    configurationB.setSource("sourceB");
    configurationB.setType("mainType");
    configurationB.setAttributes(configB);

    Map<String, String> configC = new HashMap<>();
    configC.put("entry-1", "value1");
    configC.put("entry-2", "value2-DIFF");
    configC.put("entry-4", "value4");
    configurationC = new DataStoreConfiguration();
    configurationC.setName("configC");
    configurationC.setSource("sourceC");
    configurationC.setType("mainType");
    configurationC.setAttributes(configC);
  }

  @Test
  public void sameConfigurationShouldHaveNoDiff() {
    Map<String, Map<String, String>> diffMap = DataStoreConfiguration.diff(configurationA, configurationA);
    assertThat(diffMap.keySet(), hasSize(0));
  }

  @Test
  public void nameAndSourceDiff() {
    Map<String, Map<String, String>> diffMap = DataStoreConfiguration.diff(configurationA, configurationB);
    assertThat(diffMap.keySet(), hasSize(2));
    assertThat(diffMap.keySet(), containsInAnyOrder("name", "source"));
    assertThat(diffMap.get("name").values(), containsInAnyOrder("configA", "configB"));
    assertThat(diffMap.get("source").values(), containsInAnyOrder("sourceA", "sourceB"));
  }

  @Test
  public void attributesDiffer() {
    Map<String, Map<String, String>> diffMap = DataStoreConfiguration.diff(configurationA, configurationC);
    assertThat(diffMap.keySet(), hasSize(5));
    assertThat(diffMap.keySet(),
        containsInAnyOrder("name", "source", "attributes->entry-2", "attributes->entry-3", "attributes->entry-4"));
    assertThat(diffMap.get("name").values(), containsInAnyOrder("configA", "configC"));
    assertThat(diffMap.get("source").get("configA"), equalTo("sourceA"));
    assertThat(diffMap.get("source").get("configC"), equalTo("sourceC"));
    assertThat(diffMap.get("source").values(), containsInAnyOrder("sourceA", "sourceC"));
    assertThat(diffMap.get("attributes->entry-2").values(), containsInAnyOrder("value2", "value2-DIFF"));
    assertThat(diffMap.get("attributes->entry-3").values(), containsInAnyOrder("value3", null));
    assertThat(diffMap.get("attributes->entry-4").values(), containsInAnyOrder(null, "value4"));
  }

  @Test
  public void hideSensitiveKeys() {
    configurationA.getAttributes().put("key-1", "sameValue");
    configurationB.getAttributes().put("key-1", "sameValue");

    configurationA.getAttributes().put("key-2", "differentValue-2-A");
    configurationB.getAttributes().put("key-2", "differentValue-2-B");

    configurationA.getAttributes().put("jdbcUrl", "localhost:5432/nexus?username=user&password=pass&word");
    configurationB.getAttributes().put("jdbcUrl", "localhost:5432/nexus?password=pass&word&username=user");
    Map<String, Map<String, String>> diffMap = DataStoreConfiguration.diff(configurationA, configurationB);
    assertThat(diffMap.keySet(), hasSize(4));
    assertThat(diffMap.keySet(), containsInAnyOrder("name", "source", "attributes->key-2", "attributes->jdbcUrl"));
    assertThat(diffMap.get("name").values(), containsInAnyOrder("configA", "configB"));
    assertThat(diffMap.get("source").values(), containsInAnyOrder("sourceA", "sourceB"));
    assertThat(diffMap.get("attributes->key-2").values(), containsInAnyOrder(REDACTED, REDACTED));
    assertThat(diffMap.get("attributes->jdbcUrl").values(), containsInAnyOrder(
        format("localhost:5432/nexus?username=user&password=%s", REDACTED),
        format("localhost:5432/nexus?password=%s&username=user", REDACTED)));
  }
}

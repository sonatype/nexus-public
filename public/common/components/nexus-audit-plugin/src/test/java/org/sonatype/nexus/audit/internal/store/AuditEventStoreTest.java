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
package org.sonatype.nexus.audit.internal.store;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class AuditEventStoreTest
{
  @Test
  public void testAuditEventDataFieldMapping() {
    AuditEventData data = new AuditEventData();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    data.setDomain("security.user");
    data.setType("create");
    data.setContext("admin");
    data.setTimestamp(now);
    data.setInitiator("admin/127.0.0.1");
    data.setNodeId("node-1");
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put("key", "value");
    data.setAttributes(attrs);

    assertThat(data.getDomain(), is("security.user"));
    assertThat(data.getType(), is("create"));
    assertThat(data.getContext(), is("admin"));
    assertThat(data.getTimestamp(), is(now));
    assertThat(data.getInitiator(), is("admin/127.0.0.1"));
    assertThat(data.getNodeId(), is("node-1"));
    assertThat(data.getAttributes(), notNullValue());
    assertThat(data.getAttributes().get("key"), is("value"));
  }

  @Test
  public void testAuditEventDataDefaults() {
    AuditEventData data = new AuditEventData();

    assertThat(data.getId(), is(0L));
    assertThat(data.getDomain() == null, is(true));
    assertThat(data.getType() == null, is(true));
    assertThat(data.getContext() == null, is(true));
    assertThat(data.getTimestamp() == null, is(true));
    assertThat(data.getInitiator() == null, is(true));
    assertThat(data.getNodeId() == null, is(true));
    assertThat(data.getAttributes() == null, is(true));
  }
}

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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLock;
import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLockParser;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;
import org.sonatype.nexus.repository.vulnerability.ComponentsVulnerability;
import org.sonatype.nexus.repository.vulnerability.SeverityLevel;
import org.sonatype.nexus.repository.vulnerability.Vulnerability;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.google.common.io.Resources.getResource;
import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static org.apache.commons.io.Charsets.UTF_8;
import static org.fest.util.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.npm.internal.NpmFormat.NAME;

public class ReportCreatorTest
{
  private static final String NPM_AUDIT_JSON =
      "org/sonatype/nexus/repository/npm/internal/audit/parser/package-lock.json";

  private static final String NPM_AUDIT_RESPONSE =
      "org/sonatype/nexus/repository/npm/internal/audit/report/npm-audit-response.json";

  private final Map<AuditComponent, List<Vulnerability>> dummyVulnerabilities =
      ImmutableMap.of(new AuditComponent("appId", NAME, "marked", "0.6.3"),
          newArrayList(new Vulnerability(SeverityLevel.CRITICAL, "marked", "0.0.0", "marked")),
          new AuditComponent("appId", NAME, "lodash", "2.4.2"),
          newArrayList(new Vulnerability(SeverityLevel.HIGH, "lodash", "0.0.1", "lodash"))
      );

  private final Gson gson =
      new GsonBuilder()
          .serializeNulls()
          .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
          .create();

  @Mock
  private final ComponentsVulnerability dummyReport = mock(ComponentsVulnerability.class);

  @Spy
  private final ReportCreator underTest = new ReportCreator();

  private PackageLock packageLock;

  @Before
  public void setUp() throws IOException {
    packageLock = PackageLockParser.parse(Resources.toString(getResource(NPM_AUDIT_JSON), UTF_8));
    when(dummyReport.getAuditComponents()).thenReturn(dummyVulnerabilities);
  }

  @Test
  public void testReport() throws IOException {
    ResponseReport responseReport = underTest.buildResponseReport(dummyReport, packageLock);
    String expectedResponse = Resources.toString(getResource(NPM_AUDIT_RESPONSE), UTF_8);
    assertEquals(StringUtils.deleteWhitespace(expectedResponse),
        StringUtils.deleteWhitespace(gson.toJson(responseReport)));
  }
}

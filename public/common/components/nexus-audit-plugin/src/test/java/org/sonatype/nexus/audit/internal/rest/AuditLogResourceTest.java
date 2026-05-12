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
package org.sonatype.nexus.audit.internal.rest;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.audit.internal.store.AuditEventData;
import org.sonatype.nexus.audit.internal.store.AuditEventStore;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AuditLogResourceTest
{
  @Mock
  private AuditEventStore auditEventStore;

  private AuditLogResource underTest;

  private DefaultSecurityManager securityManager;

  private TestRealm realm;

  @Before
  public void setUp() {
    underTest = new AuditLogResource(auditEventStore);

    // Set up Shiro security context for authentication and authorization
    realm = new TestRealm("test-realm");
    realm.addTestAccount("testuser");
    securityManager = new DefaultSecurityManager(realm);
    ThreadContext.bind(securityManager);

    // Create an authenticated subject
    SimplePrincipalCollection principals = new SimplePrincipalCollection("testuser", realm.getName());
    Session session = new SimpleSession();
    DelegatingSubject subject = new DelegatingSubject(principals, true, "localhost", session, securityManager);
    ThreadContext.bind(subject);
  }

  @After
  public void tearDown() {
    // Clean up Shiro security context
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
    if (securityManager != null) {
      securityManager.destroy();
    }
  }

  @Test
  public void testExportAuditLog_ReturnsCSV() throws Exception {
    OffsetDateTime now = OffsetDateTime.of(2026, 3, 12, 10, 30, 0, 0, ZoneOffset.UTC);
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("key1", "value1");
    attrs.put("key2", "value2");

    AuditEventData event1 = createAuditEvent(1L, "security.user", "created", "user1", now, "admin", "node1", attrs);
    AuditEventData event2 = createAuditEvent(2L, "repository", "updated", "repo-name", now.plusHours(1), "system",
        "node2", Collections.emptyMap());

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Arrays.asList(event1, event2));

    Response response = underTest.exportAuditLog(null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getHeaderString("Content-Disposition"), containsString("attachment; filename=\"audit-log-"));
    assertThat(response.getHeaderString("Content-Disposition"), containsString(".csv\""));

    // Extract CSV content
    StreamingOutput output = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String csvContent = baos.toString("UTF-8");

    // Verify CSV header
    assertThat(csvContent, containsString("ID,Timestamp,Domain,Type,Context,Initiator,Node ID,Attributes"));

    // Verify event data rows
    assertThat(csvContent, containsString("1,2026-03-12T10:30Z,security.user,created,user1,admin,node1,"));
    assertThat(csvContent, containsString("2,2026-03-12T11:30Z,repository,updated,repo-name,system,node2,"));
  }

  @Test
  public void testExportAuditLog_WithCategoryFilter() throws Exception {
    when(auditEventStore.findByDomains(any(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(Arrays.asList("security"), null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    verify(auditEventStore).findByDomains(any(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0));
  }

  @Test
  public void testExportAuditLog_WithDomainFilter() throws Exception {
    List<String> domains = Arrays.asList("security.user", "security.role");

    when(auditEventStore.findByDomains(eq(domains), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(null, domains, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    verify(auditEventStore).findByDomains(eq(domains), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000),
        eq(0));
  }

  @Test
  public void testExportAuditLog_WithTypeFilter() throws Exception {
    when(auditEventStore.findAll(isNull(), eq("created"), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(null, null, Arrays.asList("created"), null, null, null);

    assertThat(response.getStatus(), is(200));
    verify(auditEventStore).findAll(isNull(), eq("created"), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0));
  }

  @Test
  public void testExportAuditLog_WithDateRange() throws Exception {
    String startDate = "2026-03-01T00:00:00Z";
    String endDate = "2026-03-12T23:59:59Z";

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), any(), any(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(null, null, null, null, startDate, endDate);

    assertThat(response.getStatus(), is(200));
    verify(auditEventStore).findAll(isNull(), isNull(), isNull(), isNull(), any(OffsetDateTime.class),
        any(OffsetDateTime.class),
        eq(10000), eq(0));
  }

  @Test
  public void testExportAuditLog_EscapesSpecialCharacters() throws Exception {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("message", "value with, comma");

    AuditEventData event = createAuditEvent(1L, "test", "created", "context with \"quotes\"", OffsetDateTime.now(),
        "admin", "node1", attrs);

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.singletonList(event));

    Response response = underTest.exportAuditLog(null, null, null, null, null, null);

    StreamingOutput output = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String csvContent = baos.toString("UTF-8");

    // Verify context with quotes is properly escaped
    assertThat(csvContent, containsString("\"context with \"\"quotes\"\"\""));
    // Verify attributes with comma are escaped
    assertThat(csvContent, containsString("message=value with, comma"));
  }

  @Test
  public void testExportAuditLog_PreventsCsvInjection() throws Exception {
    // Test that formula-triggering characters are neutralized to prevent CSV injection attacks
    // These could execute malicious formulas when opened in Excel/LibreOffice
    Map<String, Object> attrs = Collections.emptyMap();

    AuditEventData eventFormula = createAuditEvent(1L, "security.user", "created",
        "=HYPERLINK(\"http://evil.com\",\"Click\")", OffsetDateTime.now(), "admin", "node1", attrs);
    AuditEventData eventPlus = createAuditEvent(2L, "security.user", "created",
        "+cmd|' /C calc'!A0", OffsetDateTime.now(), "admin", "node1", attrs);
    AuditEventData eventMinus = createAuditEvent(3L, "security.user", "created",
        "-1+1", OffsetDateTime.now(), "admin", "node1", attrs);
    AuditEventData eventAt = createAuditEvent(4L, "security.user", "created",
        "@SUM(1+1)", OffsetDateTime.now(), "admin", "node1", attrs);
    AuditEventData eventPipe = createAuditEvent(5L, "security.user", "created",
        "|calc", OffsetDateTime.now(), "admin", "node1", attrs);
    AuditEventData eventPercent = createAuditEvent(6L, "security.user", "created",
        "%00", OffsetDateTime.now(), "admin", "node1", attrs);

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Arrays.asList(eventFormula, eventPlus, eventMinus, eventAt, eventPipe, eventPercent));

    Response response = underTest.exportAuditLog(null, null, null, null, null, null);

    StreamingOutput output = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String csvContent = baos.toString("UTF-8");

    // All formula-triggering characters should be prefixed with single quote to neutralize
    assertThat(csvContent, containsString("'=HYPERLINK"));
    assertThat(csvContent, containsString("'+cmd"));
    assertThat(csvContent, containsString("'-1+1"));
    assertThat(csvContent, containsString("'@SUM"));
    assertThat(csvContent, containsString("'|calc"));
    assertThat(csvContent, containsString("'%00"));
  }

  @Test
  public void testGetAuditLog_InvalidPage() {
    Response response = underTest.getAuditLog(null, null, null, null, null, null, null, 0, 20);

    assertThat(response.getStatus(), is(400));
    assertThat(response.getEntity().toString(), containsString("Page must be >= 1"));
  }

  @Test
  public void testGetAuditLog_LimitIsCapped() throws Exception {
    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(100), eq(0)))
        .thenReturn(Collections.emptyList());
    when(auditEventStore.count(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(0);

    Response response = underTest.getAuditLog(null, null, null, null, null, null, null, 1, 500);

    assertThat(response.getStatus(), is(200));
    // Verify limit was capped at 100
    verify(auditEventStore).findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(100), eq(0));
  }

  @Test
  public void testGetAuditLog_WithCorrectPermission() {
    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(20), eq(0)))
        .thenReturn(Collections.emptyList());
    when(auditEventStore.count(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(0);

    Response response = underTest.getAuditLog(null, null, null, null, null, null, null, 1, 20);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void testGetAuditLog_WithWildcardPermission() {
    realm.addTestAccount("wildcarduser", "nexus:*");
    authenticateUser("wildcarduser");

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(20), eq(0)))
        .thenReturn(Collections.emptyList());
    when(auditEventStore.count(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(0);

    Response response = underTest.getAuditLog(null, null, null, null, null, null, null, 1, 20);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void testGetAuditLog_RejectsInsufficientPermission() {
    realm.addTestAccount("noreaduser", "nexus:audit:write");
    authenticateUser("noreaduser");

    try {
      underTest.getAuditLog(null, null, null, null, null, null, null, 1, 20);
      fail("Expected AuthorizationException for insufficient permissions");
    }
    catch (org.apache.shiro.authz.AuthorizationException e) {
      // Expected - user has write but not read permission
    }
  }

  @Test
  public void testExportAuditLog_WithCorrectPermission() {
    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void testExportAuditLog_WithWildcardPermission() {
    realm.addTestAccount("wildcarduser", "nexus:*");
    authenticateUser("wildcarduser");

    when(auditEventStore.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(10000), eq(0)))
        .thenReturn(Collections.emptyList());

    Response response = underTest.exportAuditLog(null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void testExportAuditLog_RejectsInsufficientPermission() {
    realm.addTestAccount("noreaduser", "nexus:audit:write");
    authenticateUser("noreaduser");

    try {
      underTest.exportAuditLog(null, null, null, null, null, null);
      fail("Expected AuthorizationException for insufficient permissions");
    }
    catch (org.apache.shiro.authz.AuthorizationException e) {
      // Expected - user has write but not read permission
    }
  }

  private void authenticateUser(String username) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection(username, realm.getName());
    Session session = new SimpleSession();
    DelegatingSubject subject = new DelegatingSubject(principals, true, "localhost", session, securityManager);
    ThreadContext.bind(subject);
  }

  private AuditEventData createAuditEvent(
      long id,
      String domain,
      String type,
      String context,
      OffsetDateTime timestamp,
      String initiator,
      String nodeId,
      Map<String, Object> attributes)
  {
    AuditEventData data = new AuditEventData();
    data.setId(id);
    data.setDomain(domain);
    data.setType(type);
    data.setContext(context);
    data.setTimestamp(timestamp);
    data.setInitiator(initiator);
    data.setNodeId(nodeId);
    data.setAttributes(attributes);
    return data;
  }

  /**
   * Test realm that allows adding accounts with permissions
   */
  private static class TestRealm
      extends SimpleAccountRealm
  {
    public TestRealm(String name) {
      super(name);
    }

    public void addTestAccount(String username) {
      addTestAccount(username, "nexus:audit:read");
    }

    public void addTestAccount(String username, String... permissionStrings) {
      Set<Permission> permissions = new HashSet<>();
      for (String permission : permissionStrings) {
        permissions.add(new WildcardPermission(permission));
      }
      SimpleAccount account =
          new SimpleAccount(username, "password", getName(), Collections.emptySet(), permissions);
      add(account);
    }
  }
}

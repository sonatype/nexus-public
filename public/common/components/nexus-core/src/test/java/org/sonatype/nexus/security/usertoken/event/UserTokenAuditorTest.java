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
package org.sonatype.nexus.security.usertoken.event;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserTokenAuditorTest
    extends TestSupport
{
  @Mock
  private AuditRecorder auditRecorder;

  private UserTokenAuditor underTest;

  @Before
  public void setup() {
    underTest = new UserTokenAuditor();
    // Use reflection to set the audit recorder since it's protected
    try {
      java.lang.reflect.Field field = underTest.getClass().getSuperclass().getDeclaredField("auditRecorder");
      field.setAccessible(true);
      field.set(underTest, auditRecorder);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to set audit recorder", e);
    }

    // Enable recording
    when(auditRecorder.isEnabled()).thenReturn(true);
  }

  @Test
  public void testAdminCreatedEvent_extractsAttributes() {
    // Arrange
    UserTokenAdminCreatedEvent event = new UserTokenAdminCreatedEvent(
        "targetUser",
        "NexusAuthenticatingRealm",
        "adminUser",
        "NexusAuthorizingRealm",
        "abc123");

    // Act
    underTest.on(event);

    // Assert
    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("userToken"));
    assertThat(auditData.getType(), is("adminCreated"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("targetUserId"));
    assertThat(attributes.get("targetUserId"), is("targetUser"));
    assertThat(attributes, hasKey("targetRealm"));
    assertThat(attributes.get("targetRealm"), is("NexusAuthenticatingRealm"));
    assertThat(attributes, hasKey("adminUserId"));
    assertThat(attributes.get("adminUserId"), is("adminUser"));
    assertThat(attributes, hasKey("adminRealm"));
    assertThat(attributes.get("adminRealm"), is("NexusAuthorizingRealm"));
    assertThat(attributes, hasKey("nameCode"));
    assertThat(attributes.get("nameCode"), is("abc123"));
  }

  @Test
  public void testAdminListedEvent_extractsAttributesWithFilters() {
    // Arrange
    UserTokenAdminListedEvent event = new UserTokenAdminListedEvent(
        "adminUser",
        "NexusAuthorizingRealm",
        "NexusAuthenticatingRealm",
        "testuser",
        true,
        5);

    // Act
    underTest.on(event);

    // Assert
    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("userToken"));
    assertThat(auditData.getType(), is("adminListed"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("adminUserId"));
    assertThat(attributes.get("adminUserId"), is("adminUser"));
    assertThat(attributes, hasKey("adminRealm"));
    assertThat(attributes.get("adminRealm"), is("NexusAuthorizingRealm"));
    assertThat(attributes, hasKey("realmFilter"));
    assertThat(attributes.get("realmFilter"), is("NexusAuthenticatingRealm"));
    assertThat(attributes, hasKey("userIdFilter"));
    assertThat(attributes.get("userIdFilter"), is("testuser"));
    assertThat(attributes, hasKey("includeExpired"));
    assertThat(attributes.get("includeExpired"), is(true));
    assertThat(attributes, hasKey("resultCount"));
    assertThat(attributes.get("resultCount"), is(5));
  }

  @Test
  public void testAdminListedEvent_withNullFilters() {
    // Arrange - no filters provided
    UserTokenAdminListedEvent event = new UserTokenAdminListedEvent(
        "adminUser",
        "NexusAuthorizingRealm",
        null,
        null,
        false,
        10);

    // Act
    underTest.on(event);

    // Assert
    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    Map<String, Object> attributes = auditData.getAttributes();

    // Should not include null filters
    assertThat(attributes, not(hasKey("realmFilter")));
    assertThat(attributes, not(hasKey("userIdFilter")));

    // Should include non-null values
    assertThat(attributes, hasKey("adminUserId"));
    assertThat(attributes, hasKey("includeExpired"));
    assertThat(attributes, hasKey("resultCount"));
  }

  @Test
  public void testAdminReadEvent_extractsAttributes() {
    // Arrange
    UserTokenAdminReadEvent event = new UserTokenAdminReadEvent(
        "targetUser",
        "NexusAuthenticatingRealm",
        "adminUser",
        "NexusAuthorizingRealm");

    // Act
    underTest.on(event);

    // Assert
    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("userToken"));
    assertThat(auditData.getType(), is("adminRead"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("targetUserId"));
    assertThat(attributes.get("targetUserId"), is("targetUser"));
    assertThat(attributes, hasKey("targetRealm"));
    assertThat(attributes.get("targetRealm"), is("NexusAuthenticatingRealm"));
    assertThat(attributes, hasKey("adminUserId"));
    assertThat(attributes.get("adminUserId"), is("adminUser"));
    assertThat(attributes, hasKey("adminRealm"));
    assertThat(attributes.get("adminRealm"), is("NexusAuthorizingRealm"));
  }

  @Test
  public void testAdminDeletedEvent_extractsAttributes() {
    // Arrange
    UserTokenAdminDeletedEvent event = new UserTokenAdminDeletedEvent(
        "targetUser",
        "NexusAuthenticatingRealm",
        "adminUser",
        "NexusAuthorizingRealm");

    // Act
    underTest.on(event);

    // Assert
    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("userToken"));
    assertThat(auditData.getType(), is("adminDeleted"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("targetUserId"));
    assertThat(attributes.get("targetUserId"), is("targetUser"));
    assertThat(attributes, hasKey("targetRealm"));
    assertThat(attributes.get("targetRealm"), is("NexusAuthenticatingRealm"));
    assertThat(attributes, hasKey("adminUserId"));
    assertThat(attributes.get("adminUserId"), is("adminUser"));
    assertThat(attributes, hasKey("adminRealm"));
    assertThat(attributes.get("adminRealm"), is("NexusAuthorizingRealm"));
  }
}

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
package org.sonatype.nexus.orient.internal.rest;

import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseFreezeResource}
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseFreezeResourceTest
{
  @Mock
  SecurityHelper securityHelper;

  @Mock
  DatabaseFreezeService freezeService;

  @Mock
  Subject subject;

  DatabaseFreezeResource resource;

  @Before
  public void setup() {
    resource = new DatabaseFreezeResource(freezeService, securityHelper);
    when(securityHelper.subject()).thenReturn(subject);
  }

  @Test
  public void freeze_unauthorized() {
    try {
      resource.freeze();
      fail("expected WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), equalTo(401));
    }
  }

  @Test
  public void freeze_blocked() {
    when(subject.getPrincipal()).thenReturn("username");
    try {
      resource.freeze();
      fail("expected WebApplicationException");
    } catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), equalTo(404));
    }
  }

  @Test
  public void freeze_success() {
    when(subject.getPrincipal()).thenReturn("username");
    FreezeRequest request = mock(FreezeRequest.class);
    when(freezeService.requestFreeze(InitiatorType.USER_INITIATED, "username")).thenReturn(request);
    resource.freeze();
  }

  @Test
  public void release_successful() {
    when(freezeService.releaseUserInitiatedIfPresent()).thenReturn(true);
    resource.release();
  }

  @Test (expected = WebApplicationException.class)
  public void release_notfrozen() {
    resource.release();
  }

  @Test (expected = WebApplicationException.class)
  public void forceRelease_failed() {
    resource.forceRelease();
  }

  @Test
  public void forceRelease_successful() {
    when(freezeService.releaseAllRequests()).thenReturn(Arrays.asList(mock(FreezeRequest.class)));
    resource.forceRelease();
  }
}

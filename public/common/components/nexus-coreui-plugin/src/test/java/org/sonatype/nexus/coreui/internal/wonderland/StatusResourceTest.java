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
package org.sonatype.nexus.coreui.internal.wonderland;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class StatusResourceTest
    extends Test5Support
{
  @Mock
  private ApplicationVersion applicationVersion;

  @InjectMocks
  private StatusResource underTest;

  @Test
  void getReturnsVersionAndEdition() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("PRO");

    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is("3.70.0-01"));
    assertThat(result.getEdition(), is("PRO"));
    verify(applicationVersion).getVersion();
    verify(applicationVersion).getEdition();
  }

  @Test
  void getReturnsOssEdition() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("OSS");

    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is("3.70.0-01"));
    assertThat(result.getEdition(), is("OSS"));
  }

  @Test
  void getHandlesNullValues() {
    when(applicationVersion.getVersion()).thenReturn(null);
    when(applicationVersion.getEdition()).thenReturn(null);

    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is((String) null));
    assertThat(result.getEdition(), is((String) null));
  }
}

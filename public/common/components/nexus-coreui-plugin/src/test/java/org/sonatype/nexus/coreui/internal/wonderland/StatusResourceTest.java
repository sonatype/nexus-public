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

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class StatusResourceTest
{
  @Mock
  private ApplicationVersion applicationVersion;

  private StatusResource underTest;

  @AfterEach
  void tearDown() {
    NexusDistribution.clearTestOverride();
  }

  @Test
  void getReturnsVersionAndEdition() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("PRO");
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is("3.70.0-01"));
    assertThat(result.getEdition(), is("PRO"));
    assertThat(result.getIsCloud(), is(false));
    verify(applicationVersion).getVersion();
    verify(applicationVersion).getEdition();
  }

  @Test
  void getReturnsProPlusEditionUnchanged() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("PROPLUS");
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result.getEdition(), is("PROPLUS"));
    assertThat(result.getIsCloud(), is(false));
    verify(applicationVersion).getEdition();
  }

  @Test
  void getReturnsEnterpriseEditionUnchanged() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("ENTERPRISE");
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result.getEdition(), is("ENTERPRISE"));
    assertThat(result.getIsCloud(), is(false));
    verify(applicationVersion).getEdition();
  }

  @Test
  void getReturnsOssEdition() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    when(applicationVersion.getEdition()).thenReturn("OSS");
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is("3.70.0-01"));
    assertThat(result.getEdition(), is("OSS"));
    assertThat(result.getIsCloud(), is(false));
  }

  @Test
  void getReturnsIsCloudTrueForCloudDistribution() {
    when(applicationVersion.getVersion()).thenReturn("3.70.0-01");
    // On cloud the reported edition is the resolved licensed tier (e.g. Pro); isCloud comes from the
    // distribution, not the edition id (NEXUS-54766 / NEXUS-54772).
    when(applicationVersion.getEdition()).thenReturn("PRO");
    NexusDistribution.setTestOverride(NexusDistributionType.CLOUD);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is("3.70.0-01"));
    assertThat(result.getEdition(), is("PRO"));
    assertThat(result.getIsCloud(), is(true));
  }

  @Test
  void getHandlesNullValues() {
    when(applicationVersion.getVersion()).thenReturn(null);
    when(applicationVersion.getEdition()).thenReturn(null);
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);

    underTest = new StatusResource(applicationVersion);
    StatusXO result = underTest.get();

    assertThat(result, is(notNullValue()));
    assertThat(result.getVersion(), is((String) null));
    assertThat(result.getEdition(), is((String) null));
    assertThat(result.getIsCloud(), is(false));
  }
}

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
package org.sonatype.nexus.proxy.repository;

import java.util.List;

import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Lists;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AbstractGroupRepositoryConfiguration} UTs.
 *
 * @since 2.2
 */
public class AnAbstractGroupRepositoryConfigurationTest
    extends TestSupport
{

  @Test
  public void sameMemberMultipleTime() {
    final Xpp3Dom xpp3Dom = mock(Xpp3Dom.class);
    final ApplicationConfiguration applicationConfiguration = mock(ApplicationConfiguration.class);
    final Configuration configuration = mock(Configuration.class);
    final CRepository repo1 = new CRepository();
    repo1.setId("1");
    final CRepository repo2 = new CRepository();
    repo2.setId("2");

    when(applicationConfiguration.getConfigurationModel()).thenReturn(configuration);
    when(configuration.getRepositories()).thenReturn(Lists.newArrayList(repo1, repo2));

    final AbstractGroupRepositoryConfiguration underTest = new AbstractGroupRepositoryConfiguration(
        xpp3Dom
    )
    {
      @Override
      public List<String> getMemberRepositoryIds() {
        return Lists.newArrayList("1", "1");
      }
    };

    final ValidationResponse validationResponse = underTest.doValidateChanges(
        applicationConfiguration, mock(CoreConfiguration.class), xpp3Dom
    );

    assertThat(validationResponse.isValid(), is(false));
    assertThat(validationResponse.getValidationErrors().size(), is(1));
    assertThat(
        validationResponse.getValidationErrors().get(0).getMessage(),
        is("Group repository has same member multiple times!")
    );
  }

}

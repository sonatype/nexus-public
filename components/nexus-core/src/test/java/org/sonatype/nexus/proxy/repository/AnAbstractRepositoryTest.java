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

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

/**
 * Tests for {@link AbstractRepository}
 *
 * @since 2.2
 */
public class AnAbstractRepositoryTest
    extends TestSupport
{

  private AbstractRepository absRepo = new AbstractRepository()
  {
    @Override
    protected Configurator getConfigurator() {
      return null;
    }

    @Override
    protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
      return null;
    }

    @Override
    public RepositoryKind getRepositoryKind() {
      return null;
    }

    @Override
    public ContentClass getRepositoryContentClass() {
      return null;
    }
  };

  @Test
  public void getExternalConfigurationReturnsNullWhenNotConfigured() {
    absRepo.getExternalConfiguration(false);
    absRepo.getExternalConfiguration(true);
  }
}

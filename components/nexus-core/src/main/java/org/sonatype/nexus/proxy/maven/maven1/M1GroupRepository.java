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
package org.sonatype.nexus.proxy.maven.maven1;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.maven.AbstractMavenGroupRepository;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M1ArtifactRecognizer;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @deprecated To be removed once Maven1 support is removed.
 */
@Deprecated
@Named(M1GroupRepository.ID)
@Typed(GroupRepository.class)
@Description("Maven1 Repository Group")
public class M1GroupRepository
    extends AbstractMavenGroupRepository
{
  /**
   * This "mimics" the @Named("maven1")
   */
  public static final String ID = Maven1ContentClass.ID;

  private final ContentClass contentClass;

  private final GavCalculator gavCalculator;

  private final M1GroupRepositoryConfigurator m1GroupRepositoryConfigurator;

  @Inject
  public M1GroupRepository(final @Named(Maven1ContentClass.ID) ContentClass contentClass, 
                           final @Named("maven1") GavCalculator gavCalculator,
                           final M1GroupRepositoryConfigurator m1GroupRepositoryConfigurator)
  {
    this.contentClass = checkNotNull(contentClass);
    this.gavCalculator = checkNotNull(gavCalculator);
    this.m1GroupRepositoryConfigurator = checkNotNull(m1GroupRepositoryConfigurator);
  }

  @Override
  protected M1GroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (M1GroupRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<M1GroupRepositoryConfiguration> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<M1GroupRepositoryConfiguration>()
    {
      public M1GroupRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new M1GroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  public GavCalculator getGavCalculator() {
    return gavCalculator;
  }

  @Override
  protected Configurator getConfigurator() {
    return m1GroupRepositoryConfigurator;
  }

  public boolean isMavenMetadataPath(String path) {
    return M1ArtifactRecognizer.isMetadata(path);
  }
}

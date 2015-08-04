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
package org.sonatype.nexus.proxy.maven.maven2;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.maven.LayoutConverterShadowRepository;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.ShadowRepository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A shadow repository that transforms M1 content hierarchy of master to M2 layouted shadow.
 *
 * @author cstamas
 */
@Named(M2LayoutedM1ShadowRepository.ID)
@Typed(ShadowRepository.class)
@Description("Maven1 to Maven2")
public class M2LayoutedM1ShadowRepository
    extends LayoutConverterShadowRepository
{
  /**
   * This "mimics" the @Named("m1-m2-shadow")
   */
  public static final String ID = "m1-m2-shadow";

  private final ContentClass contentClass;

  private final ContentClass masterContentClass;

  private final M2LayoutedM1ShadowRepositoryConfigurator m2LayoutedM1ShadowRepositoryConfigurator;

  @Inject
  public M2LayoutedM1ShadowRepository(final @Named(Maven2ContentClass.ID) ContentClass contentClass, 
                                      final @Named(Maven1ContentClass.ID) ContentClass masterContentClass,
                                      final M2LayoutedM1ShadowRepositoryConfigurator m2LayoutedM1ShadowRepositoryConfigurator)
  {
    this.contentClass = checkNotNull(contentClass);
    this.masterContentClass = checkNotNull(masterContentClass);
    this.m2LayoutedM1ShadowRepositoryConfigurator = checkNotNull(m2LayoutedM1ShadowRepositoryConfigurator);
  }

  @Override
  protected M2LayoutedM1ShadowRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (M2LayoutedM1ShadowRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<M2LayoutedM1ShadowRepositoryConfiguration>()
    {
      public M2LayoutedM1ShadowRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new M2LayoutedM1ShadowRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public GavCalculator getGavCalculator() {
    return getM2GavCalculator();
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  public ContentClass getMasterRepositoryContentClass() {
    return masterContentClass;
  }

  @Override
  protected Configurator getConfigurator() {
    return m2LayoutedM1ShadowRepositoryConfigurator;
  }

  @Override
  protected List<String> transformMaster2Shadow(final String path) {
    return transformM1toM2(path);
  }

  @Override
  protected List<String> transformShadow2Master(final String path) {
    return transformM2toM1(path, Collections.singletonList("ejbs"));
  }

  @Override
  public boolean isMavenMetadataPath(final String path) {
    return M2ArtifactRecognizer.isMetadata(path);
  }
}

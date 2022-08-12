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
package org.sonatype.nexus.content.maven.internal;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.search.DefaultSearchDocumentProducer;
import org.sonatype.nexus.repository.content.search.SearchDocumentExtension;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.search.MavenVersionNormalizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven implementation of {@link DefaultSearchDocumentProducer}
 *
 * @since 3.26
 */
@Singleton
@Named(Maven2Format.NAME)
public class MavenSearchDocumentProducer
    extends DefaultSearchDocumentProducer
{
  private final MavenVersionNormalizer versionNormalizer;

  private final MavenPreReleaseEvaluator preReleaseEvaluator;

  @Inject
  public MavenSearchDocumentProducer(
      final Set<SearchDocumentExtension> documentExtensions,
      final MavenVersionNormalizer versionNormalizer,
      final MavenPreReleaseEvaluator preReleaseEvaluator)
  {
    super(documentExtensions);
    this.versionNormalizer = checkNotNull(versionNormalizer);
    this.preReleaseEvaluator = checkNotNull(preReleaseEvaluator);
  }

  @Override
  protected boolean isPrerelease(final FluentComponent component) {
    return preReleaseEvaluator.isPreRelease(component);
  }

  @Override
  protected String getNormalizedVersion(final FluentComponent component) {
    return versionNormalizer.getNormalizedVersion(component.version());
  }
}

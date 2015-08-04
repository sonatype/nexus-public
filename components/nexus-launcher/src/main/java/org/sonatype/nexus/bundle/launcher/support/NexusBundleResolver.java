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
package org.sonatype.nexus.bundle.launcher.support;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.sisu.bl.support.resolver.MavenBridgedBundleResolver;
import org.sonatype.sisu.maven.bridge.MavenArtifactResolver;

/**
 * Default Nexus bundle configuration.
 *
 * @since 2.0
 */
@Named
@NexusSpecific
public class NexusBundleResolver
    extends MavenBridgedBundleResolver
{

  /**
   * Bundle coordinates configuration property key.
   */
  public static final String BUNDLE_COORDINATES = "nexus.launcher.bundleCoordinates";

  /**
   * Constructor.
   *
   * @param bundleCoordinates Maven artifact coordinates of bundle to be resolved. If injected will use the
   *                          coordinates bounded to {@link #BUNDLE_COORDINATES}
   * @param artifactResolver  artifact resolver to be used to resolve the bundle
   * @since 2.1
   */
  @Inject
  public NexusBundleResolver(final @Nullable @Named("${" + BUNDLE_COORDINATES + "}") String bundleCoordinates,
                             final MavenArtifactResolver artifactResolver)
  {
    super(bundleCoordinates, artifactResolver);
  }

}

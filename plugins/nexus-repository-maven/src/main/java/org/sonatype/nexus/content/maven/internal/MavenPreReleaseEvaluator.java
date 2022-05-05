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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Constants.SNAPSHOT_VERSION_SUFFIX;

/**
 * @since 3.38
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenPreReleaseEvaluator
    implements PreReleaseEvaluator
{

  @Override
  public boolean isPreRelease(final FluentComponent component) {
    return isPreRelease((Component) component);
  }

  @Override
  public boolean isPreRelease(final Component component, final Iterable<Asset> assets) {
    return isPreRelease(component);
  }

  private static boolean isPreRelease(final Component component) {
    String baseVersion = component.attributes().child(Maven2Format.NAME).get(P_BASE_VERSION, String.class);
    if (baseVersion == null) {
      return false;
    }
    return baseVersion.endsWith(SNAPSHOT_VERSION_SUFFIX);
  }
}

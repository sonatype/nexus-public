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
package org.sonatype.nexus.util;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.eclipse.sisu.Parameters;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ApplicationInterpolatorProvider}.
 */
@Named
@Singleton
public class DefaultApplicationInterpolatorProvider
    implements ApplicationInterpolatorProvider
{
  private final RegexBasedInterpolator interpolator;

  @Inject
  public DefaultApplicationInterpolatorProvider(final @Parameters Map<String, String> parameters) {
    checkNotNull(parameters);
    interpolator = new RegexBasedInterpolator();
    interpolator.addValueSource(new MapBasedValueSource(parameters));
    interpolator.addValueSource(new MapBasedValueSource(System.getenv()));
    interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
  }

  public Interpolator getInterpolator() {
    return interpolator;
  }
}

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
package org.sonatype.nexus.internal.selector;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.internal.status.HealthCheckComponentSupport;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * Health check that indicates if any content selectors use coordinates.
 * <p>
 * This warns about them since we will be deprecating them.
 *
 * @since 3.31
 */
@Named("Coordinate Content Selectors")
@Singleton
public class CoordinateContentSelectorHealthCheck
    extends HealthCheckComponentSupport
{
  private static final String SELECTORS = "selectors";

  private static final String SELECTOR = "selector";

  private static final String HEALTHY_MESSAGE = "No content selectors using coordinates found.";

  private static final String UNHEALTHY_MESSAGE =
      "Found %d content %s with coordinates, these will be deprecated soon: %s";

  private final SelectorManager selectorManager;

  @Inject
  public CoordinateContentSelectorHealthCheck(final SelectorManager selectorManager) {
    this.selectorManager = checkNotNull(selectorManager);
  }

  @Override
  protected Result check() {
    List<SelectorConfiguration> coordinateSelectors = selectorManager.browse()
        .stream()
        .filter(SelectorConfiguration::hasCoordinates)
        .collect(toList());

    if (coordinateSelectors.isEmpty()) {
      return Result.healthy(HEALTHY_MESSAGE);
    }
    else {
      String coordinateSelectorNames = coordinateSelectors.stream()
          .map(SelectorConfiguration::getName)
          .sorted()
          .collect(joining(", "));
      return Result.unhealthy(UNHEALTHY_MESSAGE,
          coordinateSelectors.size(),
          coordinateSelectors.size() > 1 ? SELECTORS : SELECTOR,
          truncate(coordinateSelectorNames, 144));
    }
  }
}

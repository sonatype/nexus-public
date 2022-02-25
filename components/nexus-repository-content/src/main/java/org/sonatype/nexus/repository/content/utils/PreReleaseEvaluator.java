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
package org.sonatype.nexus.repository.content.utils;

import java.util.stream.Collectors;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

/**
 * A format specifc evaluator to determine whether the component indicates a pre-release version.
 * Implementations are format specific.
 *
 * Note: an implementor which is not concerned with a component's assets should override both methods which may avoid a
 * database query.
 *
 * @since 3.38
 */
public interface PreReleaseEvaluator
{
  /**
   * Evaluates whether the component or its assets are considered to be a pre-release version.
   * Implementations are format specific.
   */
  default boolean isPreRelease(final FluentComponent component) {
    return isPreRelease(component, component.assets().stream().map(Asset.class::cast).collect(Collectors.toList()));
  }

  /**
   * Evaluates whether the component or the assets are considered to be a pre-release version.
   * Implementations are format specific.
   */
  boolean isPreRelease(Component component, Iterable<Asset> assets);
}

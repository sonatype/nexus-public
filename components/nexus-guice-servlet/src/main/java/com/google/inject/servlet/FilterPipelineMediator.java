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
package com.google.inject.servlet;

import java.lang.annotation.Annotation;

import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

/**
 * Updates the associated {@link DynamicFilterPipeline} as {@link FilterPipeline} bindings come and go.
 */
final class FilterPipelineMediator
    implements Mediator<Annotation, FilterPipeline, DynamicFilterPipeline>
{
  public void add(final BeanEntry<Annotation, FilterPipeline> entry, final DynamicFilterPipeline watcher)
      throws Exception
  {
    // initialize pipeline before exposing via cache
    final FilterPipeline pipeline = entry.getValue();
    pipeline.initPipeline(watcher.getServletContext());
    watcher.refreshCache();
  }

  public void remove(final BeanEntry<Annotation, FilterPipeline> entry, final DynamicFilterPipeline watcher)
      throws Exception
  {
    // remove pipeline from cache before disposing
    final FilterPipeline pipeline = entry.getValue();
    watcher.refreshCache();
    pipeline.destroyPipeline();
  }
}
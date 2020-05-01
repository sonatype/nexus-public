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
package org.sonatype.nexus.repository.pypi.internal.search;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;

import com.google.common.collect.ImmutableList;

/**
 * @since 3.7
 */
@Named("pypi")
@Singleton
public class PyPiSearchMappings
    extends ComponentSupport
    implements SearchMappings
{
  private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
      new SearchMapping("pypi.classifiers", "assets.attributes.pypi.classifiers", "PyPI classifiers"),
      new SearchMapping("pypi.description", "assets.attributes.pypi.description", "PyPI description"),
      new SearchMapping("pypi.keywords", "assets.attributes.pypi.keywords", "PyPI keywords"),
      new SearchMapping("pypi.summary", "assets.attributes.pypi.summary", "PyPI summary")
  );

  @Override
  public Iterable<SearchMapping> get() {
    return MAPPINGS;
  }
}

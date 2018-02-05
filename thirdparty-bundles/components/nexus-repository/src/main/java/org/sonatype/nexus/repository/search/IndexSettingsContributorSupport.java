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
package org.sonatype.nexus.repository.search;

import java.net.URL;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;

import com.google.common.io.Resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.SearchServiceImpl.MAPPING_JSON;

// TODO: Can probably simplify and avoid intf/impl here for such simple configurator

/**
 * Support for {@link IndexSettingsContributor} implementations.
 *
 * @since 3.0
 */
public class IndexSettingsContributorSupport
  extends ComponentSupport
  implements IndexSettingsContributor
{
  private final Format format;

  public IndexSettingsContributorSupport(final Format format) {
    this.format = checkNotNull(format);
  }

  @Override
  @Nullable
  public URL getIndexSettings(final Repository repository) {
    checkNotNull(repository);
    if (format.equals(repository.getFormat())) {
      return Resources.getResource(getClass(), MAPPING_JSON);
    }
    return null;
  }
}

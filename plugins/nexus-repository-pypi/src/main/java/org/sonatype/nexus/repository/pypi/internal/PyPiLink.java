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
package org.sonatype.nexus.repository.pypi.internal;

import javax.annotation.Nonnull;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Container for URL PEP 503 compatible urls.
 *
 * @since 3.20
 */
public final class PyPiLink
{
  private final String file;

  private final String link;

  private final String dataRequiresPython;

  public PyPiLink(@Nonnull final String file,
           @Nonnull final String link,
           final String dataRequiresPython) {
    this.file = checkNotNull(file);
    this.link = checkNotNull(link);
    this.dataRequiresPython = dataRequiresPython != null ? dataRequiresPython : "";
  }

  public PyPiLink(final String file, final String link) {
    this(file, link, "");
  }

  public String getLink() {
    return link;
  }

  public String getFile() {
    return file;
  }

  public String getDataRequiresPython() {
    return dataRequiresPython;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PyPiLink pyPiLink = (PyPiLink) o;
    return Objects.equal(file, pyPiLink.file) &&
        Objects.equal(link, pyPiLink.link) &&
        Objects.equal(dataRequiresPython, pyPiLink.dataRequiresPython);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(file, link, dataRequiresPython);
  }
}

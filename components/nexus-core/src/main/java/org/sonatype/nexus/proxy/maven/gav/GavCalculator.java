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
package org.sonatype.nexus.proxy.maven.gav;

/**
 * An interface to calculate <code>Gav</code> based on provided artifact path and to calculate an artifact path from
 * provided <code>Gav</code>.
 *
 * @author Tamas Cservenak
 */
public interface GavCalculator
{
  /**
   * Calculates GAV from provided <em>repository path</em>. The path has to be absolute starting from repository
   * root.
   * If path represents a proper artifact path (conforming to given layout), GAV is "calculated" from it and is
   * returned. If path represents some file that is not an artifact, but is part of the repository layout (like
   * maven-metadata.xml), or in any other case it returns null. TODO: some place for different levels of
   * "validation"?
   *
   * @param path the repository path
   * @return Gav parsed from the path
   */
  Gav pathToGav(String path);

  /**
   * Reassembles the repository path from the supplied GAV. It will be an absolute path.
   *
   * @return the path calculated from GAV, obeying current layout.
   */
  String gavToPath(Gav gav);
}

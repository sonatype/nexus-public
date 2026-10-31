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
package org.sonatype.nexus.bootstrap.entrypoint.edition;

import java.util.List;

public interface NexusEdition
{
  String getId();

  String getName();

  /**
   * Short, display-oriented name for the edition.
   * <p>
   * <b>Not a unique identifier.</b> Professional and Cloud both return {@code "PRO"}, so a
   * short-name comparison cannot tell a self-hosted Pro instance from a Cloud one. Use
   * {@link #getId()} for identity. Note that reading the {@code nexus.edition} property as a
   * {@code String} yields this short name rather than the id, because a converter is registered
   * for it during bootstrap.
   */
  String getShortName();

  int getPriority();

  boolean isActive();

  List<String> getModules();
}

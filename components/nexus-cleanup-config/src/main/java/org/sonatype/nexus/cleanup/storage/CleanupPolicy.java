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
package org.sonatype.nexus.cleanup.storage;

import java.util.Map;

/**
 * Represents a configured cleanup policy
 *
 * @since 3.14
 */
public interface CleanupPolicy //NOSONAR
{
  String ALL_CLEANUP_POLICY_FORMAT = "ALL_FORMATS";
  String ALL_FORMATS = "*";

  String getName();

  void setName(final String name);

  String getNotes();

  void setNotes(final String notes);

  String getFormat();

  void setFormat(final String format);

  String getMode();

  void setMode(final String mode);

  Map<String, String> getCriteria();

  void setCriteria(final Map<String, String> criteria);
}

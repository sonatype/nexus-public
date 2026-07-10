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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Map;

/**
 * Interface for encoding/decoding repository secrets in configuration attributes.
 * Implementations handle format-specific secret encryption (e.g., Docker ECR credentials).
 */
public interface SecretEncoder
{
  /**
   * Encode secrets in repository attributes before storage.
   *
   * @param attributes repository configuration attributes
   */
  void encodeSecrets(Map<String, Map<String, Object>> attributes);

  /**
   * Encode secrets when updating repository, comparing old and new attributes.
   *
   * @param oldAttributes previous configuration attributes
   * @param newAttributes new configuration attributes
   */
  void encodeSecrets(
      Map<String, Map<String, Object>> oldAttributes,
      Map<String, Map<String, Object>> newAttributes);

  /**
   * Remove secrets from secret store when repository is deleted.
   *
   * @param attributes repository configuration attributes
   */
  void removeSecret(Map<String, Map<String, Object>> attributes);

  /**
   * Remove old secrets when repository is updated.
   *
   * @param oldAttributes previous configuration attributes
   * @param newAttributes new configuration attributes
   */
  void removeSecret(
      Map<String, Map<String, Object>> oldAttributes,
      Map<String, Map<String, Object>> newAttributes);
}

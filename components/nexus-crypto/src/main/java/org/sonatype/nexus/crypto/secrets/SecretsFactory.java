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
package org.sonatype.nexus.crypto.secrets;

/**
 * Provides {@link Secret} instances which are backed by services for retrieval and decryption of secrets by their id.
 */
public interface SecretsFactory
{
  /**
   * <p>Create a {@link Secret} which can be used to retrieve and decrypt a persisted secret by id. For legacy reasons
   * this may also be a persisted secret.</p>
   *
   * <p>Existence of the provided token will not be verified until decryption</p>
   *
   * @param token an identifier previously provided by {@link SecretsService} or a legacy encrypted secret.
   *
   * @return a secret for the provided value.
   */
  Secret from(String token);
}

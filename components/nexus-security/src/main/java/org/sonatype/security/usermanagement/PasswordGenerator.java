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
package org.sonatype.security.usermanagement;

/**
 * Generates passwords for users.
 *
 * @author Brian Demers
 */
public interface PasswordGenerator
{
  /**
   * Generates a password.
   *
   * @param minChars the minimum number of characters in the password.
   * @param maxChars the maximum number of characters in the password.
   */
  String generatePassword(int minChars, int maxChars);

  /**
   * Hash a password String.
   *
   * @param password to be hashed.
   * @return the hash password String.
   * @deprecated use only to generate legacy unsalted password hashes
   */
  String hashPassword(String password);
}

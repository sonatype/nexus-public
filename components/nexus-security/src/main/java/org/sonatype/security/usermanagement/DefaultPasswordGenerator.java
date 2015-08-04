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

import java.util.Random;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default implementation of PasswordGenerator.
 */
@Singleton
@Typed(PasswordGenerator.class)
@Named("default")
public class DefaultPasswordGenerator
    implements PasswordGenerator
{
  private int getRandom(int min, int max) {
    Random random = new Random();
    int total = max - min + 1;
    int next = Math.abs(random.nextInt() % total);

    return min + next;
  }

  @Override
  public String generatePassword(int minChars, int maxChars) {
    int length = getRandom(minChars, maxChars);

    byte bytes[] = new byte[length];

    for (int i = 0; i < length; i++) {
      if (i % 2 == 0) {
        bytes[i] = (byte) getRandom('a', 'z');
      }
      else {
        bytes[i] = (byte) getRandom('0', '9');
      }
    }

    return new String(bytes);
  }

  @Override
  public String hashPassword(String password) {
    return StringDigester.getSha1Digest(password);
  }
}

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
package org.sonatype.nexus.common.cooperation2;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import com.google.common.hash.Hashing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a key used for {@link Cooperation2}, includes the original human readable string, and a reduced length
 * hash useful for storing in a database.
 *
 * @since 3.41
 */
public class CooperationKey
{
  private final String humanReadable;

  private final String derivedKey;

  private CooperationKey(final String derivedkey, final String humanReadable) {
    this.humanReadable = checkNotNull(humanReadable);
    this.derivedKey = checkNotNull(derivedkey);
  }

  public String getHashedKey() {
    return derivedKey;
  }

  public String getKey() {
    return humanReadable;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(derivedKey);
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof CooperationKey) {
      CooperationKey o = (CooperationKey) other;
      return Objects.equals(this.derivedKey, o.derivedKey);
    }
    return false;
  }

  @Override
  public String toString() {
    return humanReadable + "\n\tkey:" + derivedKey;
  }

  /**
   * Creates a {@code CooperationKey} representing the provided scope, action, and optional nested scopes.
   */
  public static CooperationKey create(final String scope, final String action, final String... nestedScope) {
    StringJoiner joiner = new StringJoiner(":");
    joiner.add(checkNotNull(scope, "scope may not be null"));
    joiner.add(checkNotNull(action, "action may not be null"));
    Arrays.asList(nestedScope).forEach(joiner::add);

    String humanReadable = joiner.toString();
    String derivedKey = Hashing.farmHashFingerprint64().hashString(humanReadable, StandardCharsets.UTF_8).toString();

    return new CooperationKey(derivedKey, humanReadable);
  }
}

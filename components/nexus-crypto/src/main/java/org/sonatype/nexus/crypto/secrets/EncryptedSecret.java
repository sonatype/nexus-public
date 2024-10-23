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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Java class representing a secret stored in PHC string format
 * <p>
 * For further info , check <a href="https://github.com/P-H-C/phc-string-format/blob/master/phc-sf-spec.md">PHC String
 * format</a>
 */
public class EncryptedSecret
{
  private static final String DOLLAR_SIGN = "$";

  private static final String COMMA = ",";

  private static final String EQUAL_SIGN = "=";

  private static final String VERSION_KEY = "v";

  private final String algorithm;

  private final String version;

  private final String salt;

  private final String value;

  private final Map<String, String> attributes;

  public EncryptedSecret(
      final String algorithm,
      final String version,
      final String salt,
      final String value,
      final Map<String, String> attributes)
  {
    this.algorithm = checkNotNull(algorithm);
    this.version = version;
    this.salt = checkNotNull(salt);
    this.value = checkNotNull(value);
    this.attributes = attributes;
  }

  /**
   * The algorithm used to encrypt the secret
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /**
   * The algorithm version if present
   */
  public String getVersion() {
    return version;
  }

  /**
   * The random salt used to encrypt this secret.
   */
  public String getSalt() {
    return salt;
  }

  /**
   * Returns the encrypted secret value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns algorithm specific attributes which were used to encrypt this secret
   */
  public Map<String, String> getAttributes() {
    return attributes;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EncryptedSecret encryptedSecret = (EncryptedSecret) o;
    return Objects.equals(algorithm, encryptedSecret.algorithm)
        && Objects.equals(version, encryptedSecret.version)
        && Objects.equals(salt, encryptedSecret.salt)
        && Objects.equals(value, encryptedSecret.value)
        && Objects.equals(attributes, encryptedSecret.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(algorithm, version, salt, value, attributes);
  }

  /**
   * Parses the given PHC formatted string into a {@link EncryptedSecret} instance
   *
   * @param phcString the string to be parsed
   * @return a {@link EncryptedSecret} instance
   * @throws IllegalArgumentException if the specified string is not a valid PHC string
   * @see <a href="https://github.com/P-H-C/phc-string-format/blob/master/phc-sf-spec.md">PHC String format</a>
   */
  public static EncryptedSecret parse(final String phcString) {
    checkNotNull(phcString);

    List<String> chunks = Stream.of(phcString.trim().split("\\$"))
        .filter(chunk -> !chunk.isEmpty())
        .collect(Collectors.toList());

    checkArgument(chunks.size() >= 4 && chunks.size() <= 5, "Not a valid PHC formatted string");

    String algorithm = chunks.get(0);
    String version = findVersion(chunks);
    String attributesStr = findAttributes(chunks);
    String salt = chunks.get(chunks.size() - 2);
    String value = chunks.get(chunks.size() - 1);

    if (version != null) {
      //ensure the version string has the expected format
      checkArgument(version.contains(EQUAL_SIGN), "Not a valid version");
      version = version.substring(2);
    }

    Map<String, String> attributes = new LinkedHashMap<>();

    if (attributesStr != null) {
      //ensure the attributes string has the expected format
      checkArgument(attributesStr.contains(EQUAL_SIGN) || attributesStr.contains(COMMA), "Not a valid attributes map");
      Stream.of(attributesStr.split(COMMA))
          .filter(kv -> !kv.isEmpty())
          .map(kv -> kv.split(EQUAL_SIGN))
          .forEach(kv -> attributes.put(kv[0], kv[1]));
    }

    return new EncryptedSecret(algorithm, version, salt, value, attributes);
  }

  private static String findVersion(final List<String> chunks) {
    if (chunks.size() == 5) {
      //if we have all the params then version should be always the 2nd param
      return chunks.get(1);
    }

    String maybeVersion = chunks.get(1);

    //we know we have a version attribute if there is only one key=val and key is == v
    if (maybeVersion.contains(EQUAL_SIGN) && !maybeVersion.contains(COMMA) &&
        maybeVersion.startsWith(VERSION_KEY + EQUAL_SIGN)) {
      return maybeVersion;
    }

    return null;
  }

  private static String findAttributes(final List<String> chunks) {
    if (chunks.size() == 5) {
      //if we have all the params then attributes should be always the 3rd param
      return chunks.get(2);
    }

    String maybeAttributes = chunks.get(1);

    //we know we have an attributes map if there is a comma, or if there is a singleton map and key is not 'v'
    if (maybeAttributes.contains(COMMA) || !maybeAttributes.startsWith(VERSION_KEY + EQUAL_SIGN)) {
      return maybeAttributes;
    }

    return null;
  }

  /**
   * Converts the current instance into a PHC string
   *
   * @return a {@link String} reference representing the given values in PHC string format
   */
  public String toPhcString() {
    StringBuilder result = new StringBuilder();

    result.append(DOLLAR_SIGN).append(algorithm);

    if (version != null) {
      result.append(DOLLAR_SIGN).append(VERSION_KEY).append(EQUAL_SIGN).append(version);
    }

    if (attributes != null && !attributes.isEmpty()) {
      result.append(DOLLAR_SIGN);
      Iterator<Entry<String, String>> values = attributes.entrySet().iterator();

      while (values.hasNext()) {
        Entry<String, String> entry = values.next();

        result.append(entry.getKey()).append(EQUAL_SIGN).append(entry.getValue());

        if (values.hasNext()) {
          result.append(COMMA);
        }
      }
    }

    result.append(DOLLAR_SIGN).append(salt);
    result.append(DOLLAR_SIGN).append(value);
    return result.toString();
  }
}

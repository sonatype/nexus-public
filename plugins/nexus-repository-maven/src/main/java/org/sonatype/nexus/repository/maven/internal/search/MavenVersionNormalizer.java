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
package org.sonatype.nexus.repository.maven.internal.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizer;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;

import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.content.maven.internal.search.Maven2ComponentFinder.SNAPSHOT_TIMESTAMP;

/**
 * Translates maven version number into a string that when sorted lexically matches some maven rules for version
 * ordering.
 *
 * <p>
 * Source of truth for Maven sorting rules.<br/>
 * https://github.com/apache/maven/blob/master/maven-artifact/src/main/java/org/apache/maven/artifact/versioning/ComparableVersion.java
 * </p>
 *
 * <p>
 * NOTE: This implementation should work correctly for any version string that follows the Maven standards.
 * https://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html
 * </p>
 * <p>
 * There are version strings in the wild that don't conform to the standard. In some cases this algorithm may sort in
 * the same order but in other cases it may not.
 * </p>
 *
 * <p>
 * Examples of Maven sorting order that the normalization process handles differently:
 * <ol>
 * <li>1-alpha2-beta < 1-alpha2 (multiple qualifiers in relation to each other)</li>
 * <li>2.0.a < 2.0.0.a (normalization process considers these the same)</li>
 * <li>2.1.0.1-alpha < 2.1.0.1 (too many numeric version parts)</li>
 * <li>2.1.a < 2.1-a  (???)</li>
 * </ol>
 * </p>
 *
 * @since 3.37
 */
@Named(Maven2Format.NAME)
public class MavenVersionNormalizer
    extends ComponentSupport
    implements VersionNormalizer
{
  // major + minor + patch
  private static final int VERSION_LENGTH = 3;

  private static final Pattern ALPHA_PLUS_NUMERIC_PATTERN = Pattern.compile("^([A-Za-z]+)([0-9]+)?(.*)$");

  public static final String ALPHA = "alpha";

  public static final String BETA = "beta";

  public static final String CR = "cr";

  public static final String FINAL = "final";

  public static final String GA = "ga";

  public static final String MILESTONE = "milestone";

  public static final String RELEASE = "release";

  public static final String RC = "rc";

  public static final String SP = "sp";

  private enum QualifierType
  {
    BEFORE_RELEASE("a"),
    SNAPSHOT("b"),
    RELEASE("c"),
    AFTER_RELEASE("d"),
    UNKNOWN("e"),
    BUILD_NUMBER("f");

    // defines order that each type will appear when sorted
    private final String key;

    QualifierType(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }

  @Override
  public String getNormalizedVersion(final String originalVersion) {
    if (isBlank(originalVersion)) {
      return "";
    }

    String trimmedVersion = originalVersion.trim();
    String[] result = splitQualifier(trimmedVersion);
    String version = result[0];
    String qualifier = result[1];

    String[] versionList = version.split("\\.");
    if (!isRecognizedFormat(versionList)) {
      // just expand versions with no other processing
      return VersionNumberExpander.expand(trimmedVersion);
    }
    else {
      versionList = fillMissingParts(versionList);
    }

    qualifier = standardizeQualifier(qualifier);

    return getNormalizedValue(versionList, qualifier, trimmedVersion);
  }

  // 1 -> 1.0.0
  private String[] fillMissingParts(final String[] parts) {
    if (parts.length == VERSION_LENGTH) {
      return parts;
    }
    String[] result = new String[VERSION_LENGTH];
    for (int i = 0; i < VERSION_LENGTH; i++) {
      if (i < parts.length) {
        result[i] = parts[i];
      }
      else {
        result[i] = "0";
      }
    }
    return result;
  }

  // Splits version into version/qualifier by dividing the string at the first non '.' and non digit character.
  private String[] splitQualifier(final String version) {
    String v = null;
    String q = null;

    int i = 0;
    while (v == null && i < version.length()) {
      char c = version.charAt(i);
      if (c != '.' && !Character.isDigit(c)) {
        v = version.substring(0, i);
        q = version.substring(c == '-' ? i + 1 : i); // remove divider character if it's a '-'
      }
      i++;
    }

    return new String[]{v != null ? v : version, q};
  }

  private String standardizeQualifier(final String originalQualifier) {
    if (originalQualifier == null) {
      return null;
    }

    String qualifier = originalQualifier.toLowerCase();

    // cr is equivalent to rc
    if (qualifier.startsWith(CR)) {
      qualifier = RC + qualifier.substring(2);
    }

    return standardizeAlphaPlusNumericQualifier(qualifier);
  }

  // alpha1 or a1 or a-1 -> alpha-1
  // alpha1release or a1release or a-1-release -> alpha-1-release
  private String standardizeAlphaPlusNumericQualifier(final String qualifier) {
    Matcher matcher = ALPHA_PLUS_NUMERIC_PATTERN.matcher(qualifier);

    if (matcher.find()) {
      String alphaPart = matcher.group(1);
      String numberPart = matcher.group(2);
      String suffix = matcher.group(3);

      if (numberPart != null && !numberPart.equals("")) {
        if (alphaPart.equals("a")) {
          alphaPart = ALPHA;
        }
        else if (alphaPart.equals("b")) {
          alphaPart = BETA;
        }
        else if (alphaPart.equals("m")) {
          alphaPart = MILESTONE;
        }

        String result = alphaPart + "-" + numberPart;
        if (!suffix.equals("")) {
          result = result + "-" + suffix;
        }
        return result;
      }
    }
    return qualifier;
  }

  private QualifierType getQualifierType(final String qualifier, final String originalVersion) {
    if (Strings2.isBlank(qualifier) || qualifier.equals(GA) || qualifier.equals(RELEASE) || qualifier.equals(FINAL)) {
      return QualifierType.RELEASE;
    }
    else if (SNAPSHOT_TIMESTAMP.matcher(originalVersion).matches()) {
      return QualifierType.SNAPSHOT;
    }
    else if (qualifier.startsWith(ALPHA) || qualifier.startsWith(BETA) ||
        qualifier.startsWith(MILESTONE) || qualifier.startsWith(RC)) {
      return QualifierType.BEFORE_RELEASE;
    }
    else if (qualifier.startsWith(SP)) {
      return QualifierType.AFTER_RELEASE;
    }
    else if (Character.isDigit(qualifier.charAt(0))) {
      return QualifierType.BUILD_NUMBER;
    }
    else {
      return QualifierType.UNKNOWN;
    }
  }

  private String getNormalizedValue(final String[] versionList, final String qualifier, final String originalVersion) {
    QualifierType type = getQualifierType(qualifier, originalVersion);

    String v = String.join(".", versionList) + "." + type.getKey();
    if (type != QualifierType.RELEASE) {
      v += "." + qualifier;
    }

    return VersionNumberExpander.expand(v);
  }

  private boolean isRecognizedFormat(final String[] versionList) {
    return versionList.length > 0 && versionList.length <= VERSION_LENGTH && !isBlank(versionList[0]);
  }
}

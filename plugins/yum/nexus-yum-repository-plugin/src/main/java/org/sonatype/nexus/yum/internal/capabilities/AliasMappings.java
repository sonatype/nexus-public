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
package org.sonatype.nexus.yum.internal.capabilities;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.shiro.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since yum 3.0
 */
public class AliasMappings
{

  private Map<String, String> aliases;

  public AliasMappings(final Map<String, String> aliases) {
    this.aliases = Maps.newTreeMap();
    this.aliases.putAll(checkNotNull(aliases));
  }

  public AliasMappings(final String mappings) {
    aliases = parseAliases(mappings);
  }

  public Map<String, String> aliases() {
    return aliases;
  }

  @Override
  public String toString() {
    return StringUtils.join(aliases.entrySet().iterator(), ",");
  }

  private static Map<String, String> parseAliases(final String mappings) {
    final Map<String, String> parsedAliases = Maps.newHashMap();

    if (mappings != null && !mappings.trim().isEmpty()) {
      final String[] segments = mappings.split(",");
      for (final String segment : segments) {
        if (!segment.trim().isEmpty()) {
          final String[] parts = segment.split("=");
          if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid format for entry '" + segment + "'. Expected <alias>=<version>"
            );
          }
          parsedAliases.put(parts[0], parts[1]);
        }
      }
    }

    return parsedAliases;
  }

}

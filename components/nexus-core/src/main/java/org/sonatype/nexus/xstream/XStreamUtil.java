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
package org.sonatype.nexus.xstream;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.sonatype.sisu.goodies.common.SystemProperty;

import com.google.common.annotations.VisibleForTesting;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replicates the behaviour of the old forked version of XStream which used properties to whitelist classes.
 * https://support.sonatype.com/hc/en-us/articles/213464858
 */
public class XStreamUtil
{
  private static final Logger log = LoggerFactory.getLogger(XStreamUtil.class);

  private static final String TYPE_WHITELIST = "com.thoughtworks.xstream.whitelist.TypeWhitelist";

  @VisibleForTesting
  public static final SystemProperty allowAllProperty = new SystemProperty(TYPE_WHITELIST + ".allowAll");

  @VisibleForTesting
  public static final SystemProperty allowedTypesProperty = new SystemProperty(TYPE_WHITELIST + ".allowedTypes");

  @VisibleForTesting
  public static final SystemProperty allowedPackagesProperty = new SystemProperty(TYPE_WHITELIST + ".allowedPackages");

  @VisibleForTesting
  public static final SystemProperty allowedPatternsProperty = new SystemProperty(TYPE_WHITELIST + ".allowedPatterns");

  /**
   * Configures the allowed types.
   */
  public static void configure(XStream xstream) {
    boolean allowAll = allowAllProperty.get(Boolean.class, false);
    if (allowAll) {
      log.warn("Disabling XStream whitelist");
      xstream.allowTypesByWildcard(new String[] {"**"});
    }

    List<String> allowedTypes = allowedTypesProperty.asList();
    log.debug("Setting allowTypes to {}", allowedTypes);
    apply(allowedTypes, xstream::allowTypes);

    List<String> allowedPackages = allowedPackagesProperty.asList().stream()
        .map(pkg -> pkg + '*')
        .collect(Collectors.toList());
    log.debug("Setting allowTypesByWildcard to {}", allowedPackages);
    apply(allowedPackages, xstream::allowTypesByWildcard);

    List<Pattern> allowedPatterns = allowedPatternsProperty.asList().stream()
        .map(Pattern::compile)
        .collect(Collectors.toList());
    log.debug("Setting allowTypesByRegExp to {}", allowedPatterns);
    xstream.allowTypesByRegExp(allowedPatterns.toArray(new Pattern[allowedPatterns.size()]));
  }

  private static void apply(List<String> list, Consumer<String[]> consumer) {
    if (list == null || list.isEmpty()) {
      return;
    }
   consumer.accept(list.toArray(new String[list.size()]));
  }
}

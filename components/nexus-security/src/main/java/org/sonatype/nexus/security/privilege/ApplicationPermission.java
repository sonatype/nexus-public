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
package org.sonatype.nexus.security.privilege;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.security.authz.WildcardPermission2;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Application permission, using {@code nexus:} namespace.
 *
 * @since 3.0
 */
public class ApplicationPermission
    extends WildcardPermission2
{
  public static final String SYSTEM = "nexus";

  private static final char PART_SEPARATOR = ':';

  private static final Splitter PART_SPLITTER = Splitter.on(PART_SEPARATOR);

  private final String domain;

  private final List<String> actions;

  public ApplicationPermission(final String domain, final List<String> actions) {
    this.domain = checkNotNull(domain);
    this.actions = checkNotNull(actions);

    if (domain.indexOf(PART_SEPARATOR) < 0) {
      setParts(ImmutableList.of(SYSTEM, domain), actions);
    }
    else {
      // complex domain we need to split into parts
      setParts(ImmutableList.<String> builder()
          .add(SYSTEM)
          .addAll(PART_SPLITTER.split(domain))
          .build(), actions);
    }
  }

  public ApplicationPermission(final String domain, final String... actions) {
    this(domain, Arrays.asList(actions));
  }

  public String getDomain() {
    return domain;
  }

  public List<String> getActions() {
    return actions;
  }
}

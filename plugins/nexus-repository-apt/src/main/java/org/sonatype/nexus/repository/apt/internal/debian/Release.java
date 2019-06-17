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
package org.sonatype.nexus.repository.apt.internal.debian;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @since 3.17
 */
public class Release
{
  private final ControlFile index;

  public Release(final ControlFile index) {
    super();
    this.index = index;
  }

  public Optional<String> getOrigin() {
    return getValue("Origin");
  }

  public Optional<String> getLabel() {
    return getValue("Label");
  }

  public Optional<String> getSuite() {
    return getValue("Suite");
  }

  public Optional<String> getVersion() {
    return getValue("Version");
  }

  public Optional<String> getCodename() {
    return getValue("Codename");
  }

  public List<String> getComponents() {
    return index.getField("Components").map(s -> s.listValue()).orElse(Collections.emptyList());
  }

  public List<String> getArchitectures() {
    return index.getField("Architectures").map(s -> s.listValue()).orElse(Collections.emptyList());
  }

  public Optional<String> getDescription() {
    return getValue("Description");
  }

  private Optional<String> getValue(final String name) {
    return index.getField(name).map(e -> e.value);
  }
}

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
package com.sonatype.nexus.docker.testsupport.framework;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Configuration object for a Command Line Docker Container. Creation is available through it's {@link #builder()}.
 *
 * @since 3.6.1
 */
public class DockerCommandLineConfig
{
  private List<String> pathBinds = new ArrayList<>();

  private DockerCommandLineConfig(final Builder builder) {
    this.pathBinds = builder.pathBinds;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private List<String> pathBinds = new ArrayList<>();

    private Builder() {
    }

    public Builder addPathBind(String local, String container) {
      addPathBinds(local + ":" + container);
      return this;
    }

    public Builder addPathBinds(String... pathBinds) {
      this.pathBinds.addAll(asList(pathBinds));
      return this;
    }

    public Builder removePathBinds() {
      this.pathBinds.clear();
      return this;
    }

    public DockerCommandLineConfig build() {
      return new DockerCommandLineConfig(this);
    }
  }

  public List<String> getPathBinds() {
    return unmodifiableList(pathBinds);
  }
}

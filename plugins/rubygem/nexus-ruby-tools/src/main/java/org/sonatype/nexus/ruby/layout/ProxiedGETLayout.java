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
package org.sonatype.nexus.ruby.layout;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.sonatype.nexus.ruby.BundlerApiFile;
import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.DependencyHelper;
import org.sonatype.nexus.ruby.RubygemsGateway;

public class ProxiedGETLayout
    extends GETLayout
{
  private final ProxyStorage store;

  public ProxiedGETLayout(RubygemsGateway gateway, ProxyStorage store) {
    super(gateway, store);
    this.store = store;
  }

  @Override
  public DependencyFile dependencyFile(String name) {
    DependencyFile file = super.dependencyFile(name);
    store.retrieve(file);
    return file;
  }

  @Override
  protected void retrieveAll(BundlerApiFile file, DependencyHelper deps) throws IOException {
    List<String> expiredNames = new LinkedList<>();
    for (String name : file.gemnames()) {
      DependencyFile dep = super.dependencyFile(name);
      if (store.isExpired(dep)) {
        expiredNames.add(name);
      }
      else {
        store.retrieve(dep);
        try (InputStream is = store.getInputStream(dep)) {
          deps.add(is);
        }
      }
    }
    if (expiredNames.size() > 0) {
      BundlerApiFile expired = super.bundlerApiFile(expiredNames.toArray(new String[expiredNames.size()]));
      store.retrieve(expired);
      if (expired.hasException()) {
        file.setException(expired.getException());
      }
      else if (expired.hasPayload()) {
        DependencyHelper bundlerDeps = gateway.newDependencyHelper();
        try (InputStream bundlerResult = store.getInputStream(expired)) {
          bundlerDeps.add(bundlerResult);
        }
        for(String gemname: bundlerDeps.getGemnames()) {
          DependencyFile dep = super.dependencyFile(gemname);
          // first store the data for caching
          store.update(bundlerDeps.getInputStreamOf(gemname), dep);
          // then add it to collector
          try (InputStream is = store.getInputStream(dep)) {
            deps.add(is);
          }
        }
      }
      else {
        // no payload so let's fall back and add the expired content
        for (String name : expiredNames) {
          DependencyFile dep = super.dependencyFile(name);
          store.retrieve(dep);
          try (InputStream is = store.getInputStream(dep)) {
            deps.add(is);
          }
        }
      }
    }
  }
}

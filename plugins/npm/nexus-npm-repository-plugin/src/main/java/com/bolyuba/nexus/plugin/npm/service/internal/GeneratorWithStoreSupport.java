/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.SimpleFormat;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.Generator;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Generator} support class that has {@link MetadataStore} that is used to source packages.
 */
public abstract class GeneratorWithStoreSupport<R extends NpmRepository>
    extends GeneratorSupport<R>
{
  protected final MetadataStore metadataStore;

  protected GeneratorWithStoreSupport(final R npmRepository,
                                      final MetadataParser metadataParser,
                                      final MetadataStore metadataStore)
  {
    super(npmRepository, metadataParser);
    this.metadataStore = checkNotNull(metadataStore);
  }

  @Override
  protected PackageRootIterator doGenerateRegistryRoot(final PackageRequest request) throws IOException {
    return new MetadataStorePackageRootIterator(metadataStore.listPackageNames(getNpmRepository()));
  }

  // ==

  protected class MetadataStorePackageRootIterator
      implements PackageRootIterator
  {
    private final Iterator<String> packageNames;

    protected MetadataStorePackageRootIterator(final List<String> packageNames) {
      this.packageNames = packageNames.iterator();
    }

    @Override
    public void close() throws IOException {
      // nop
    }

    @Override
    public boolean hasNext() {
      return packageNames.hasNext();
    }

    @Override
    public PackageRoot next() {
      final PackageRoot root = metadataStore.getPackageByName(getNpmRepository(), packageNames.next());
      if (root == null) {
        return null; // TODO: This is error actually, as iterator should not return null
      }
      if (!root.isUnpublished()) {
        root.shrinkPackageVersions();
      }
      filterPackageRoot(root);
      return root;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported.");
    }
  }
}

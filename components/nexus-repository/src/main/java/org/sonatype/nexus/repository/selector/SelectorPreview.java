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
package org.sonatype.nexus.repository.selector;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import static java.util.stream.StreamSupport.stream;

/**
 * Selector preview template.
 *
 * @since 3.0
 */
public class SelectorPreview
{
  private static final String ASSET = "asset";
  private static final String COMPONENT = "component";
  private static final String REPOSITORY = "repository";
  private static final Runnable NO_OP = () -> {};
  public enum ContentType { repository, component, asset }

  private final RepositoryManager repositoryManager;
  private final ContentType contentType;
  private final Selector selector;

  Runnable preExecute = NO_OP;
  Runnable postExecute = NO_OP;

  Runnable preRepository = NO_OP;
  Consumer<Repository> eachRepository = r -> {};
  Runnable postRepository = NO_OP;

  Runnable preComponent = NO_OP;
  Function<Repository, Consumer<Component>> eachComponent = r -> c -> {};
  Runnable postComponent = NO_OP;

  Runnable preAsset = NO_OP;
  Function<Entry<Repository, Component>, Consumer<Asset>> eachAsset = e -> a -> {};
  Runnable postAsset = NO_OP;

  public SelectorPreview(final RepositoryManager repositoryManager, final ContentType contentType, final Selector selector) {
    this.repositoryManager = repositoryManager;
    this.contentType = contentType;
    this.selector = selector;
  }

  public void executePreview() {
    preExecute.run();
    switch(contentType) {
      case repository:
        doRepository();
        break;
      case component:
        doComponent();
        break;
      case asset:
        doAsset();
        break;
      default:
        break;
    }
    postExecute.run();
  }

  private void doRepository() {
    preRepository.run();
    stream(repositoryManager.browse().spliterator(), true)
      .filter(r -> selector.evaluate(ConstantVariableResolver.sourceFor(r, REPOSITORY)))
      .forEach(eachRepository);
    postRepository.run();
  }

  private void doComponent() {
    preComponent.run();
    stream(repositoryManager.browse().spliterator(), true)
      .forEach(r -> {
        try (StorageTx tx = r.facet(StorageFacet.class).txSupplier().get()) {
          tx.begin();
          stream(tx.browseComponents(tx.findBucket(r)).spliterator(), false)
              .filter(c -> selector.evaluate(sourceFor(r, c)))
              .forEach(eachComponent.apply(r));
        }
      });
    postComponent.run();
  }

  private void doAsset() {
    preAsset.run();
    stream(repositoryManager.browse().spliterator(), true)
      .forEach(r -> {
        try (StorageTx tx = r.facet(StorageFacet.class).txSupplier().get()) {
          tx.begin();
          stream(tx.browseComponents(tx.findBucket(r)).spliterator(), false)
            .forEach(c -> stream(tx.browseAssets(c).spliterator(), false)
                .filter(a -> selector.evaluate(sourceFor(r, c, a)))
                .forEach(eachAsset.apply(new SimpleImmutableEntry(r, c))));
        }
      });
    postAsset.run();
  }

  private static VariableSource sourceFor(Repository repository, Component component) {
    return new VariableSourceBuilder()
        .addResolver(new ConstantVariableResolver(repository, REPOSITORY))
        .addResolver(new ConstantVariableResolver(component, COMPONENT))
        .build();
  }

  private static VariableSource sourceFor(Repository repository, Component component, Asset asset) {
    return new VariableSourceBuilder()
        .addResolver(new ConstantVariableResolver(repository, REPOSITORY))
        .addResolver(new ConstantVariableResolver(component, COMPONENT))
        .addResolver(new ConstantVariableResolver(asset, ASSET))
        .build();
  }
}

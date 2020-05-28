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
package org.sonatype.nexus.repository.content.store;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Named;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Types;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Extend this module if your format adds methods to its content store APIs.
 * Declare your stores and annotate the module with the name of your format.
 * You can mix custom stores with standard stores in the same bespoke module:
 *
 * <code><pre>
 * &#64;Named("example")
 * public class ExampleStoreModule
 *     extends BespokeFormatStoreModule&lt;ContentRepositoryStore&lt;ExampleContentRepositoryDAO&gt;,
 *                                      ExampleComponentStore,
 *                                      ExampleAssetStore,
 *                                      AssetBlobStore&lt;ExampleAssetBlobDAO&gt;&gt;
 * {
 *   // nothing to add...
 * }
 * </pre></code>
 *
 * In this scenario {@code ExampleContentStore} and {@code ExampleAssetStore}
 * have added methods that query supplementary data held in columns attached
 * to the format's component and asset tables.
 *
 * Most formats don't need this flexibility and should use {@link FormatStoreModule}.
 *
 * @since 3.24
 */
@SuppressWarnings("unused")
public abstract class BespokeFormatStoreModule<CONTENT_REPOSITORY_STORE extends ContentRepositoryStore<?>,
                                               COMPONENT_STORE extends ComponentStore<?>,
                                               ASSET_STORE extends AssetStore<?>,
                                               ASSET_BLOB_STORE extends AssetBlobStore<?>>
    extends AbstractModule
{
  private final Class<?>[] daoClasses;

  private final Key<?> factoryKey;

  protected BespokeFormatStoreModule() {
    // first fetch the store types captured by this module
    Type[] typeArguments = typeArguments(getClass(), BespokeFormatStoreModule.class);
    int numTypes = typeArguments.length;

    // next extract the DAO types captured by each store type
    daoClasses = new Class[numTypes];
    for (int i = 0; i < numTypes; i++) {
      daoClasses[i] = (Class<?>) typeArguments(typeArguments[i], ContentStoreSupport.class)[0];
    }

    // combine store and DAO types to get the factory 'spec' for Assisted-Inject
    Type[] factorySpec = new Type[numTypes * 2];
    System.arraycopy(typeArguments, 0, factorySpec, 0, numTypes);
    System.arraycopy(daoClasses, 0, factorySpec, numTypes, numTypes);

    // apply spec to our Assisted-Inject generic template to get the required factory API
    Type factoryType = Types.newParameterizedType(FormatStoreFactory.class, factorySpec);

    Named qualifier = getClass().getAnnotation(Named.class);
    checkArgument(qualifier != null && qualifier.value().length() > 0,
        getClass() + " must be annotated with @Named(\"myformat\")");

    factoryKey = Key.get(factoryType, qualifier);
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void configure() {
    // build Assisted-Inject factory that supplies store instances for this format
    install(new FactoryModuleBuilder().build(factoryKey));

    // create a manager that picks the right DAO class for us (the factory will pick the right store)
    FormatStoreManager storeManager = new FormatStoreManager(getProvider((Key) factoryKey), daoClasses);
    bind(FormatStoreManager.class).annotatedWith(factoryKey.getAnnotation()).toInstance(storeManager);
  }

  /**
   * Extracts the actual types assigned to type variables declared by the given supertype.
   */
  private static Type[] typeArguments(final Type type, final Class<?> supertype) {
    TypeLiteral<?> superType = TypeLiteral.get(type).getSupertype(supertype);
    return ((ParameterizedType) superType.getType()).getActualTypeArguments();
  }
}

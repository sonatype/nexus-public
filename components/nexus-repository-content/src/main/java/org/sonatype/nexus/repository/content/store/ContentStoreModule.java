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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.inject.name.Names.named;
import static com.google.inject.util.Types.newParameterizedType;
import static org.sonatype.nexus.common.text.Strings2.capitalize;

/**
 * Support class to help create format-specific store instances in different data stores.
 *
 * @since 3.26
 */
public abstract class ContentStoreModule<STORE extends ContentStoreSupport<?>>
    extends AbstractModule
{
  final Named format;

  final String formatClassPrefix;

  private final Type[] formatStoreTypes;

  /**
   * Constructor used by simple format modules which only define a single store type.
   */
  protected ContentStoreModule() {
    this(ContentStoreModule.class);
  }

  /**
   * Use a different module class as the template defining the generic store type parameters.
   */
  @SuppressWarnings("rawtypes")
  protected ContentStoreModule(final Class<? extends ContentStoreModule> templateModule) {
    format = getClass().getAnnotation(Named.class);

    checkArgument(format != null && format.value().length() > 0,
        "%s must be annotated with @Named(\"myformat\")", getClass());

    // extract the expected 'TitleCase' form of the format string
    formatClassPrefix = getClass().getSimpleName().substring(0, format.value().length());

    checkArgument(formatClassPrefix.equalsIgnoreCase(format.value()),
        "%s must start with %s", getClass(), capitalize(format.value()));

    formatStoreTypes = typeArguments(getClass(), templateModule);
  }

  @Override
  protected void configure() {
    // bind a FormatStoreFactory for all stores declared by this module
    for (Type formatStoreType : formatStoreTypes) {
      bindFormatStoreFactory(formatStoreType);
    }

    // leave FormatStoreManager binding to the main FormatStoreModule / BespokeFormatStoreModule
  }

  /**
   * Generate a {@link FormatStoreFactory} binding to produce stores of the given parameterized type.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void bindFormatStoreFactory(final Type formatStoreType) {
    // extract the format-specific DAO class from the store type declaration: MyStore<MyDao> -> MyDao
    Class<?> formatDaoClass = (Class<?>) typeArguments(formatStoreType, ContentStoreSupport.class)[0];

    // verify the DAO class starts with the same format prefix as this module
    checkArgument(formatDaoClass.getSimpleName().startsWith(formatClassPrefix),
        "%s must start with %s", formatDaoClass, formatClassPrefix);

    // parameterize the Assisted-Inject template to match the store API for this particular format
    Type factoryType = newParameterizedType(ContentStoreFactory.class, formatStoreType, formatDaoClass);
    Key factoryKey = Key.get(factoryType, format);

    install(new FactoryModuleBuilder().build(factoryKey));

    // combine the Assisted-Inject store factory with the DAO class to get our format-specific factory
    FormatStoreFactory formatStoreFactory = new FormatStoreFactory(getProvider(factoryKey), formatDaoClass);
    bind(FormatStoreFactory.class).annotatedWith(named(formatDaoClass.getSimpleName())).toInstance(formatStoreFactory);
  }

  /**
   * Extracts the actual types assigned to type variables declared by the given supertype.
   */
  private static Type[] typeArguments(final Type type, final Class<?> supertype) {
    TypeLiteral<?> superType = TypeLiteral.get(type).getSupertype(supertype);
    return ((ParameterizedType) superType.getType()).getActualTypeArguments();
  }
}

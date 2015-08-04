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
package org.sonatype.nexus.plugins.capabilities.internal.guice;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.plugins.capabilities.internal.ActivationConditionHandlerFactory;
import org.sonatype.nexus.plugins.capabilities.internal.ValidityConditionHandlerFactory;
import org.sonatype.nexus.plugins.capabilities.internal.storage.CapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.storage.XmlCapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.validator.ValidatorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import io.kazuki.v0.store.easy.EasyKeyValueStoreModule;
import io.kazuki.v0.store.jdbi.JdbiDataSourceConfiguration;
import io.kazuki.v0.store.keyvalue.KeyValueStoreConfiguration;
import io.kazuki.v0.store.lifecycle.LifecycleModule;
import io.kazuki.v0.store.sequence.SequenceServiceConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.internal.storage.KazukiCapabilityStorage.CAPABILITY_SCHEMA;

/**
 * Capabilities plugin Guice module.
 *
 * @since capabilities 1.0
 */
@Named
public class GuiceModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(CapabilityStorage.class).to(XmlCapabilityStorage.class);

    install(new FactoryModuleBuilder().build(ActivationConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidityConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidatorFactory.class));

    install(new LifecycleModule("nexuscapability"));

    bind(JdbiDataSourceConfiguration.class).annotatedWith(Names.named("nexuscapability"))
        .toProvider(JdbiConfigurationProvider.class).in(Scopes.SINGLETON);

    install(new EasyKeyValueStoreModule("nexuscapability", null)
        .withSequenceConfig(getSequenceServiceConfiguration())
        .withKeyValueStoreConfig(getKeyValueStoreConfiguration())
    );
  }

  private SequenceServiceConfiguration getSequenceServiceConfiguration() {
    SequenceServiceConfiguration.Builder builder = new SequenceServiceConfiguration.Builder();

    builder.withDbType("h2");
    builder.withGroupName("nexus");
    builder.withStoreName("capability");
    builder.withStrictTypeCreation(true);

    return builder.build();
  }

  private KeyValueStoreConfiguration getKeyValueStoreConfiguration() {
    KeyValueStoreConfiguration.Builder builder = new KeyValueStoreConfiguration.Builder();

    builder.withDbType("h2");
    builder.withGroupName("nexus");
    builder.withStoreName("capability");
    builder.withPartitionName("default");
    builder.withPartitionSize(100_000L);
    builder.withStrictTypeCreation(true);
    builder.withDataType(CAPABILITY_SCHEMA);

    return builder.build();
  }


  // TODO: Extract helper for jdbi config, as the location for databases will be normalized

  private static class JdbiConfigurationProvider
      implements Provider<JdbiDataSourceConfiguration>
  {
    private final ApplicationDirectories directories;

    @Inject
    public JdbiConfigurationProvider(final ApplicationDirectories directories) {
      this.directories = checkNotNull(directories);
    }

    @Override
    public JdbiDataSourceConfiguration get() {
      JdbiDataSourceConfiguration.Builder builder = new JdbiDataSourceConfiguration.Builder();

      builder.withJdbcDriver("org.h2.Driver");

      File dir = directories.getWorkDirectory("db/capabilities", false);
      File file = new File(dir, dir.getName());
      builder.withJdbcUrl("jdbc:h2:" + file.getAbsolutePath());

      builder.withJdbcUser("root");
      builder.withJdbcPassword("not_really_used");
      builder.withPoolMinConnections(25);
      builder.withPoolMaxConnections(25);

      return builder.build();
    }
  }
}

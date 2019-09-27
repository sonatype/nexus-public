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
package org.sonatype.nexus.datastore.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.datastore.DataStoreSupport;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.transaction.Transaction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Splitter.onPattern;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.common.thread.TcclBlock.begin;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.ADVANCED;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.JDBC_URL;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.SCHEMA;

/**
 * MyBatis {@link DataStore}.
 *
 * @since 3.19
 */
@Named(MyBatisDataStoreDescriptor.NAME)
@SuppressWarnings("rawtypes")
public class MyBatisDataStore
    extends DataStoreSupport<Transaction, MyBatisDataSession>
{
  private static final String REGISTERED_MESSAGE = "Registered {} with MyBatis";

  private static final Key<TypeHandler> TYPE_HANDLER_KEY = Key.get(TypeHandler.class);

  private static final TypeHandlerMediator TYPE_HANDLER_MEDIATOR = new TypeHandlerMediator();

  private static final Splitter BY_LINE = onPattern("\\r?\\n").trimResults().omitEmptyStrings();

  private static final Splitter KEY_VALUE = onPattern("[=:]").limit(2).trimResults().omitEmptyStrings();

  private static final MapSplitter TO_MAP = BY_LINE.withKeyValueSeparator(KEY_VALUE);

  private final ApplicationDirectories directories;

  private final BeanLocator beanLocator;

  private HikariDataSource dataSource;

  private SqlSessionFactory sessionFactory;

  @Inject
  public MyBatisDataStore(final ApplicationDirectories directories, final BeanLocator beanLocator) {
    this.directories = checkNotNull(directories);
    this.beanLocator = checkNotNull(beanLocator);
  }

  @VisibleForTesting
  public MyBatisDataStore() {
    this.directories = null;
    this.beanLocator = null;
  }

  @Override
  protected void doStart(final String storeName, final Map<String, String> attributes) throws Exception {

    dataSource = new HikariDataSource(configureHikari(storeName, attributes));
    Environment environment = new Environment(storeName, new JdbcTransactionFactory(), dataSource);
    sessionFactory = new DefaultSqlSessionFactory(configureMyBatis(environment));

    if (beanLocator == null) {
      // add standard handlers for testing
      register(new AttributesTypeHandler());
      register(new EntityUUIDTypeHandler());
    }
    else {
      beanLocator.watch(TYPE_HANDLER_KEY, TYPE_HANDLER_MEDIATOR, this);
    }
  }

  @Override
  protected void doStop() throws Exception {
    sessionFactory = null;
    try {
      dataSource.close();
    }
    finally {
      dataSource = null;
    }
  }

  @Override
  public void register(final Class<? extends DataAccess> accessType) {

    // support use of package-less names
    registerSimpleAliases(accessType);

    // now register the actual mapper
    sessionFactory.getConfiguration().addMapper(accessType);
    info(REGISTERED_MESSAGE, accessType);

    // finally create the schema for this DAO
    try (SqlSession session = sessionFactory.openSession()) {
      session.getMapper(accessType).createSchema();
      session.commit();
    }
  }

  @Override
  public void unregister(Class<? extends DataAccess> accessType) {
    // unregistration of custom access types is not supported
  }

  @Guarded(by = STARTED)
  @Override
  public MyBatisDataSession openSession() {
    return new MyBatisDataSession(sessionFactory.openSession());
  }

  @Guarded(by = STARTED)
  @Override
  public Connection openConnection() throws SQLException {
    return dataSource.getConnection();
  }

  /**
   * Supplies the populated Hikari configuration for this store.
   */
  private HikariConfig configureHikari(final String storeName, final Map<String, String> attributes) {
    Properties properties = new Properties();
    properties.put("poolName", storeName);
    properties.putAll(attributes);

    // workaround https://github.com/pgjdbc/pgjdbc/issues/265
    if (attributes.get(JDBC_URL).startsWith("jdbc:postgresql")) {
      properties.put("dataSource.stringtype", "unspecified");
    }

    // Parse and unflatten advanced attributes
    Object advanced = properties.remove(ADVANCED);
    if (advanced instanceof String) {
      TO_MAP.split((String) advanced).forEach(properties::putIfAbsent);
    }

    // Hikari doesn't like blank schemas in its config
    if (isBlank(properties.getProperty(SCHEMA))) {
      properties.remove(SCHEMA);
    }

    return new HikariConfig(properties);
  }

  /**
   * Supplies the populated MyBatis configuration for this store.
   */
  private Configuration configureMyBatis(final Environment environment) throws IOException {
    Configuration myBatisConfig = loadMyBatisConfiguration(environment);

    String databaseId = new VendorDatabaseIdProvider().getDatabaseId(environment.getDataSource());
    info("MyBatis databaseId: {}", databaseId);

    // configuration elements that must always be applied
    myBatisConfig.setEnvironment(environment);
    myBatisConfig.setDatabaseId(databaseId);
    myBatisConfig.setObjectFactory(new DefaultObjectFactory()
    {
      @Override
      public <T> boolean isCollection(Class<T> type) {
        // modify MyBatis behaviour to let it map collection results to Iterable
        return Iterable.class.isAssignableFrom(type) || super.isCollection(type);
      }
    });

    if (CONFIG_DATASTORE_NAME.equals(environment.getId())) {
      myBatisConfig.addInterceptor(new EntityInterceptor());
    }

    return myBatisConfig;
  }

  /**
   * Loads the MyBatis configuration for this store.
   */
  @VisibleForTesting
  protected Configuration loadMyBatisConfiguration(final Environment environment) throws IOException {
    if (directories == null) {
      info("Using default MyBatis configuration");
      return new Configuration();
    }

    Path configPath = myBatisConfigPath(environment);

    info("Loading MyBatis configuration from {}", configPath);
    try (InputStream in = newInputStream(configPath); TcclBlock block = begin(getClass())) {
      return new XMLConfigBuilder(in).parse();
    }
  }

  /**
   * Returns the path to the MyBatis configuration to use for this store.
   */
  private Path myBatisConfigPath(final Environment environment) {
    Path fabricPath = directories.getConfigDirectory("fabric").toPath();
    Path configPath = fabricPath.resolve(environment.getId() + "-store-mybatis.xml");
    if (!exists(configPath)) {
      // fallback to the shared MyBatis configuration
      configPath = fabricPath.resolve("mybatis.xml");
    }
    return configPath;
  }

  /**
   * Register simple package-less aliases for all parameter and return types in the DAO.
   */
  private void registerSimpleAliases(final Class<? extends DataAccess> accessType) {
    TypeAliasRegistry registry = sessionFactory.getConfiguration().getTypeAliasRegistry();
    TypeLiteral<?> resolvedType = TypeLiteral.get(accessType);
    for (Method method : accessType.getMethods()) {
      for (TypeLiteral<?> parameterType : resolvedType.getParameterTypes(method)) {
        registerSimpleAlias(registry, parameterType);
      }
      registerSimpleAlias(registry, resolvedType.getReturnType(method));
    }
  }

  /**
   * Registers a simple package-less alias for the resolved type.
   */
  private void registerSimpleAlias(final TypeAliasRegistry registry, final TypeLiteral<?> type) {
    Class<?> clazz = type.getRawType();
    if (!clazz.isPrimitive() && !clazz.getName().startsWith("java.")) {
      try {
        registry.registerAlias(clazz);
      }
      catch (RuntimeException | LinkageError e) {
        debug("Unable to register type alias", e);
      }
    }
  }

  /**
   * Registers the given {@link Interceptor} with MyBatis.
   */
  @VisibleForTesting
  public void register(final Interceptor interceptor) {
    sessionFactory.getConfiguration().addInterceptor(interceptor);
    info(REGISTERED_MESSAGE, interceptor.getClass());
  }

  /**
   * Registers the given {@link TypeHandler} with MyBatis.
   */
  @VisibleForTesting
  public void register(final TypeHandler<?> handler) {
    sessionFactory.getConfiguration().getTypeHandlerRegistry().register(handler);
    info(REGISTERED_MESSAGE, handler.getClass());
  }

  /**
   * Tracks and registers custom {@link TypeHandler}s with MyBatis.
   */
  static class TypeHandlerMediator
      implements Mediator<Named, TypeHandler, MyBatisDataStore>
  {
    @Override
    public void add(final BeanEntry<Named, TypeHandler> entry, final MyBatisDataStore store) {
      store.register(entry.getValue());
    }

    @Override
    public void remove(final BeanEntry<Named, TypeHandler> entry, final MyBatisDataStore store) {
      // unregistration of custom type handlers is not supported
    }
  }
}

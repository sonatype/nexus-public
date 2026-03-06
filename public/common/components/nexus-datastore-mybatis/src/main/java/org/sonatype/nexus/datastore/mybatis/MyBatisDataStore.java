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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.io.FileFinder;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StatePrerequisitesInvalidException;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.MavenCipherImpl;
import org.sonatype.nexus.datastore.DataStoreSupport;
import org.sonatype.nexus.datastore.TransactionalStoreSupport;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataAccessException;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.datastore.mybatis.handlers.AttributesTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.DateTimeTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.EncryptedStringTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.EntityUUIDTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.ExternalMetadataTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.LenientUUIDTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.ListTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.MapTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.NestedAttributesMapTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.PasswordCharacterArrayTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.PrincipalCollectionTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.QuotingTypeHandler;
import org.sonatype.nexus.datastore.mybatis.handlers.SetTypeHandler;
import org.sonatype.nexus.security.PasswordHelper;
import org.sonatype.nexus.transaction.TransactionIsolation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import jakarta.inject.Inject;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.OffsetDateTimeTypeHandler;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Splitter.onPattern;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.apache.ibatis.session.TransactionIsolationLevel.SERIALIZABLE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.common.text.Strings2.lower;
import static org.sonatype.nexus.common.thread.TcclBlock.begin;
import static org.sonatype.nexus.crypto.PhraseService.LEGACY_PHRASE_SERVICE;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.ADVANCED;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.JDBC_URL;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.SCHEMA;
import static org.sonatype.nexus.datastore.mybatis.PlaceholderTypes.configurePlaceholderTypes;
import static org.sonatype.nexus.datastore.mybatis.SensitiveAttributes.buildSensitiveAttributeFilter;

/**
 * MyBatis {@link DataStore}.
 *
 * @since 3.19
 */
@Qualifier(MyBatisDataStoreDescriptor.NAME)
@Component
@SuppressWarnings("rawtypes")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MyBatisDataStore
    extends DataStoreSupport<MyBatisDataSession>
    implements Ordered
{
  private static final String REGISTERED_MESSAGE = "Registered {}";

  public static final String H2_DATABASE = "H2";

  private static final Splitter BY_LINE = onPattern("\\r?\\n").trimResults().omitEmptyStrings();

  private static final Splitter KEY_VALUE = onPattern("[=:]").limit(2).trimResults().omitEmptyStrings();

  private static final MapSplitter TO_MAP = BY_LINE.withKeyValueSeparator(KEY_VALUE);

  private static final Pattern MAPPER_BODY = compile(".*<mapper[^>]*>(.*)</mapper>", DOTALL);

  private static final int DEFAULT_CONTENT_STORE_MAX_POOL_SIZE = 100;

  /**
   * Default initialization timeout for HikariCP in milliseconds.
   * Applies to all PostgreSQL database connections managed by this data store and
   * allows up to 10 seconds for database connectivity to be established during startup.
   * This is especially beneficial for cloud deployments, such as ECS/Fargate task
   * restarts, where network latency or database provisioning delays may occur.
   */
  private static final long DEFAULT_INITIALIZATION_FAIL_TIMEOUT_MS = 10000L;

  private final List<TransactionalStoreSupport> declaredAccessTypes;

  private final List<TypeHandler> declaredTypeHandlers;

  private final Set<Class<?>> registeredAccessTypes = new HashSet<>();

  private final AtomicBoolean frozenMarker = new AtomicBoolean();

  private final PbeCipher databaseCipher;

  private final PasswordHelper passwordHelper;

  private final ApplicationDirectories directories;

  private final LogManager logManager;

  private ManagedLifecycleManager managedLifecycleManager;

  private HikariDataSource dataSource;

  private Configuration mybatisConfig;

  private Optional<Configuration> previousConfig = empty();

  @Nullable
  private Predicate<String> sensitiveAttributeFilter;

  private final boolean orientWarning;

  @Inject
  public MyBatisDataStore(
      @Qualifier("mybatis") final PbeCipher databaseCipher,
      final PasswordHelper passwordHelper,
      final ApplicationDirectories directories,
      final LogManager logManager,
      @Lazy final List<TransactionalStoreSupport> declaredAccessTypes,
      @Lazy final List<TypeHandler> declaredTypeHandlers,
      @Value(FeatureFlags.ORIENT_WARNING_NAMED_VALUE) final boolean orientWarning)
  {
    checkState(databaseCipher instanceof MyBatisCipher);
    this.databaseCipher = checkNotNull(databaseCipher);
    this.passwordHelper = checkNotNull(passwordHelper);
    this.directories = checkNotNull(directories);
    this.logManager = checkNotNull(logManager);

    useMyBatisClassLoaderForEntityProxies();

    this.declaredAccessTypes = checkNotNull(declaredAccessTypes);
    this.declaredTypeHandlers = checkNotNull(declaredTypeHandlers);
    this.orientWarning = orientWarning;
  }

  @VisibleForTesting
  public MyBatisDataStore() {
    try {
      // use static configuration for testing
      this.databaseCipher = new MyBatisCipher();
      MavenCipherImpl passwordCipher = new MavenCipherImpl(new CryptoHelperImpl(false));
      this.passwordHelper = new PasswordHelper(passwordCipher, LEGACY_PHRASE_SERVICE);
    }
    catch (Exception e) {
      throw new IllegalStateException("Unexpected error during setup", e);
    }
    this.directories = null;
    this.logManager = null;

    this.declaredAccessTypes = List.of();
    this.declaredTypeHandlers = List.of();
    this.orientWarning = false;
  }

  @Override
  protected void doStart(final String storeName, final Map<String, String> attributes) throws Exception {
    // Silence Hikari Logging for this block as we will throw any necessary exceptions and we expect some exceptions
    // that should not be logged at ERROR level.
    // We also have to null-check because there is a constructor just for testing that will break otherwise, which is
    // a terrible idea but apparently somebody thought it made sense.
    LoggerLevel originalHikariPoolLogLevel = LoggerLevel.DEFAULT;
    if (logManager != null) {
      originalHikariPoolLogLevel = logManager.getLoggerLevel(HikariPool.class.getName());
      logManager.setLoggerLevelDirect(HikariPool.class.getName(), LoggerLevel.OFF);
    }

    if (directories != null) {
      verifyOrientDatabaseDoesNotExist();
    }

    HikariConfig hikariConfig = configureHikari(storeName, attributes);
    dataSource = new HikariDataSource(hikariConfig);

    if (logManager != null) {
      // Re-enable Hikari logging
      logManager.setLoggerLevelDirect(HikariPool.class.getName(), originalHikariPoolLogLevel);
    }

    Environment environment = new Environment(storeName, new JdbcTransactionFactory(), dataSource);

    if (previousConfig.isPresent()) {
      mybatisConfig = previousConfig.get();
      mybatisConfig.setEnvironment(environment);
    }
    else {
      mybatisConfig = configureMyBatis(environment);

      registerCommonTypeHandlers();

      // register the appropriate type handlers with the store
      declaredTypeHandlers.forEach(this::register);
    }
    findDaos();
  }

  @EventListener
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    log.trace("onApplicationEvent {}", event);
    if (isStarted()) {
      findDaos();
    }
  }

  @Override
  public int getOrder() {
    return -1;
  }

  private void findDaos() {
    declaredAccessTypes
        .stream()
        .map(TransactionalStoreSupport::getDaoClass)
        .forEach(this::register);
  }

  @Override
  protected void doStop() throws Exception {
    previousConfig = ofNullable(mybatisConfig);
    mybatisConfig = null;
    try {
      // Execute H2 SHUTDOWN if this is an H2 database
      if (isH2Database()) {
        log.info("Shutting down H2 database");
        executeH2Shutdown();
      }
      dataSource.close();
    }
    finally {
      dataSource = null;
    }
  }

  boolean isH2Database() {
    String jdbcUrl = configuration.getAttributes().get("jdbcUrl");
    return jdbcUrl != null && jdbcUrl.toLowerCase().contains("jdbc:h2:");
  }

  private void executeH2Shutdown() {
    if (dataSource.isClosed()) {
      log.debug("Data source is already closed, skipping H2 SHUTDOWN command");
      return;
    }

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("SHUTDOWN");
      log.info("Executed H2 SHUTDOWN command");
    }
    catch (SQLException e) {
      if (isDatabaseAlreadyClosed(e)) {
        log.debug("H2 database was already closed during shutdown");
      }
      else {
        log.warn("Failed to execute H2 SHUTDOWN command", e);
      }
    }
  }

  private boolean isDatabaseAlreadyClosed(final SQLException e) {
    return e.getMessage() != null &&
        e.getMessage().contains("Database is already closed") &&
        e.getErrorCode() == 90121;
  }

  @Override
  public void register(final Class<? extends DataAccess> accessType) {
    if (!registeredAccessTypes.add(accessType) || accessType.isAnnotationPresent(SchemaTemplate.class)) {
      return; // skip registration if we've already seen this type or this is a template
    }

    registerSimpleAliases(accessType);
    registerDataAccessMapper(accessType);

    // finally create the schema for this DAO
    info("Creating schema for {}", accessType.getSimpleName());
    try (SqlSession session = new DataAccessSqlSession(mybatisConfig)) {
      DataAccess dao = session.getMapper(accessType);
      // Creating schema with retry mechanism to avoid failures in HA mode when nodes run simultaneously
      DataAccessException exception = null;
      int tries = 0;
      while (tries < 5) {
        try {
          dao.createSchema();
          dao.extendSchema();
          session.commit();
          exception = null;
          break;
        }
        catch (DataAccessException dae) {
          exception = dae;
          log.debug("Schema creation failed", dae);
          session.rollback();
          try {
            Thread.sleep(100L);
          }
          catch (InterruptedException e) {
            log.warn("Schema creation thread interrupted, proceeding with creation");
          }
          tries++;
        }
      }
      if (exception != null) {
        throw exception;
      }
    }
  }

  @Override
  public void unregister(final Class<? extends DataAccess> accessType) {
    // unregistration of custom access types is not supported
  }

  @Guarded(by = STARTED)
  @Override
  public MyBatisDataSession openSession() {
    return new MyBatisDataSession(new DataAccessSqlSession(mybatisConfig));
  }

  @Guarded(by = STARTED)
  @Override
  public MyBatisDataSession openSession(final TransactionIsolation isolationLevel) {
    return switch (isolationLevel) {
      case SERIALIZABLE -> new MyBatisDataSession(new DataAccessSqlSession(mybatisConfig, SERIALIZABLE));
      default -> new MyBatisDataSession(new DataAccessSqlSession(mybatisConfig));
    };
  }

  @Guarded(by = STARTED)
  @Override
  public Connection openConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Guarded(by = STARTED)
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public void freeze() {
    frozenMarker.set(true);
  }

  @Override
  public void unfreeze() {
    frozenMarker.set(false);
  }

  @Override
  public boolean isFrozen() {
    return frozenMarker.get();
  }

  @Guarded(by = STARTED)
  @Override
  public void backup(final String location) throws SQLException {
    try (Connection conn = openConnection()) {
      if (H2_DATABASE.equals(conn.getMetaData().getDatabaseProductName())) {
        try (PreparedStatement backupStmt = conn.prepareStatement("BACKUP TO ?")) {
          backupStmt.setString(1, location);
          backupStmt.execute();
        }
      }
      else {
        throw new UnsupportedOperationException("The underlying database is not supported for backup.");
      }
    }
  }

  @Inject
  public void setManagedLifecycleManager(@Lazy final ManagedLifecycleManager managedLifecycleManager) {
    this.managedLifecycleManager = checkNotNull(managedLifecycleManager);
  }

  private void verifyOrientDatabaseDoesNotExist() throws Exception {
    if (orientWarning && isOrientDbPresent() && (isSqlDbConfigured())) {
      log.warn(
          "Database directory contains unsupported legacy database files; remove legacy files as soon as possible.");
    }
    else if (orientWarning && isOrientDbPresent() && !(isSqlDbConfigured())) {
      StringBuilder buf = new StringBuilder();
      buf.append("\n-------------------------------------------------------------------------------------------" +
          "----------------------------------------------------------------------------------\n\n");
      buf.append("This instance is using a legacy Orient database. " +
          "\nYou must migrate to H2 or PostgreSQL before upgrading to this version. " +
          "See our database migration help documentation at: " +
          "\nhttps://links.sonatype.com/products/nxrm3/docs/unsupported-db.");
      buf.append("\n\n-----------------------------------------------------------------------------------------" +
          "------------------------------------------------------------------------------------\n\n");
      log.error(buf.toString());
      managedLifecycleManager.shutdownWithExitCode(1);
      throw new StatePrerequisitesInvalidException(
          "An unsupported orient database is present in the database directory, " +
              "you need to migrate your data before running this version of nexus");
    }
  }

  private boolean isOrientDbPresent() {
    Path dbPath = directories.getWorkDirectory("db", false).toPath();
    Set<String> orientFolderNames = ImmutableSet.of("config", "security", "component");

    return FileFinder.pathContainsFolder(dbPath, orientFolderNames);
  }

  private boolean isH2DbPresent() {
    Path dbPath = directories.getWorkDirectory("db", false).toPath();
    Path h2Db = dbPath.resolve("nexus.mv.db");

    return Files.exists(h2Db);
  }

  private static boolean isJdbcUrlSet() {
    String systemProperty = System.getProperty("nexus.datastore.nexus.jdbcUrl");
    String environmentVariable = System.getenv("NEXUS_DATASTORE_NEXUS_JDBCURL");

    if (systemProperty != null) {
      return true;
    }
    return environmentVariable != null;
  }

  private boolean checkNexusStorePropertyExists() {
    Path fabricPath = directories.getWorkDirectory("etc/fabric").toPath();
    Path nexusStorePropertiesPath = fabricPath.resolve("nexus-store.properties");

    Properties properties = new Properties();

    try (InputStream input = Files.newInputStream(nexusStorePropertiesPath)) {
      properties.load(input);
    }
    catch (IOException e) {
      return false;
    }

    return properties.getProperty(JDBC_URL).startsWith("jdbc:postgresql");
  }

  private boolean isSqlDbConfigured() {
    return isJdbcUrlSet() || isH2DbPresent() || checkNexusStorePropertyExists();
  }

  /**
   * Supplies the populated Hikari configuration for this store.
   */
  @VisibleForTesting
  HikariConfig configureHikari(final String storeName, final Map<String, String> attributes) {
    Properties properties = new Properties();
    properties.put("poolName", storeName);
    properties.putAll(attributes);
    if (properties.getProperty(JDBC_URL, "").contains("${karaf.data}")) {
      String jdbcUrl = properties.getProperty(JDBC_URL);
      jdbcUrl = jdbcUrl.replace("${karaf.data}", directories.getWorkDirectory("db", true).toString());
      log.info("jdbcUrl updated to {}", jdbcUrl);
      properties.setProperty(JDBC_URL, jdbcUrl);
    }
    // Parse and unflatten advanced attributes
    Object advanced = properties.remove(ADVANCED);
    if (advanced instanceof String) {
      TO_MAP.split((String) advanced).forEach(properties::putIfAbsent);
    }

    if (attributes.get(JDBC_URL).startsWith("jdbc:postgresql")) {
      properties.put("driverClassName", "org.postgresql.Driver");
      // workaround https://github.com/pgjdbc/pgjdbc/issues/265
      properties.put("dataSource.stringtype", "unspecified");

      properties.putIfAbsent("maximumPoolSize", DEFAULT_CONTENT_STORE_MAX_POOL_SIZE);
      // Allow more time for database connectivity during startup, especially in cloud environments
      // where network latency or database provisioning delays may occur
      properties.putIfAbsent("initializationFailTimeout", DEFAULT_INITIALIZATION_FAIL_TIMEOUT_MS);
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
    myBatisConfig.setMapUnderscoreToCamelCase(true);
    myBatisConfig.setReturnInstanceForEmptyRow(true);
    myBatisConfig.setObjectFactory(new DefaultObjectFactory()
    {
      @Override
      public <T> boolean isCollection(final Class<T> type) {
        // modify MyBatis behaviour to let it map collection results to Iterable
        return Iterable.class.isAssignableFrom(type) || super.isCollection(type);
      }

      @Override
      protected Class<?> resolveInterface(final Class<?> type) {
        if (type == Continuation.class) {
          return ContinuationArrayList.class;
        }
        return super.resolveInterface(type);
      }
    });

    return myBatisConfig;
  }

  /**
   * Register common {@link TypeHandler}s.
   */
  @SuppressWarnings("unchecked")
  private void registerCommonTypeHandlers() {
    boolean lenient = configurePlaceholderTypes(mybatisConfig);

    // register raw/simple mappers first
    register(new ListTypeHandler());
    register(new SetTypeHandler());
    register(new MapTypeHandler());
    register(new DateTimeTypeHandler());
    register(new OffsetDateTimeTypeHandler());
    register(new ExternalMetadataTypeHandler());

    // mapping of entity ids needs some extra handling
    TypeHandler entityIdHandler = new EntityUUIDTypeHandler(lenient);
    register(EntityUUID.class, entityIdHandler);
    register(EntityId.class, entityIdHandler);
    if (lenient) {
      // also support querying by UUID string when database may not have a UUID type
      register(new LenientUUIDTypeHandler());
    }

    // generate new entity ids on-demand
    register(new EntityInterceptor(new FrozenChecker(frozenMarker)));

    // security handlers that used to only exist in the config store
    register(new PasswordCharacterArrayTypeHandler(passwordHelper));
    register(new PrincipalCollectionTypeHandler());
    registerDetached(new EncryptedStringTypeHandler()); // detached so it doesn't apply to all Strings
    registerDetached(new QuotingTypeHandler()); // detached so it doesn't apply to all Strings

    // enable automatic encryption of sensitive JSON fields in the config store
    sensitiveAttributeFilter = buildSensitiveAttributeFilter(mybatisConfig);

    // finally register more complex mappers on top of the raw/simple mappers - this way we can have
    // automatic encryption on by default while individual DAOs can choose to use the raw mapper(s)
    // (for example if they handle encryption themselves outside of the persistence layer)

    register(new AttributesTypeHandler());
    register(new NestedAttributesMapTypeHandler());
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
      return new XMLConfigBuilder(in, null, new Properties()).parse();
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
    TypeAliasRegistry registry = mybatisConfig.getTypeAliasRegistry();

    TypeToken.of(accessType).getTypes().forEach(type -> {
      Map<TypeVariable<?>, Type> typeArguments =
          type.getType() instanceof ParameterizedType pt ? TypeUtils.getTypeArguments(pt) : Map.of();
      for (Method method : type.getRawType().getDeclaredMethods()) {
        for (Type typeArg : method.getGenericParameterTypes()) {
          Class<?> arg = TypeToken.of(typeArguments.getOrDefault(typeArg, typeArg)).getRawType();
          registerSimpleAlias(registry, arg);
        }
        Type returnType = method.getGenericReturnType();
        if (returnType != null && !Void.class.equals(returnType)) {
          Class<?> returnClass = TypeToken.of(TypeUtils.unrollVariables(typeArguments, returnType)).getRawType();
          registerSimpleAlias(registry, returnClass);
        }
      }
    });
  }

  /**
   * Registers a simple package-less alias for the resolved type.
   */
  private void registerSimpleAlias(final TypeAliasRegistry registry, final Class<?> clazz) {
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
   * Registers the {@link DataAccess} type with MyBatis.
   */
  private void registerDataAccessMapper(final Class<? extends DataAccess> accessType) {
    Class<?> templateType = findTemplateType(accessType);
    if (templateType != null) {
      registerTemplatedMapper(accessType, templateType);
    }
    else {
      registerSimpleMapper(accessType);
    }
    debug(REGISTERED_MESSAGE, accessType.getSimpleName());
  }

  /**
   * Searches the directly declared interfaces for one annotated with {@link SchemaTemplate}.
   */
  @Nullable
  private static Class<?> findTemplateType(final Class<? extends DataAccess> accessType) {
    for (Class<?> candidate : accessType.getInterfaces()) {
      if (candidate.isAnnotationPresent(SchemaTemplate.class)) {
        return candidate;
      }
    }
    return null; // accessType does not directly extend an interface annotated with @SchemaTemplate
  }

  /**
   * Registers a simple {@link DataAccess} type with MyBatis.
   */
  private void registerSimpleMapper(final Class<? extends DataAccess> accessType) {
    // make sure any types expected by the access type are registered first
    Expects expects = accessType.getAnnotation(Expects.class);
    if (expects != null) {
      asList(expects.value()).forEach(this::register);
    }

    // MyBatis will load the corresponding XML schema
    mybatisConfig.addMapper(accessType);
  }

  /**
   * Registers a templated {@link DataAccess} type with MyBatis.
   */
  @SuppressWarnings("unchecked")
  private void registerTemplatedMapper(final Class<? extends DataAccess> accessType, final Class<?> templateType) {

    // make sure any types expected by the template are registered first
    Expects expects = templateType.getAnnotation(Expects.class);
    if (expects != null) {
      asList(expects.value()).forEach(expectedType -> {
        if (expectedType.isAnnotationPresent(SchemaTemplate.class)) {
          // also a template which we need to resolve using the current context
          register(expectedAccessType(accessType, templateType, expectedType));
        }
        else {
          register(expectedType);
        }
      });
    }

    // lower-case prefix of the access type, excluding the package; for example MavenAssetDAO / AssetDAO = maven
    String prefix = extractPrefix(accessType.getSimpleName(), templateType.getSimpleName());

    // the variable in the schema XML that we'll replace with the local prefix
    String placeholder = templateType.getAnnotation(SchemaTemplate.class).value();

    // load and populate the template's mapper XML
    String xml = loadMapperXml(templateType, true)
        .replace("${namespace}", accessType.getName())
        .replace("${" + placeholder + '}', prefix);

    // now append the access type's mapper XML if it has one (this is where it can define extra fields/methods)
    String includeXml = loadMapperXml(accessType, false);
    Matcher mapperBody = MAPPER_BODY.matcher(includeXml);
    if (mapperBody.find()) {
      xml = xml.replace("</mapper>", format("<!-- %s -->%s</mapper>", accessType.getName(), mapperBody.group(1)));
    }

    try {
      log.trace(xml);

      XMLMapperBuilder xmlParser = new XMLMapperBuilder(
          new ByteArrayInputStream(xml.getBytes(UTF_8)),
          mybatisConfig,
          accessType.getName(),
          mybatisConfig.getSqlFragments(),
          accessType.getName());

      xmlParser.parse();

      // when the type is not visible from this classloader then MyBatis will prepare the mapper
      // but not do the final registration - so this call is needed to complete the registration
      if (!mybatisConfig.hasMapper(accessType)) {
        mybatisConfig.addMapper(accessType);
      }
    }
    catch (RuntimeException e) {
      log.warn(xml, e);
      throw e;
    }
  }

  /**
   * Finds the expected access type for the expected template type, given the current access and template types.
   */
  private Class expectedAccessType(
      final Class currentAccessType,
      final Class currentTemplateType,
      final Class expectedTemplateType)
  {
    String currentTemplateName = currentTemplateType.getSimpleName();
    String expectedTemplateName = expectedTemplateType.getSimpleName();

    // MavenAssetDAO -> s/AssetDAO/ComponentDAO/ -> MavenComponentDAO
    String currentAccessName = currentAccessType.getSimpleName();
    String expectedAccessName = currentAccessName.replace(currentTemplateName, expectedTemplateName);

    // check registered types first (includes UT registered DAOs) before searching declared DAOs
    Class expectedAccessType = findRegisteredAccessType(expectedAccessName)
        .orElseGet(() -> findDeclaredAccessType(expectedAccessName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Access type " + expectedAccessName + " expected by " + currentAccessType + " is missing")));

    // sanity check that this type has the expected class hierarchy
    checkArgument(expectedTemplateType.isAssignableFrom(expectedAccessType),
        "%s must extend %s", expectedAccessType, expectedTemplateType);

    return expectedAccessType;
  }

  /**
   * Returns the first DAO with the given simpleName registered with this store.
   */
  private Optional<Class<?>> findRegisteredAccessType(final String simpleName) {
    return registeredAccessTypes.stream()
        .filter(accessType -> simpleName.equals(accessType.getSimpleName()))
        .findFirst();
  }

  /**
   * Returns the first DAO with the given simpleName declared by a plugin/bundle.
   */
  private Optional<? extends Class> findDeclaredAccessType(final String simpleName) {
    return daos(declaredAccessTypes)
        .filter(accessType -> simpleName.equals(accessType.getSimpleName()))
        .findFirst();
  }

  /**
   * Returns the lower-case form of the prefix after removing templateName from accessName.
   */
  private static String extractPrefix(final String accessName, final String templateName) {
    checkArgument(accessName.endsWith(templateName), "%s must end with %s", accessName, templateName);
    String prefix = lower(accessName.substring(0, accessName.length() - templateName.length()));
    checkArgument(!prefix.isEmpty(), "%s must add a prefix to %s", accessName, templateName);
    return prefix;
  }

  /**
   * Absolute resource path to the type's mapper XML.
   */
  private static String mapperXmlPath(final Class type) {
    return '/' + type.getName().replace('.', '/') + ".xml";
  }

  /**
   * Attempts to load the type's mapper XML. If the XML is missing and required then it throws
   * {@link IllegalArgumentException}. If it is not required then it returns the empty string.
   */
  private static String loadMapperXml(final Class type, final boolean required) {
    URL resource = type.getResource(mapperXmlPath(type));
    checkArgument(!required || resource != null, "XML resource for %s is missing", type);
    try {
      return resource != null ? Resources.toString(resource, UTF_8) : "";
    }
    catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + resource, e);
    }
  }

  /**
   * Registers the given {@link Interceptor} with MyBatis.
   */
  @VisibleForTesting
  public void register(final Interceptor interceptor) {
    mybatisConfig.addInterceptor(interceptor);
    debug(REGISTERED_MESSAGE, interceptor.getClass().getSimpleName());
  }

  /**
   * Registers the given {@link TypeHandler} with MyBatis.
   */
  @VisibleForTesting
  public void register(final TypeHandler<?> handler) {
    prepare(handler).register(handler);
    debug(REGISTERED_MESSAGE, handler.getClass().getSimpleName());
  }

  /**
   * Registers the given {@link TypeHandler} with MyBatis binding it to an explicit type.
   */
  private <T> void register(final Class<T> type, final TypeHandler<? extends T> handler) {
    prepare(handler).register(type, handler);
    debug(REGISTERED_MESSAGE, handler.getClass().getSimpleName() + " (" + type.getSimpleName() + ")");
  }

  /**
   * Registers the given {@link TypeHandler} with MyBatis without binding it to any type.
   */
  private <T> void registerDetached(final TypeHandler<? extends T> handler) {
    prepare(handler).register(null, null, handler);
    debug(REGISTERED_MESSAGE, handler.getClass().getSimpleName() + " (detached)");
  }

  /**
   * Prepare the {@link TypeHandler} for use with this datastore.
   */
  private TypeHandlerRegistry prepare(final TypeHandler<?> handler) {
    if (handler instanceof CipherAwareTypeHandler<?>) {
      ((CipherAwareTypeHandler<?>) handler).setCipher(databaseCipher);
    }

    registerSimpleAlias(mybatisConfig.getTypeAliasRegistry(), handler.getClass());
    Type handledType = ((BaseTypeHandler) handler).getRawType();
    if (handledType instanceof Class<?>) {
      registerSimpleAlias(mybatisConfig.getTypeAliasRegistry(), (Class<?>) handledType);
    }
    return mybatisConfig.getTypeHandlerRegistry();
  }

  /**
   * Force use of MyBatis {@link ClassLoader} when generating entity proxies.
   */
  private void useMyBatisClassLoaderForEntityProxies() {

    // MyBatis uses an embedded copy of Javassist to generate proxies for lazy entities
    // which by default uses the entity classloader. Unfortunately the proxies also want
    // to see the Javassist proxy package which is embedded in MyBatis but not exported.

    // To workaround this visibility issue we need to set a custom 'ClassLoaderProvider'
    // that always returns the MyBatis classloader. This can see the embedded Javassist
    // code as well as the entity (thanks to MyBatis having "DynamicImport-Package:*")

    // This isn't as clean as it could be because we can't refer directly to these types.
    // Instead we need to use reflection and a JDK proxy to create and set our provider.

    try {
      ClassLoader myBatisLoader = Configuration.class.getClassLoader();

      String proxyFactoryName = "org.apache.ibatis.javassist.util.proxy.ProxyFactory";
      Class<?> proxyFactoryClass = myBatisLoader.loadClass(proxyFactoryName);
      String classLoaderProviderName = proxyFactoryName + "$ClassLoaderProvider";
      Class<?>[] classLoaderProviderApi = {myBatisLoader.loadClass(classLoaderProviderName)};
      Field classLoaderProviderField = proxyFactoryClass.getField("classLoaderProvider");

      classLoaderProviderField.set(null, Proxy.newProxyInstance(myBatisLoader,
          classLoaderProviderApi, (proxy, method, args) -> myBatisLoader));
    }
    catch (Exception | LinkageError e) {
      log.warn("Problem applying MyBatis proxy workaround", e);
    }
  }

  private static Stream<Class<DataAccess>> daos(final List<TransactionalStoreSupport> stores) {
    return stores.stream()
        .map(TransactionalStoreSupport::getDaoClass);
  }
}

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
package org.sonatype.nexus.testsuite.testsupport.system.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.GroupRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.HostedRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.ProxyRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.RepositoryConfig;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

public abstract class FormatRepositoryTestSystemSupport
    <HOSTED extends HostedRepositoryConfig<?>,
        PROXY extends ProxyRepositoryConfig<?>,
        GROUP extends GroupRepositoryConfig<?>>
    implements FormatRepositoryTestSystem
{
  public static final String ATTRIBUTES_MAP_KEY_STORAGE = "storage";

  public static final String ATTRIBUTES_MAP_KEY_REPLICATION = "replication";

  public static final String ATTRIBUTES_MAP_KEY_HTTPCLIENT = "httpclient";

  public static final String ATTRIBUTES_MAP_KEY_PROXY = "proxy";

  public static final String ATTRIBUTES_MAP_KEY_GROUP = "group";

  public static final String ATTRIBUTES_MAP_KEY_NEGATIVE_CACHE = "negativeCache";

  public static final String ATTRIBUTES_MAP_KEY_CONNECTION = "connection";

  public static final String ATTRIBUTES_MAP_KEY_AUTHENTICATION = "authentication";

  public static final String ATTRIBUTES_KEY_BLOBSTORE = "blobStoreName";

  public static final String ATTRIBUTES_KEY_DATA_STORE_NAME = "dataStoreName";

  public static final String ATTRIBUTES_KEY_STRICT_CONTENT_VALIDATION = "strictContentTypeValidation";

  public static final String ATTRIBUTES_KEY_WRITE_POLICY = "writePolicy";

  public static final String ATTRIBUTES_KEY_LATEST_POLICY = "latestPolicy";

  public static final String ATTRIBUTES_KEY_BLOCKED = "blocked";

  public static final String ATTRIBUTES_KEY_AUTO_BLOCKED = "autoBlock";

  public static final String ATTRIBUTES_KEY_USERNAME = "username";

  public static final String ATTRIBUTES_KEY_PASSWORD = "password";

  public static final String ATTRIBUTES_KEY_AUTHENTICATION_TYPE = "type";

  public static final String ATTRIBUTES_VALUE_AUTHENTICATION_TYPE_USERNAME = "username";

  public static final String ATTRIBUTES_KEY_REMOTE_URL = "remoteUrl";

  public static final String ATTRIBUTES_KEY_CONTENT_MAX_AGE = "contentMaxAge";

  public static final String ATTRIBUTES_KEY_METADATA_MAX_AGE = "metadataMaxAge";

  public static final String ATTRIBUTES_KEY_NC_ENABLED = "enabled";

  public static final String ATTRIBUTES_KEY_NC_TTL = "timeToLive";

  public static final String ATTRIBUTES_KEY_MEMBERS = "memberNames";

  public static final String ATTRIBUTES_KEY_GROUP_WRITE_MEMBER = "groupWriteMember";

  public static final String ATTRIBUTES_KEY_REPLICATION_ENABLED = "enabled";

  public static final String ATTRIBUTES_KEY_PULL_REPLICATION_ENABLED = "preemptivePullEnabled";

  public static final String ATTRIBUTES_ASSET_PATH_REGEX = "assetPathRegex";

  private final RepositoryManager repositoryManager;

  private Consumer<String> tracker;

  public FormatRepositoryTestSystemSupport(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public void installTracker(Consumer<String> tracker) {
    this.tracker = tracker;
  }

  protected Repository doCreate(final Configuration configuration) {
    boolean baseUrlSet = BaseUrlHolder.isSet();
    try {
      if (!baseUrlSet) {
        BaseUrlHolder.set("http://localhost:1234", "");
      }
      Repository repository = repositoryManager.create(configuration);
      if (tracker != null) {
        tracker.accept(repository.getName());
      }
      return repository;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      if (!baseUrlSet) {
        BaseUrlHolder.unset();
      }
    }
  }

  protected Configuration createHostedConfiguration(HOSTED config) {
    return applyHostedAttributes(applyCommonAttributes(repositoryManager.newConfiguration(), config), config);
  }

  protected Configuration createProxyConfiguration(PROXY config) {
    return applyProxyAttributes(applyCommonAttributes(repositoryManager.newConfiguration(), config), config);
  }

  protected Configuration createGroupConfiguration(GROUP config) {
    return applyGroupAttributes(applyCommonAttributes(repositoryManager.newConfiguration(), config), config);
  }

  private <T extends RepositoryConfig<?>> Configuration applyCommonAttributes(Configuration configuration, T config) {
    if (config.getName() != null) {
      configuration.setRepositoryName(config.getName());
    }
    if (config.getRecipe() != null) {
      configuration.setRecipeName(config.getRecipe());
    }
    if (config.isOnline() != null) {
      configuration.setOnline(config.isOnline());
    }

    NestedAttributesMap storage = configuration.attributes(ATTRIBUTES_MAP_KEY_STORAGE);
    addConfigIfNotNull(storage, ATTRIBUTES_KEY_DATA_STORE_NAME, config.getDatastoreName());
    addConfigIfNotNull(storage, ATTRIBUTES_KEY_BLOBSTORE, config.getBlobstore());
    addConfigIfNotNull(storage, ATTRIBUTES_KEY_STRICT_CONTENT_VALIDATION, config.isStrictContentTypeValidation());

    return configuration;
  }

  private Configuration applyHostedAttributes(Configuration configuration, HOSTED config) {
    NestedAttributesMap storage = configuration.attributes(ATTRIBUTES_MAP_KEY_STORAGE);
    addConfigIfNotNull(storage, ATTRIBUTES_KEY_WRITE_POLICY, config.getWritePolicy());
    addConfigIfNotNull(configuration.attributes(ATTRIBUTES_MAP_KEY_REPLICATION), ATTRIBUTES_KEY_REPLICATION_ENABLED, config.isReplicationEnabled());

    return configuration;
  }

  private Configuration applyProxyAttributes(Configuration configuration, PROXY config) {
    NestedAttributesMap httpclient = configuration.attributes(ATTRIBUTES_MAP_KEY_HTTPCLIENT);

    addConfigIfNotNull(httpclient, ATTRIBUTES_KEY_BLOCKED, config.isBlocked());
    addConfigIfNotNull(httpclient, ATTRIBUTES_KEY_AUTO_BLOCKED, config.isAutoBlocked());

    addToMapCreateIfNeeded(httpclient, ATTRIBUTES_MAP_KEY_AUTHENTICATION, ATTRIBUTES_KEY_USERNAME,
        config.getUsername());
    addToMapCreateIfNeeded(httpclient, ATTRIBUTES_MAP_KEY_AUTHENTICATION, ATTRIBUTES_KEY_PASSWORD,
        config.getPassword());
    addToMapIfNotEmpty(httpclient, ATTRIBUTES_MAP_KEY_AUTHENTICATION, ATTRIBUTES_KEY_AUTHENTICATION_TYPE,
        ATTRIBUTES_VALUE_AUTHENTICATION_TYPE_USERNAME);

    NestedAttributesMap proxy = configuration.attributes(ATTRIBUTES_MAP_KEY_PROXY);
    addConfigIfNotNull(proxy, ATTRIBUTES_KEY_REMOTE_URL, config.getRemoteUrl());
    addConfigIfNotNull(proxy, ATTRIBUTES_KEY_CONTENT_MAX_AGE, config.getContentMaxAge());
    addConfigIfNotNull(proxy, ATTRIBUTES_KEY_METADATA_MAX_AGE, config.getMetadataMaxAge());

    NestedAttributesMap negativeCache = configuration.attributes(ATTRIBUTES_MAP_KEY_NEGATIVE_CACHE);
    addConfigIfNotNull(negativeCache, ATTRIBUTES_KEY_NC_ENABLED, config.isNegativeCacheEnabled());
    addConfigIfNotNull(negativeCache, ATTRIBUTES_KEY_NC_TTL, config.getNegativeCacheTimeToLive());

    NestedAttributesMap replication = configuration.attributes(ATTRIBUTES_MAP_KEY_REPLICATION);
    addConfigIfNotNull(replication, ATTRIBUTES_KEY_PULL_REPLICATION_ENABLED, config.isPreemptivePullEnabled());
    addConfigIfNotNull(replication, ATTRIBUTES_ASSET_PATH_REGEX, config.getAssetPathRegex());

    return configuration;
  }

  private Configuration applyGroupAttributes(final Configuration configuration, final GROUP config) {
    NestedAttributesMap group = configuration.attributes(ATTRIBUTES_MAP_KEY_GROUP);
    addConfigIfNotNull(group, ATTRIBUTES_KEY_MEMBERS, config.getMembers());
    addConfigIfNotNull(group, ATTRIBUTES_KEY_GROUP_WRITE_MEMBER, config.getGroupWriteMember());
    return configuration;
  }

  protected void addConfigIfNotNull(
      final NestedAttributesMap nestedAttributesMap,
      final String key,
      final String[] value)
  {
    if (value != null) {
      nestedAttributesMap.set(key, asList(value));
    }
  }

  protected void addConfigIfNotNull(
      final NestedAttributesMap nestedAttributesMap,
      final String key,
      final Object value)
  {
    if (value != null) {
      nestedAttributesMap.set(key, value);
    }
  }

  protected void addConfigIfNotNull(
      final NestedAttributesMap nestedAttributesMap,
      final String key,
      final Enum<?> value)
  {
    if (value != null) {
      nestedAttributesMap.set(key, value.name());
    }
  }

  protected void addConfigIfNotNull(final Map<String, Object> map, final String key, final Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  protected void addMapIfNotEmpty(
      final NestedAttributesMap nestedAttributesMap,
      final String key,
      final Map<String, Object> map)
  {
    if (!map.isEmpty()) {
      nestedAttributesMap.set(key, map);
    }
  }

  protected void addToMapCreateIfNeeded(
      final NestedAttributesMap nestedAttributesMap,
      final String mapKey,
      final String key,
      final Object value)
  {
    Map<String, Object> map = getMapAttribute(nestedAttributesMap, mapKey);
    if (map == null) {
      map = new HashMap<>();
    }
    addConfigIfNotNull(map, key, value);
    addMapIfNotEmpty(nestedAttributesMap, mapKey, map);
  }

  protected void addToMapIfNotEmpty(
      final NestedAttributesMap nestedAttributesMap,
      final String mapKey,
      final String key,
      final Object value)
  {
    Map<String, Object> map = getMapAttribute(nestedAttributesMap, mapKey);
    if (map != null && !map.isEmpty()) {
      map.put(key, value);
    }
  }

  protected void removeConfig(final Configuration configuration, final String key) {
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes != null) {
      attributes.remove(key);
    }
  }

  private Map<String, Object> getMapAttribute(final NestedAttributesMap nestedAttributesMap, final String key) {
    return (Map<String, Object>) nestedAttributesMap.get(key);
  }
}

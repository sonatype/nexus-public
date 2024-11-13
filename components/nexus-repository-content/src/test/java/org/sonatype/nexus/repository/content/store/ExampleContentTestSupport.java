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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.toHexString;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

/**
 * Support for {@link RepositoryContent} tests.
 */
public class ExampleContentTestSupport
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME)
      .handle(new BlobRefTypeHandler())
      .access(TestContentRepositoryDAO.class)
      .access(TestComponentDAO.class)
      .access(TestAssetBlobDAO.class)
      .access(TestAssetDAO.class)
      .access(ConfigurationDAO.class);

  private Random random = new Random();

  private List<String> namespaces;

  private List<String> names;

  private List<String> versions;

  private List<String> paths;

  private List<ContentRepositoryData> repositories;

  private List<ComponentData> components;

  private List<AssetBlobData> assetBlobs;

  private List<AssetData> assets;

  private List<ConfigurationData> configurations;

  public ExampleContentTestSupport() {
    // do nothing
  }

  public ExampleContentTestSupport(final Class<? extends DataAccess> accessType) {
    sessionRule.access(accessType);
  }

  @SafeVarargs
  public ExampleContentTestSupport(final Class<? extends DataAccess>... accessTypes) {
    for (Class<? extends DataAccess> accessType : accessTypes) {
      sessionRule.access(accessType);
    }
  }

  protected List<ContentRepositoryData> generatedRepositories() {
    return unmodifiableList(repositories);
  }

  protected List<String> generatedNamespaces() {
    return unmodifiableList(namespaces);
  }

  protected List<String> generatedNames() {
    return unmodifiableList(names);
  }

  protected List<String> generatedVersions() {
    return unmodifiableList(versions);
  }

  protected List<Component> generatedComponents() {
    return unmodifiableList(components);
  }

  protected List<AssetBlob> generatedAssetBlobs() {
    return unmodifiableList(assetBlobs);
  }

  protected List<Asset> generatedAssets() {
    return unmodifiableList(assets);
  }

  protected List<ConfigurationData> generatedConfigurations() {
    return unmodifiableList(configurations);
  }

  protected void generateRandomNamespaces(final int maxNamespaces) {
    Set<String> uniqueNamespaces = new HashSet<>();
    StringBuilder buf = new StringBuilder();
    while (uniqueNamespaces.size() < maxNamespaces) {
      buf.append(random.nextBoolean() ? "com" : "org");
      for (int i = 0, maxSegments = random.nextInt(10); i < maxSegments; i++) {
        buf.append('.').append(toHexString(random.nextInt()));
      }
      uniqueNamespaces.add(buf.toString());
      buf.setLength(0);
    }
    namespaces = ImmutableList.copyOf(uniqueNamespaces);
  }

  protected void generateRandomNames(final int maxNames) {
    Set<String> uniqueNames = new HashSet<>();
    while (uniqueNames.size() < maxNames) {
      uniqueNames.add(toHexString(random.nextInt()));
    }
    names = ImmutableList.copyOf(uniqueNames);
  }

  protected void generateRandomVersions(final int maxVersions) {
    Set<String> uniqueVersions = new HashSet<>();
    while (uniqueVersions.size() < maxVersions) {
      uniqueVersions.add(random.nextInt(1000) + "." + random.nextInt(1000) + "." + random.nextInt(1000));
    }
    versions = ImmutableList.copyOf(uniqueVersions);
  }

  protected void generateRandomPaths(final int maxPaths) {
    Set<String> uniquePaths = new HashSet<>();
    StringBuilder buf = new StringBuilder();
    while (uniquePaths.size() < maxPaths) {
      buf.append(random.nextBoolean() ? "/com" : "/org");
      for (int i = 0, maxSegments = random.nextInt(10); i < maxSegments; i++) {
        buf.append('/').append(toHexString(random.nextInt()));
      }
      uniquePaths.add(buf.toString());
      buf.setLength(0);
    }
    paths = ImmutableList.copyOf(uniquePaths);
  }

  protected void generateConfiguration() {
    final ConfigurationData configurationData = getConfigurationData();
    if (doCommit(session -> session.access(ConfigurationDAO.class).create(configurationData))) {
      configurations = Collections.singletonList(configurationData);
    }
  }

  @NotNull
  private ConfigurationData getConfigurationData() {
    ConfigurationData configurationData = new ConfigurationData();
    Map<String, Object> key = Collections.singletonMap("key", "value");
    Map<String, Map<String, Object>> attributes = Collections.singletonMap("key", key);
    configurationData.setAttributes(attributes);
    configurationData.setName("repo-name");
    configurationData.setOnline(true);
    configurationData.setRecipeName("recipe");
    return configurationData;
  }

  protected void generateRandomRepositories(final int maxRepositories) {
    repositories = new ArrayList<>();
    while (repositories.size() < maxRepositories) {
      ContentRepositoryData repository = randomContentRepository();
      if (doCommit(session -> session.access(TestContentRepositoryDAO.class).createContentRepository(repository))) {
        repositories.add(repository);
      }
    }
  }

  protected void generateSingleRepository(final UUID configRepositoryUUID) {
    ContentRepositoryData repository = randomContentRepository();
    repository.setConfigRepositoryId(new EntityUUID(configRepositoryUUID));
    if (doCommit(session -> session.access(TestContentRepositoryDAO.class).createContentRepository(repository))) {
      repositories = Collections.singletonList(repository);
    }
  }

  protected void generateRandomContent(final int maxComponents, final int maxAssets, final boolean entityVersionEnabled) {
    components = new ArrayList<>();
    while (components.size() < maxComponents) {
      int repositoryId = repositories.get(random.nextInt(repositories.size())).repositoryId;
      ComponentData component = randomComponent(repositoryId);
      if (doCommit(session -> session.access(TestComponentDAO.class).createComponent(component, entityVersionEnabled))) {
        components.add(component);
      }
    }

    assets = new ArrayList<>();
    assetBlobs = new ArrayList<>();
    while (assets.size() < maxAssets) {
      ComponentData component = components.get(random.nextInt(components.size()));
      AssetData asset = randomAsset(component.repositoryId);
      if (random.nextInt(100) > 10) {
        asset.setComponent(component);
      }
      if (doCommit(session -> session.access(TestAssetDAO.class).createAsset(asset, false))) {
        assets.add(asset);
        AssetBlobData assetBlob = randomAssetBlob();
        if (doCommit(session -> {
          session.access(TestAssetBlobDAO.class).createAssetBlob(assetBlob);
          if (random.nextInt(100) > 10) {
            asset.setAssetBlob(assetBlob);
            session.access(TestAssetDAO.class).updateAssetBlobLink(asset, false);
          }
        })) {
          assetBlobs.add(assetBlob);
        }
      }
    }
  }

  // Generate the component with asset and blob
  protected void generateContent(final int maxComponents, final boolean entityVersionEnabled) {
    List<String> componentNames = new ArrayList<>(maxComponents);
    IntStream
        .range(0, maxComponents)
        .forEach(i -> componentNames.add("component_name" + i));
    generateContent(componentNames, entityVersionEnabled);
  }

  // Generate the components with asset and blob
  protected void generateContent(final List<String> componentNames, final boolean entityVersionEnabled) {
    int maxComponents = componentNames.size();
    components = new ArrayList<>(maxComponents);
    for (String componentName : componentNames) {
      int repositoryId = repositories.get(random.nextInt(repositories.size())).repositoryId;
      ComponentData component = randomComponent(repositoryId, componentName);
      if (doCommit(session -> session.access(TestComponentDAO.class).createComponent(component, entityVersionEnabled))) {
        components.add(component);
      }
    }

    assets = new ArrayList<>(maxComponents);
    assetBlobs = new ArrayList<>(maxComponents);
    for (ComponentData component : components) {
      AssetData asset = generateAsset(component.repositoryId, "/" + UUID.randomUUID());
      asset.setAssetId(component.componentId);
      asset.setComponent(component);

      if (doCommit(session -> session.access(TestAssetDAO.class).createAsset(asset, false))) {
        assets.add(asset);
        AssetBlobData assetBlob = randomAssetBlob();
        if (doCommit(session -> {
          session.access(TestAssetBlobDAO.class).createAssetBlob(assetBlob);
          asset.setAssetBlob(assetBlob);
          session.access(TestAssetDAO.class).updateAssetBlobLink(asset, false);
        })) {
          assetBlobs.add(assetBlob);
        }
      }
    }
  }

  protected void generateProvidedContent(final List<ComponentData> providedComponents, final boolean entityVersionEnabled) {
    List<ComponentData> components = new ArrayList<>(providedComponents.size());
    providedComponents.forEach(component -> {
      if (doCommit(session -> session.access(TestComponentDAO.class).createComponent(component, entityVersionEnabled))) {
            components.add(component);
          }
        });

    assets = new ArrayList<>(components.size());
    assetBlobs = new ArrayList<>(components.size());
    for (ComponentData component : components) {
      AssetData asset = generateAsset(component.repositoryId, "/" + UUID.randomUUID());
      asset.setAssetId(component.componentId);
      asset.setComponent(component);

      if (doCommit(session -> session.access(TestAssetDAO.class).createAsset(asset, false))) {
        assets.add(asset);
        AssetBlobData assetBlob = randomAssetBlob();
        if (doCommit(session -> {
          session.access(TestAssetBlobDAO.class).createAssetBlob(assetBlob);
          asset.setAssetBlob(assetBlob);
          session.access(TestAssetDAO.class).updateAssetBlobLink(asset, false);
        })) {
          assetBlobs.add(assetBlob);
        }
      }
    }
  }

  protected ContentRepositoryData randomContentRepository() {
    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    repository.setAttributes(newAttributes("repository"));
    return repository;
  }

  protected ComponentData randomComponent(final int repositoryId) {
    String name = names.get(random.nextInt(names.size()));
    return randomComponent(repositoryId, name);
  }

  protected ComponentData randomComponent(final int repositoryId, final String name) {
    ComponentData component = new ComponentData();
    component.setRepositoryId(repositoryId);
    if (random.nextInt(100) > 10) {
      component.setNamespace(namespaces.get(random.nextInt(namespaces.size())));
    }
    else {
      component.setNamespace("");
    }
    component.setName(name);
    if (random.nextInt(100) > 10) {
      component.setVersion(versions.get(random.nextInt(versions.size())));
    }
    else {
      component.setVersion("");
    }
    component.setNormalizedVersion(component.version());
    component.setAttributes(newAttributes("component"));
    component.setKind("aKind");
    return component;
  }
  protected ComponentData component(final int repositoryId, final String namespace, final String name, final String version) {
    ComponentData component = new ComponentData();
    component.setRepositoryId(repositoryId);
    component.setNamespace(namespace);
    component.setName(name);
    component.setVersion(version);
    component.setNormalizedVersion(version);
    component.setAttributes(newAttributes("component"));
    component.setKind("aKind");
    return component;
  }

  protected TestAssetData randomAsset(final int repositoryId) {
    return generateAsset(repositoryId, paths.get(random.nextInt(paths.size())));
  }

  protected TestAssetData randomAsset(final int repositoryId, final String kind) {
    TestAssetData asset = randomAsset(repositoryId);
    asset.setKind(kind);
    return asset;
  }

  protected TestAssetData generateAsset(final int repositoryId, final String path) {
    TestAssetData asset = new TestAssetData();
    asset.setRepositoryId(repositoryId);
    asset.setPath(path);
    asset.setKind("test");
    asset.setAttributes(newAttributes("asset"));
    asset.setLastUpdated(OffsetDateTime.now());
    return asset;
  }

  protected AssetBlobData randomAssetBlob(final OffsetDateTime blobCreated) {
    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef("test-node", "test-store", randomUUID().toString()));
    assetBlob.setBlobSize(random.nextInt(1024 * 1024));
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(blobCreated);
    assetBlob.setAddedToRepository(blobCreated);
    return assetBlob;
  }
  
  protected AssetBlobData randomAssetBlob(){
    return randomAssetBlob(UTC.now().minusMinutes(120));
  }

  protected NestedAttributesMap newAttributes(final String key) {
    return new NestedAttributesMap("attributes", new HashMap<>(ImmutableMap.of(key, "test-value")));
  }

  private boolean doCommit(final Consumer<DataSession<?>> consumer) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      consumer.accept(session);
      session.getTransaction().commit();
      return true;
    }
    catch (DuplicateKeyException e) {
      logger.debug("Skipping duplicate generated content", e);
      return false;
    }
  }

  protected static Matcher<ContentRepository> sameConfigRepository(final ContentRepository expected) {
    return new FieldMatcher<>(expected, ContentRepository::configRepositoryId);
  }

  protected static Matcher<Component> sameCoordinates(final Component expected) {
    return new FieldMatcher<>(expected, Component::namespace, Component::name, Component::kind, Component::version);
  }

  protected static Matcher<Component> sameKind(final Component expected) {
    return new FieldMatcher<>(expected, Component::kind);
  }

  protected static Matcher<Asset> samePath(final Asset expected) {
    return new FieldMatcher<>(expected, Asset::path);
  }

  protected static Matcher<Asset> sameKind(final Asset expected) {
    return new FieldMatcher<>(expected, Asset::kind);
  }

  protected static Matcher<Asset> sameBlob(final Asset expected) {
    return new FieldMatcher<>(expected, (Function<Asset, ?>) asset -> asset.blob().map(AssetBlob::blobRef));
  }

  protected static Matcher<AssetBlob> sameBlob(final AssetBlob expected) {
    return new FieldMatcher<>(expected, AssetBlob::blobRef, AssetBlob::blobSize, AssetBlob::contentType,
        assetBlob -> assetBlob.blobCreated().truncatedTo(ChronoUnit.SECONDS),
        AssetBlob::createdBy, AssetBlob::createdByIp);
  }

  protected static Matcher<Asset> sameLastDownloaded(final Asset expected) {
    return new FieldMatcher<>(expected, Asset::lastDownloaded);
  }

  protected static Matcher<RepositoryContent> sameAttributes(final RepositoryContent expected) {
    return new FieldMatcher<>(expected, (Function<RepositoryContent, ?>) content -> content.attributes().backing());
  }

  protected static Matcher<RepositoryContent> sameCreated(final RepositoryContent expected) {
    return new FieldMatcher<>(expected, RepositoryContent::created);
  }

  protected static Matcher<RepositoryContent> sameLastUpdated(final RepositoryContent expected) {
    return new FieldMatcher<>(expected, RepositoryContent::lastUpdated);
  }

  protected static Matcher<Component> sameEntityVersion(final Component expected) {
    return new FieldMatcher<>(expected, Component::entityVersion);
  }

  protected static class FieldMatcher<T>
      extends TypeSafeDiagnosingMatcher<T>
  {
    private final T expected;

    private final List<Function<T, ?>> extractors;

    @SafeVarargs
    public FieldMatcher(final T expected, final Function<T, ?>... extractors) {
      this.expected = checkNotNull(expected);
      this.extractors = asList(checkNotNull(extractors));
    }

    @Override
    protected boolean matchesSafely(final T actual, final Description description) {
      List<Object> actualValues = extractors.stream()
          .map(extractor -> extractor.apply(actual))
          .collect(toList());

      List<Matcher<?>> matchers = extractors.stream()
          .map(extractor -> extractor.apply(expected))
          .map(v -> v != null ? is(v) : nullValue())
          .collect(toList());

      @SuppressWarnings({"unchecked", "rawtypes"})
      // (use hamcrest class directly as javac picks the wrong static varargs method)
      boolean matches = new IsIterableContainingInOrder(matchers).matches(actualValues);
      if (!matches) {
        for (int i = 0; i < extractors.size(); i++) {
          if (i > 0) {
            description.appendText(" AND ");
          }
          matchers.get(i).describeMismatch(actualValues.get(i), description);
        }
      }
      return matches;
    }

    @Override
    public void describeTo(final Description description) {
      for (int i = 0; i < extractors.size(); i++) {
        if (i > 0) {
          description.appendText(" AND ");
        }
        description.appendDescriptionOf(is(extractors.get(i).apply(expected)));
      }
    }
  }
}

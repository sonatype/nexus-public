/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.hosted;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.util.AlphanumComparator;
import org.sonatype.nexus.util.io.StreamSupport;
import org.sonatype.sisu.goodies.common.Iso8601Date;

import com.bolyuba.nexus.plugin.npm.service.HostedMetadataService;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Processor that rebuilds/updates npm metadata from data extracted from tarballs.
 *
 * @since 2.11
 */
public class RecreateMetadataWalkerProcessor
    extends AbstractWalkerProcessor
{
  private static final String PACKAGE_JSON_PATH = "/package.json";

  private static final int MAX_PACKAGE_JSON_FILE_SIZE = 1_000_000;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ObjectMapper objectMapper;

  private final HostedMetadataService hostedMetadataService;

  private int packageRoots;

  private int packageVersions;

  public RecreateMetadataWalkerProcessor(final HostedMetadataService hostedMetadataService)
  {
    this.objectMapper = new ObjectMapper();
    this.hostedMetadataService = hostedMetadataService;
    this.packageRoots = 0;
    this.packageVersions = 0;
  }

  public int getPackageRoots() {
    return packageRoots;
  }

  public int getPackageVersions() {
    return packageVersions;
  }

  @Override
  public void processItem(final WalkerContext context, final StorageItem item)
      throws Exception
  {
    // nop
  }

  public void onCollectionExit(final WalkerContext context, final StorageCollectionItem coll)
      throws Exception
  {
    final TreeMap<String, Map<String, Object>> versions = Maps.newTreeMap(new AlphanumComparator());

    // collect all the possible tarballs
    final Collection<StorageItem> items = coll.list();
    for (StorageItem item : items) {
      if (!(item instanceof StorageFileItem)) {
        // not a file, just skip it
        continue;
      }
      if (!item.getName().endsWith(".tgz")) {
        // not a tarball, just skip it
        continue;
      }
      try {
        final StorageFileItem file = (StorageFileItem) item;
        final Map<String, Object> versionJson = extractPackageJson(file);
        if (!versionJson.containsKey("name") ||
            !versionJson.containsKey("version")) {
          // not containing needed elements, just skip it but this time log it
          log.info("Malformed package.json in {}", file.getRepositoryItemUid());
          continue;
        }
        final String version = String.valueOf(versionJson.get("version"));
        final Map<String, Object> dist = Maps.newHashMap();
        dist.put("tarball", "generated-on-request"); // We lost original value, but does not matter as is NX hosted
        dist.put("shasum", file.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_SHA1_KEY));
        versionJson.put("dist", dist);
        versions.put(version, versionJson);
      }
      catch (Exception e) {
        log.info("Failed to extract or malformed package.json from {}", item.getRepositoryItemUid(), e);
      }
    }

    // ie. dir empty or all extract failed?
    if (versions.isEmpty()) {
      return;
    }

    // rebuild package root as possible and push it into database
    final Map<String, Object> rootJson = Maps.newHashMap();
    rootJson.put("dist-tags", Maps.<String, Object>newHashMap());
    rootJson.put("versions", Maps.<String, Object>newHashMap());
    rootJson.put("x-nx-rebuilt", Iso8601Date.format(new Date()));

    for (String version : versions.keySet()) {
      try {
        processPackageJson(rootJson, versions.get(version));
      }
      catch (IllegalArgumentException e) {
        log.info("Failed to process package.json from {} version {}", coll.getPath(), version, e);
      }
    }

    // ie. all processPackageJson failed?
    if (((Map<String, Object>) rootJson.get("versions")).isEmpty()) {
      return;
    }

    final Map<String, Object> lastPackage = versions.get(versions.lastKey());
    final String lastVersion = String.valueOf(lastPackage.get("version"));
    final Map<String, Object> distTags = (Map<String, Object>) rootJson.get("dist-tags");
    distTags.remove(lastVersion);
    distTags.put("latest", lastVersion);

    final PackageRoot packageRoot = new PackageRoot(context.getRepository().getId(), rootJson);
    hostedMetadataService.consumePackageRoot(packageRoot);
    packageRoots = packageRoots + 1;
    packageVersions = packageVersions + versions.size();
  }

  private Map<String, Object> extractPackageJson(final StorageFileItem file) throws IOException {
    try (final GZIPInputStream gzipInputStream = new GZIPInputStream(file.getInputStream())) {
      log.debug("Examining TAR file");
      final TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);
      TarArchiveEntry tarEntry;
      // npm uses tar option "--strip-components=1", so we need to strip first path element to have proper names
      String tarEntryName;
      do {
        tarEntry = tarArchiveInputStream.getNextTarEntry();
        tarEntryName = null;
        if (tarEntry != null) {
          // tar entry names are "pathelem1/pathelem2/.../filename"
          int indexOfSlash = tarEntry.getName().indexOf("/");
          if (indexOfSlash > 0) {
            tarEntryName = tarEntry.getName().substring(indexOfSlash);
          }
          log.debug("tarEntry={}, name={}", tarEntry.getName(), tarEntryName);
        }
      }
      while (tarEntryName != null && !PACKAGE_JSON_PATH.equalsIgnoreCase(tarEntryName));
      // checks for corrupted data, we do want to report these
      checkArgument(tarEntryName != null, "Tar does not contains %s?", PACKAGE_JSON_PATH);
      checkArgument(tarEntry.isFile(), "Tar content %s not a file?", PACKAGE_JSON_PATH);
      checkArgument(tarEntry.getSize() < MAX_PACKAGE_JSON_FILE_SIZE, "Tar content too big %s: %s bytes",
          PACKAGE_JSON_PATH, tarEntry.getSize());
      // do this in memory, as package.json is expected to be fairly small (few kb at most)
      final ByteArrayOutputStream bos = new ByteArrayOutputStream(Ints.checkedCast(tarEntry.getSize()));
      StreamSupport.copy(tarArchiveInputStream, bos, StreamSupport.BUFFER_SIZE);

      try {
        return objectMapper.readValue(bos.toByteArray(), new TypeReference<HashMap<String, Object>>() { });
      }
      catch (JsonParseException e) {
        // fallback
        if (e.getMessage().contains("Invalid UTF-8 middle byte")) {
          log.debug("Tarball {} contains non-UTF package.json, parsing as ISO-8859-1: {}",
              file.getRepositoryItemUid(), e.getMessage());
          // try again, but assume ISO8859-1 encoding now, that is illegal for JSON
          return objectMapper.readValue(new String(bos.toByteArray(), Charsets.ISO_8859_1),
              new TypeReference<HashMap<String, Object>>() { });
        }
        throw e;
      }
    }
  }

  private void processPackageJson(final Map<String, Object> rootJson, final Map<String, Object> packageJson)
      throws Exception
  {
    copyAttributes(rootJson, packageJson);
    final String version = String.valueOf(packageJson.get("version"));
    final Map<String, Object> distTags = (Map<String, Object>) rootJson.get("dist-tags");
    final Map<String, Object> versions = (Map<String, Object>) rootJson.get("versions");
    distTags.put(version, version);
    versions.put(version, packageJson);
  }

  private void copyAttributes(final Map<String, Object> higher, final Map<String, Object> lower) {
    copyAttributeOrEnforce(higher, lower, "name");
    copyAttribute(higher, lower, "description");
    copyAttribute(higher, lower, "keywords");
    copyAttribute(higher, lower, "homepage");
    // TODO: figure out what else?
  }

  private void copyAttributeOrEnforce(final Map<String, Object> higher, final Map<String, Object> lower,
                                      final String attributeKey)
  {
    if (higher.containsKey(attributeKey) && lower.containsKey(attributeKey)) {
      checkArgument(
          Objects.equals(higher.get(attributeKey), lower.get(attributeKey)),
          "inconsistent attribute: %s higher=%s, lower=%s",
          attributeKey,
          higher.get(attributeKey),
          lower.get(attributeKey)
      );
    }
    else {
      copyAttribute(higher, lower, attributeKey);
    }
  }

  private void copyAttribute(final Map<String, Object> higher, final Map<String, Object> lower,
                             final String attributeKey)
  {
    if (lower.containsKey(attributeKey)) {
      higher.put(attributeKey, lower.get(attributeKey));
    }
  }
}

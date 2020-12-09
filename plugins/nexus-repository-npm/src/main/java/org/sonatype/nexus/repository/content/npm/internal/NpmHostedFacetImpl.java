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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.NpmPublishRequest;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.missingRevFieldMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteTarballUrlMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.DIST_TAGS;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_REV;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.selectVersionByTarballName;

/**
 * @since 3.28
 */
@Named
public class NpmHostedFacetImpl
    extends NpmFacetSupport
    implements NpmHostedFacet
{
  private final NpmRequestParser npmRequestParser;

  @Inject
  public NpmHostedFacetImpl(final NpmRequestParser npmRequestParser, final NpmPackageParser npmPackageParser) {
    super(npmPackageParser);
    this.npmRequestParser = npmRequestParser;
  }

  @Override
  public void deleteDistTags(final NpmPackageId packageId, final String tag, final Payload payload) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tag);
    log.debug("Deleting distTags: {}", packageId);

    if ("latest".equals(tag)) {
      throw new IOException("Unable to delete latest");
    }

    try {
      deleteDistTags(packageId, tag);
    }
    catch (IOException e) {
      log.info("Unable to obtain dist-tags for {}", packageId.id(), e);
    }
  }

  @Override
  public Set<String> deletePackage(final NpmPackageId packageId, final String revision) throws IOException {
    return deletePackage(packageId, revision, true);
  }

  @Override
  public Set<String> deletePackage(
      final NpmPackageId packageId,
      @Nullable final String revision,
      final boolean deleteBlobs) throws IOException
  {
    checkNotNull(packageId);

    Optional<NestedAttributesMap> oldPackageRoot = loadPackageRoot(packageId, content());
    if (revision != null) {
      oldPackageRoot.ifPresent(attr -> checkArgument(revision.equals(attr.get(META_REV, String.class))));

      return Collections.singleton(NpmContentFacet.tarballPath(packageId, revision));
    }

    if (!oldPackageRoot.isPresent()) {
      return Collections.emptySet();
    }

    Set<String> deletedPaths = new HashSet<>();

    oldPackageRoot.get().child("versions").keys().forEach(version -> {
      try {
        if (content().delete(packageId, version)) {
          deletedPaths.add(NpmContentFacet.tarballPath(packageId, version));
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    if (content().delete(packageId)) {
      deletedPaths.add(NpmContentFacet.metadataPath(packageId));
    }

    return deletedPaths;
  }

  @Override
  public Optional<String> deleteTarball(final NpmPackageId packageId, final String tarballName) throws IOException {
    return deleteTarball(packageId, tarballName, true);
  }

  @Override
  public Optional<String> deleteTarball(
      final NpmPackageId packageId,
      final String tarballName,
      final boolean deleteBlob) throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(tarballName);

    Optional<FluentAsset> tarballAsset = findTarballAsset(packageId, tarballName)
        .filter(FluentAsset::delete);
    if (!tarballAsset.isPresent()) {
      return Optional.empty();
    }

    tarballAsset.flatMap(FluentAsset::component)
        .flatMap(component -> findPackageTarballComponent(packageId, component.version()))
        .ifPresent(FluentComponent::delete);

    return tarballAsset.map(FluentAsset::path);
  }

  @Override
  public Optional<Content> getDistTags(final NpmPackageId packageId) throws IOException {
    checkNotNull(packageId);
    log.debug("Getting package: {}", packageId);

    try {
      NestedAttributesMap packageRoot = loadPackageRoot(packageId, content()).orElse(null);

      if (packageRoot == null) {
        return Optional.empty();
      }
      NestedAttributesMap distTags = packageRoot.child(DIST_TAGS);

      return Optional.of(distTagsToContent(distTags));
    }
    catch (IOException e) {
      log.info("Unable to obtain dist-tags for {}", packageId.id(), e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Content> getPackage(final NpmPackageId packageId) throws IOException {
    return content().get(packageId)
        .map(NpmFacetSupport::toNpmContent)
        .map(content -> content.fieldMatchers(asList(
            missingRevFieldMatcher(() -> "1"),// TODO unclear when this situation might occur
            rewriteTarballUrlMatcher(getRepository().getName(), packageId.id()))))
        .map(content -> content.packageId(packageId.id()));
  }

  @Override
  public Optional<Content> getTarball(final NpmPackageId packageId, final String tarballName) throws IOException {
    return findTarballAsset(packageId, tarballName).map(FluentAsset::download);
  }

  @Override
  public void putDistTags(final NpmPackageId packageId, final String tag, final Payload payload) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tag);
    log.debug("Updating distTags: {}", packageId);

    if ("latest".equals(tag)) {
      throw new IOException("Unable to update latest tag");
    }

    String version = parseVersionToTag(packageId, tag, payload);
    doPutDistTags(packageId, tag, version);
  }

  protected void doPutDistTags(final NpmPackageId packageId, final String tag, final String version)
      throws IOException
  {
    Optional<Content> packageRootAsset = content().get(packageId);
    if (!packageRootAsset.isPresent()) {
      return;
    }

    if (!findPackageTarballComponent(packageId, version).isPresent()) {
      throw new IOException(String
          .format("version %s of package %s is not present in repository %s", version, packageId.id(),
              getRepository().getName()));
    }

    try {
      updateDistTags(packageId, tag, version);
    }
    catch (IOException e) {
      log.error("Unable to update dist-tags for {}", packageId.id(), e);
    }
  }


  @Override
  public void putPackage(
      final NpmPackageId packageId,
      final String revision,
      final Payload payload) throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(payload);
    try (NpmPublishRequest request = npmRequestParser.parsePublish(getRepository(), payload)) {
      putPublishRequest(packageId, revision, request);
    }
  }

  @Override
  public void putPackageRoot(
      final NpmPackageId packageId,
      final String revision,
      final NestedAttributesMap newPackageRoot) throws IOException
  {
    boolean update = false;
    NestedAttributesMap packageRoot = newPackageRoot;
    NestedAttributesMap oldPackageRoot = loadPackageRoot(packageId, content()).orElse(null);

    if (oldPackageRoot != null) {
      String rev = revision;
      if (rev == null) {
        rev = packageRoot.get(META_REV, String.class);
      }
      // ensure revision is expected, client updates package that is in expected state
      if (rev != null) {
        // if revision is present, full document is being sent, no overlay must occur
        checkArgument(rev.equals(oldPackageRoot.get(META_REV, String.class)));
        update = true;
      }
      else {
        // if no revision present, snippet is being sent, overlay it (if old exists)
        packageRoot = NpmMetadataUtils.overlay(oldPackageRoot, packageRoot);
      }
    }

    updateRevision(packageRoot, NpmContentFacet.metadataPath(packageId), oldPackageRoot == null);

    savePackageRoot(packageId, packageRoot);

    if (update) {
      updateDeprecationFlags(packageId, packageRoot);
    }
  }

  private void updateRevision(final NestedAttributesMap packageRoot,
                              final String path,
                              final boolean createdPackageRoot)
  {
    String newRevision = "1";

    if (!createdPackageRoot) {
      if (packageRoot.contains(META_REV)) {
        String rev = packageRoot.get(META_REV, String.class);
        newRevision = Integer.toString(Integer.parseInt(rev) + 1);
      }
      else {
        /*
          This is covering the edge case when a new package is uploaded to a repository where the packageRoot already
          exists.

          If that packageRoot was created using an earlier version of NXRM where we didn't store the rev then we need
          to add it in. We also add the rev in on download but it is possible that someone is uploading a package where
          the packageRoot has never been downloaded before.
         */
        newRevision = "1"; // TODO previously this used Orient's document version
      }
    }

    packageRoot.set(META_ID, path.substring(1));
    packageRoot.set(META_REV, newRevision);
  }

  /**
   * Updates all the tarball components that belong to given package, updating their deprecated flags. Only changed
   * {@link Component}s are modified and saved.
   */
  private void updateDeprecationFlags(final NpmPackageId packageId,
                                      final NestedAttributesMap packageRoot)
  {
    final NestedAttributesMap versions = packageRoot.child(NpmMetadataUtils.VERSIONS);
    for (String componentVersion : findPackageTarballComponents(packageId)) {
      findPackageTarballComponent(packageId, componentVersion).ifPresent(tarballComponent -> {
        // integrity check: package doc must contain the tarball version
        checkState(versions.contains(tarballComponent.version()), "Package %s lacks tarball version %s", packageId,
            tarballComponent.version());
        final NestedAttributesMap version = versions.child(tarballComponent.version());
        final String deprecationMessage = version.get(NpmMetadataUtils.DEPRECATED, String.class);
        // in npm JSON, deprecated with non-empty string means deprecated, with empty or not present is not deprecated
        final boolean deprecated = !Strings2.isBlank(deprecationMessage);
        if (deprecated && !deprecationMessage
            .equals(tarballComponent.attributes().get(NpmAttributes.P_DEPRECATED, String.class))) {
          tarballComponent.attributes(AttributeChange.SET, NpmAttributes.P_DEPRECATED, deprecationMessage);
        }
        else if (!deprecated && tarballComponent.attributes().contains(NpmAttributes.P_DEPRECATED)) {
          tarballComponent.attributes(AttributeChange.REMOVE, NpmAttributes.P_DEPRECATED, null);
        }
      });
    }
  }

  protected void putPublishRequest(
      final NpmPackageId packageId,
      @Nullable final String revision,
      final NpmPublishRequest request) throws IOException
  {
    log.debug("Storing package: {}", packageId);

    NestedAttributesMap packageRoot = request.getPackageRoot();

    // process attachments, if any
    NestedAttributesMap attachments = packageRoot.child("_attachments");
    if (!attachments.isEmpty()) {
      for (String name : attachments.keys()) {
        NestedAttributesMap attachment = attachments.child(name);
        NestedAttributesMap packageVersion = selectVersionByTarballName(packageRoot, name);
        putTarball(packageId, packageVersion, attachment, request);
      }
    }

    putPackageRoot(packageId, revision, packageRoot);
  }

  private void putTarball(
      final NpmPackageId packageId,
      final NestedAttributesMap packageVersion,
      final NestedAttributesMap attachment,
      final NpmPublishRequest request) throws IOException
  {
    String tarballName = NpmMetadataUtils.extractTarballName(attachment.getKey());
    String version = packageVersion.get(NpmMetadataUtils.VERSION, String.class);
    log.debug("Storing tarball: {}@{} ({})", packageId, version, tarballName);

    try (TempBlob tempBlob = request.requireBlob(attachment.require("data", String.class))) {
      Map<String, Object> npmAttributes = maybeExtractFormatAttributes(packageId.id(), version, tempBlob);
      content().put(packageId, version, npmAttributes, tempBlob);
    }
  }

  private String parseVersionToTag(final NpmPackageId packageId,
                                   @Nullable final String tag,
                                   final Payload payload) throws IOException
  {
    String version;
    try (InputStream is = payload.openInputStream()) {
      version = IOUtils.toString(is).replaceAll("\"", "");
      log.debug("Adding tag {}:{} to {}", tag, version, packageId);
    }
    return version;
  }
}

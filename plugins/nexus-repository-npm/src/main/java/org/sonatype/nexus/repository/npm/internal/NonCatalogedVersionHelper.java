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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.firewall.event.ComponentDetailsRequest;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.removeObjectFieldMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteLatest;
import static org.sonatype.nexus.repository.npm.internal.NpmFormat.NAME;

/**
 *
 * @since 3.next
 */
@Named
@Singleton
public class NonCatalogedVersionHelper
    extends ComponentSupport
{
  public static final String REMOVE_NON_CATALOGED_KEY = "removeNonCataloged";

  public static final String CLM_CAPABILITY_TYPE_ID = "firewall.audit";

  public static final String REPOSITORY_KEY = "repository";

  private final int limit;

  private final EventManager eventManager;

  private final int componentDetailsTimeout;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public NonCatalogedVersionHelper(final EventManager eventManager,
                                   final CapabilityRegistry capabilityRegistry,
                                   @Named("${nexus.npm.firewall.check_last_limit:-5}") final int checkLastLimit,
                                   @Named("${nexus.npm.firewall.component_details_timeout:-10}")
                                   final int componentDetailsTimeout)
  {
    this.eventManager = eventManager;
    this.capabilityRegistry = capabilityRegistry;
    checkArgument(checkLastLimit > 0, "Invalid npm firewall check last version limit: %s", checkLastLimit);
    this.limit = checkLastLimit;
    checkArgument(componentDetailsTimeout > 0, "Invalid npm firewall component details timeout: %s", componentDetailsTimeout);
    this.componentDetailsTimeout = componentDetailsTimeout;
  }


  public void maybeAddExcludedVersionsFieldMatchers(final List<NpmFieldMatcher> fieldMatchers,
                                                     final Asset packageRootAsset,
                                                     final Repository repository)
  {
    Boolean removeNonCatalogedVersions = repository.getConfiguration()
        .attributes(NAME)
        .get(REMOVE_NON_CATALOGED_KEY, Boolean.class);
    if (!TRUE.equals(removeNonCatalogedVersions) || !isFirewallEnabled(repository)) {
      return;
    }
    try {
      BlobRef blobRef = packageRootAsset.blobRef();
      if (blobRef != null) {
        Blob blob = repository.facet(StorageFacet.class).blobStore().get(blobRef.getBlobId());
        if (blob != null) {
          List<String> versions = MetadataVersionParser.readVersions(blob.getInputStream());
          List<String> nonCatalogedVersions = nonCatalogedVersions(packageRootAsset.name(), last(limit, versions));
          List<NpmFieldMatcher> excludeVersions = nonCatalogedVersions.stream()
              .map(v -> removeObjectFieldMatcher(v, "/versions/" + v))
              .collect(toList());
          maybeRewriteLatest(excludeVersions, nonCatalogedVersions, versions);
          fieldMatchers.addAll(excludeVersions);
        }
      }
    }
    catch (Exception e) {
      log.warn("Error removing non-cataloged versions from metadata");
      log.debug("Error removing non-cataloged versions from metadata", e);
    }
  }

  private boolean isFirewallEnabled(final Repository repository) {
    CapabilityReferenceFilter capabilityReferenceFilter = CapabilityReferenceFilterBuilder.capabilities()
        .withType(new CapabilityType(CLM_CAPABILITY_TYPE_ID))
        .withProperty(REPOSITORY_KEY, repository.getName())
        .enabled();
    Collection<? extends CapabilityReference> capabilityReferences = capabilityRegistry.get(capabilityReferenceFilter);
    return !capabilityReferences.isEmpty();
  }

  @VisibleForTesting
  static List<String> last(final int n, final List<String> versions) {
    if (versions.isEmpty()) {
      return emptyList();
    }
    return versions.subList(versions.size() > n ? versions.size() - n : 0, versions.size());
  }

  private void maybeRewriteLatest(final List<NpmFieldMatcher> excludeVersions,
                                  final List<String> nonCatalogedVersions,
                                  final List<String> allVersions)
  {
    if (excludeVersions.size() > 0) {
      excludeVersions.add(rewriteLatest(nonCatalogedVersions, allVersions));
    }
  }

  private List<String> nonCatalogedVersions(final String packageName, final List<String> versions) {
    try {
      PackageNameParser parser = new PackageNameParser(packageName);
      List<PackageUrl> packageUrls = versions.stream()
          .map(v -> PackageUrl.builder().type(NAME).namespace(parser.namespace).name(parser.name).version(v).build())
          .collect(toList());
      final ComponentDetailsRequest componentDetailsRequest = new ComponentDetailsRequest(parser.name, packageUrls);
      eventManager.post(componentDetailsRequest);
      final Map<String, Date> catalogDates = componentDetailsRequest.getResult().get(componentDetailsTimeout, SECONDS);
      List<String> nonCatalogedVersions = packageUrls.stream()
          .filter(p -> {
            String pUrl = p.toString();
            // replace is a workaround due to CLM-16740
            String key = catalogDates.containsKey(pUrl) ? pUrl : pUrl.replace("%40", "%2540");
            return catalogDates.containsKey(key) && catalogDates.get(key) == null;
          })
          .map(PackageUrl::getVersion)
          .collect(toList());
      log.trace("Non-cataloged version for component {} => {}", parser.name, nonCatalogedVersions);
      return nonCatalogedVersions;
    }
    catch (InterruptedException | TimeoutException | ExecutionException e) {
      log.error("Error getting non cataloged versions for {}", packageName, e);
      throw new RuntimeException(e);
    }
  }

  private static class PackageNameParser
  {

    final String namespace;

    final String name;

    public PackageNameParser(final String packageName) {
      String[] namespaceAndName = packageName.split("/", 2);
      namespace = namespaceAndName.length == 2 ? namespaceAndName[0] : null;
      name = namespaceAndName.length == 2 ? namespaceAndName[1] : namespaceAndName[0];
    }
  }
}

/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.mapping.RequestRepositoryMapper;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.util.AlphanumComparator;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.service.GroupMetadataService;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link GroupMetadataService} implementation.
 */
public class GroupMetadataServiceImpl
    extends GeneratorSupport
    implements GroupMetadataService
{
  private final NpmGroupRepository npmGroupRepository;

  private final AlphanumComparator comparator;

  private final RequestRepositoryMapper requestRepositoryMapper;

  private boolean mergeMetadata = SystemPropertiesHelper.getBoolean(
      "nexus.npm.mergeGroupMetadata",
      true
  );

  public GroupMetadataServiceImpl(final NpmGroupRepository npmGroupRepository,
                                  final MetadataParser metadataParser,
                                  final RequestRepositoryMapper requestRepositoryMapper)
  {
    super(npmGroupRepository, metadataParser);
    this.npmGroupRepository = checkNotNull(npmGroupRepository);
    this.requestRepositoryMapper = checkNotNull(requestRepositoryMapper);
    this.comparator = new AlphanumComparator();
  }

  @Override
  public boolean isMergeMetadata() {
    return mergeMetadata;
  }

  @Override
  public void setMergeMetadata(final boolean mergeMetadata) {
    this.mergeMetadata = mergeMetadata;
  }

  @Override
  protected PackageRootIterator doGenerateRegistryRoot(final PackageRequest request) throws IOException {
    final List<NpmRepository> members = getMappedMembers(request);
    final List<PackageRootIterator> iterators = Lists.newArrayList();
    for (NpmRepository member : members) {
      iterators.add(member.getMetadataService().generateRegistryRoot(request));
    }
    return new AggregatedPackageRootIterator(iterators);
  }

  @Nullable
  @Override
  protected PackageRoot doGeneratePackageRoot(final PackageRequest request) throws IOException {
    final List<NpmRepository> members = getMembers();
    if (isMergeMetadata()) {
      PackageRoot root = null;
      String latestVersion = null;
      // apply in reverse order to have "first wins", as package overlay makes overlaid prevail
      for (NpmRepository member : Lists.reverse(members)) {
        final PackageRoot memberRoot = member.getMetadataService().generatePackageRoot(request);
        if (memberRoot != null) {
          if (root == null) {
            root = new PackageRoot(npmGroupRepository.getId(), memberRoot.getRaw());
            latestVersion = getDistTagsLatest(root);
          }
          else {
            String memberLatestVersion = getDistTagsLatest(memberRoot);
            root.overlayIgnoringOrigin(memberRoot);
            if (memberLatestVersion != null) {
              if (latestVersion == null ||
                  comparator.compare(memberLatestVersion, latestVersion) > 0) {
                latestVersion = memberLatestVersion;
              }
            }
          }
        }
      }
      // fix up latest: is biggest of all latest versions we met
      if (latestVersion != null) {
        setDistTagsLatest(root, latestVersion);
      }
      return root;
    }
    else {
      for (NpmRepository member : members) {
        final PackageRoot root = member.getMetadataService().generatePackageRoot(request);
        if (root != null) {
          return root;
        }
      }
      return null;
    }
  }

  @Nullable
  private String getDistTagsLatest(final PackageRoot root) {
    final Map<String, String> distTags = (Map<String, String>) root.getRaw().get("dist-tags");
    if (distTags == null) {
      return null;
    }
    return distTags.get("latest");
  }

  private void setDistTagsLatest(final PackageRoot root, final String version) {
    Map<String, String> distTags = (Map<String, String>) root.getRaw().get("dist-tags");
    if (distTags == null) {
      distTags = Maps.newHashMap();
      root.getRaw().put("dist-tags", distTags);
    }
    distTags.put("latest", version);
  }

  @Nullable
  @Override
  protected PackageVersion doGeneratePackageVersion(final PackageRequest request) throws IOException {
    final List<NpmRepository> members = getMappedMembers(request);
    for (NpmRepository member : members) {
      final PackageVersion version = member.getMetadataService().generatePackageVersion(request);
      if (version != null) {
        return version;
      }
    }
    return null;
  }

  // ==

  /**
   * Returns all group members using {@link RequestRepositoryMapper} for non-root requests.
   */
  private List<NpmRepository> getMappedMembers(final PackageRequest request) {
    try {
      return filterMembers(
          requestRepositoryMapper.getMappedRepositories(
              npmGroupRepository,
              request.getStoreRequest(),
              npmGroupRepository.getMemberRepositories()
          )
      );
    } catch (NoSuchResourceStoreException e) {
      // requestRepositoryMapper already logged: stale ref to non-existed repo in mapping, fallback
      return getMembers();
    }
  }

  /**
   * Returns all group members that are for certain NPM repositories.
   */
  private List<NpmRepository> getMembers() {
    return filterMembers(npmGroupRepository.getMemberRepositories());
  }

  /**
   * Returns all group members that are for certain NPM repositories.
   */
  private List<NpmRepository> filterMembers(final List<Repository> members) {
    final List<NpmRepository> npmMembers = Lists.newArrayList();
    for (Repository member : members) {
      final NpmRepository npmMember = member.adaptToFacet(NpmRepository.class);
      if (npmMember != null) {
        npmMembers.add(npmMember);
      }
    }
    return npmMembers;
  }

  // ==

  /**
   * Aggregates multiple {@link PackageRootIterator}s but keeps package names unique.
   */
  private static class AggregatedPackageRootIterator
      implements PackageRootIterator
  {
    private final List<PackageRootIterator> iterators;

    private final Iterator<PackageRootIterator> iteratorsIterator;

    private final HashSet<String> names;

    private PackageRootIterator currentIterator;

    private PackageRoot next;

    private AggregatedPackageRootIterator(final List<PackageRootIterator> iterators) {
      this.iterators = iterators;
      this.iteratorsIterator = iterators.iterator();
      this.names = Sets.newHashSet();
      this.currentIterator = iteratorsIterator.hasNext() ? iteratorsIterator.next() : PackageRootIterator.EMPTY;
      this.next = getNext();
    }

    private PackageRoot getNext() {
      while (currentIterator != PackageRootIterator.EMPTY) {
        while (currentIterator.hasNext()) {
          final PackageRoot next = currentIterator.next();
          if (names.add(next.getName())) {
            return next;
          }
        }
        if (iteratorsIterator.hasNext()) {
          currentIterator = iteratorsIterator.next();
        }
        else {
          currentIterator = PackageRootIterator.EMPTY;
        }
      }
      return null; // no more
    }

    @Override
    public void close() {
      for (PackageRootIterator iterator : iterators) {
        try {
          iterator.close();
        }
        catch (IOException e) {
          // swallow
        }
      }
    }

    @Override
    public boolean hasNext() {
      boolean hasNext = next != null;
      if (!hasNext) {
        close();
      }
      return hasNext;
    }

    @Override
    public PackageRoot next() {
      final PackageRoot result = next;
      if (result == null) {
        throw new NoSuchElementException("Iterator depleted");
      }
      next = getNext();
      if (next == null) {
        close();
      }
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}

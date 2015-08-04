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
package org.sonatype.nexus.yum.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.repository.AbstractRequestStrategy;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RequestStrategy;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumGroup;
import org.sonatype.nexus.yum.YumRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * A request strategy that applies to yum enabled groups that will fetch repomd.xml from each proxy member repository
 * before serving group repomd.xml. If repomd.xml changes (new one is downloaded) it will trigger a re-merge so served
 * group repomd.xml will be up to date.
 *
 * @since 2.7
 */
@Named
@Singleton
public class MergeMetadataRequestStrategy
    extends AbstractRequestStrategy
    implements RequestStrategy
{

  private static final Logger log = LoggerFactory.getLogger(MergeMetadataRequestStrategy.class);

  private static final String PATH_OF_REPOMD_XML = "/" + Yum.PATH_OF_REPOMD_XML;

  private final YumRegistry yumRegistry;

  @Inject
  public MergeMetadataRequestStrategy(final YumRegistry yumRegistry) {
    this.yumRegistry = checkNotNull(yumRegistry);
  }

  @Override
  public void onHandle(final Repository repository, final ResourceStoreRequest request, final Action action)
      throws ItemNotFoundException
  {
    GroupRepository groupRepository = repository.adaptToFacet(GroupRepository.class);
    final String requestPath = request.getRequestPath();
    if (Action.read.equals(action) && requestPath.endsWith(PATH_OF_REPOMD_XML) && groupRepository != null) {
      Yum yum = yumRegistry.get(groupRepository.getId());
      if (yum != null && yum instanceof YumGroup) {
        for (Repository member : groupRepository.getMemberRepositories()) {
          if (member.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
            try {
              log.debug("Fetching {}:{} member of {}", member.getId(), PATH_OF_REPOMD_XML, groupRepository.getId());
              member.retrieveItem(new ResourceStoreRequest(PATH_OF_REPOMD_XML));
            }
            catch (ItemNotFoundException e) {
              // proxy repo is not a yum repository, go on
            }
            catch (Exception e) {
              log.debug(
                  "Could not retrieve {} from {}, member of yum enabled group {}. Ignoring.",
                  PATH_OF_REPOMD_XML, member.getId(), groupRepository.getId(), e
              );
            }
          }
        }
        try {
          // this will trigger a merge if group yum metadata is dirty and will wait for the task to finish
          yum.getYumRepository();
        }
        catch (Exception e) {
          throw new ItemNotFoundException(reasonFor(request, repository, e.getMessage()));
        }
      }
    }
  }

}
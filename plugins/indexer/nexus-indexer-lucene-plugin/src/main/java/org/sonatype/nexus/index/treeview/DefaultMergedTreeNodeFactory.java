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
package org.sonatype.nexus.index.treeview;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.Loggers;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.treeview.DefaultTreeNodeFactory;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeNode.Type;
import org.apache.maven.index.treeview.TreeViewRequest;
import org.slf4j.Logger;

/**
 * A default implementation of merged TreeNodeFactory, that is failry simple to extend. Note: this implementation
 * assumes that index is <b>superset</b> of the local storage content, which is true for both proxied (if remote idx
 * exists and is downloaded) and hosted reposes (where actually the sets are equal)!
 *
 * @author cstamas
 */
public class DefaultMergedTreeNodeFactory
    extends DefaultTreeNodeFactory
{
  private static final String CHECK_LOCAL_AVAILABILITY_KEY =
      "org.sonatype.nexus.index.treeview.DefaultMergedTreeNodeFactory.checkLocalAvailability";

  private static final boolean CHECK_LOCAL_AVAILABILITY = SystemPropertiesHelper.getBoolean(
      CHECK_LOCAL_AVAILABILITY_KEY, false);

  private final Logger logger = Loggers.getLogger(getClass());

  private final Repository repository;

  public DefaultMergedTreeNodeFactory(Repository repository) {
    super(repository.getId());
    this.repository = repository;
  }

  public Repository getRepository() {
    return repository;
  }

  @Override
  protected TreeNode decorateGNode(IndexTreeView tview, TreeViewRequest req, String path, String groupName,
                                   TreeNode node)
  {
    final DefaultMergedTreeNode mnode =
        (DefaultMergedTreeNode) super.decorateGNode(tview, req, path, groupName, node);
    mnode.setLocallyAvailable(isPathAvailable(path));
    return mnode;
  }

  @Override
  protected TreeNode decorateANode(IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                   TreeNode node)
  {
    final DefaultMergedTreeNode mnode = (DefaultMergedTreeNode) super.decorateANode(tview, req, ai, path, node);
    mnode.setLocallyAvailable(isPathAvailable(path));
    return mnode;
  }

  @Override
  protected TreeNode decorateVNode(IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                   TreeNode node)
  {
    final DefaultMergedTreeNode mnode = (DefaultMergedTreeNode) super.decorateVNode(tview, req, ai, path, node);
    mnode.setLocallyAvailable(isPathAvailable(path));
    return mnode;
  }

  @Override
  protected TreeNode decorateArtifactNode(IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                          TreeNode node)
  {
    final DefaultMergedTreeNode mnode =
        (DefaultMergedTreeNode) super.decorateArtifactNode(tview, req, ai, path, node);
    gatherArtifactNodeInfoIfAvailable(path, mnode);
    return node;
  }

  @Override
  protected TreeNode createNode(IndexTreeView tview, TreeViewRequest req, String path, boolean leaf,
                                String nodeName, Type type)
  {
    final TreeNode result = super.createNode(tview, req, path, leaf, nodeName, type);
    result.setRepositoryId(getRepository().getId());
    return result;
  }

  @Override
  protected TreeNode instantiateNode(IndexTreeView tview, TreeViewRequest req, String path, boolean leaf,
                                     String nodeName)
  {
    return new DefaultMergedTreeNode(tview, req);
  }

  protected ResourceStoreRequest getResourceStoreRequest(String path) {
    return new ResourceStoreRequest(path, true);
  }

  protected boolean isPathAvailable(String path) {
    if (!CHECK_LOCAL_AVAILABILITY) {
      return false;
    }
    try {
      final ResourceStoreRequest request = getResourceStoreRequest(path);
      return getRepository().getLocalStorage().containsItem(getRepository(), request);
    }
    catch (Exception e) {
      // for whatever reason, couldn't see item, so it's not cached locally we shall say
      logger.warn("IO related problem during local storage availability check in repository {}",
          RepositoryStringUtils.getHumanizedNameString(getRepository()), e);
    }
    return false;
  }

  protected void gatherArtifactNodeInfoIfAvailable(final String path, final DefaultMergedTreeNode mnode) {
    if (!CHECK_LOCAL_AVAILABILITY) {
      return;
    }

    final ResourceStoreRequest request = getResourceStoreRequest(path);
    // default it to not available
    mnode.setLocallyAvailable(false);
    try {
      final StorageItem item = getRepository().retrieveItem(request);
      if (item instanceof StorageFileItem) {
        mnode.setLocallyAvailable(true);
        mnode.setArtifactTimestamp(item.getModified());
        mnode.setArtifactMd5Checksum(item.getRepositoryItemAttributes().get(
            DigestCalculatingInspector.DIGEST_MD5_KEY));
        mnode.setArtifactSha1Checksum(item.getRepositoryItemAttributes().get(
            DigestCalculatingInspector.DIGEST_SHA1_KEY));
        mnode.setInitiatorUserId(item.getRepositoryItemAttributes().get(AccessManager.REQUEST_USER));
        mnode.setInitiatorIpAddress(item.getRepositoryItemAttributes().get(
            AccessManager.REQUEST_REMOTE_ADDRESS));
        mnode.setArtifactOriginUrl(item.getRemoteUrl());
        if (!StringUtils.isEmpty(mnode.getArtifactOriginUrl())) {
          mnode.setArtifactOriginReason("cached");
        }
        else {
          mnode.setArtifactOriginReason("deployed");
        }
      }
    }
    catch (ItemNotFoundException e) {
      // mute it, probably not available locally
    }
    catch (AccessDeniedException e) {
      // mute it, probably user does not have authz to access this part of repo
    }
    catch (IllegalOperationException e) {
      // like "repo is out of service", but why is then tree view accessed at all? In that case it's a bug
      logger.warn("Illegal operation tried against repository {}",
          RepositoryStringUtils.getHumanizedNameString(getRepository()), e);
    }
    catch (StorageException e) {
      // this is lethal, some "io related" problem. Is basically IOException and is a problem on your instance or HW/net
      logger.warn("IO related problem in repository {}",
          RepositoryStringUtils.getHumanizedNameString(getRepository()), e);
    }
  }

}

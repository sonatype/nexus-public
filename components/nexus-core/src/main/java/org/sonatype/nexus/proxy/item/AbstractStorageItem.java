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
package org.sonatype.nexus.proxy.item;

import java.io.File;
import java.util.Map;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.attributes.internal.DefaultAttributes;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.util.PathUtils;

import com.google.common.base.Strings;

/**
 * Abstract class encapsulating properties what all item kinds in Nexus share.
 */
public abstract class AbstractStorageItem
    implements StorageItem
{

  /**
   * The request
   */
  private transient ResourceStoreRequest request;

  /**
   * The repository item uid.
   */
  private transient RepositoryItemUid repositoryItemUid;

  /**
   * The store.
   */
  private transient ResourceStore store;

  /**
   * The item context
   */
  private transient RequestContext context;

  /**
   * the attributes
   */
  private transient Attributes itemAttributes;
  
  // NOTE: these fields below are deprecated but needed for pre-2.0 instances upgraded to post-2.0!
  // Do not remove, unless we stop supporting upgrading of pre-2.0 to current version!

  /**
   * Used for versioning of attribute
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private int generation = 0;

  /**
   * The path.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private String path;

  /**
   * The readable.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private boolean readable;

  /**
   * The writable.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private boolean writable;

  /**
   * The repository id.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private String repositoryId;

  /**
   * The created.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private long created;

  /**
   * The modified.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private long modified;

  /**
   * The stored locally.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private long storedLocally;

  /**
   * The last remoteCheck timestamp.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private long lastTouched;

  /**
   * The last requested timestamp.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private long lastRequested;

  /**
   * Expired flag
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private boolean expired;

  /**
   * The remote url.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private String remoteUrl;

  /**
   * The persisted attributes.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen.
   */
  @Deprecated
  private Map<String, String> attributes;

  // ==

  public Attributes getRepositoryItemAttributes() {
    return itemAttributes;
  }

  /**
   * This method should be called ONLY when you load up a _legacy_ attribute using _legacy_ attribute store! Basically
   * it "repacks" the deprecated fields as loaded by XStream (as back then XStream was used to serialize Item instances)
   * into new Attributes.
   */
  public void upgrade() {
    this.context = new RequestContext();
    this.itemAttributes = new DefaultAttributes();

    // this here is for ITs only, some of them use "manually crafter" attributes XML files and would NPE
    // In "real life", all the files stored in Nexus have at least sha1/md5 set as attributes, meaning,
    // all the real life items has at least two attributes and this map would never be null!
    if (attributes != null) {
      getRepositoryItemAttributes().putAll(attributes);
    }

    getRepositoryItemAttributes().setGeneration(generation);
    getRepositoryItemAttributes().setPath(path);
    getRepositoryItemAttributes().setReadable(readable);
    getRepositoryItemAttributes().setWritable(writable);
    getRepositoryItemAttributes().setRepositoryId(repositoryId);
    getRepositoryItemAttributes().setCreated(created);
    getRepositoryItemAttributes().setModified(modified);
    getRepositoryItemAttributes().setStoredLocally(storedLocally);
    getRepositoryItemAttributes().setCheckedRemotely(lastTouched);
    getRepositoryItemAttributes().setLastRequested(lastRequested);
    getRepositoryItemAttributes().setExpired(expired);
    if (!Strings.isNullOrEmpty(remoteUrl)) {
      getRepositoryItemAttributes().setRemoteUrl(remoteUrl);
    }
  }

  // ==

  /**
   * Default constructor, needed for XStream.
   * 
   * @deprecated This constructor is here for legacy reasons, do not use it!
   */
  @Deprecated
  private AbstractStorageItem() {
    this.context = new RequestContext();
    this.itemAttributes = new DefaultAttributes();
  }

  public AbstractStorageItem(final ResourceStoreRequest request, final boolean readable, final boolean writable) {
    this();
    this.request = request.cloneAndDetach();
    this.context.setParentContext(request.getRequestContext());
    setPath(request.getRequestPath());
    setReadable(readable);
    setWritable(writable);
    setCreated(System.currentTimeMillis());
    setModified(getCreated());
  }

  public AbstractStorageItem(Repository repository, ResourceStoreRequest request, boolean readable, boolean writable) {
    this(request, readable, writable);
    this.store = repository;
    this.repositoryItemUid = repository.createUid(getPath());
    setRepositoryId(repository.getId());
  }

  public AbstractStorageItem(RepositoryRouter router, ResourceStoreRequest request, boolean readable, boolean writable)
  {
    this(request, readable, writable);
    this.store = router;
  }

  /**
   * {@link ResourceStore} is superclass of {@link Repository} and {@link RepositoryRouter}. This is for virtual items,
   * when they do not originate from a {@link Repository}.
   */
  public ResourceStore getStore() {
    return this.store;
  }

  @Override
  public ResourceStoreRequest getResourceStoreRequest() {
    return request;
  }

  public void setResourceStoreRequest(ResourceStoreRequest request) {
    this.request = request;
    this.context = new RequestContext(request.getRequestContext());
  }

  @Override
  public RepositoryItemUid getRepositoryItemUid() {
    return repositoryItemUid;
  }

  @Override
  public void setRepositoryItemUid(RepositoryItemUid repositoryItemUid) {
    this.repositoryItemUid = repositoryItemUid;
    this.store = repositoryItemUid.getRepository();
    getRepositoryItemAttributes().setRepositoryId(repositoryItemUid.getRepository().getId());
    getRepositoryItemAttributes().setPath(repositoryItemUid.getPath());
  }

  @Override
  public String getRepositoryId() {
    return getRepositoryItemAttributes().getRepositoryId();
  }

  public void setRepositoryId(String repositoryId) {
    getRepositoryItemAttributes().setRepositoryId(repositoryId);
  }

  @Override
  public long getCreated() {
    return getRepositoryItemAttributes().getCreated();
  }

  public void setCreated(long created) {
    getRepositoryItemAttributes().setCreated(created);
  }

  @Override
  public long getModified() {
    return getRepositoryItemAttributes().getModified();
  }

  public void setModified(long modified) {
    getRepositoryItemAttributes().setModified(modified);
  }

  @Override
  public boolean isReadable() {
    return getRepositoryItemAttributes().isReadable();
  }

  public void setReadable(boolean readable) {
    getRepositoryItemAttributes().setReadable(readable);
  }

  @Override
  public boolean isWritable() {
    return getRepositoryItemAttributes().isWritable();
  }

  public void setWritable(boolean writable) {
    getRepositoryItemAttributes().setWritable(writable);
  }

  @Override
  public String getPath() {
    return getRepositoryItemAttributes().getPath();
  }

  public void setPath(String path) {
    getRepositoryItemAttributes().setPath(PathUtils.cleanUpTrailingSlash(path));
  }

  @Override
  public boolean isExpired() {
    return getRepositoryItemAttributes().isExpired();
  }

  @Override
  public void setExpired(boolean expired) {
    getRepositoryItemAttributes().setExpired(expired);
  }

  @Override
  public String getName() {
    return new File(getPath()).getName();
  }

  @Override
  public String getParentPath() {
    return PathUtils.getParentPath(getPath());
  }

  @Override
  public int getPathDepth() {
    return PathUtils.getPathDepth(getPath());
  }

  @Override
  public RequestContext getItemContext() {
    return context;
  }

  @Override
  public boolean isVirtual() {
    return getRepositoryItemUid() == null;
  }

  @Override
  public String getRemoteUrl() {
    return getRepositoryItemAttributes().getRemoteUrl();
  }

  public void setRemoteUrl(String remoteUrl) {
    getRepositoryItemAttributes().setRemoteUrl(remoteUrl);
  }

  @Override
  public long getStoredLocally() {
    return getRepositoryItemAttributes().getStoredLocally();
  }

  @Override
  public void setStoredLocally(long storedLocally) {
    getRepositoryItemAttributes().setStoredLocally(storedLocally);
  }

  @Override
  public long getRemoteChecked() {
    return getRepositoryItemAttributes().getCheckedRemotely();
  }

  @Override
  public void setRemoteChecked(long lastTouched) {
    getRepositoryItemAttributes().setCheckedRemotely(lastTouched);
  }

  @Override
  public long getLastRequested() {
    return getRepositoryItemAttributes().getLastRequested();
  }

  @Override
  public void setLastRequested(long lastRequested) {
    getRepositoryItemAttributes().setLastRequested(lastRequested);
  }

  // ==

  @Override
  public String toString() {
    if (isVirtual()) {
      return getPath();
    }
    else {
      return getRepositoryItemUid().toString();
    }
  }
}

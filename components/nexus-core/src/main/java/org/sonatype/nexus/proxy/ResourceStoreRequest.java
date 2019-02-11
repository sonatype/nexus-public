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
package org.sonatype.nexus.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Request for a resource. It drives many aspects of the request itself.
 *
 * @author cstamas
 */
public class ResourceStoreRequest
{
  /**
   * The path we want to retrieve.
   */
  private String requestPath;

  /**
   * Extra data associated with this request.
   */
  private final RequestContext requestContext;

  /**
   * Used internally by Routers.
   */
  private final Stack<String> pathStack;

  /**
   * Used internally to track reposes where this request was
   */
  private final List<String> processedRepositories;

  /**
   * Used internally to track applied mappins
   */
  private final Map<String, List<String>> appliedMappings;

  /**
   * Constructor.
   *
   * @param requestPath the request path.
   * @param localOnly   See {@link RequestContext#CTX_LOCAL_ONLY_FLAG}.
   * @param remoteOnly  See {@link RequestContext#CTX_REMOTE_ONLY_FLAG}.
   */
  public ResourceStoreRequest(String requestPath, boolean localOnly, boolean remoteOnly) {
    this.requestPath = requestPath;
    this.pathStack = new Stack<String>();
    this.processedRepositories = new ArrayList<String>();
    this.appliedMappings = new HashMap<String, List<String>>();
    this.requestContext = new RequestContext();
    this.requestContext.setRequestLocalOnly(localOnly);
    this.requestContext.setRequestRemoteOnly(remoteOnly);
  }

  /**
   * Shortcut constructor.
   *
   * @deprecated use {@link #ResourceStoreRequest(String, boolean, boolean)} instead.
   */
  public ResourceStoreRequest(String requestPath, boolean localOnly) {
    this(requestPath, localOnly, false);
  }

  /**
   * Shortcut constructor.
   */
  public ResourceStoreRequest(String requestPath) {
    this(requestPath, false, false);
  }

  /**
   * Creates a request aimed at given path denoted by RepositoryItemUid.
   *
   * @param uid the uid
   * @deprecated use {@link #ResourceStoreRequest(String, boolean, boolean)} instead.
   */
  public ResourceStoreRequest(RepositoryItemUid uid, boolean localOnly) {
    this(uid.getPath(), localOnly, false);
  }

  /**
   * Creates a request for a given item that is expected to be already present (locally).
   */
  public ResourceStoreRequest(final StorageItem item) {
    this(item.getRepositoryItemUid().getPath(), true, false);
    getRequestContext().setParentContext(item.getItemContext());
  }

  /**
   * Creates a request for a given item.
   */
  public ResourceStoreRequest(final StorageItem item, final boolean localOnly) {
    this(item.getRepositoryItemUid().getPath(), localOnly, false);
    getRequestContext().setParentContext(item.getItemContext());
  }

  /**
   * Creates a new request off from a given one, item is expected to be already present (locally).
   */
  public ResourceStoreRequest(final ResourceStoreRequest request) {
    this(request.getRequestPath(), true, false);
    getRequestContext().setParentContext(request.getRequestContext());
  }

  // ==

  /**
   * Gets the request context.
   *
   * @return the request context
   */
  public RequestContext getRequestContext() {
    return requestContext;
  }

  /**
   * Gets the request path.
   *
   * @return the request path
   */
  public String getRequestPath() {
    return requestPath;
  }

  /**
   * Sets the request path.
   *
   * @param requestPath the new request path
   */
  public ResourceStoreRequest setRequestPath(String requestPath) {
    this.requestPath = requestPath;
    return this;
  }

  /**
   * Push request path. Used internally by Router.
   *
   * @param requestPath the request path
   */
  public void pushRequestPath(String requestPath) {
    pathStack.push(this.requestPath);

    this.requestPath = requestPath;
  }

  /**
   * Pop request path. Used internally by Router.
   *
   * @return the string
   */
  public String popRequestPath() {
    this.requestPath = pathStack.pop();

    return getRequestPath();
  }

  /**
   * Checks if is request local only.
   *
   * @return true, if is request local only
   */
  public boolean isRequestLocalOnly() {
    return getRequestContext().isRequestLocalOnly();
  }

  /**
   * Sets the request local only.
   *
   * @param requestLocalOnly the new request local only
   */
  public ResourceStoreRequest setRequestLocalOnly(boolean requestLocalOnly) {
    getRequestContext().setRequestLocalOnly(requestLocalOnly);
    return this;
  }

  /**
   * Checks if is request remote only.
   *
   * @return true, if is request remote only
   */
  public boolean isRequestRemoteOnly() {
    return getRequestContext().isRequestRemoteOnly();
  }

  /**
   * Sets the request remote only.
   *
   * @param requestRemoteOnly the new request remote only
   */
  public ResourceStoreRequest setRequestRemoteOnly(boolean requestRemoteOnly) {
    getRequestContext().setRequestRemoteOnly(requestRemoteOnly);
    return this;
  }

  /**
   * Checks if is request group local only.
   *
   * @return true, if is request group local only
   */
  public boolean isRequestGroupLocalOnly() {
    return getRequestContext().isRequestGroupLocalOnly();
  }

  /**
   * Checks if is request group members only.
   *
   * @return true, if is request group members only
   */
  public boolean isRequestGroupMembersOnly() {
    return getRequestContext().isRequestGroupMembersOnly();
  }

  /**
   * Sets the request as expired.
   *
   * @param asExpired the new request asExpired value
   */
  public ResourceStoreRequest setRequestAsExpired(boolean asExpired) {
    getRequestContext().setRequestAsExpired(asExpired);
    return this;
  }

  /**
   * Checks if request should be handled as expired.
   *
   * @return true, if request should be handled as expired.
   */
  public boolean isRequestAsExpired() {
    return getRequestContext().isRequestAsExpired();
  }

  /**
   * Sets the request group local only.
   *
   * @param requestGroupLocal the new request group local only
   */
  public ResourceStoreRequest setRequestGroupLocalOnly(boolean requestGroupLocal) {
    getRequestContext().setRequestGroupLocalOnly(requestGroupLocal);
    return this;
  }

  /**
   * Sets the request group members only.
   *
   * @param requestGroupMembers the new request group local only
   */
  public ResourceStoreRequest setRequestGroupMembersOnly(boolean requestGroupMembers) {
    getRequestContext().setRequestGroupMembersOnly(requestGroupMembers);
    return this;
  }

  /**
   * Returns the list of processed repositories.
   */
  public List<String> getProcessedRepositories() {
    return Collections.unmodifiableList(processedRepositories);
  }

  /**
   * Adds the repository ID to the list of processed repository IDs.
   * 
   * @since 2.7.0
   */
  public void addProcessedRepository(String repositoryId) {
    processedRepositories.add(checkNotNull(repositoryId));
  }

  /**
   * Returns true if the request is conditional.
   *
   * @return true if this request is conditional.
   */
  public boolean isConditional() {
    return getRequestContext().isConditional();
  }

  /**
   * Returns the timestamp to check against.
   */
  public long getIfModifiedSince() {
    return getRequestContext().getIfModifiedSince();
  }

  /**
   * Sets the timestamp to check against.
   */
  public ResourceStoreRequest setIfModifiedSince(long ifModifiedSince) {
    getRequestContext().setIfModifiedSince(ifModifiedSince);
    return this;
  }

  /**
   * Gets the ETag (SHA1 in Nexus) to check item against.
   */
  public String getIfNoneMatch() {
    return getRequestContext().getIfNoneMatch();
  }

  /**
   * Sets the ETag (SHA1 in Nexus) to check item against.
   */
  public ResourceStoreRequest setIfNoneMatch(String tag) {
    getRequestContext().setIfNoneMatch(tag);
    return this;
  }

  /**
   * Returns the URL of the original request.
   */
  public String getRequestUrl() {
    return getRequestContext().getRequestUrl();
  }

  /**
   * Sets the URL of the original request.
   */
  public ResourceStoreRequest setRequestUrl(String url) {
    getRequestContext().setRequestUrl(url);
    return this;
  }

  /**
   * Returns {@code true} if this request is external, made by client outside of Nexus. Returns {@code false}
   * for requests made internally, like for example made from tasks.
   *
   * @since 2.6
   */
  public boolean isExternal() {
    return getRequestContext().isRequestIsExternal();
  }

  /**
   * Sets if this requst is external.
   *
   * @since 2.6
   */
  public ResourceStoreRequest setExternal(boolean external) {
    getRequestContext().setRequestIsExternal(external);
    return this;
  }

  /**
   * Returns {@code true} if this is a 'describe' request; otherwise {@code false}.
   *
   * @since 2.14.3
   */
  public boolean isDescribe() {
    return getRequestContext().isRequestIsDescribe();
  }

  /**
   * Sets if this is a 'describe' request.
   *
   * @since 2.14.3
   */
  public ResourceStoreRequest setDescribe(boolean describe) {
    getRequestContext().setRequestIsDescribe(describe);
    return this;
  }

  /**
   * Adds a list of applied mappings that happened in given repository.
   */
  public void addAppliedMappingsList(Repository repository, List<String> mappingList) {
    appliedMappings.put(repository.getId(), mappingList);
  }

  /**
   * Returns the applied mappings.
   */
  public Map<String, List<String>> getAppliedMappings() {
    return appliedMappings;
  }

  // ==

  /**
   * Creates a clone of this request, but also "detaches" it from any parent context relationship, effectively making
   * result new "detached" instance that captures the snapshot of the request in a moment when the clone was created.
   *
   * @return the clone of this request, detached from any outgoing relations.
   * @since 2.5
   */
  public ResourceStoreRequest cloneAndDetach() {
    final ResourceStoreRequest result = new ResourceStoreRequest(getRequestPath());
    result.requestContext.setParentContext(null);
    for (Map.Entry<String, Object> entry : requestContext.flatten().entrySet()) {
      result.requestContext.put(entry.getKey(), entry.getValue());
    }
    result.pathStack.clear();
    result.processedRepositories.clear();
    result.processedRepositories.addAll(processedRepositories);
    result.appliedMappings.clear();
    result.appliedMappings.putAll(appliedMappings);
    return result;
  }

  @Override
  public String toString() {
    return "ResourceStoreRequest{" + "requestPath='" + requestPath + '\'' + ", requestContext=" + requestContext
        + ", pathStack=" + pathStack + ", processedRepositories=" + processedRepositories + ", appliedMappings="
        + appliedMappings + '}';
  }
}

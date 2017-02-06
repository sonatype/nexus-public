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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

import com.google.common.collect.Maps;
import org.codehaus.plexus.util.StringUtils;

/**
 * Context map, used in {@link ResourceStoreRequest}, but also in {@link StorageItem} and related events.
 *
 * @author cstamas
 */
public class RequestContext
{
  /**
   * Context URL of the original resource requested on the incoming connector.
   */
  public static final String CTX_REQUEST_URL = "request.url";

  /**
   * Context flag to mark that request entered on the incoming connector, is external.
   */
  public static final String CTX_REQUEST_IS_EXTERNAL = "request.external";

  /**
   * Context flag to mark that this is a 'describe' request.
   *
   * @since 2.14.3
   */
  public static final String CTX_REQUEST_IS_DESCRIBE = "request.describe";

  /**
   * Context flag to mark a request local only. For {@link ProxyRepository} instances: do not attempt remote access
   * at
   * all, else: no effect.
   */
  public static final String CTX_LOCAL_ONLY_FLAG = "request.localOnly";

  /**
   * Context flag to mark a request local only. For {@link ProxyRepository} instances: force remote access -- might
   * still serve local if cache is fresh, else: no effect.
   */
  public static final String CTX_REMOTE_ONLY_FLAG = "request.remoteOnly";

  /**
   * Context flag to mark a request local only. For {@link GroupRepository} instances: do not "dive" into members,
   * else: no effect.
   */
  public static final String CTX_GROUP_LOCAL_ONLY_FLAG = "request.groupLocalOnly";

  /**
   * Context flag to mark a request group members only. For {@link GroupRepository} instances: do not look in local
   * storage, only into into members, else: no effect.
   */
  public static final String CTX_GROUP_MEMEBRS_ONLY_FLAG = "request.groupMembersOnly";

  /**
   * Context flag to mark a request be processed as the item would be expired. For {@link ProxyRepository} instances:
   * do check remote for newer but take into account local cache content, else: no effect.
   */
  public static final String CTX_AS_EXPIRED_FLAG = "request.asExpired";

  /**
   * Context key for condition "if-modified-since"
   */
  public static final String CTX_CONDITION_IF_MODIFIED_SINCE = "request.condition.ifModifiedSince";

  /**
   * Context key for condition "if-none-match"
   */
  public static final String CTX_CONDITION_IF_NONE_MATCH = "request.condition.ifNoneMatch";

  /**
   * Context key to mark request as used for auth check only, so repo impl will know there is no work required (i.e.
   * interpolation, etc.)
   */
  public static final String CTX_AUTH_CHECK_ONLY = "request.auth.check.only";

  private RequestContext parent;

  private final HashMap<String, Object> delegate;

  public RequestContext() {
    this(null);
  }

  public RequestContext(RequestContext parent) {
    setParentContext(parent);
    this.delegate = Maps.newHashMap();
  }

  public RequestContext getParentContext() {
    return parent;
  }

  public void setParentContext(RequestContext context) {
    if (context != null) {
      if (this == context) {
        throw new IllegalArgumentException(
            "The context cannot be parent of itself! The parent instance cannot equals to this instance!");
      }
      RequestContext otherParentContext = context.getParentContext();
      while (otherParentContext != null) {
        if (this == otherParentContext) {
          throw new IllegalArgumentException("The context cannot be an ancestor of itself! Cycle detected!");
        }
        otherParentContext = otherParentContext.getParentContext();
      }
    }

    this.parent = context;
  }

  public Object get(Object key, boolean fallBackToParent) {
    if (containsKey(key, false)) {
      return delegate.get(key);
    }
    else if (fallBackToParent && getParentContext() != null && getParentContext() != this) {
      return getParentContext().get(key);
    }
    else {
      return null;
    }
  }

  public boolean containsKey(Object key, boolean fallBackToParent) {
    boolean result = delegate.containsKey(key);
    if (fallBackToParent && !result && getParentContext() != null && getParentContext() != this) {
      result = getParentContext().containsKey(key);
    }
    return result;
  }

  public Object get(Object key) {
    return get(key, true);
  }

  public boolean containsKey(Object key) {
    return containsKey(key, true);
  }

  public Object put(final String key, final Object value) {
    return delegate.put(key, value);
  }

  @Deprecated
  public Object put(final Object key, final Object value) {
    return delegate.put((String) key, value);
  }

  public Object remove(final Object key) {
    return delegate.remove(key);
  }

  /**
   * Enforces sane state of the 3 related flag: localOnly, remoteOnly and asExpired. Only one of those
   * might be {@code true}.
   */
  protected void checkLocalRemoteExpiredFlags()
      throws IllegalArgumentException
  {
    final HashSet<String> enabledFlags = new HashSet<String>();
    if (isRequestLocalOnly()) {
      enabledFlags.add("localOnly");
    }
    if (isRequestRemoteOnly()) {
      enabledFlags.add("remoteOnly");
    }
    if (isRequestAsExpired()) {
      enabledFlags.add("asExpired");
    }
    if (enabledFlags.size() > 1) {
      throw new IllegalArgumentException(
          "Only one of the \"localOnly\", \"remoteOnly\" and \"asExpired\" might be enabled (set to true), but enabled ones are: "
              + enabledFlags);
    }
  }

  /**
   * Checks if is request local only.
   *
   * @return true, if is request local only
   */
  public boolean isRequestLocalOnly() {
    if (containsKey(CTX_LOCAL_ONLY_FLAG)) {
      return (Boolean) get(CTX_LOCAL_ONLY_FLAG);
    }
    else {
      return false;
    }
  }

  /**
   * Sets the request local only.
   *
   * @param requestLocalOnly the new request local only
   */
  public void setRequestLocalOnly(boolean requestLocalOnly) {
    put(CTX_LOCAL_ONLY_FLAG, requestLocalOnly);
    checkLocalRemoteExpiredFlags();
  }

  /**
   * Checks if is request remote only.
   *
   * @return true, if is request remote only
   */
  public boolean isRequestRemoteOnly() {
    if (containsKey(CTX_REMOTE_ONLY_FLAG)) {
      return (Boolean) get(CTX_REMOTE_ONLY_FLAG);
    }
    else {
      return false;
    }
  }

  /**
   * Sets the request remote only.
   *
   * @param requestRemoteOnly the new request remote only
   */
  public void setRequestRemoteOnly(boolean requestRemoteOnly) {
    put(CTX_REMOTE_ONLY_FLAG, requestRemoteOnly);
    checkLocalRemoteExpiredFlags();
  }

  /**
   * Checks if is request group local only.
   *
   * @return true, if is request group local only
   */
  public boolean isRequestGroupLocalOnly() {
    if (containsKey(CTX_GROUP_LOCAL_ONLY_FLAG)) {
      return (Boolean) get(CTX_GROUP_LOCAL_ONLY_FLAG);
    }
    else {
      return false;
    }
  }

  /**
   * Sets the request group local only.
   *
   * @param requestGroupLocal the new request group local only
   */
  public void setRequestGroupLocalOnly(boolean requestGroupLocal) {
    put(CTX_GROUP_LOCAL_ONLY_FLAG, requestGroupLocal);
  }

  /**
   * Checks if is request group members only.
   *
   * @return true, if is request group members only
   */
  public boolean isRequestGroupMembersOnly() {
    if (containsKey(CTX_GROUP_MEMEBRS_ONLY_FLAG)) {
      return (Boolean) get(CTX_GROUP_MEMEBRS_ONLY_FLAG);
    }
    else {
      return false;
    }
  }

  /**
   * Sets the request group members only.
   *
   * @param requestGroupMembers the new request group memebrs only
   */
  public void setRequestGroupMembersOnly(boolean requestGroupMembers) {
    put(CTX_GROUP_MEMEBRS_ONLY_FLAG, requestGroupMembers);
  }

  /**
   * Checks if request should be handled as expired.
   *
   * @return true, if request should be handled as expired
   */
  public boolean isRequestAsExpired() {
    if (containsKey(CTX_AS_EXPIRED_FLAG)) {
      return (Boolean) get(CTX_AS_EXPIRED_FLAG);
    }
    else {
      return false;
    }
  }

  /**
   * Sets the request to be handled as expired.
   *
   * @param asExpired the new asExpired value
   */
  public void setRequestAsExpired(boolean asExpired) {
    put(CTX_AS_EXPIRED_FLAG, asExpired);
    checkLocalRemoteExpiredFlags();
  }

  /**
   * Returns true if the request is conditional.
   *
   * @return true if this request is conditional.
   */
  public boolean isConditional() {
    return containsKey(CTX_CONDITION_IF_MODIFIED_SINCE) || containsKey(CTX_CONDITION_IF_NONE_MATCH);
  }

  /**
   * Returns the timestamp to check against.
   */
  public long getIfModifiedSince() {
    if (containsKey(CTX_CONDITION_IF_MODIFIED_SINCE)) {
      return (Long) get(CTX_CONDITION_IF_MODIFIED_SINCE);
    }
    else {
      return 0;
    }
  }

  /**
   * Sets the timestamp to check against.
   */
  public void setIfModifiedSince(long ifModifiedSince) {
    if (ifModifiedSince != 0) {
      put(CTX_CONDITION_IF_MODIFIED_SINCE, Long.valueOf(ifModifiedSince));
    }
    else {
      remove(CTX_CONDITION_IF_MODIFIED_SINCE);
    }
  }

  /**
   * Gets the ETag (SHA1 in Nexus) to check item against.
   */
  public String getIfNoneMatch() {
    return (String) get(CTX_CONDITION_IF_NONE_MATCH);
  }

  /**
   * Sets the ETag (SHA1 in Nexus) to check item against.
   */
  public void setIfNoneMatch(String tag) {
    if (!StringUtils.isEmpty(tag)) {
      put(CTX_CONDITION_IF_NONE_MATCH, tag);
    }
    else {
      remove(CTX_CONDITION_IF_NONE_MATCH);
    }
  }

  /**
   * Returns the URL of the original request.
   */
  public String getRequestUrl() {
    return (String) get(CTX_REQUEST_URL);
  }

  /**
   * Sets the URL of the original request.
   */
  public void setRequestUrl(String url) {
    if (!StringUtils.isBlank(url)) {
      put(CTX_REQUEST_URL, url);
    }
    else {
      remove(CTX_REQUEST_URL);
    }
  }

  /**
   * Returns {@code true} if this request is external, made by client outside of Nexus. Returns {@code false}
   * for requests made internally, like for example made from tasks.
   *
   * @since 2.6
   */
  public boolean isRequestIsExternal() {
    if (containsKey(CTX_REQUEST_IS_EXTERNAL)) {
      return (Boolean) get(CTX_REQUEST_IS_EXTERNAL);
    }
    else {
      return false;
    }
  }

  /**
   * Sets if this requst is external.
   *
   * @since 2.6
   */
  public void setRequestIsExternal(boolean external) {
    put(CTX_REQUEST_IS_EXTERNAL, external);
  }

  /**
   * Returns {@code true} if this is a 'describe' request; otherwise {@code false}.
   *
   * @since 2.14.3
   */
  public boolean isRequestIsDescribe() {
    if (containsKey(CTX_REQUEST_IS_DESCRIBE)) {
      return (Boolean) get(CTX_REQUEST_IS_DESCRIBE);
    }
    else {
      return false;
    }
  }

  /**
   * Sets if this is a 'describe' request.
   *
   * @since 2.14.3
   */
  public void setRequestIsDescribe(boolean describe) {
    put(CTX_REQUEST_IS_DESCRIBE, describe);
  }

  // ==

  /**
   * Returns a new map instance that contains flattened RequestContext as it is "viewed" by callers (overlaid in
   * proper order).
   */
  public Map<String, Object> flatten() {
    final HashMap<String, Object> result = new HashMap<String, Object>();
    RequestContext ctx = this;
    final Stack<RequestContext> stack = new Stack<RequestContext>();
    while (ctx != null) {
      stack.push(ctx);
      ctx = ctx.getParentContext();
    }
    while (!stack.isEmpty()) {
      ctx = stack.pop();
      for (Map.Entry<String, Object> entry : ctx.delegate.entrySet()) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  // ==

  @Override
  public String toString() {
    return "RequestContext{" + "this=" + super.toString() + ", parent=" + parent + '}';
  }
}

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
package org.sonatype.nexus.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageEOFException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RemoteStorageTransportOverloadedException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageCompositeItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.item.uid.IsRemotelyAccessibleAttribute;
import org.sonatype.nexus.proxy.repository.GroupItemNotFoundException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.rest.model.ContentListDescribeRequestResource;
import org.sonatype.nexus.rest.model.ContentListDescribeResource;
import org.sonatype.nexus.rest.model.ContentListDescribeResourceResponse;
import org.sonatype.nexus.rest.model.ContentListDescribeResponseResource;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.NotFoundReasoning;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.security.filter.authc.NexusHttpAuthenticationFilter;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.nexus.web.ErrorStatusRuntimeException;
import org.sonatype.plexus.rest.representation.VelocityRepresentation;
import org.sonatype.security.SecuritySystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;
import com.noelios.restlet.http.HttpResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.Context;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.util.Series;

import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * This is an abstract resource handler that uses ResourceStore implementor and publishes those over REST.
 *
 * @author cstamas
 */
public abstract class AbstractResourceStoreContentPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String IS_DESCRIBE_PARAMETER = "describe";

  public static final String REQUEST_RECEIVED_KEY = "request.received.timestamp";

  public static final String OVERRIDE_FILENAME_KEY = "override-filename";

  private SecuritySystem securitySystem;

  private ApplicationStatusSource applicationStatusSource;

  public Map<String, ArtifactViewProvider> viewProviders;
  
  public AbstractResourceStoreContentPlexusResource() {
    super();

    setReadable(true);

    setModifiable(true);
  }

  @VisibleForTesting
  AbstractResourceStoreContentPlexusResource(final SecuritySystem securitySystem,
                                             final ApplicationStatusSource applicationStatusSource,
                                             final Map<String, ArtifactViewProvider> viewProviders)
  {
    this.securitySystem = securitySystem;
    this.applicationStatusSource = applicationStatusSource;
    this.viewProviders = viewProviders;
  }

  @Inject
  public void setSecuritySystem(final SecuritySystem securitySystem) {
    this.securitySystem = securitySystem;
  }

  @Inject
  public void setApplicationStatusSource(final ApplicationStatusSource applicationStatusSource) {
    this.applicationStatusSource = applicationStatusSource;
  }

  @Inject
  public void setViewProviders(final Map<String, ArtifactViewProvider> viewProviders) {
    this.viewProviders = viewProviders;
  }

  @Override
  public boolean acceptsUpload() {
    return true;
  }

  protected String getResourceStorePath(Request request) {
    try {
      // #getRemainingPart(true, true) would use Restlet decoding, that relies
      // on same URLDecoder as below, but without this "trick" below
      // source: http://stackoverflow.com/questions/2632175/java-decoding-uri-query-string
      final String remainingPart = request.getResourceRef().getRemainingPart(false, false);
      final String remainingDecodedPart =
          URLDecoder.decode(remainingPart.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
      return parsePathFromUri(remainingDecodedPart);
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "Nexus cannot operate on platform not supporting UTF-8 character encoding!", e);
    }
  }

  protected boolean isDescribe(Request request) {
    // check do we need describe
    return request.getResourceRef().getQueryAsForm().getFirst(IS_DESCRIBE_PARAMETER) != null;
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {

    try {
      final ResourceStore store = getResourceStore(request);
      final ResourceStoreRequest req = getResourceStoreRequest(request);

      try {
        StorageItem item = store.retrieveItem(req);

        return renderItem(context, request, response, variant, store, item);
      }
      catch (ItemNotFoundException e) {
        if (isDescribe(request)) {
          return renderDescribeItem(context, request, response, variant, store, req, null, e);
        }
        else {
          throw e;
        }
      }
    }
    catch (Exception e) {
      handleException(request, response, e);

      return null;
    }
  }

  private void addNoCacheHeaders(final Response response) {
    // NEXUS-5155 Force browsers to not cache this page
    final Series<Parameter> headers = ((HttpResponse) response).getHttpCall().getResponseHeaders();
    headers.add("Pragma", "no-cache"); // HTTP/1.0
    headers.add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"); // HTTP/1.1
    headers.add("Cache-Control", "post-check=0, pre-check=0"); // MS IE
    headers.add("Expires", "0"); // No caching on Proxies in between client and Nexus
  }

  @Override
  public Object upload(Context context, Request request, Response response, List<FileItem> files)
      throws ResourceException
  {
    // NEXUS-4151: Do not accept upload/deploy requests with media type (Content-Type) of
    // "application/x-www-form-urlencoded", since ad 1, it's wrong, ad 2, we do know
    // Jetty's Request object "eats" up it's body to parse request parameters, invoked
    // way earlier in security filters
    if (request.isEntityAvailable()) {
      MediaType mt = request.getEntity().getMediaType();

      if (mt != null && MediaType.APPLICATION_WWW_FORM.isCompatible(mt)) {
        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Content-type of \"" + mt.toString()
            + "\" is not acceptable for uploads!");
      }
    }

    try {
      final ResourceStoreRequest req = getResourceStoreRequest(request);

      for (FileItem fileItem : files) {
        getResourceStore(request).storeItem(req, fileItem.getInputStream(), null);
      }
    }
    catch (Exception t) {
      handleException(request, response, t);
    }
    return null;
  }

  @Override
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    try {
      final ResourceStore store = getResourceStore(request);
      final ResourceStoreRequest req = getResourceStoreRequest(request);

      store.deleteItem(req);

      getLogger().info(
          "Storage item(s) on path \"" + req.getRequestPath() + "\" (and below) were deleted from repository ["
              + request.getAttributes().get(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) + "]");
    }
    catch (Exception e) {
      handleException(request, response, e);
    }
  }

  protected String parsePathFromUri(String parsedPath) {
    // get rid of query part
    if (parsedPath.contains("?")) {
      parsedPath = parsedPath.substring(0, parsedPath.indexOf('?'));
    }

    // get rid of reference part
    if (parsedPath.contains("#")) {
      parsedPath = parsedPath.substring(0, parsedPath.indexOf('#'));
    }

    // Remove all leading `/`es at the front to prevent SECREP-418
    parsedPath = parsedPath.replaceAll("^/+", "");

    // and then make sure request is again prefixed with a single '/' as paths are expectedd to start with '/'
    if (!parsedPath.startsWith("/")) {
      parsedPath = "/" + parsedPath;
    }

    return parsedPath;
  }

  /**
   * A strategy to get ResourceStore implementor. To be implemented by subclass.
   */
  protected abstract ResourceStore getResourceStore(final Request request)
      throws NoSuchResourceStoreException, ResourceException;

  /**
   * Centralized way to create ResourceStoreRequests, since we have to fill in various things in Request context,
   * like
   * authenticated username, etc.
   */
  protected ResourceStoreRequest getResourceStoreRequest(Request request) {
    return getResourceStoreRequest(request, getResourceStorePath(request));
  }

  /**
   * Centralized way to create ResourceStoreRequests, since we have to fill in various things in Request context,
   * like
   * authenticated username, etc.
   */
  protected ResourceStoreRequest getResourceStoreRequest(Request request, String resourceStorePath) {
    ResourceStoreRequest result = new ResourceStoreRequest(resourceStorePath);

    getLogger().trace("Created ResourceStore request for {}", result.getRequestPath());

    // honor the local only and remote only
    result.setRequestLocalOnly(isLocal(request, resourceStorePath));
    result.setRequestRemoteOnly(isRemote(request, resourceStorePath));
    result.setRequestAsExpired(asExpired(request, resourceStorePath));
    result.setExternal(true);

    // honor the describe, add timing
    if (isDescribe(request)) {
      result.getRequestContext().put(REQUEST_RECEIVED_KEY, System.currentTimeMillis());
      result.setDescribe(true);
    }

    // honor if-modified-since
    if (request.getConditions().getModifiedSince() != null) {
      result.setIfModifiedSince(request.getConditions().getModifiedSince().getTime());
    }

    // honor if-none-match
    if (request.getConditions().getNoneMatch() != null && request.getConditions().getNoneMatch().size() > 0) {
      final Tag tag = request.getConditions().getNoneMatch().get(0);
      // NEXUS-5704: 500 Internal Server Error when "If-None-Match" in header
      // Restlet 1.1 is very strict about properly formatted ETags (must be quoted)
      // If unquoted, their presence is detected (IF above evals to true), but will
      // actually return null as parsing the tag
      if (tag != null && tag.getName() != null) {
        result.setIfNoneMatch(tag.getName());
      }
    }

    // stuff in the originating remote address
    result.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, getValidRemoteIPAddress(request));

    // stuff in the user id if we have it in request
    Subject subject = securitySystem.getSubject();
    if (subject != null && subject.getPrincipal() != null) {
      result.getRequestContext().put(AccessManager.REQUEST_USER, subject.getPrincipal().toString());
    }
    result.getRequestContext().put(AccessManager.REQUEST_AGENT, request.getClientInfo().getAgent());

    // this is HTTPS, get the cert and stuff it too for later
    if (request.isConfidential()) {
      result.getRequestContext().put(AccessManager.REQUEST_CONFIDENTIAL, Boolean.TRUE);

      List<?> certs = (List<?>) request.getAttributes().get("org.restlet.https.clientCertificates");

      if (certs != null) {
        result.getRequestContext().put(AccessManager.REQUEST_CERTIFICATES, certs);
      }
    }

    // put the incoming URLs
    result.setRequestUrl(request.getOriginalRef().toString());

    return result;
  }

  protected Object renderItem(Context context, Request req, Response res, Variant variant, ResourceStore store,
                              StorageItem item)
      throws IOException, AccessDeniedException, NoSuchResourceStoreException, IllegalOperationException,
             ItemNotFoundException, StorageException, ResourceException
  {
    if (isDescribe(req)) {
      return renderDescribeItem(context, req, res, variant, store, item.getResourceStoreRequest(), item, null);
    }

    if (!item.isVirtual()) {
      if (!item.getRepositoryItemUid().getBooleanAttributeValue(IsRemotelyAccessibleAttribute.class)) {
        getLogger().debug(
            String.format("Request for remotely non-accessible UID %s is made and refused",
                item.getRepositoryItemUid().toString()));

        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Resource is not found.");
      }
    }

    if (item instanceof StorageFileItem) {
      return renderStorageFileItem(req, (StorageFileItem) item);
    }
    else if (item instanceof StorageLinkItem) {
      return renderStorageLinkItem(context, req, res, variant, store, (StorageLinkItem) item);
    }
    else if (item instanceof StorageCollectionItem) {
      return renderStorageCollectionItem(context, req, res, variant, store, (StorageCollectionItem) item);
    }

    return null;
  }

  @VisibleForTesting
  Object renderStorageCollectionItem(final Context context, final Request req, final Response res,
                                     final Variant variant, final ResourceStore store,
                                     final StorageCollectionItem coll)
      throws IOException, NoSuchResourceStoreException, IllegalOperationException, ItemNotFoundException,
             AccessDeniedException, ResourceException
  {
    String resPath = parsePathFromUri(req.getResourceRef().toString());

    if (!resPath.endsWith("/")) {
      res.redirectPermanent(createRedirectReference(req).getTargetRef().toString() + "/");

      return null;
    }

    // NXCM-5155 do not cache index pages
    addNoCacheHeaders(res);

    if (Method.HEAD.equals(req.getMethod())) {
      return renderHeadResponseItem(context, req, res, variant, store, coll.getResourceStoreRequest(), coll);
    }

    Collection<StorageItem> children = coll.list();

    ContentListResourceResponse response = new ContentListResourceResponse();

    ContentListResource resource;

    List<String> uniqueNames = new ArrayList<String>(children.size());

    for (StorageItem child : children) {
      if (child.isVirtual() || !child.getRepositoryItemUid().getBooleanAttributeValue(IsHiddenAttribute.class)) {
        if (!uniqueNames.contains(child.getName())) {
          resource = new ContentListResource();

          resource.setText(child.getName());

          resource.setLeaf(!StorageCollectionItem.class.isAssignableFrom(child.getClass()));

          String uri = getResourceUri(req, resource, child);
          resource.setResourceURI(uri);

          resource.setRelativePath(child.getPath() + (resource.isLeaf() ? "" : "/"));

          resource.setLastModified(new Date(child.getModified()));

          resource.setSizeOnDisk(
              StorageFileItem.class.isAssignableFrom(child.getClass()) ? ((StorageFileItem) child).getLength()
                  : -1);

          response.addData(resource);

          uniqueNames.add(child.getName());
        }
      }
    }

    if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
      Representation result = serialize(context, req, variant, response);

      result.setModificationDate(new Date(coll.getModified()));

      return result;
    }
    else {
      return response;
    }
  }

  @VisibleForTesting
  Object renderStorageLinkItem(final Context context, final Request req, final Response res, final Variant variant,
                               final ResourceStore store, final StorageLinkItem item)
      throws ResourceException
  {
    // we have a link, dereference it
    // TODO: we should be able to do HTTP redirects too! (parametrize the dereferencing?)
    try {
      return renderItem(context, req, res, variant, store, getRepositoryRouter().dereferenceLink(item));
    }
    catch (Exception e) {
      handleException(req, res, e);

      return null;
    }
  }

  @VisibleForTesting
  Representation renderStorageFileItem(final Request req, final StorageFileItem file)
      throws ResourceException
  {
    final StorageFileItemRepresentation fileRepresentation = new StorageFileItemRepresentation(file);
    if (file.getResourceStoreRequest().getIfModifiedSince() != 0
        && file.getModified() <= file.getResourceStoreRequest().getIfModifiedSince()) {
      // this is a conditional GET using time-stamp
      throw new ResourceException(Status.REDIRECTION_NOT_MODIFIED, "Resource is not modified.");
    }
    else if (file.getResourceStoreRequest().getIfNoneMatch() != null && fileRepresentation.getTag() != null
        && file.getResourceStoreRequest().getIfNoneMatch().equals(fileRepresentation.getTag().getName())) {
      // this is a conditional GET using ETag
      throw new ResourceException(Status.REDIRECTION_NOT_MODIFIED, "Resource is not modified.");
    }
    else {
      return fileRepresentation;
    }
  }

  private String getResourceUri(Request req, ContentListResource resource, StorageItem child) {
    // NEXUS-4244: simply force both baseURLs, coming from nexus.xml and extracted from current request
    // to end with slash ("/").
    Reference root = getContextRoot(req);
    if (StringUtils.isBlank(root.getPath()) || !root.getPath().endsWith("/")) {
      root.setPath(StringUtils.defaultString(root.getPath(), "") + "/");
    }
    Reference requestRoot = req.getRootRef().getParentRef().getParentRef();
    if (StringUtils.isBlank(requestRoot.getPath()) || !requestRoot.getPath().endsWith("/")) {
      requestRoot.setPath(StringUtils.defaultString(requestRoot.getPath(), "") + "/");
    }

    final Reference ref = req.getResourceRef().getTargetRef();
    String uri = ref.toString();

    if (ref.getQuery() != null) {
      uri = uri.substring(0, uri.length() - ref.getQuery().length() - 1);
    }

    if (!uri.endsWith("/")) {
      uri += "/";
    }
    uri += child.getName();
    if (!resource.isLeaf()) {
      uri += "/";
    }

    if (root == requestRoot || root.equals(requestRoot)) {
      return uri;
    }
    else {
      return uri.replace(requestRoot.toString(), root.toString());
    }
  }

  protected Representation serialize(Context context, Request req, Variant variant, Object payload)
      throws IOException
  {
    // TEXT_HTML is requested by direct browsing (IE)
    // APPLICATION_XML is requested by direct browsing (FF)
    if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
      HashMap<String, Object> dataModel = new HashMap<String, Object>();

      dataModel.put("listItems", sortContentListResource(((ContentListResourceResponse) payload).getData()));
      dataModel.put("request", req);
      dataModel.put("nexusVersion", applicationStatusSource.getSystemStatus().getVersion());
      dataModel.put("nexusRoot", BaseUrlHolder.get());

      final VelocityRepresentation representation =
          new VelocityRepresentation(context, "/templates/repositoryContentHtml.vm",
              getClass().getClassLoader(), dataModel, variant.getMediaType());
      return representation;
    }

    return null;
  }

  protected Object renderHeadResponseItem(Context context, Request req, Response res, Variant variant,
                                          ResourceStore store, ResourceStoreRequest request,
                                          StorageCollectionItem coll)
      throws IOException, AccessDeniedException, NoSuchResourceStoreException, IllegalOperationException,
             ItemNotFoundException, StorageException, ResourceException
  {
    // we are just returning anything, the connector will strip off content anyway.
    return new StorageItemRepresentation(variant.getMediaType(), coll);
  }

  protected Object renderDescribeItem(Context context, Request req, Response res, Variant variant,
                                      ResourceStore store, ResourceStoreRequest request, StorageItem item,
                                      Throwable t)
      throws IOException, AccessDeniedException, NoSuchResourceStoreException, IllegalOperationException,
             ItemNotFoundException, StorageException, ResourceException
  {
    Parameter describeParameter = req.getResourceRef().getQueryAsForm().getFirst(IS_DESCRIBE_PARAMETER);

    if (StringUtils.isNotEmpty(describeParameter.getValue())) {
      // if item is null throw not found
      String key = describeParameter.getValue();

      // check
      if (!viewProviders.containsKey(key)) {
        throw new IllegalRequestException(request, "No view for key: " + key);
      }

      Object result = viewProviders.get(key).retrieveView(store, request, item, req);

      // make sure we have valid content
      if (result == null) {
        throw new ItemNotFoundException(reasonFor(request,
            "View provider keyed \"%s\" did not provide content.", key));
      }
      else {
        return result;
      }
    }

    ContentListDescribeResourceResponse result = new ContentListDescribeResourceResponse();

    ContentListDescribeResource resource = new ContentListDescribeResource();

    resource.setRequestUrl(req.getOriginalRef().toString());

    if (request.getRequestContext().containsKey(REQUEST_RECEIVED_KEY)) {
      long received = (Long) request.getRequestContext().get(REQUEST_RECEIVED_KEY);

      resource.setProcessingTimeMillis(System.currentTimeMillis() - received);
    }
    else {
      resource.setProcessingTimeMillis(-1);
    }

    resource.setRequest(describeRequest(context, req, res, variant, request));

    resource.setResponse(describeResponse(context, req, res, variant, request, item, t));

    result.setData(resource);

    return result;
  }

  protected ContentListDescribeRequestResource describeRequest(Context context, Request req, Response res,
                                                               Variant variant, ResourceStoreRequest request)
  {
    ContentListDescribeRequestResource result = new ContentListDescribeRequestResource();

    result.setRequestUrl(request.getRequestUrl());

    result.setRequestPath(request.getRequestPath());

    for (Map.Entry<String, Object> entry : request.getRequestContext().flatten().entrySet()) {
      result.addRequestContext(entry.toString());
    }

    return result;
  }

  protected NotFoundReasoning buildNotFoundReasoning(final Repository repository, final Throwable t) {
    final NotFoundReasoning reasoning = new NotFoundReasoning();

    reasoning.setReasonMessage(t.getMessage());
    reasoning.setThrowableType(t.getClass().getName());
    if (repository != null) {
      reasoning.setRepositoryId(repository.getId());
    }

    if (t instanceof GroupItemNotFoundException) {
      final GroupItemNotFoundException ginf = (GroupItemNotFoundException) t;
      reasoning.setRepositoryId(ginf.getReason().getRepository().getId());

      for (Map.Entry<Repository, Throwable> r : ginf.getMemberReasons().entrySet()) {
        reasoning.addNotFoundReasoning(buildNotFoundReasoning(r.getKey(), r.getValue()));
      }
    }

    return reasoning;
  }

  protected ContentListDescribeResponseResource describeResponse(Context context, Request req, Response res,
                                                                 Variant variant, ResourceStoreRequest request,
                                                                 StorageItem item, Throwable e)
  {
    ContentListDescribeResponseResource result = new ContentListDescribeResponseResource();

    result.getProcessedRepositoriesList().addAll(request.getProcessedRepositories());

    // applied mappings
    for (Map.Entry<String, List<String>> mappingEntry : request.getAppliedMappings().entrySet()) {
      result.addAppliedMapping(mappingEntry.getKey() + " repository applied " + mappingEntry.getValue());
    }

    if (item == null) {
      result.setResponseType("NOT_FOUND");

      if (e != null) {
        result.addNotFoundReasoning(buildNotFoundReasoning(null, e));
      }

      return result;
    }

    if (item instanceof StorageFileItem) {
      result.setResponseType("FILE");

    }
    else if (item instanceof StorageCollectionItem) {
      result.setResponseType("COLL");

    }
    else if (item instanceof StorageLinkItem) {
      result.setResponseType("LINK");
    }
    else {
      result.setResponseType(item.getClass().getName());
    }

    result.setResponseActualClass(item.getClass().getName());

    result.setResponsePath(item.getPath());

    if (!item.isVirtual()) {
      result.setResponseUid(item.getRepositoryItemUid().toString());

      result.setOriginatingRepositoryId(item.getRepositoryItemUid().getRepository().getId());

      result.setOriginatingRepositoryName(item.getRepositoryItemUid().getRepository().getName());

      result.setOriginatingRepositoryMainFacet(
          item.getRepositoryItemUid().getRepository().getRepositoryKind().getMainFacet().getName());
    }
    else {
      result.setResponseUid("virtual");
    }

    // properties
    result.addProperty("created=" + item.getCreated());
    result.addProperty("modified=" + item.getModified());
    result.addProperty("lastRequested=" + item.getLastRequested());
    result.addProperty("remoteChecked=" + item.getRemoteChecked());
    result.addProperty("remoteUrl=" + item.getRemoteUrl());
    result.addProperty("storedLocally=" + item.getStoredLocally());
    result.addProperty("isExpired=" + item.isExpired());
    result.addProperty("readable=" + item.isReadable());
    result.addProperty("writable=" + item.isWritable());
    result.addProperty("virtual=" + item.isVirtual());

    // attributes
    final TreeMap<String, String> sortedAttributes = Maps.newTreeMap();
    sortedAttributes.putAll(item.getRepositoryItemAttributes().asMap());
    for (Map.Entry<String, String> entry : sortedAttributes.entrySet()) {
      result.addAttribute(entry.toString());
    }

    // sources
    if (item instanceof StorageCompositeItem) {
      StorageCompositeItem composite = (StorageCompositeItem) item;
      for (StorageItem source : composite.getSources()) {
        if (!source.isVirtual()) {
          result.addSource(source.getRepositoryItemUid().toString());
        }
        else {
          result.addSource(source.getPath());
        }
      }
    }

    return result;
  }

  private List<ContentListResource> sortContentListResource(Collection<ContentListResource> list) {
    List<ContentListResource> result = new ArrayList<ContentListResource>(list);

    Collections.sort(result, new Comparator<ContentListResource>()
    {
      public int compare(ContentListResource o1, ContentListResource o2) {
        if (!o1.isLeaf()) {
          if (!o2.isLeaf()) {
            // 2 directories, do a path compare
            return o1.getText().compareTo(o2.getText());
          }
          else {
            // first item is a dir, second is a file, dirs always win
            return 1;
          }
        }
        else if (!o2.isLeaf()) {
          // first item is a file, second is a dir, dirs always win
          return -1;
        }
        else {
          // 2 files, do a path compare
          return o1.getText().compareTo(o2.getText());
        }
      }
    });

    return result;
  }

  /**
   * ResourceStore iface is pretty "chatty" with Exceptions. This is a centralized place to handle them and convert
   * them to proper HTTP status codes and response.
   */
  protected void handleException(final Request req, final Response res, final Exception t)
      throws ResourceException
  {
    // just set this flag to true in any if-else branch you want to see
    // complete error loglines with stack traces.
    // Note: when Nexus is in DEBUG logging mode, then we log _all_ exceptions
    // with stack traces except ItemNotFoundException (to lessen the noise).
    boolean shouldLogInfoStackTrace = false;

    try {
      if (t instanceof ResourceException) {
        throw (ResourceException) t;
      }
      else if (t instanceof LocalStorageEOFException) {
        // in case client drops connection, this makes not much sense, as he will not
        // receive this response, but we have to end it somehow.
        // but, in case when remote proxy peer drops connection on us regularly
        // this makes sense
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_NOT_FOUND, t), t);
      }
      else if (t instanceof IllegalArgumentException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_BAD_REQUEST, t), t);
      }
      else if (t instanceof RemoteStorageTransportOverloadedException) {
        throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, t);
      }
      else if (t instanceof RepositoryNotAvailableException) {
        throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, t);
      }
      else if (t instanceof IllegalRequestException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_BAD_REQUEST, t), t);
      }
      else if (t instanceof IllegalOperationException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_BAD_REQUEST, t), t);
      }
      else if (t instanceof UnsupportedStorageOperationException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_BAD_REQUEST, t), t);
      }
      else if (t instanceof NoSuchRepositoryAccessException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_FORBIDDEN, t), t);
      }
      else if (t instanceof NoSuchRepositoryException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_NOT_FOUND, t), t);
      }
      else if (t instanceof NoSuchResourceStoreException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_NOT_FOUND, t), t);
      }
      else if (t instanceof ItemNotFoundException) {
        throw new ResourceException(getStatus(Status.CLIENT_ERROR_NOT_FOUND, t), t);
      }
      else if (t instanceof AccessDeniedException) {
        challengeIfNeeded(req, res, (AccessDeniedException) t);
      }
      else if (t instanceof ErrorStatusRuntimeException) {
        ErrorStatusRuntimeException ex = (ErrorStatusRuntimeException) t;
        throw new ResourceException(new Status(ex.getResponseCode(), ex.getReasonPhrase(), ex.getMessage(), null), t);
      }
      else {
        // Internal error, we force it to log
        shouldLogInfoStackTrace = true;

        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, t);
      }
    }
    finally {
      if (t instanceof ResourceException) {
        ResourceException re = (ResourceException) t;

        // See NEXUS-4380
        // do not spam the log with non-error responses, we need only errors to have logged in case when
        // exception to be handled is ResourceException
        if (re.getStatus() == null || re.getStatus().isError()) {
          handleErrorConstructLogMessage(req, res, t, shouldLogInfoStackTrace);
        }
      }
      else {
        handleErrorConstructLogMessage(req, res, t, shouldLogInfoStackTrace);
      }
    }
  }

  private Status getStatus(final Status status, final Exception e) {
    if (e == null || e.getMessage() == null) {
      return status;
    }
    return new Status(status, e.getMessage());
  }

  protected void handleErrorConstructLogMessage(final Request req, final Response res, final Exception t,
                                                final boolean shouldLogInfoStackTrace)
  {
    String message =
        "Got exception during processing request \"" + req.getMethod() + " " + req.getResourceRef().toString()
            + "\": ";

    // NEXUS-4417: logging of 'repository not found' should be debug level
    // NEXUS-4788 log remote 'Forbidden' response only on debug
    if (t instanceof NoSuchRepositoryException || t instanceof AccessDeniedException) {
      getLogger().debug(message, t);
    }
    else if (getLogger().isDebugEnabled()) {
      // if DEBUG level, we log _all_ errors with stack traces, except the ItemNotFoundException

      if (t instanceof ItemNotFoundException) {
        // we are "muting" item not found exception stack traces, it pollutes the DEBUG logs
        getLogger().error(message + t.getMessage());
      }
      else {
        // in debug mode, we log _with_ stack trace
        getLogger().error(message, t);
      }
    }
    else {
      // if not in DEBUG mode, we obey the flag to decide whether we need to log or not the stack trace
      if ((t instanceof ItemNotFoundException || t instanceof IllegalRequestException)
          && !shouldLogInfoStackTrace) {
        // mute it
      }
      else {
        if (shouldLogInfoStackTrace) {
          // in INFO mode, we obey the shouldLogInfoStackTrace flag for serious errors (like internal is)
          getLogger().error(message, t);
        }
        else {
          // in INFO mode, we want one liners usually
          getLogger().error(message + t.getMessage());
        }
      }
    }
  }

  /**
   * TODO: this code below should be removed. It is better than the one before (it actually reuses Shiro filter, and
   * will not try to reassemble the Challenge anymore, but stil...
   */
  public static void challengeIfNeeded(final Request req, final Response res, final AccessDeniedException t) {
    // TODO: a big fat problem here!
    // this makes restlet code tied to Servlet code, and we what is happening here is VERY dirty!
    HttpServletRequest servletRequest = ((ServletCall) ((HttpRequest) req).getHttpCall()).getRequest();

    String scheme = (String) servletRequest.getAttribute(NexusHttpAuthenticationFilter.AUTH_SCHEME_KEY);

    ChallengeScheme challengeScheme = null;

    if (NexusHttpAuthenticationFilter.FAKE_AUTH_SCHEME.equals(scheme)) {
      challengeScheme = new ChallengeScheme("HTTP_NXBASIC", "NxBasic", "Fake basic HTTP authentication");
    }
    else {
      challengeScheme = ChallengeScheme.HTTP_BASIC;
    }

    String realm = (String) servletRequest.getAttribute(NexusHttpAuthenticationFilter.AUTH_REALM_KEY);

    if (servletRequest.getAttribute(NexusHttpAuthenticationFilter.ANONYMOUS_LOGIN) != null) {
      res.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
    }
    else {
      res.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
    }

    res.getChallengeRequests().add(new ChallengeRequest(challengeScheme, realm));

    // TODO: this below would be _slightly_ better.
    // HttpServletRequest servletRequest = ( (ServletCall) ( (HttpRequest) req ).getHttpCall() ).getRequest();
    //
    // if ( servletRequest.getAttribute( NexusHttpAuthenticationFilter.ANONYMOUS_LOGIN ) != null )
    // {
    // // setting this flag to notify NexusHttpAuthenticationFilter that a challenge is needed
    // // without actually _knowing_ what kind of challenge we use
    // servletRequest.setAttribute( NexusJSecurityFilter.REQUEST_IS_AUTHZ_REJECTED, Boolean.TRUE );
    //
    // res.setStatus( Status.CLIENT_ERROR_UNAUTHORIZED );
    // }
    // else
    // {
    // res.setStatus( Status.CLIENT_ERROR_FORBIDDEN );
    // }
  }
}

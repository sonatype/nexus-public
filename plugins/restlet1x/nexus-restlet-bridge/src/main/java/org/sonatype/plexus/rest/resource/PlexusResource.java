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
package org.sonatype.plexus.rest.resource;

import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.fileupload.FileItem;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * An automatically managed Rest Resource.
 *
 * @author cstamas
 */
public interface PlexusResource
{
  /**
   * The location to attach this resource to.
   */
  String getResourceUri();

  /**
   * A factory method to create an instance of DTO.
   */
  Object getPayloadInstance();

  /**
   * A factory method to create an instance of DTO per method.
   */
  Object getPayloadInstance(Method method);

  /**
   * A Resource may add some configuration stuff to the XStream, and control the serialization of the payloads it
   * uses.
   */
  void configureXStream(XStream xstream);

  /**
   * A permission prefix to be applied when securing the resource.
   */
  PathProtectionDescriptor getResourceProtection();

  /**
   * Presents a modifiable list of available variants.
   */
  List<Variant> getVariants();

  /**
   * Does resource accepts any call?
   */
  boolean isAvailable();

  /**
   * Does resource accepts GET method?
   */
  boolean isReadable();

  /**
   * Does resource accepts PUT, POST, DELETE methods?
   */
  boolean isModifiable();

  /**
   * If true, Restlet will try to negotiate the "best" content.
   */
  boolean isNegotiateContent();

  /**
   * If true, will redirect POST and PUT to as many upload() method calls, as many files are in request.
   */
  boolean acceptsUpload();

  /**
   * If true, strict (exact) checking should be used for resource URI matching
   */
  boolean requireStrictChecking();

  /**
   * Method invoked on incoming GET request. The method may return: Representation (will be passed unchanged to
   * restlet engine), InputStream (will be wrapped into InputStreamRepresentation), String (will be wrapped into
   * StringRepresentation) and Object. If Object is none of those previously listed, an XStream serialization is
   * applied to it (into variant originally negotiated with client).
   *
   * @param context  - the cross-request context
   * @param request  - the request
   * @param response - the response
   * @param variant  - the result of the content negotiation (for use by PlexusResources that want's to cruft
   *                 manually
   *                 some Representation).
   * @return Object to be returned to the client. Object may be: InputStream, restlet.org Representation, String or
   *         any object. The "any" object will be serialized by XStream to a proper mediaType if possible.
   */
  Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException;

  /**
   * Method invoked on incoming POST request. For return Object, see GET method.
   *
   * @param context  - the cross-request context
   * @param request  - the request
   * @param response = the response
   * @param payload  - the deserialized payload (if it was possible to deserialize). Otherwise, the Representation is
   *                 accessible thru request. If deserialization was not possible it is null.
   */
  Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException;

  /**
   * Method invoked on incoming PUT request. For return Object, see GET method.
   *
   * @param context  - the cross-request context
   * @param request  - the request
   * @param response = the response
   * @param payload  - the deserialized payload (if it was possible to deserialize). Otherwise, the Representation is
   *                 accessible thru request. If deserialization was not possible it is null.
   */
  Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException;

  /**
   * Method invoked on incoming DELETE request.
   *
   * @param context  - the cross-request context
   * @param request  - the request
   * @param response = the response
   */
  void delete(Context context, Request request, Response response)
      throws ResourceException;

  /**
   * "Catch all" method if this method accepts uploads (acceptsUpload() returns true). In this case, the PUT and POST
   * requests will be redirected to this method. For return Object, see GET method.
   *
   * @param context  - the cross-request context
   * @param request  - the request
   * @param response = the response
   */
  Object upload(Context context, Request request, Response response, List<FileItem> files)
      throws ResourceException;
  
  /**
   * Method invoked on incoming OPTIONS request.
   * 
   * @param context - the cross-request context
   * @param request - the request
   * @param response = the response
   * @throws ResourceException
   */
  void options( Context context, Request request, Response response )
      throws ResourceException;

}

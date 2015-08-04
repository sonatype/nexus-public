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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.plexus.rest.PlexusRestletApplicationBridge;
import org.sonatype.plexus.rest.representation.InputStreamRepresentation;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.sisu.goodies.common.Loggers;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;

/**
 * The delegating resource.
 *
 * @author Jason van Zyl
 * @author cstamas
 */
public class RestletResource
    extends Resource
{
  protected final Logger logger = Loggers.getLogger(getClass());

  private PlexusResource delegate;

  public RestletResource(Context context, Request request, Response response, PlexusResource delegate) {
    super(context, request, response);

    this.delegate = delegate;

    // set variants
    getVariants().clear();
    getVariants().addAll(delegate.getVariants());

    // mimic the constructor
    setAvailable(delegate.isAvailable());
    setReadable(delegate.isReadable());
    setModifiable(delegate.isModifiable());
    setNegotiateContent(delegate.isNegotiateContent());
  }

  /**
   * For file uploads we are using commons-fileupload integration with restlet.org. We are storing one
   * FileItemFactory
   * instance in context. This method simply encapsulates gettting it from Resource context.
   */
  protected FileItemFactory getFileItemFactory() {
    return (FileItemFactory) getContext().getAttributes().get(PlexusRestletApplicationBridge.FILEITEM_FACTORY);
  }

  protected XStreamRepresentation createRepresentation(Variant variant)
      throws ResourceException
  {
    XStreamRepresentation representation = null;

    try {
      // check is this variant a supported one, to avoid calling getText() on potentially huge representations
      if (MediaType.APPLICATION_JSON.equals(variant.getMediaType(), true)
          || MediaType.APPLICATION_XML.equals(variant.getMediaType(), true)
          || MediaType.TEXT_HTML.equals(variant.getMediaType(), true)) {
        String text = (variant instanceof Representation) ? ((Representation) variant).getText() : "";

        XStream xstream;
        if (MediaType.APPLICATION_JSON.equals(variant.getMediaType(), true)
            || MediaType.TEXT_HTML.equals(variant.getMediaType(), true)) {
          xstream = (XStream) getContext().getAttributes().get(PlexusRestletApplicationBridge.JSON_XSTREAM);
        }
        else if (MediaType.APPLICATION_XML.equals(variant.getMediaType(), true)) {
          xstream = (XStream) getContext().getAttributes().get(PlexusRestletApplicationBridge.XML_XSTREAM);
        }
        else {
          return null;
        }

        if (text != null) {
          CharacterSet charset = variant.getCharacterSet();
          if (charset == null) {
            charset = CharacterSet.ISO_8859_1;
          }
          if (!CharacterSet.UTF_8.equals(charset)) {
            // must fix text encoding NXCM-2494
            text = new String(new String(text.getBytes(), "UTF-8").getBytes(charset.getName()));
          }
        }

        representation = new XStreamRepresentation(xstream, text, variant.getMediaType());
        return representation;
      }
      else {
        return null;
      }
    }
    catch (IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot get the representation!", e);
    }
  }

  protected Representation serialize(Variant variant, Object payload)
      throws ResourceException
  {
    if (payload == null) {
      return null;
    }

    XStreamRepresentation result = createRepresentation(variant);

    if (result == null) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "The requested mediaType='"
          + variant.getMediaType() + "' is unsupported!");
    }

    result.setPayload(payload);

    return result;
  }

  protected Object deserialize(Object root)
      throws ResourceException
  {

    Object result = null;

    if (root != null) {

      if (String.class.isAssignableFrom(root.getClass())) {
        try {
          result = getRequest().getEntity().getText();
        }
        catch (IOException e) {
          throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot get the representation!", e);
        }
      }

      XStreamRepresentation representation = createRepresentation(getRequest().getEntity());

      if (representation != null) {
        try {
          result = representation.getPayload(root);
        }
        catch (XStreamException e) {
          logger
              .warn("Invalid XML, unable to parse using XStream {}", (delegate == null ? "" : delegate.getClass()), e);

          throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
              "Invalid XML, unable to parse using XStream", e);
        }
      }
    }
    return result;
  }

  protected Representation doRepresent(Object payload, Variant variant)
      throws ResourceException
  {
    if (Representation.class.isAssignableFrom(payload.getClass())) {
      // representation
      return (Representation) payload;
    }
    else if (InputStream.class.isAssignableFrom(payload.getClass())) {
      // inputStream
      return new InputStreamRepresentation(variant.getMediaType(), (InputStream) payload);
    }
    else if (String.class.isAssignableFrom(payload.getClass())) {
      // inputStream
      return new StringRepresentation((String) payload, variant.getMediaType());
    }
    else {
      // object, make it a representation
      return serialize(variant, payload);
    }
  }

  private Representation doRepresent(Object payload, Variant variant, Response response)
      throws ResourceException
  {
    final Representation representation = doRepresent(payload, variant);
    if (representation != null && representation instanceof RestletResponseCustomizer) {
      ((RestletResponseCustomizer) representation).customize(response);
    }
    return representation;
  }

  // == mkcol

  public boolean allowMkcol() {
    return false;
  }

  // == mkcol

  @Override
  public Representation represent(Variant variant)
      throws ResourceException
  {
    Object result;
    try {
      result = delegate.get(getContext(), getRequest(), getResponse(), variant);
    }
    catch (PlexusResourceException e) {
      // set the status
      getResponse().setStatus(e.getStatus());
      // try to get the responseObject
      result = e.getResultObject();
    }

    return (result != null) ? doRepresent(result, variant, getResponse()) : null;
  }

  @Override
  public void acceptRepresentation(Representation representation)
      throws ResourceException
  {
    if (delegate.acceptsUpload()) {
      upload(representation);
    }
    else {
      Object payloadInstance = delegate.getPayloadInstance(Method.POST);
      if (payloadInstance == null) {
        payloadInstance = delegate.getPayloadInstance();
      }
      Object payload = deserialize(payloadInstance);

      Object result = null;

      try {
        result = delegate.post(getContext(), getRequest(), getResponse(), payload);

        // This is a post, so set the status correctly
        // but only if the status was not changed to be something else, like a 202
        if (getResponse().getStatus() == Status.SUCCESS_OK) {
          getResponse().setStatus(Status.SUCCESS_CREATED);
        }
      }
      catch (PlexusResourceException e) {
        // set the status
        getResponse().setStatus(e.getStatus());
        // try to get the responseObject
        result = e.getResultObject();
      }

      if (result != null) {
        getResponse().setEntity(doRepresent(result, representation, getResponse()));
      }
    }
  }

  @Override
  public void storeRepresentation(Representation representation)
      throws ResourceException
  {
    if (delegate.acceptsUpload()) {
      upload(representation);
    }
    else {
      Object payloadInstance = delegate.getPayloadInstance(Method.PUT);
      if (payloadInstance == null) {
        payloadInstance = delegate.getPayloadInstance();
      }
      Object payload = deserialize(payloadInstance);

      Object result = null;
      try {
        result = delegate.put(getContext(), getRequest(), getResponse(), payload);

      }
      catch (PlexusResourceException e) {
        // set the status
        getResponse().setStatus(e.getStatus());
        // try to get the responseObject
        result = e.getResultObject();
      }

      if (result != null) {
        getResponse().setEntity(doRepresent(result, representation, getResponse()));
      }
    }
  }

  @Override
  public void removeRepresentations()
      throws ResourceException
  {
    delegate.delete(getContext(), getRequest(), getResponse());

    // if we have an Entity set, then return a 200 (default)
    // if not return a 204
    if (getResponse().getStatus() == Status.SUCCESS_OK && !getResponse().isEntityAvailable()) {
      getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    }
  }

  @Override
  public void handleOptions() {
  	super.handleOptions();
      try {
      	delegate.options(getContext(), getRequest(), getResponse());
      } catch (ResourceException re) {
          getResponse().setStatus(re.getStatus(), re);
      }
  }

  public void upload(Representation representation)
      throws ResourceException
  {
    Object result = null;

    List<FileItem> files = null;

    try {
      RestletFileUpload uploadRequest = new RestletFileUpload(getFileItemFactory());

      files = uploadRequest.parseRepresentation(representation);

      result = delegate.upload(getContext(), getRequest(), getResponse(), files);
    }
    catch (FileUploadException e) {
      // try to take simply the body as stream
      String name = getRequest().getResourceRef().getPath();

      if (name.contains("/")) {
        name = name.substring(name.lastIndexOf("/") + 1, name.length());
      }

      FileItem file = new FakeFileItem(name, representation);

      files = new ArrayList<FileItem>();

      files.add(file);

      result = delegate.upload(getContext(), getRequest(), getResponse(), files);
    }

    // only if the status was not changed to be something else, like a 202
    if (getResponse().getStatus() == Status.SUCCESS_OK) {
      getResponse().setStatus(Status.SUCCESS_CREATED);
    }

    if (result != null) {
      // TODO: representation cannot be returned as multipart! (representation above is possibly multipart)
      getResponse().setEntity(doRepresent(result, getPreferredVariant(), getResponse()));
    }
  }

  // ==

  private class FakeFileItem
      implements FileItem
  {
    private static final long serialVersionUID = 414885488690939983L;

    private final String name;

    private final Representation representation;

    public FakeFileItem(String name, Representation representation) {
      this.name = name;

      this.representation = representation;
    }

    @Override
    public String getContentType() {
      return representation.getMediaType().getName();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getFieldName() {
      return getName();
    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
      return representation.getStream();
    }

    // == ignored methods

    @Override
    public void delete() {
      // TODO Auto-generated method stub
    }

    @Override
    public byte[] get() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public OutputStream getOutputStream()
        throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public String getString() {
      return null;
    }

    @Override
    public String getString(String encoding)
        throws UnsupportedEncodingException
    {
      return null;
    }

    @Override
    public boolean isFormField() {
      return false;
    }

    @Override
    public boolean isInMemory() {
      return false;
    }

    @Override
    public void setFieldName(String name) {
    }

    @Override
    public void setFormField(boolean state) {
    }

    @Override
    public void write(File file)
        throws Exception
    {
    }

    @Override
    public FileItemHeaders getHeaders() {
      return null;
    }

    @Override
    public void setHeaders(final FileItemHeaders headers) {

    }
  }

}

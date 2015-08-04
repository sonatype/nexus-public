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
package org.osgi.impl.bundle.obr.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

public class ResourceImpl
    implements Resource
{
  List capabilities = new ArrayList();

  List requirements = new ArrayList();

  URL url;

  String symbolicName;

  VersionRange version;

  List categories = new ArrayList();

  long size = -1;

  String id;

  static int ID = 1;

  Map map = new HashMap();

  RepositoryImpl repository;

  String presentationName;

  File file;


  public ResourceImpl(RepositoryImpl repository, String name,
                      VersionRange version)
  {
    this.version = version;
    if (version == null) {
      this.version = new VersionRange("0");
    }
    this.symbolicName = name;
    this.repository = repository;
  }

  public ResourceImpl(RepositoryImpl repository, XmlPullParser parser)
      throws IOException, XmlPullParserException
  {
    this.repository = repository;
    parser.require(XmlPullParser.START_TAG, null, "resource");
    symbolicName = parser.getAttributeValue(null, "symbolicname");
    if (symbolicName == null) {
      symbolicName = parser.getAttributeValue(null, "name");
    }

    map.put(SYMBOLIC_NAME, symbolicName);
    presentationName = parser.getAttributeValue(null, PRESENTATION_NAME);
    if (presentationName != null) {
      map.put(PRESENTATION_NAME, presentationName);
    }
    String v = parser.getAttributeValue(null, "version");
    if (v == null) {
      setVersion(new VersionRange("0"));
    }
    else {
      setVersion(new VersionRange(v));
    }

    setURL(toURL(parser.getAttributeValue(null, "uri")));

    while (parser.nextTag() == XmlPullParser.START_TAG) {
      if (parser.getName().equals("category")) {
        categories.add(parser.getAttributeValue(null, "id").trim());
      }
      else if (parser.getName().equals("require")) {
        addRequirement(new RequirementImpl(parser));
      }
      else if (parser.getName().equals("capability")) {
        addCapability(new CapabilityImpl(parser));
      }
      else {
        String text = parser.nextText();
        if (text != null) {
          map.put(parser.getName(), text.trim());
        }
      }
      parser.next();
    }
    parser.require(XmlPullParser.END_TAG, null, "resource");
  }

  public ResourceImpl(RepositoryImpl impl) {
    this.repository = impl;
  }

  private URL toURL(String attributeValue) throws IOException {
    if (attributeValue == null) {
      return null;
    }

    return new URL(repository.getURL(), attributeValue);
  }

  public void addCategory(String category) {
    categories.add(category);
  }

  public void addCapability(CapabilityImpl capability) {
    if (capability != null) {
      capabilities.add(capability);
    }
  }

  public void addRequirement(RequirementImpl requirement) {
    if (requirement != null) {
      requirements.add(requirement);
    }
  }

  public void setLicense(URL license) {
    if (license != null) {
      map.put(LICENSE_URL, license);
    }
  }

  public String getDescription() {
    return (String) map.get(DESCRIPTION);
  }

  public void setDescription(String description) {
    if (description != null) {
      map.put(DESCRIPTION, description);
    }
  }

  public Capability[] getCapabilities() {
    return (Capability[]) capabilities.toArray(new Capability[capabilities
        .size()]);
  }

  public URL getLicense() {
    return (URL) map.get(LICENSE_URL);
  }

  public String getSymbolicName() {
    return symbolicName;
  }

  public Requirement[] getRequirements() {
    return (Requirement[]) requirements
        .toArray(new Requirement[requirements.size()]);
  }

  public Tag toXML() {
    return toXML(this);
  }

  public static interface UrlTransformer
  {
    String transform(URL url);
  }

  static final class IdentityUrlTransformer
      implements UrlTransformer
  {
    public String transform(URL url) {
      return url.toExternalForm();
    }
  }

  static final class RelativeUrlTransformer
      implements UrlTransformer
  {
    URL baseUrl;

    public RelativeUrlTransformer(URL baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String transform(URL url) {
      return makeRelative(baseUrl, url);
    }
  }

  public static Tag toXML(Resource resource) {
    return toXML(resource, true);
  }

  public static Tag toXML(Resource resource, boolean relative) {
    if (relative) {
      return toXML(resource, new RelativeUrlTransformer(resource.getRepository().getURL()));
    }
    return toXML(resource, new IdentityUrlTransformer());
  }

  public static Tag toXML(Resource resource, UrlTransformer urlTransformer) {
    Tag meta = new Tag("resource");
    meta.addAttribute("uri", urlTransformer.transform(resource.getURL()));
    meta.addAttribute(SYMBOLIC_NAME, resource.getSymbolicName());
    if (resource.getPresentationName() != null) {
      meta
          .addAttribute(PRESENTATION_NAME, resource
              .getPresentationName());
    }
    meta.addAttribute(VERSION, resource.getVersion().toString());
    meta.addAttribute("id", resource.getId());
    Map map = new TreeMap(resource.getProperties());
    for (Object key : map.keySet()) {
      if (!(key.equals(URL) || key.equals(SYMBOLIC_NAME) || key
          .equals(VERSION) || key.equals(PRESENTATION_NAME))) {
        Object value = map.get(key);
        if (value != null) {
          if (value instanceof URL) {
            value = urlTransformer.transform((URL) value);
          }
          meta.addContent(new Tag((String) key, value.toString()));
        }
      }
    }

    String[] categories = resource.getCategories();
    for (int i = 0; i < categories.length; i++) {
      String category = categories[i];
      meta.addContent(new Tag("category", new String[]{
          "id",
          category.toLowerCase()
      }));
    }

    Capability[] capabilities = resource.getCapabilities();
    for (int i = 0; i < capabilities.length; i++) {
      meta.addContent(CapabilityImpl.toXML(capabilities[i]));
    }

    Requirement[] requirements = resource.getRequirements();
    for (int i = 0; i < requirements.length; i++) {
      meta.addContent(RequirementImpl.toXML(requirements[i]));
    }
    return meta;
  }

  public URL getURL() {
    return url;
  }

  static String makeRelative(URL repository, URL url) {
    try {
      if (repository != null) {
        String a = url.toExternalForm();
        String b = repository.toExternalForm();
        int index = b.lastIndexOf('/');
        if (index > 0) {
          b = b.substring(0, index + 1);
        }
        if (a.startsWith(b)) {
          return a.substring(b.length());
        }
      }
    }
    catch (Exception e) {
      // Ignore
    }
    return url.toExternalForm();
  }

  public void setURL(URL url) {
    this.url = url;
    if (url != null) {
      map.put(URL, url);
    }
  }

  public String getCopyright() {
    return (String) map.get(COPYRIGHT);
  }

  public Version getVersion() {
    if (version == null) {
      version = new VersionRange("0");
    }
    return version.low;
  }

  void setVersion(VersionRange version) {
    if (version == null) {
      this.version = new VersionRange("0");
    }
    else {
      this.version = version;
    }
  }

  public void setCopyright(String copyright) {
    if (copyright != null) {
      map.put(COPYRIGHT, copyright);
    }
  }

  public URL getDocumentation() {
    return (URL) map.get(DOCUMENTATION_URL);
  }

  public void setDocumentation(URL documentation) {
    if (documentation != null) {
      map.put(DOCUMENTATION_URL, documentation);
    }
  }

  public URL getSource() {
    return (URL) map.get(SOURCE_URL);
  }

  public void setSource(URL source) {
    if (source != null) {
      map.put(SOURCE_URL, source);
    }
  }

  public boolean satisfies(RequirementImpl requirement) {
    for (Iterator i = capabilities.iterator(); i.hasNext(); ) {
      CapabilityImpl capability = (CapabilityImpl) i.next();
      if (requirement.isSatisfied(capability)) {
        return true;
      }
    }
    return false;
  }

  public String toString() {
    return symbolicName + "-" + version;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
    map.put(SIZE, new Long(size));
  }

  public Collection getRequirementList() {
    return requirements;
  }

  public Collection getCapabilityList() {
    return capabilities;
  }

  public int hashCode() {
    return symbolicName.hashCode() ^ version.hashCode();
  }

  public boolean equals(Object o) {
    try {
      ResourceImpl other = (ResourceImpl) o;
      return symbolicName.equals(other.symbolicName)
          && version.equals(other.version);
    }
    catch (ClassCastException e) {
      return false;
    }
  }

  public String[] getCategories() {
    return (String[]) categories.toArray(new String[categories.size()]);
  }

  public Map getProperties() {
    return Collections.unmodifiableMap(map);
  }

  public synchronized String getId() {
    if (id == null) {
      id = symbolicName + "/" + version;
    }
    return id;
  }

  public Repository getRepository() {
    return repository;
  }

  void setName(String value) {
    this.symbolicName = value;
  }

  void put(String name, Object value) {
    map.put(name, value);
  }

  public void setPresentationName(String name) {
    presentationName = name;
    if (name != null) {
      map.put(PRESENTATION_NAME, name);
    }
  }

  public String getPresentationName() {
    return presentationName;
  }

  public void setFile(File zipFile) {
    file = zipFile;
  }

  public Set getExtendList() {
    Set set = new HashSet();
    for (Iterator i = requirements.iterator(); i.hasNext(); ) {
      RequirementImpl impl = (RequirementImpl) i.next();
      if (impl.isExtend()) {
        set.add(impl);
      }
    }
    return set;
  }

}

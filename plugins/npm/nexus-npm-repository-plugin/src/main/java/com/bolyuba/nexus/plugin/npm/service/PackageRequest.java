/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.service;

import javax.annotation.Nonnull;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commonjs package request. Represents domain of valid requests including registry "special" like {@code /-/all}
 *
 * @author <a href="mailto:georgy@bolyuba.com">Georgy Bolyuba</a>
 */
public class PackageRequest
{
  private final ResourceStoreRequest storeRequest;

  private final PackageCoordinates coordinates;

  public PackageRequest(@Nonnull ResourceStoreRequest storeRequest) throws IllegalArgumentException {

    this.storeRequest = checkNotNull(storeRequest);
    this.coordinates = PackageCoordinates.coordinatesFromUrl(storeRequest.getRequestPath());
  }

  public ResourceStoreRequest getStoreRequest() {
    return storeRequest;
  }

  public boolean isRegistryRoot() {
    return PackageCoordinates.Type.REGISTRY_ROOT == coordinates.getType();
  }

  public boolean isPackageRoot() {
    return PackageCoordinates.Type.PACKAGE_ROOT == coordinates.getType();
  }

  public boolean isPackageVersion() {
    return PackageCoordinates.Type.PACKAGE_VERSION == coordinates.getType();
  }

  public boolean isRegistrySpecial() {
    return PackageCoordinates.Type.REGISTRY_SPECIAL == coordinates.getType();
  }

  public boolean isPackage() {
    return isPackageRoot() || isPackageVersion();
  }

  public boolean isMetadata() {
    return isRegistryRoot() || isPackageRoot() || isPackageVersion();
  }

  public boolean isScoped() { return !Strings.isNullOrEmpty(coordinates.getScope()); }

  public String getPath() {
    return coordinates.getPath();
  }

  public String getScope() {
    return coordinates.getScope();
  }

  public String getName() {
    return coordinates.getPackageName();
  }

  public String getVersion() {
    return coordinates.getPackageVersion();
  }

  @Override
  public String toString() {
    return "PackageRequest{" +
        "storeRequest=" + storeRequest +
        ", coordinates=" + coordinates +
        '}';
  }

  // == coordinate

  static class PackageCoordinates
  {
    static enum Type
    {
      REGISTRY_ROOT,
      PACKAGE_ROOT,
      PACKAGE_VERSION,
      REGISTRY_SPECIAL
    }

    private PackageCoordinates() {}

    private Type type;

    private String scope;

    private String packageName;

    private String packageVersion;

    private String path;

    public String getName() {
      if (Strings.isNullOrEmpty(getScope())) {
        return getPackageName();
      } else {
        return "@" + getScope() + "/" + getPackageName();
      }
    }

    public String getScope() { return scope; }

    public String getPackageName() {
      return packageName;
    }

    public String getPackageVersion() {
      return packageVersion;
    }

    public String getPath() {
      return path;
    }

    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return "PackageCoordinates{" +
          "type=" + type +
          ", scope='" + scope + '\'' +
          ", packageName='" + packageName + '\'' +
          ", packageVersion='" + packageVersion + '\'' +
          ", path='" + path + '\'' +
          '}';
    }

    public static PackageCoordinates coordinatesFromUrl(@Nonnull String requestPath) throws IllegalArgumentException {
      PackageCoordinates coordinates = new PackageCoordinates();
      coordinates.path = requestPath;

      if (RepositoryItemUid.PATH_SEPARATOR.equals(requestPath)) {
        coordinates.type = Type.REGISTRY_ROOT;
        return coordinates;
      }

      if (requestPath.startsWith(
          RepositoryItemUid.PATH_SEPARATOR + NpmRepository.NPM_REGISTRY_SPECIAL + RepositoryItemUid.PATH_SEPARATOR)) {
        coordinates.type = Type.REGISTRY_SPECIAL;
        return coordinates;
      }

      String correctedPath =
          requestPath.startsWith(RepositoryItemUid.PATH_SEPARATOR) ?
              requestPath.substring(1, requestPath.length()) :
              requestPath;
      String[] explodedPath = correctedPath.split(RepositoryItemUid.PATH_SEPARATOR);

      if (explodedPath.length == 2) {
        if (explodedPath[0].startsWith("@")) {
          coordinates.type = Type.PACKAGE_ROOT;
          coordinates.scope = validate(explodedPath[0].substring(1), "Invalid package scope: ");
          coordinates.packageName = "@" + coordinates.scope + "/" + validate(explodedPath[1], "Invalid package name: ");
          return coordinates;
        } else {
          coordinates.type = Type.PACKAGE_VERSION;
          coordinates.packageName = validate(explodedPath[0], "Invalid package name: ");
          coordinates.packageVersion = validate(explodedPath[1], "Invalid package version: ");
          return coordinates;
        }
      }
      if (explodedPath.length == 1) {
        coordinates.type = Type.PACKAGE_ROOT;
        coordinates.packageName = validate(explodedPath[0], "Invalid package name: ");
        return coordinates;
      }

      throw new IllegalArgumentException("Path " + requestPath + " cannot be turned into PackageCoordinates");
    }

    /**
     * See http://wiki.commonjs.org/wiki/Packages/Registry#Changes_to_Packages_Spec
     */
    private static String validate(@Nonnull String nameOrVersion, String errorPrefix)
        throws IllegalArgumentException
    {
      if (nameOrVersion.startsWith(NpmRepository.NPM_REGISTRY_SPECIAL)) {
        throw new IllegalArgumentException(errorPrefix + nameOrVersion);
      }
      if (nameOrVersion.equals(".")) {
        throw new IllegalArgumentException(errorPrefix + nameOrVersion);
      }
      if (nameOrVersion.equals("..")) {
        throw new IllegalArgumentException(errorPrefix + nameOrVersion);
      }
      return nameOrVersion;
    }
  }
}

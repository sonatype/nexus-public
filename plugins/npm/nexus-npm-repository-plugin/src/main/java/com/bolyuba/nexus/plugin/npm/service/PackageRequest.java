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
import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
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

  public PackageCoordinates getCoordinates() {
    return coordinates;
  }

  public boolean isRegistryRoot() {
    return PackageCoordinates.Type.REGISTRY_ROOT == getCoordinates().getType();
  }

  public boolean isPackageRoot() {
    return PackageCoordinates.Type.PACKAGE_ROOT == getCoordinates().getType();
  }

  public boolean isPackageVersion() {
    return PackageCoordinates.Type.PACKAGE_VERSION == getCoordinates().getType();
  }

  public boolean isRegistrySpecial() {
    return PackageCoordinates.Type.REGISTRY_SPECIAL == getCoordinates().getType();
  }

  public boolean isPackage() {
    return isPackageRoot() || isPackageVersion();
  }

  public boolean isMetadata() {
    return isRegistryRoot() || isPackageRoot() || isPackageVersion();
  }

  @Override
  public String toString() {
    return "PackageRequest{" +
        "storeRequest=" + storeRequest +
        ", coordinates=" + coordinates +
        '}';
  }

  // == coordinate

  public static class PackageCoordinates
  {
    enum Type
    {
      REGISTRY_ROOT, // '/'
      PACKAGE_ROOT, // '/$name' OR '/@$scope/$name'
      PACKAGE_VERSION, // /$name/$version' OR '/$scope/$name/$version'
      REGISTRY_SPECIAL // prefix of '/-/'
    }

    public PackageCoordinates(final Type type,
                              final String path,
                              @Nullable final String scope,
                              @Nullable final String name,
                              @Nullable final String version) {
      this.type = checkNotNull(type);
      this.path = checkNotNull(path);
      this.scope = scope;
      this.name = name;
      this.version = version;
    }

    private final Type type;

    private final String path;

    private final String scope;

    private final String name;

    private final String version;

    public boolean isScoped() { return !Strings.isNullOrEmpty(getScope()); }

    /**
     * Returns the full package name in form of {@code @scope/name} or just {@code name} if not scoped. Returns
     * {@code null} if this coordinate is not about package or package version (which implies package name).
     */
    @Nullable
    public String getPackageName() {
      if (Strings.isNullOrEmpty(getScope())) {
        return getName();
      } else {
        return "@" + getScope() + "/" + getName();
      }
    }

    /**
     * Returns the scope or {@code null} if unscoped or not a package request.
     */
    @Nullable
    public String getScope() { return scope; }

    /**
     * Returns the name (without scope if scoped) or {@code null} if not a package request.
     */
    @Nullable
    public String getName() {
      return name;
    }

    /**
     * Returns the version or {@code null} if not a package version request.
     */
    @Nullable
    public String getVersion() {
      return version;
    }

    /**
     * Returns the original request path used to create this instance, never {@code null}.
     */
    public String getPath() {
      return path;
    }

    /**
     * Returns the coordinate {@link Type} of this instance, never {@code null}.
     */
    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return "PackageCoordinates{" +
          "type=" + type +
          ", scope='" + scope + '\'' +
          ", name='" + name + '\'' +
          ", version='" + version + '\'' +
          ", path='" + path + '\'' +
          '}';
    }

    public static PackageCoordinates coordinatesFromUrl(@Nonnull String requestPath) throws IllegalArgumentException {
      checkNotNull(requestPath);
      if (RepositoryItemUid.PATH_SEPARATOR.equals(requestPath)) {
        return new PackageCoordinates(Type.REGISTRY_ROOT, requestPath, null, null, null);
      }

      if (requestPath.startsWith(RepositoryItemUid.PATH_SEPARATOR + NpmRepository.NPM_REGISTRY_SPECIAL + RepositoryItemUid.PATH_SEPARATOR)) {
        return new PackageCoordinates(Type.REGISTRY_SPECIAL, requestPath, null, null, null);
      }

      final String correctedPath =
          requestPath.startsWith(RepositoryItemUid.PATH_SEPARATOR) ?
              requestPath.substring(1, requestPath.length()) :
              requestPath;
      final String[] explodedPath = correctedPath.split(RepositoryItemUid.PATH_SEPARATOR);

      if (explodedPath[0].startsWith("@")) {
        // scoped: explodedPath.length must be at least 2 but max 3 (@$scope/$name OR @$scope/$name/$version)
        checkArgument(explodedPath.length >= 2 && explodedPath.length <= 3, "Invalid path: %s", correctedPath);
        if (explodedPath.length == 3) {
          return new PackageCoordinates(
                  Type.PACKAGE_VERSION,
                  requestPath,
                  validate(explodedPath[0].substring(1), "Invalid package scope: "),
                  validate(explodedPath[1], "Invalid package name: "),
                  validate(explodedPath[2], "Invalid package version: ")
          );
        } else {
          return new PackageCoordinates(
                  Type.PACKAGE_ROOT,
                  requestPath,
                  validate(explodedPath[0].substring(1), "Invalid package scope: "),
                  validate(explodedPath[1], "Invalid package name: "),
                  null
          );
        }
      }
      else {
        // un-scoped: explodedPath.length might be 1 or 2 ($name OR $name/$version)
        checkArgument(explodedPath.length >= 1 && explodedPath.length <= 2, "Invalid path: %s", correctedPath);
        if (explodedPath.length == 2) {
          return new PackageCoordinates(
                  Type.PACKAGE_VERSION,
                  requestPath,
                  null,
                  validate(explodedPath[0], "Invalid package name: "),
                  validate(explodedPath[1], "Invalid package version: ")
          );
        } else {
          return new PackageCoordinates(
                  Type.PACKAGE_ROOT,
                  requestPath,
                  null,
                  validate(explodedPath[0], "Invalid package name: "),
                  null
          );
        }
      }
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

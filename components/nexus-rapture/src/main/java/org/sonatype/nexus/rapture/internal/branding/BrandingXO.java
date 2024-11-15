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
package org.sonatype.nexus.rapture.internal.branding;

/**
 * Branding exchange object.
 *
 * @since 3.0
 */
public class BrandingXO
{
  private boolean headerEnabled;

  private String headerHtml;

  private boolean footerEnabled;

  private String footerHtml;

  public BrandingXO() {
  }

  public BrandingXO(
      final boolean headerEnabled,
      final String headerHtml,
      final boolean footerEnabled,
      final String footerHtml)
  {
    this.headerEnabled = headerEnabled;
    this.headerHtml = headerHtml;
    this.footerEnabled = footerEnabled;
    this.footerHtml = footerHtml;
  }

  public boolean isHeaderEnabled() {
    return headerEnabled;
  }

  public void setHeaderEnabled(boolean headerEnabled) {
    this.headerEnabled = headerEnabled;
  }

  public String getHeaderHtml() {
    return headerHtml;
  }

  public void setHeaderHtml(String headerHtml) {
    this.headerHtml = headerHtml;
  }

  public boolean isFooterEnabled() {
    return footerEnabled;
  }

  public void setFooterEnabled(Boolean footerEnabled) {
    this.footerEnabled = footerEnabled;
  }

  public String getFooterHtml() {
    return footerHtml;
  }

  public void setFooterHtml(String footerHtml) {
    this.footerHtml = footerHtml;
  }

  @Override
  public String toString() {
    return "BrandingXO{" +
        "headerEnabled=" + headerEnabled +
        ", headerHtml='" + headerHtml + '\'' +
        ", footerEnabled=" + footerEnabled +
        ", footerHtml='" + footerHtml + '\'' +
        '}';
  }
}

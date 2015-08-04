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
/*
 * $Id$
 */

package org.sonatype.nexus.client.internal.msg;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * COPIED FROM plexus-restlet-bridge to cease the dependency on it (as it would pull in Restlet and many other
 * dependencies).
 * <p/>
 * Class ErrorResponse.
 *
 * @since 2.1
 */
public class ErrorResponse
    implements java.io.Serializable
{

  //--------------------------/
  //- Class/Member Variables -/
  //--------------------------/

  /**
   * Field errors.
   */
  private java.util.List<ErrorMessage> errors;

  //-----------/
  //- Methods -/
  //-----------/

  /**
   * Method addError.
   */
  public void addError(ErrorMessage errorMessage) {
    if (!(errorMessage instanceof ErrorMessage)) {
      throw new ClassCastException(
          "ErrorResponse.addErrors(ErrorResponse) parameter must be instanceof " + ErrorMessage.class.getName());
    }
    getErrors().add(errorMessage);
  } //-- void addError(ErrorMessage)

  /**
   * Method getErrors.
   *
   * @return java.util.List
   */
  public java.util.List<ErrorMessage> getErrors() {
    if (this.errors == null) {
      this.errors = new java.util.ArrayList<ErrorMessage>();
    }

    return this.errors;
  } //-- java.util.List getErrors()

  /**
   * Method removeError.
   */
  public void removeError(ErrorMessage errorMessage) {
    if (!(errorMessage instanceof ErrorMessage)) {
      throw new ClassCastException("ErrorResponse.removeErrors(errorMessage) parameter must be instanceof "
          + ErrorMessage.class.getName());
    }
    getErrors().remove(errorMessage);
  } //-- void removeError(NexusError)

  /**
   * Set the errors field.
   */
  public void setErrors(java.util.List<ErrorMessage> errors) {
    this.errors = errors;
  } //-- void setErrors(java.util.List)

  //
  //    private String modelEncoding = "UTF-8";
  //
  //    /**
  //     * Set an encoding used for reading/writing the model.
  //     *
  //     * @param modelEncoding the encoding used when reading/writing the model.
  //     */
  //    public void setModelEncoding( String modelEncoding )
  //    {
  //        this.modelEncoding = modelEncoding;
  //    }
  //
  //    /**
  //     * @return the current encoding used when reading/writing this model.
  //     */
  //    public String getModelEncoding()
  //    {
  //        return modelEncoding;
  //    }
}

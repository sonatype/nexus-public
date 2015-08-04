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
package org.sonatype.plexus.rest.resource.error;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class ErrorResponse.
 *
 * @version $Revision$ $Date$
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
  private java.util.List errors;


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
  public java.util.List getErrors() {
    if (this.errors == null) {
      this.errors = new java.util.ArrayList();
    }

    return this.errors;
  } //-- java.util.List getErrors()

  /**
   * Method removeError.
   */
  public void removeError(ErrorMessage errorMessage) {
    if (!(errorMessage instanceof ErrorMessage)) {
      throw new ClassCastException(
          "ErrorResponse.removeErrors(errorMessage) parameter must be instanceof " + ErrorMessage.class.getName());
    }
    getErrors().remove(errorMessage);
  } //-- void removeError(NexusError)

  /**
   * Set the errors field.
   */
  public void setErrors(java.util.List errors) {
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

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
/**
 * 
 */
package config.i18n.client;

import java.util.Date;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author Luis Faria
 * 
 */
public interface BrowseMessages extends Messages {

  /*********************************************************/
  /******************* OLD MESSAGES ***********************/
  /*********************************************************/

  // Tree
  @DefaultMessage("See {0}-{1}")
  public String previousItems(int from, int to);

  @DefaultMessage("See {0}-{1} (total {2})")
  public String nextItems(int from, int to, int total);

  // Item Popup
  @DefaultMessage("To lock wait {0} sec.")
  public String waitToLock(int sec);

  @DefaultMessage("Click here to close")
  public String close();

  // Browse
  @DefaultMessage("{0} fonds")
  public String totalFondsNumber(int count);

  @DefaultMessage("There is no such element in the repository identified by{0}.")
  public String noSuchRODAObject(String pid);

  // Edit
  @DefaultMessage("Unable to save the changes. Details: {0}")
  public String editSaveError(String message);

  @DefaultMessage("Unable to move the element because the levels of description are not appropriate. Details: {0}")
  public String moveIllegalOperation(String message);

  @DefaultMessage("Unable to move the element because it or the destination were not found in the repository. Details: {0}")
  public String moveNoSuchObject(String message);

  // Representations Panel
  @DefaultMessage("Disseminations of {0} - ''{1}''")
  public String representationsTitle(String id, String title);

  @DefaultMessage("{0} does not have associated representations")
  public String noRepresentationsTitle(String id);

  @DefaultMessage("Download representation with format {0}, {1} files, {2} bytes uncompressed")
  public String representationDownloadTooltip(String format, int numberOfFiles, long sizeOfFiles);

  // Preservation Metadata Panel
  @DefaultMessage("{0} (original)")
  public String preservationRepOriginal(String format);

  @DefaultMessage("{0} (normalized)")
  public String preservationRepNormalized(String format);

  @DefaultMessage("{0}")
  public String preservationRepAlternative(String format);

  @DefaultMessage("{0} files, {1} bytes")
  public String preservationRepTooltip(int numberOfFiles, long sizeOfFiles);

  /*********************************************************/

  /******************* NEW MESSAGES ***********************/
  /*********************************************************/

  @DefaultMessage("Error in line {0}, column {1}: {2}")
  SafeHtml metadataParseError(int line, int column, String message);

  @DefaultMessage("Error")
  SafeHtml notFoundErrorTitle();

  @DefaultMessage("Item with id {0} could not be found.")
  SafeHtml notFoundErrorMessage(String id);

  @DefaultMessage("Error")
  SafeHtml genericErrorTitle();

  @DefaultMessage("An unexpected error occurred when retrieving item. <pre><code>{0}</code></pre>")
  SafeHtml genericErrorMessage(String message);

  @DefaultMessage("Error transforming descriptive metadata into HTML")
  SafeHtml descriptiveMetadataTranformToHTMLError();

  @DefaultMessage("Error transforming preservation metadata into HTML")
  SafeHtml preservationMetadataTranformToHTMLError();

  @DefaultMessage("All collections")
  String allCollectionsTitle();

  @DefaultMessage("Error loading descriptive metadata: {0}")
  String errorLoadingDescriptiveMetadata(String message);

  @DefaultMessage("Error loading preservation metadata: {0}")
  String errorLoadingPreservationMetadata(String message);

  @DefaultMessage("download")
  SafeHtml download();

  @DefaultMessage("PREMIS")
  String premisTitle();

  @DefaultMessage("{0,number} files, {1}")
  @AlternateMessage({"one", "One file, {1}"})
  String downloadRepresentationInfo(@PluralCount int numberOfFiles, String readableFileSize);

  @DefaultMessage("Original and normalized document")
  SafeHtml downloadTitleOriginalAndNormalized();

  @DefaultMessage("Original document")
  SafeHtml downloadTitleOriginal();

  @DefaultMessage("Normalized document")
  SafeHtml downloadTitleNormalized();

  @DefaultMessage("Document")
  SafeHtml downloadTitleDefault();

  @DefaultMessage("")
  String titleDatesEmpty();

  @DefaultMessage("From {0,localdatetime,predef:DATE_MEDIUM}")
  String titleDatesNoFinal(Date dateInitial);

  @DefaultMessage("Up to {0,localdatetime,predef:DATE_MEDIUM}")
  String titleDatesNoInitial(Date dateFinal);

  @DefaultMessage("From {0,localdatetime,predef:DATE_MEDIUM} to {1,localdatetime,predef:DATE_MEDIUM}")
  String titleDates(Date dateInitial, Date dateFinal);

  @DefaultMessage("Created at {0,localdatetime,predef:DATE_TIME_MEDIUM}, with {1}, from {2}")
  public String ingestTransferItemInfo(Date creationDate, String readableFileSize, String owner);
  
  

}

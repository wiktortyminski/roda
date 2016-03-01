/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.plugins.ingest.characterization;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.roda.core.common.PremisV3Utils;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata.PreservationMetadataType;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.model.ModelService;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PremisSkeletonPluginUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PremisSkeletonPlugin.class);

  public static void createPremisSkeletonOnRepresentation(ModelService model, StorageService storage, AIP aip,
    String representationId) throws IOException, RequestNotValidException, GenericException,
      NotFoundException, AuthorizationDeniedException, XmlException, ValidationException, AlreadyExistsException {

    gov.loc.premis.v3.Representation representation = PremisV3Utils.createBaseRepresentation(aip.getId(), representationId);
    boolean notifyInSteps = false;

    boolean recursive = true;
    CloseableIterable<File> allFiles = model.listFilesUnder(aip.getId(), representationId, recursive);
    for (File file : allFiles) {
      if (!file.isDirectory()) {
        LOGGER.debug("Processing " + file);
        ContentPayload filePreservation = PremisV3Utils.createBaseFile(file, model);
        model.createPreservationMetadata(PreservationMetadataType.OBJECT_FILE, aip.getId(), representationId,
          file.getPath(), file.getId(), filePreservation, notifyInSteps);
        PremisV3Utils.linkFileToRepresentation(file, RodaConstants.PREMIS_RELATIONSHIP_TYPE_STRUCTURAL,
          RodaConstants.PREMIS_RELATIONSHIP_SUBTYPE_HASPART, representation);
      }
    }
    IOUtils.closeQuietly(allFiles);

    ContentPayload representationPayload = PremisV3Utils.representationToBinary(representation);
    model.createPreservationMetadata(PreservationMetadataType.OBJECT_REPRESENTATION, representationId, aip.getId(),
      representationId, representationPayload, notifyInSteps);
  }

}

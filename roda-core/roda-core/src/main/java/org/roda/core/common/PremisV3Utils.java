/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.common;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationAgentType;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.metadata.Fixity;
import org.roda.core.data.v2.ip.metadata.LinkingIdentifier;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata.PreservationMetadataType;
import org.roda.core.data.v2.user.RODAMember;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.Plugin;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.util.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import gov.loc.premis.v3.AgentComplexType;
import gov.loc.premis.v3.AgentDocument;
import gov.loc.premis.v3.AgentIdentifierComplexType;
import gov.loc.premis.v3.ContentLocationComplexType;
import gov.loc.premis.v3.CreatingApplicationComplexType;
import gov.loc.premis.v3.EventComplexType;
import gov.loc.premis.v3.EventDetailInformationComplexType;
import gov.loc.premis.v3.EventDocument;
import gov.loc.premis.v3.EventIdentifierComplexType;
import gov.loc.premis.v3.EventOutcomeDetailComplexType;
import gov.loc.premis.v3.EventOutcomeInformationComplexType;
import gov.loc.premis.v3.FixityComplexType;
import gov.loc.premis.v3.FormatComplexType;
import gov.loc.premis.v3.FormatDesignationComplexType;
import gov.loc.premis.v3.FormatRegistryComplexType;
import gov.loc.premis.v3.LinkingAgentIdentifierComplexType;
import gov.loc.premis.v3.LinkingObjectIdentifierComplexType;
import gov.loc.premis.v3.ObjectCharacteristicsComplexType;
import gov.loc.premis.v3.ObjectComplexType;
import gov.loc.premis.v3.ObjectDocument;
import gov.loc.premis.v3.ObjectIdentifierComplexType;
import gov.loc.premis.v3.RelatedObjectIdentifierComplexType;
import gov.loc.premis.v3.RelationshipComplexType;
import gov.loc.premis.v3.Representation;
import gov.loc.premis.v3.StorageComplexType;
import gov.loc.premis.v3.StringPlusAuthority;

public class PremisV3Utils {
  private static final Logger LOGGER = LoggerFactory.getLogger(PremisV3Utils.class);

  private static final Set<String> MANDATORY_CHECKSUM_ALGORITHMS = new HashSet<>(Arrays.asList("SHA-256"));
  private static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

  public static Fixity calculateFixity(Binary binary, String digestAlgorithm, String originator)
    throws IOException, NoSuchAlgorithmException {
    InputStream dsInputStream = binary.getContent().createInputStream();
    Fixity fixity = new Fixity(digestAlgorithm, FileUtility.calculateChecksumInHex(dsInputStream, digestAlgorithm),
      originator);
    dsInputStream.close();
    return fixity;
  }

  public static boolean isPremisV2(Binary binary) throws IOException, SAXException {
    boolean premisV2 = true;
    InputStream inputStream = binary.getContent().createInputStream();
    InputStream schemaStream = RodaCoreFactory.getConfigurationFileAsStream("schemas/premis-v2-0.xsd");
    Source xmlFile = new StreamSource(inputStream);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
    Validator validator = schema.newValidator();
    RodaErrorHandler errorHandler = new RodaErrorHandler();
    validator.setErrorHandler(errorHandler);
    try {
      validator.validate(xmlFile);
      List<SAXParseException> errors = errorHandler.getErrors();
      if (!errors.isEmpty()) {
        premisV2 = false;
      }
    } catch (SAXException e) {
      premisV2 = false;
    }
    IOUtils.closeQuietly(inputStream);
    IOUtils.closeQuietly(schemaStream);
    return premisV2;
  }

  public static Binary updatePremisToV3IfNeeded(Binary binary) throws IOException, SAXException, TransformerException,
    RequestNotValidException, NotFoundException, GenericException {
    if (isPremisV2(binary)) {
      LOGGER.debug("Binary {} is Premis V2... Needs updated...", binary.getStoragePath());
      return updatePremisV2toV3(binary);
    } else {
      return binary;
    }

  }

  private static Binary updatePremisV2toV3(Binary binary)
    throws IOException, TransformerException, RequestNotValidException, NotFoundException, GenericException {
    InputStream transformerStream = null;
    InputStream bais = null;

    Reader reader = null;
    try {
      reader = new InputStreamReader(binary.getContent().createInputStream());
      Map<String, Object> stylesheetOpt = new HashMap<String, Object>();
      transformerStream = RodaCoreFactory.getConfigurationFileAsStream("crosswalks/migration/v2Tov3.xslt");
      Reader xsltReader = new InputStreamReader(transformerStream);
      CharArrayWriter transformerResult = new CharArrayWriter();
      RodaUtils.applyStylesheet(xsltReader, reader, stylesheetOpt, transformerResult);
      Path p = Files.createTempFile("preservation", ".tmp");
      bais = new ByteArrayInputStream(transformerResult.toString().getBytes("UTF-8"));
      Files.copy(bais, p, StandardCopyOption.REPLACE_EXISTING);

      return (Binary) FSUtils.convertPathToResource(p.getParent(), p);
    } finally {
      IOUtils.closeQuietly(transformerStream);
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(reader);
    }
  }

  private static class RodaErrorHandler extends DefaultHandler {
    List<SAXParseException> errors;

    public RodaErrorHandler() {
      errors = new ArrayList<SAXParseException>();
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public List<SAXParseException> getErrors() {
      return errors;
    }

    public void setErrors(List<SAXParseException> errors) {
      this.errors = errors;
    }

  }

  public static void updateFileFormat(gov.loc.premis.v3.File file, String formatDesignationName,
    String formatDesignationVersion, String pronom, String mimeType) {

    if (StringUtils.isNotBlank(formatDesignationName)) {
      FormatDesignationComplexType fdct = getFormatDesignation(file);
      fdct.setFormatName(getStringPlusAuthority(formatDesignationName));
    }

    if (StringUtils.isNotBlank(formatDesignationVersion)) {
      FormatDesignationComplexType fdct = getFormatDesignation(file);
      fdct.setFormatVersion(formatDesignationVersion);
    }

    if (StringUtils.isNotBlank(pronom)) {
      FormatRegistryComplexType frct = getFormatRegistry(file, RodaConstants.PRESERVATION_REGISTRY_PRONOM);
      frct.setFormatRegistryKey(getStringPlusAuthority(pronom));
    }
    if (StringUtils.isNotBlank(mimeType)) {
      FormatRegistryComplexType frct = getFormatRegistry(file, RodaConstants.PRESERVATION_REGISTRY_MIME);
      frct.setFormatRegistryKey(getStringPlusAuthority(mimeType));
    }

  }

  public static void updateCreatingApplication(gov.loc.premis.v3.File file, String creatingApplicationName,
    String creatingApplicationVersion, String dateCreatedByApplication) {
    if (StringUtils.isNotBlank(creatingApplicationName)) {
      CreatingApplicationComplexType cact = getCreatingApplication(file);
      cact.setCreatingApplicationName(getStringPlusAuthority(creatingApplicationName));
    }

    if (StringUtils.isNotBlank(creatingApplicationVersion)) {
      CreatingApplicationComplexType cact = getCreatingApplication(file);
      cact.setCreatingApplicationVersion(creatingApplicationVersion);
    }

    if (StringUtils.isNotBlank(dateCreatedByApplication)) {
      CreatingApplicationComplexType cact = getCreatingApplication(file);
      cact.setDateCreatedByApplication(dateCreatedByApplication);
    }
  }

  private static CreatingApplicationComplexType getCreatingApplication(gov.loc.premis.v3.File f) {
    ObjectCharacteristicsComplexType occt;
    CreatingApplicationComplexType cact;
    if (f.getObjectCharacteristicsArray() != null && f.getObjectCharacteristicsArray().length > 0) {
      occt = f.getObjectCharacteristicsArray(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getCreatingApplicationArray() != null && occt.getCreatingApplicationArray().length > 0) {
      cact = occt.getCreatingApplicationArray(0);
    } else {
      cact = occt.addNewCreatingApplication();
    }
    return cact;
  }

  public static FormatRegistryComplexType getFormatRegistry(gov.loc.premis.v3.File f, String registryName) {
    ObjectCharacteristicsComplexType occt;
    FormatRegistryComplexType frct = null;
    if (f.getObjectCharacteristicsArray() != null && f.getObjectCharacteristicsArray().length > 0) {
      occt = f.getObjectCharacteristicsArray(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getFormatArray() != null && occt.getFormatArray().length > 0) {
      for (FormatComplexType fct : occt.getFormatArray()) {
        if (fct.getFormatRegistry() != null
          && fct.getFormatRegistry().getFormatRegistryName().getStringValue().equalsIgnoreCase(registryName)) {
          frct = fct.getFormatRegistry();
          break;
        }
      }
      if (frct == null) {
        FormatComplexType fct = occt.addNewFormat();
        frct = fct.addNewFormatRegistry();
        frct.setFormatRegistryName(getStringPlusAuthority(registryName));
      }
    } else {
      FormatComplexType fct = occt.addNewFormat();
      frct = fct.addNewFormatRegistry();
      frct.setFormatRegistryName(getStringPlusAuthority(registryName));
    }
    return frct;
  }

  private static FormatDesignationComplexType getFormatDesignation(gov.loc.premis.v3.File f) {
    ObjectCharacteristicsComplexType occt;
    FormatComplexType fct;
    FormatDesignationComplexType fdct;
    if (f.getObjectCharacteristicsArray() != null && f.getObjectCharacteristicsArray().length > 0) {
      occt = f.getObjectCharacteristicsArray(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getFormatArray() != null && occt.getFormatArray().length > 0) {
      fct = occt.getFormatArray(0);
    } else {
      fct = occt.addNewFormat();
    }
    if (fct.getFormatDesignation() != null) {
      fdct = fct.getFormatDesignation();
    } else {
      fdct = fct.addNewFormatDesignation();
    }
    return fdct;
  }

  public static ContentPayload createPremisEventBinary(String eventID, Date date, String type, String details,
    List<LinkingIdentifier> sources, List<LinkingIdentifier> outcomes, String outcome, String detailNote,
    String detailExtension, List<String> agentIds) throws GenericException, ValidationException {
    EventDocument event = EventDocument.Factory.newInstance();
    EventComplexType ect = event.addNewEvent();
    EventIdentifierComplexType eict = ect.addNewEventIdentifier();
    eict.setEventIdentifierValue(eventID);
    eict.setEventIdentifierType(getStringPlusAuthority("local"));
    ect.setEventDateTime(DateParser.getIsoDate(date));
    ect.setEventType(getStringPlusAuthority(type));
    EventDetailInformationComplexType edict = ect.addNewEventDetailInformation();
    edict.setEventDetail(details);
    if (sources != null) {
      for (LinkingIdentifier identifier : sources) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(identifier.getValue());
        loict.setLinkingObjectIdentifierType(getStringPlusAuthority(identifier.getType()));
        if (identifier.getRoles() != null) {
          loict.setLinkingObjectRoleArray(getStringPlusAuthorityArray(identifier.getRoles()));
        }
      }
    }

    if (outcomes != null) {
      for (LinkingIdentifier identifier : outcomes) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(identifier.getValue());
        loict.setLinkingObjectIdentifierType(getStringPlusAuthority(identifier.getType()));
        if (identifier.getRoles() != null) {
          loict.setLinkingObjectRoleArray(getStringPlusAuthorityArray(identifier.getRoles()));
        }
      }
    }

    if (agentIds != null) {
      for (String agentId : agentIds) {
        LinkingAgentIdentifierComplexType agentIdentifier = ect.addNewLinkingAgentIdentifier();
        // FIXME lfaria 20160523: put agent identifier type in constant
        agentIdentifier.setLinkingAgentIdentifierType(getStringPlusAuthority("local"));
        agentIdentifier.setLinkingAgentIdentifierValue(agentId);
      }
    }
    EventOutcomeInformationComplexType outcomeInformation = ect.addNewEventOutcomeInformation();
    outcomeInformation.setEventOutcome(getStringPlusAuthority(outcome));
    StringBuilder outcomeDetailNote = new StringBuilder(detailNote);
    if (StringUtils.isNotBlank(detailExtension)) {
      outcomeDetailNote.append("\n").append(detailExtension);
    }
    EventOutcomeDetailComplexType eodct = outcomeInformation.addNewEventOutcomeDetail();
    eodct.setEventOutcomeDetailNote(outcomeDetailNote.toString());

    return MetadataUtils.saveToContentPayload(event, true);

  }

  public static ContentPayload createPremisAgentBinary(String id, String name, PreservationAgentType type,
    String extension, String note, String version) throws GenericException, ValidationException {
    AgentDocument agent = AgentDocument.Factory.newInstance();

    AgentComplexType act = agent.addNewAgent();
    AgentIdentifierComplexType agentIdentifier = act.addNewAgentIdentifier();
    agentIdentifier.setAgentIdentifierType(getStringPlusAuthority("local"));
    agentIdentifier.setAgentIdentifierValue(id);

    act.setAgentType(getStringPlusAuthority(type.toString()));

    if (StringUtils.isNotBlank(name)) {
      act.addNewAgentName().setStringValue(name);
    }

    if (StringUtils.isNotBlank(note)) {
      act.addAgentNote(note);
    }

    if (StringUtils.isNotBlank(version)) {
      act.setAgentVersion(version);
    }
    if (StringUtils.isNotBlank(extension)) {
      try {
        act.addNewAgentExtension().set(XmlObject.Factory.parse(extension));
      } catch (XmlException e) {
        // e.getError()
        // TODO convert XmlException to a Valiation Exception in
        // MetadataUtils
        throw new ValidationException(e.getMessage());
      }
    }

    return MetadataUtils.saveToContentPayload(agent, true);
  }

  public static Representation createBaseRepresentation(String aipID, String representationId)
    throws GenericException, ValidationException {

    Representation representation = Representation.Factory.newInstance();
    ObjectIdentifierComplexType oict = representation.addNewObjectIdentifier();
    oict.setObjectIdentifierType(getStringPlusAuthority("local"));
    String identifier = IdUtils.getPreservationMetadataId(PreservationMetadataType.OBJECT_REPRESENTATION, aipID,
      representationId);
    oict.setObjectIdentifierValue(identifier);
    representation.addNewPreservationLevel().setPreservationLevelValue(getStringPlusAuthority(""));

    return representation;
  }

  public static ContentPayload createBaseFile(File originalFile, ModelService model) throws GenericException,
    RequestNotValidException, NotFoundException, AuthorizationDeniedException, ValidationException, XmlException {
    ObjectDocument document = ObjectDocument.Factory.newInstance();
    gov.loc.premis.v3.File file = gov.loc.premis.v3.File.Factory.newInstance();
    file.addNewPreservationLevel()
      .setPreservationLevelValue(getStringPlusAuthority(RodaConstants.PRESERVATION_LEVEL_FULL));
    ObjectIdentifierComplexType oict = file.addNewObjectIdentifier();
    String identifier = IdUtils.getFileId(originalFile.getAipId(), originalFile.getRepresentationId(),
      originalFile.getPath(), originalFile.getId());
    oict.setObjectIdentifierValue(identifier);
    oict.setObjectIdentifierType(getStringPlusAuthority("local"));
    ObjectCharacteristicsComplexType occt = file.addNewObjectCharacteristics();
    // TODO
    // occt.setCompositionLevel(CompositionLevelComplexType.Factory.parse("0"));
    FormatComplexType fct = occt.addNewFormat();
    FormatDesignationComplexType fdct = fct.addNewFormatDesignation();
    fdct.setFormatName(getStringPlusAuthority(""));
    fdct.setFormatVersion("");
    Binary binary = model.getStorage().getBinary(ModelUtils.getFileStoragePath(originalFile));

    if (binary.getContentDigest() != null && !binary.getContentDigest().isEmpty()) {
      // TODO use binary content digest information
    } else {
      // if binary does not contain digest, create a new one
      try {
        for (String algorithm : MANDATORY_CHECKSUM_ALGORITHMS) {
          // TODO set better originator
          Fixity fixity = calculateFixity(binary, algorithm, "RODA");
          FixityComplexType premis_fixity = occt.addNewFixity();
          premis_fixity.setMessageDigest(fixity.getMessageDigest());
          premis_fixity.setMessageDigestAlgorithm(getStringPlusAuthority(fixity.getMessageDigestAlgorithm()));
          premis_fixity.setMessageDigestOriginator(getStringPlusAuthority(fixity.getMessageDigestOriginator()));
        }
      } catch (IOException | NoSuchAlgorithmException e) {
        LOGGER.warn("Could not calculate fixity for file " + originalFile);
      }
    }

    occt.setSize(binary.getSizeInBytes());
    // occt.addNewObjectCharacteristicsExtension().set("");
    file.addNewOriginalName().setStringValue(originalFile.getId());
    StorageComplexType sct = file.addNewStorage();
    ContentLocationComplexType clct = sct.addNewContentLocation();
    clct.setContentLocationType(getStringPlusAuthority(""));
    clct.setContentLocationValue("");

    document.setObject(file);

    return MetadataUtils.saveToContentPayload(document, true);
  }

  public static List<Fixity> extractFixities(Binary premisFile) throws GenericException, XmlException, IOException {
    List<Fixity> fixities = new ArrayList<Fixity>();
    InputStream inputStream = premisFile.getContent().createInputStream();
    gov.loc.premis.v3.File f = binaryToFile(inputStream);
    if (f.getObjectCharacteristicsArray() != null && f.getObjectCharacteristicsArray().length > 0) {
      ObjectCharacteristicsComplexType occt = f.getObjectCharacteristicsArray(0);
      if (occt.getFixityArray() != null && occt.getFixityArray().length > 0) {
        for (FixityComplexType fct : occt.getFixityArray()) {
          Fixity fix = new Fixity();
          fix.setMessageDigest(fct.getMessageDigest());
          fix.setMessageDigestAlgorithm(fct.getMessageDigestAlgorithm().getStringValue());
          fix.setMessageDigestOriginator(fct.getMessageDigestOriginator().getStringValue());
          fixities.add(fix);
        }
      }
    }
    IOUtils.closeQuietly(inputStream);
    return fixities;
  }

  public static gov.loc.premis.v3.Representation binaryToRepresentation(InputStream binaryInputStream)
    throws XmlException, IOException, GenericException {
    ObjectDocument objectDocument = ObjectDocument.Factory.parse(binaryInputStream);

    ObjectComplexType object = objectDocument.getObject();
    if (object instanceof Representation) {
      return (Representation) object;
    } else {
      throw new GenericException("Trying to load a representation but was a " + object.getClass().getSimpleName());
    }
  }

  public static gov.loc.premis.v3.File binaryToFile(InputStream binaryInputStream)
    throws XmlException, IOException, GenericException {
    ObjectDocument objectDocument = ObjectDocument.Factory.parse(binaryInputStream);

    ObjectComplexType object = objectDocument.getObject();
    if (object instanceof gov.loc.premis.v3.File) {
      return (gov.loc.premis.v3.File) object;
    } else {
      throw new GenericException("Trying to load a file but was a " + object.getClass().getSimpleName());
    }
  }

  public static EventComplexType binaryToEvent(InputStream binaryInputStream) throws XmlException, IOException {
    return EventDocument.Factory.parse(binaryInputStream).getEvent();
  }

  public static AgentComplexType binaryToAgent(InputStream binaryInputStream) throws XmlException, IOException {
    return AgentDocument.Factory.parse(binaryInputStream).getAgent();
  }

  public static gov.loc.premis.v3.Representation binaryToRepresentation(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    Representation representation;
    InputStream inputStream = null;
    try {
      inputStream = payload.createInputStream();
      representation = binaryToRepresentation(inputStream);

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !representation.validate(validationOptions)) {
        throw new ValidationException(MetadataUtils.xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return representation;
  }

  public static gov.loc.premis.v3.File binaryToFile(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    gov.loc.premis.v3.File file;
    List<XmlValidationError> validationErrors = new ArrayList<>();
    InputStream inputStream = null;
    try {
      inputStream = payload.createInputStream();
      file = binaryToFile(inputStream);

      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !file.validate(validationOptions)) {
        throw new ValidationException(MetadataUtils.xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException e) {
      ValidationException exception = new ValidationException(e);
      exception.setReport(MetadataUtils.xmlValidationErrorsToValidationReport(validationErrors));
      throw exception;
    } catch (IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return file;
  }

  public static ContentPayload fileToBinary(gov.loc.premis.v3.File file) throws GenericException, ValidationException {
    ObjectDocument d = ObjectDocument.Factory.newInstance();
    d.setObject(file);
    return MetadataUtils.saveToContentPayload(d, true);
  }

  public static ContentPayload representationToBinary(Representation representation)
    throws GenericException, ValidationException {
    ObjectDocument d = ObjectDocument.Factory.newInstance();
    d.setObject(representation);
    return MetadataUtils.saveToContentPayload(d, true);
  }

  public static EventComplexType binaryToEvent(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    EventComplexType event;
    InputStream inputStream = null;
    try {
      inputStream = payload.createInputStream();
      event = binaryToEvent(inputStream);

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !event.validate(validationOptions)) {
        throw new ValidationException(MetadataUtils.xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return event;
  }

  public static AgentComplexType binaryToAgent(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    AgentComplexType agent;
    InputStream inputStream = null;
    try {
      inputStream = payload.createInputStream();
      agent = binaryToAgent(inputStream);

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !agent.validate(validationOptions)) {
        throw new ValidationException(MetadataUtils.xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return agent;
  }

  public static SolrInputDocument getSolrDocument(Binary premisBinary) throws GenericException {

    SolrInputDocument doc = new SolrInputDocument();

    InputStream inputStream = null;
    try {
      inputStream = premisBinary.getContent().createInputStream();
      gov.loc.premis.v3.File premisFile = binaryToFile(inputStream);
      if (premisFile.getOriginalName() != null) {
        doc.setField(RodaConstants.FILE_ORIGINALNAME, premisFile.getOriginalName().getStringValue());

        // TODO extension
      }
      if (premisFile.getObjectCharacteristicsArray() != null && premisFile.getObjectCharacteristicsArray().length > 0) {
        ObjectCharacteristicsComplexType occt = premisFile.getObjectCharacteristicsArray(0);
        doc.setField(RodaConstants.FILE_SIZE, occt.getSize());
        if (occt.getFixityArray() != null && occt.getFixityArray().length > 0) {
          List<String> hashes = new ArrayList<>();
          for (FixityComplexType fct : occt.getFixityArray()) {
            StringBuilder fixityPrint = new StringBuilder();
            fixityPrint.append(fct.getMessageDigest());
            fixityPrint.append(" (");
            fixityPrint.append(fct.getMessageDigestAlgorithm().getStringValue());
            if (StringUtils.isNotBlank(fct.getMessageDigestOriginator().getStringValue())) {
              fixityPrint.append(", "); //
              fixityPrint.append(fct.getMessageDigestOriginator().getStringValue());
            }
            fixityPrint.append(")");
            hashes.add(fixityPrint.toString());
          }
          doc.addField(RodaConstants.FILE_HASH, hashes);
        }
        if (occt.getFormatArray() != null && occt.getFormatArray().length > 0) {
          FormatComplexType fct = occt.getFormatArray(0);
          if (fct.getFormatDesignation() != null) {
            doc.addField(RodaConstants.FILE_FILEFORMAT, fct.getFormatDesignation().getFormatName().getStringValue());
            doc.addField(RodaConstants.FILE_FORMAT_VERSION, fct.getFormatDesignation().getFormatVersion());
          }

          FormatRegistryComplexType pronomRegistry = getFormatRegistry(premisFile,
            RodaConstants.PRESERVATION_REGISTRY_PRONOM);
          if (pronomRegistry != null) {
            if (pronomRegistry.getFormatRegistryKey() != null) {
              doc.addField(RodaConstants.FILE_PRONOM, pronomRegistry.getFormatRegistryKey().getStringValue());
            }
          }
          FormatRegistryComplexType mimeRegistry = getFormatRegistry(premisFile,
            RodaConstants.PRESERVATION_REGISTRY_MIME);
          if (mimeRegistry != null) {
            if (mimeRegistry.getFormatRegistryKey() != null) {
              doc.addField(RodaConstants.FILE_FORMAT_MIMETYPE, mimeRegistry.getFormatRegistryKey().getStringValue());
            }
          }
          // TODO extension
        }
        if (occt.getCreatingApplicationArray() != null && occt.getCreatingApplicationArray().length > 0) {
          CreatingApplicationComplexType cact = occt.getCreatingApplicationArray(0);
          if (cact.getCreatingApplicationName() != null) {
            doc.addField(RodaConstants.FILE_CREATING_APPLICATION_NAME,
              cact.getCreatingApplicationName().getStringValue());
          }
          doc.addField(RodaConstants.FILE_CREATING_APPLICATION_VERSION, cact.getCreatingApplicationVersion());
          doc.addField(RodaConstants.FILE_DATE_CREATED_BY_APPLICATION, cact.getDateCreatedByApplication());
        }
      }

    } catch (XmlException | IOException e) {
      LOGGER.error("Error updating Solr document", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return doc;
  }

  public static PreservationMetadata createPremisAgentBinary(Plugin<?> plugin, ModelService model, boolean notify)
    throws GenericException, NotFoundException, RequestNotValidException, AuthorizationDeniedException,
    ValidationException, AlreadyExistsException {
    String id = IdUtils.getPluginAgentId(plugin.getClass().getName(), plugin.getVersion());
    String extension = "";
    ContentPayload agentPayload = PremisV3Utils.createPremisAgentBinary(id, plugin.getName(), plugin.getAgentType(),
      extension, plugin.getDescription(), plugin.getVersion());
    return model.createPreservationMetadata(PreservationMetadataType.AGENT, id, agentPayload, notify);
  }

  public static void linkFileToRepresentation(File file, String relationshipType, String relationshipSubType,
    Representation r) throws GenericException, RequestNotValidException, NotFoundException,
    AuthorizationDeniedException, XmlException, IOException, ValidationException {

    RelationshipComplexType relationship = r.addNewRelationship();
    relationship.setRelationshipType(getStringPlusAuthority(relationshipType));
    relationship.setRelationshipSubType(getStringPlusAuthority(relationshipSubType));
    RelatedObjectIdentifierComplexType roict = relationship.addNewRelatedObjectIdentifier();
    roict.setRelatedObjectIdentifierType(getStringPlusAuthority(RodaConstants.PREMIS_IDENTIFIER_TYPE_LOCAL));
    String id = IdUtils.getPreservationMetadataId(PreservationMetadataType.OBJECT_FILE, file.getAipId(),
      file.getRepresentationId(), file.getPath(), file.getId());
    roict.setRelatedObjectIdentifierValue(id);
  }

  public static List<LinkingIdentifier> extractAgentsFromEvent(Binary b) throws ValidationException, GenericException {
    List<LinkingIdentifier> identifiers = new ArrayList<LinkingIdentifier>();
    EventComplexType event = PremisV3Utils.binaryToEvent(b.getContent(), true);
    if (event.getLinkingAgentIdentifierArray() != null && event.getLinkingAgentIdentifierArray().length > 0) {
      for (LinkingAgentIdentifierComplexType laict : event.getLinkingAgentIdentifierArray()) {
        LinkingIdentifier li = new LinkingIdentifier();
        li.setType(laict.getLinkingAgentIdentifierType().getStringValue());
        li.setValue(laict.getLinkingAgentIdentifierValue());
        li.setRoles(stringplusArrayToStringList(laict.getLinkingAgentRoleArray()));
        identifiers.add(li);
      }
    }
    return identifiers;
  }

  public static List<LinkingIdentifier> extractObjectFromEvent(Binary binary)
    throws ValidationException, GenericException {
    List<LinkingIdentifier> identifiers = new ArrayList<LinkingIdentifier>();
    EventComplexType event = PremisV3Utils.binaryToEvent(binary.getContent(), true);
    if (event.getLinkingObjectIdentifierArray() != null && event.getLinkingObjectIdentifierArray().length > 0) {
      for (LinkingObjectIdentifierComplexType loict : event.getLinkingObjectIdentifierArray()) {
        LinkingIdentifier li = new LinkingIdentifier();
        li.setType(loict.getLinkingObjectIdentifierType().getStringValue());
        li.setValue(loict.getLinkingObjectIdentifierValue());
        li.setRoles(stringplusArrayToStringList(loict.getLinkingObjectRoleArray()));
        identifiers.add(li);
      }
    }
    return identifiers;
  }

  private static StringPlusAuthority getStringPlusAuthority(String value) {
    return getStringPlusAuthority(value, "");
  }

  private static StringPlusAuthority getStringPlusAuthority(String value, String authority) {
    StringPlusAuthority spa = StringPlusAuthority.Factory.newInstance();
    spa.setStringValue(value);
    if (StringUtils.isNotBlank(authority)) {
      spa.setAuthority(authority);
    }
    return spa;
  }

  private static StringPlusAuthority[] getStringPlusAuthorityArray(List<String> values) {
    List<StringPlusAuthority> l = new ArrayList<StringPlusAuthority>();
    if (values != null && !values.isEmpty()) {
      for (String value : values) {
        l.add(getStringPlusAuthority(value));
      }
    }
    return l.toArray(new StringPlusAuthority[l.size()]);
  }

  private static List<String> stringplusArrayToStringList(StringPlusAuthority[] source) {
    List<String> dst = new ArrayList<String>();
    if (source != null && source.length > 0) {
      for (StringPlusAuthority spa : source) {
        dst.add(spa.getStringValue());
      }
    }
    return dst;
  }

  public static PreservationMetadata createPremisUserAgentBinary(String username, ModelService model,
    IndexService index, boolean notify) throws GenericException, ValidationException, NotFoundException,
    RequestNotValidException, AuthorizationDeniedException, AlreadyExistsException {
    PreservationMetadata pm = null;

    if (StringUtils.isNotBlank(username)) {
      RODAMember member = index.retrieve(RODAMember.class, username);
      String id = IdUtils.getUserAgentId(username);
      ContentPayload agentPayload;

      // TODO set agent extension
      String extension = "";
      String note = "";
      String version = "";
      agentPayload = PremisV3Utils.createPremisAgentBinary(id, member.getFullName(), PreservationAgentType.PERSON,
        extension, note, version);
      pm = model.createPreservationMetadata(PreservationMetadataType.AGENT, id, agentPayload, notify);
    }

    return pm;
  }

  // /**
  // * @deprecated
  // */
  // @Deprecated
  // public static IndexedPreservationAgent getPreservationUserAgent(Plugin<?>
  // plugin, ModelService model,
  // IndexService index) throws NotFoundException, GenericException {
  // Job job = PluginHelper.getJobFromIndex(plugin, index);
  // String id = job.getUsername();
  // IndexedPreservationAgent agent = new IndexedPreservationAgent();
  // agent.setId(id);
  // agent.setName(plugin.getName());
  // return agent;
  // }
}
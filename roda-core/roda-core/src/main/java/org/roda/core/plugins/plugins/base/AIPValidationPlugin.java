/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.plugins.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.roda.core.common.validation.ValidationUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.IdUtils.LinkingObjectType;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.metadata.LinkingIdentifier;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.jobs.Report.PluginState;
import org.roda.core.data.v2.validation.ValidationReport;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.plugins.PluginHelper;
import org.roda.core.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME rename this to SIPValidationPlugin
public class AIPValidationPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AIPValidationPlugin.class);

  public static final PluginParameter PARAMETER_VALIDATE_DESCRIPTIVE_METADATA = new PluginParameter(
    "parameter.validate_descriptive_metadata", "Validate descriptive metadata", PluginParameterType.BOOLEAN, "true",
    true, false, "If true the descriptive metadata is validated against existing schemas.");

  public static final PluginParameter PARAMETER_METADATA_TYPE = new PluginParameter("parameter.metadata_type",
    "Descriptive metadata type", PluginParameterType.METADATA_TYPE, null, false, false,
    "Descriptive metadata type to be used as fallback or if metadata type is forced.");

  public static final PluginParameter PARAMETER_FORCE_DESCRIPTIVE_METADATA_TYPE = new PluginParameter(
    "parameter.force_type", "Force metadata type in all", PluginParameterType.BOOLEAN, "false", true, false,
    "If true, bypass current metadata type with metadata type passed as parameter. If false, if metadata type passed as parameter is defined use as fallback, else no fallback");

  public static final PluginParameter PARAMETER_VALIDATE_PREMIS = new PluginParameter("parameter.validate_premis",
    "Validate Premis", PluginParameterType.BOOLEAN, "true", true, false, "Validate Premis");

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public String getName() {
    return "Descriptive metadata validation";
  }

  @Override
  public String getDescription() {
    return "Checks whether the descriptive metadata is included in the SIP and if this metadata is valid according to the established policy.";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> pluginParameters = new ArrayList<PluginParameter>();
    pluginParameters.add(PARAMETER_VALIDATE_DESCRIPTIVE_METADATA);
    pluginParameters.add(PARAMETER_METADATA_TYPE);
    pluginParameters.add(PARAMETER_FORCE_DESCRIPTIVE_METADATA_TYPE);
    pluginParameters.add(PARAMETER_VALIDATE_PREMIS);
    return pluginParameters;
  }

  @Override
  public Report execute(IndexService index, ModelService model, StorageService storage, List<AIP> list)
    throws PluginException {

    boolean validateDescriptiveMetadata = Boolean.parseBoolean(getParameterValues().getOrDefault(
      PARAMETER_VALIDATE_DESCRIPTIVE_METADATA.getId(), PARAMETER_VALIDATE_DESCRIPTIVE_METADATA.getDefaultValue()));
    boolean validatePremis = Boolean.parseBoolean(getParameterValues().getOrDefault(PARAMETER_VALIDATE_PREMIS.getId(),
      PARAMETER_VALIDATE_PREMIS.getDefaultValue()));
    boolean forceDescriptiveMetadataType = Boolean.parseBoolean(getParameterValues().getOrDefault(
      PARAMETER_FORCE_DESCRIPTIVE_METADATA_TYPE.getId(), PARAMETER_FORCE_DESCRIPTIVE_METADATA_TYPE.getDefaultValue()));
    String metadataType = getParameterValues().getOrDefault(PARAMETER_METADATA_TYPE.getId(),
      PARAMETER_METADATA_TYPE.getDefaultValue());

    List<ValidationReport> reports = new ArrayList<ValidationReport>();
    for (AIP aip : list) {
      Report reportItem = PluginHelper.createPluginReportItem(this, aip.getId(), null);
      try {
        LOGGER.debug("VALIDATING AIP " + aip.getId());
        ValidationReport report = ValidationUtils.isAIPMetadataValid(forceDescriptiveMetadataType,
          validateDescriptiveMetadata, metadataType, validatePremis, model, aip.getId());
        reports.add(report);
        if (report.isValid()) {
          reportItem.setPluginState(PluginState.SUCCESS);
        } else {
          reportItem.setPluginState(PluginState.FAILURE).setPluginDetails(report.toString());
        }

        boolean notify = true;
        createEvent(aip, model, reportItem.getPluginState(), notify);
      } catch (RODAException mse) {
        LOGGER.error("Error processing AIP " + aip.getId() + ": " + mse.getMessage(), mse);
      }

      try {
        PluginHelper.updateJobReport(this, model, index, reportItem);
      } catch (Throwable e) {
        LOGGER.error("Error updating job report", e);
      }
    }
    return null;
  }

  private void createEvent(AIP aip, ModelService model, PluginState state, boolean notify) throws PluginException {
    try {
      List<LinkingIdentifier> sources = Arrays.asList(PluginHelper.getLinkingIdentifier(LinkingObjectType.AIP,
        aip.getId(), null, null, null, RodaConstants.PRESERVATION_LINKING_OBJECT_SOURCE));
      List<LinkingIdentifier> outcomes = null;
      PluginHelper.createPluginEvent(this, aip.getId(), null, null, null, model, sources, outcomes, state, "", notify);
      if (notify) {
        model.notifyAIPUpdated(aip.getId());
      }
    } catch (RODAException e) {
      throw new PluginException(e.getMessage(), e);
    }

  }

  @Override
  public Report beforeExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {

    return null;
  }

  @Override
  public Report afterExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {

    return null;
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new AIPValidationPlugin();
  }

  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.WELLFORMEDNESS_CHECK;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Checked whether the descriptive metadata is included in the SIP and if this metadata is valid according to the established policy.";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Descriptive metadata is well formed and complete.";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "Descriptive metadata was not well formed or failed to meet the established ingest policy.";
  }
}

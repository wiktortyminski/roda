/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.utils;

import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.Job.JOB_STATE;
import org.roda.core.data.v2.jobs.Report.PluginState;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import config.i18n.client.BrowseMessages;

public class HtmlSnippetUtils {

  private static final BrowseMessages messages = GWT.create(BrowseMessages.class);

  public static SafeHtml getJobStateHtml(Job job) {
    SafeHtml ret = null;
    if (job != null) {
      JOB_STATE state = job.getState();
      if (JOB_STATE.COMPLETED.equals(state)) {
        if (job.getJobStats().getSourceObjectsCount() == job.getJobStats().getSourceObjectsProcessedWithSuccess()) {
          ret = SafeHtmlUtils
            .fromSafeConstant("<span class='label-success'>" + messages.showJobStatusCompleted() + "</span>");
        } else {
          ret = SafeHtmlUtils
            .fromSafeConstant("<span class='label-danger'>" + messages.showJobStatusCompleted() + "</span>");
        }
      } else if (JOB_STATE.FAILED_DURING_CREATION.equals(state)) {
        ret = SafeHtmlUtils
          .fromSafeConstant("<span class='label-default'>" + messages.showJobStatusFailedDuringCreation() + "</span>");
      } else if (JOB_STATE.FAILED_TO_COMPLETE.equals(state)) {
        ret = SafeHtmlUtils
          .fromSafeConstant("<span class='label-default'>" + messages.showJobStatusFailedToComplete() + "</span>");
      } else if (JOB_STATE.CREATED.equals(state)) {
        ret = SafeHtmlUtils.fromSafeConstant("<span class='label-info'>" + messages.showJobStatusCreated() + "</span>");
      } else if (JOB_STATE.STARTED.equals(state)) {
        ret = SafeHtmlUtils.fromSafeConstant("<span class='label-info'>" + messages.showJobStatusStarted() + "</span>");
      } else {
        ret = SafeHtmlUtils.fromSafeConstant("<span class='label-warning'>" + state + "</span>");
      }
    }
    return ret;
  }

  public static SafeHtml getAIPStateHTML(AIPState aipState) {
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    switch (aipState) {
      case ACTIVE:
        b.append(SafeHtmlUtils.fromSafeConstant("<span class='label-success'>"));
        break;
      case UNDER_APPRAISAL:
        b.append(SafeHtmlUtils.fromSafeConstant("<span class='label-warning'>"));
        break;
      default:
        b.append(SafeHtmlUtils.fromSafeConstant("<span class='label-danger'>"));
        break;
    }

    b.append(SafeHtmlUtils.fromString(messages.aipState(aipState)));
    b.append(SafeHtmlUtils.fromSafeConstant("</span>"));

    return b.toSafeHtml();
  }

  public static SafeHtml getPluginStateHTML(PluginState pluginState) {
    SafeHtml pluginStateHTML;
    switch (pluginState) {
      case SUCCESS:
        pluginStateHTML = SafeHtmlUtils.fromSafeConstant("<span class='label-success'>" + pluginState + "</span>");
        break;
      case RUNNING:
        pluginStateHTML = SafeHtmlUtils.fromSafeConstant("<span class='label-default'>" + pluginState + "</span>");
        break;
      case FAILURE:
      default:
        pluginStateHTML = SafeHtmlUtils.fromSafeConstant("<span class='label-danger'>" + pluginState + "</span>");
        break;
    }
    return pluginStateHTML;
  }

}
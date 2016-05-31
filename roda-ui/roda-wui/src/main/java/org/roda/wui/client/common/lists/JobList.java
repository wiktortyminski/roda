/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.lists;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.adapter.facet.Facets;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.sort.Sorter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.client.common.utils.HtmlSnippetUtils;
import org.roda.wui.common.client.tools.Humanize;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * 
 * @author Luis Faria <lfaria@keep.pt>
 *
 */
public class JobList extends BasicAsyncTableCell<Job> {

  // private final ClientLogger logger = new ClientLogger(getClass().getName());
  // private static final BrowseMessages messages =
  // GWT.create(BrowseMessages.class);

  private TextColumn<Job> nameColumn;
  private TextColumn<Job> usernameColumn;
  private Column<Job, Date> startDateColumn;
  private TextColumn<Job> durationColumn;
  private Column<Job, SafeHtml> statusColumn;
  private TextColumn<Job> progressColumn;
  private TextColumn<Job> objectsTotalCountColumn;
  private Column<Job, SafeHtml> objectsSuccessCountColumn;
  private Column<Job, SafeHtml> objectsFailureCountColumn;
  private Column<Job, SafeHtml> objectsProcessingCountColumn;
  private Column<Job, SafeHtml> objectsWaitingCountColumn;

  public JobList() {
    this(null, null, null, false);
  }

  public JobList(Filter filter, Facets facets, String summary, boolean selectable) {
    super(filter, facets, summary, selectable);
    super.setSelectedClass(Job.class);
  }

  @Override
  protected void configureDisplay(CellTable<Job> display) {

    nameColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getName() : null;
      }
    };

    usernameColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getUsername() : null;
      }
    };

    startDateColumn = new Column<Job, Date>(
      new DateCell(DateTimeFormat.getFormat(RodaConstants.DEFAULT_DATETIME_FORMAT))) {
      @Override
      public Date getValue(Job job) {
        return job != null ? job.getStartDate() : null;
      }
    };

    durationColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        if (job == null) {
          return null;
        }
        Date end = job.getEndDate() != null ? job.getEndDate() : getDate();
        return Humanize.durationInShortDHMS(job.getStartDate(), end);
      }
    };

    statusColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        return HtmlSnippetUtils.getJobStateHtml(job);
      }
    };

    objectsTotalCountColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        String ret = "";
        if (job != null) {
          if (job.getJobStats().getSourceObjectsCount() > 0) {
            ret = job.getJobStats().getSourceObjectsCount() + "";
          }
        }
        return ret;
      }
    };

    objectsSuccessCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(
            job.getJobStats().getSourceObjectsProcessedWithSuccess() > 0 ? SafeHtmlUtils.fromSafeConstant("<span>")
              : SafeHtmlUtils.fromSafeConstant("<span class='ingest-process-counter-0'>"));
          b.append(job.getJobStats().getSourceObjectsProcessedWithSuccess());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    objectsFailureCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(SafeHtmlUtils.fromSafeConstant("<span"));
          if (job.getJobStats().getSourceObjectsProcessedWithFailure() > 0) {
            b.append(SafeHtmlUtils.fromSafeConstant(" class='ingest-process-failed-column'"));
          } else {
            b.append(SafeHtmlUtils.fromSafeConstant(" class='ingest-process-counter-0'"));
          }
          b.append(SafeHtmlUtils.fromSafeConstant(">"));
          b.append(job.getJobStats().getSourceObjectsProcessedWithFailure());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    objectsWaitingCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(
            job.getJobStats().getSourceObjectsWaitingToBeProcessed() > 0 ? SafeHtmlUtils.fromSafeConstant("<span>")
              : SafeHtmlUtils.fromSafeConstant("<span class='ingest-process-counter-0'>"));
          b.append(job.getJobStats().getSourceObjectsWaitingToBeProcessed());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    objectsProcessingCountColumn = new Column<Job, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Job job) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (job != null) {
          b.append(job.getJobStats().getSourceObjectsBeingProcessed() > 0 ? SafeHtmlUtils.fromSafeConstant("<span>")
            : SafeHtmlUtils.fromSafeConstant("<span class='ingest-process-counter-0'>"));
          b.append(job.getJobStats().getSourceObjectsBeingProcessed());
          b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
        }
        return b.toSafeHtml();
      }
    };

    progressColumn = new TextColumn<Job>() {

      @Override
      public String getValue(Job job) {
        return job != null ? job.getJobStats().getCompletionPercentage() + "%" : null;
      }
    };

    nameColumn.setSortable(true);
    usernameColumn.setSortable(true);
    startDateColumn.setSortable(true);
    statusColumn.setSortable(true);
    objectsTotalCountColumn.setSortable(true);
    objectsSuccessCountColumn.setSortable(true);
    objectsFailureCountColumn.setSortable(true);
    objectsWaitingCountColumn.setSortable(true);
    objectsProcessingCountColumn.setSortable(true);
    progressColumn.setSortable(true);

    // TODO externalize strings into constants

    addColumn(nameColumn, "Name", true, false);
    addColumn(usernameColumn, "Creator", true, false);
    addColumn(startDateColumn, "Start date", true, false, 11);
    addColumn(durationColumn, "Duration", true, true, 6);
    addColumn(statusColumn, "Status", true, false, 7);
    addColumn(progressColumn, "Progress", true, true, 5);
    addColumn(objectsTotalCountColumn, "Total", true, true, 5);
    addColumn(objectsSuccessCountColumn, "Successful", true, true, 6);
    addColumn(objectsFailureCountColumn, "Failed", true, true, 5);
    addColumn(objectsProcessingCountColumn, "Processing", true, true, 6);
    addColumn(objectsWaitingCountColumn, "Waiting", true, true, 5);

    // default sorting
    display.getColumnSortList().push(new ColumnSortInfo(startDateColumn, false));

  }

  @Override
  protected void getData(Sublist sublist, ColumnSortList columnSortList, AsyncCallback<IndexResult<Job>> callback) {

    Filter filter = getFilter();

    Map<Column<Job, ?>, List<String>> columnSortingKeyMap = new HashMap<Column<Job, ?>, List<String>>();
    columnSortingKeyMap.put(nameColumn, Arrays.asList(RodaConstants.JOB_NAME));
    columnSortingKeyMap.put(startDateColumn, Arrays.asList(RodaConstants.JOB_START_DATE));
    columnSortingKeyMap.put(statusColumn, Arrays.asList(RodaConstants.JOB_STATE));
    columnSortingKeyMap.put(progressColumn, Arrays.asList(RodaConstants.JOB_COMPLETION_PERCENTAGE));
    columnSortingKeyMap.put(objectsTotalCountColumn, Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_COUNT));
    columnSortingKeyMap.put(objectsSuccessCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_PROCESSED_WITH_SUCCESS));
    columnSortingKeyMap.put(objectsFailureCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_PROCESSED_WITH_FAILURE));
    columnSortingKeyMap.put(objectsProcessingCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_BEING_PROCESSED));
    columnSortingKeyMap.put(objectsWaitingCountColumn,
      Arrays.asList(RodaConstants.JOB_SOURCE_OBJECTS_WAITING_TO_BE_PROCESSED));
    columnSortingKeyMap.put(usernameColumn, Arrays.asList(RodaConstants.JOB_USERNAME));

    Sorter sorter = createSorter(columnSortList, columnSortingKeyMap);

    boolean justActive = true;
    BrowserService.Util.getInstance().find(Job.class.getName(), filter, sorter, sublist, getFacets(),
      LocaleInfo.getCurrentLocale().getLocaleName(), justActive, callback);
  }

}

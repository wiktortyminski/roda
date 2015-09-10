package pt.gov.dgarq.roda.wui.dissemination.browse.server;

import java.util.Date;
import java.util.List;

import org.roda.common.UserUtility;

import pt.gov.dgarq.roda.common.RodaCoreService;
import pt.gov.dgarq.roda.core.common.AuthorizationDeniedException;
import pt.gov.dgarq.roda.core.common.RODAException;
import pt.gov.dgarq.roda.core.data.adapter.facet.Facets;
import pt.gov.dgarq.roda.core.data.adapter.filter.Filter;
import pt.gov.dgarq.roda.core.data.adapter.sort.Sorter;
import pt.gov.dgarq.roda.core.data.adapter.sublist.Sublist;
import pt.gov.dgarq.roda.core.data.v2.IndexResult;
import pt.gov.dgarq.roda.core.data.v2.RodaUser;
import pt.gov.dgarq.roda.core.data.v2.SimpleDescriptionObject;
import pt.gov.dgarq.roda.wui.common.client.GenericException;
import pt.gov.dgarq.roda.wui.dissemination.browse.client.BrowseItemBundle;

public class Browser extends RodaCoreService {

  private Browser() {
    super();
  }

  public static BrowseItemBundle getItemBundle(RodaUser user, String aipId, String localeString)
    throws AuthorizationDeniedException, GenericException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "browse");

    // delegate
    BrowseItemBundle itemBundle = BrowserHelper.getItemBundle(aipId, localeString);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "getItemBundle", aipId, duration, "aipId", aipId);

    return itemBundle;
  }

  public static IndexResult<SimpleDescriptionObject> findDescriptiveMetadata(RodaUser user, Filter filter,
    Sorter sorter, Sublist sublist, Facets facets) throws RODAException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "browse");

    // delegate
    IndexResult<SimpleDescriptionObject> descriptiveMetadata = BrowserHelper.findDescriptiveMetadata(filter, sorter,
      sublist, facets);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "findDescriptiveMetadata", null, duration, "filter", filter.toString(), "sorter",
      sorter.toString(), "sublist", sublist.toString());

    return descriptiveMetadata;
  }

  public static Long countDescriptiveMetadata(RodaUser user, Filter filter)
    throws AuthorizationDeniedException, GenericException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "browse");

    // delegate
    Long count = BrowserHelper.countDescriptiveMetadata(filter);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "countDescriptiveMetadata", null, duration, "filter", filter.toString());

    return count;
  }

  public static SimpleDescriptionObject getSimpleDescriptionObject(RodaUser user, String aipId)
    throws AuthorizationDeniedException, GenericException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "browse");

    // delegate
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "getSimpleDescriptionObject", aipId, duration, "aipId", aipId);

    return sdo;
  }

  public static List<SimpleDescriptionObject> getAncestors(RodaUser user, SimpleDescriptionObject sdo)
    throws AuthorizationDeniedException, GenericException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "browse");

    // delegate
    List<SimpleDescriptionObject> ancestors = BrowserHelper.getAncestors(sdo);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "getParent", sdo.getId(), duration, "sdo", sdo.toString());

    return ancestors;
  }

  public static SimpleDescriptionObject moveInHierarchy(RodaUser user, String aipId, String parentId)
    throws AuthorizationDeniedException, GenericException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, "administration.metadata_editor");

    // delegate
    SimpleDescriptionObject sdo = BrowserHelper.moveInHierarchy(aipId, parentId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, "Browser", "moveInHierarchy", sdo.getId(), duration, "aip", aipId, "toParent", parentId);

    return sdo;

  }

}

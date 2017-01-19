package org.roda.wui.client.common.slider;

import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.wui.client.common.actions.FileActions;

public class OptionsSliderHelper {

  private OptionsSliderHelper() {

  }

  static <T extends IsIndexed> void updateOptionsObjectSliderPanel(T object, SliderPanel slider) {
    if (object instanceof IndexedFile) {
      updateOptionsSliderPanel((IndexedFile) object, slider);
    } else if (object instanceof IndexedRepresentation) {
      updateOptionsSliderPanel((IndexedRepresentation) object, slider);
    } else if (object instanceof IndexedAIP) {
      updateOptionsSliderPanel((IndexedAIP) object, slider);
    } else {
      // do nothing
    }
  }

  private static void updateOptionsSliderPanel(IndexedAIP aip, SliderPanel slider) {
    // TODO Auto-generated method stub

  }

  private static void updateOptionsSliderPanel(IndexedRepresentation representation, SliderPanel slider) {
    // TODO Auto-generated method stub

  }

  private static void updateOptionsSliderPanel(final IndexedFile file, final SliderPanel slider) {
    slider.clear();
    slider.addContent(FileActions.get().createActionsLayout(file));
  }

}

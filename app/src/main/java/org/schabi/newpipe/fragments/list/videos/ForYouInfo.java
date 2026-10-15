/*
 * ForYouInfo - holds aggregated related items from watch history
 * Based on RelatedItemsInfo pattern
 */

package org.schabi.newpipe.fragments.list.videos;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import java.util.ArrayList;
import java.util.Collections;

public final class ForYouInfo extends ListInfo<InfoItem> {
    public ForYouInfo() {
        super(0, new ListLinkHandler("", "", "", Collections.emptyList(), null), "For You");
        setRelatedItems(new ArrayList<>());
    }
}

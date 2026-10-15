/*
 * ForYouFragment - shows video recommendations based on watch history
 * Aggregates related items from recently watched videos
 */

package org.schabi.newpipe.fragments.list.videos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.stream.StreamStatisticsEntry;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.list.BaseListInfoFragment;
import org.schabi.newpipe.info_list.ItemViewMode;
import org.schabi.newpipe.ktx.ViewUtils;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.ExtractorHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ForYouFragment extends BaseListInfoFragment<InfoItem, ForYouInfo> {

    private static final int MAX_HISTORY_VIDEOS = 10;
    private static final int MAX_RECOMMENDATIONS = 50;

    public static ForYouFragment getInstance() {
        return new ForYouFragment();
    }

    public ForYouFragment() {
        super(UserAction.REQUESTED_STREAM);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_related_items,
                container, false);
    }

    @Override
    protected Single<ForYouInfo> loadResult(final boolean forceLoad) {
        final HistoryRecordManager historyManager =
                new HistoryRecordManager(requireContext());

        return historyManager.getStreamStatistics()
                .firstOrError()
                .flatMap((List<StreamStatisticsEntry> entries) -> {
                    final int count = Math.min(entries.size(),
                            MAX_HISTORY_VIDEOS);
                    final Set<String> historyUrls = new HashSet<>();
                    final List<Single<List<InfoItem>>> singles =
                            new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final StreamStatisticsEntry entry = entries.get(i);
                        final String url = entry.getStreamEntity().getUrl();
                        final int serviceId =
                                entry.getStreamEntity().getServiceId();
                        historyUrls.add(url);
                        singles.add(
                            ExtractorHelper.getStreamInfo(
                                serviceId, url, false)
                                .map((StreamInfo info) -> {
                                    final List<InfoItem> related =
                                            info.getRelatedItems();
                                    return related != null
                                            ? related
                                            : new ArrayList<InfoItem>();
                                })
                                .onErrorReturnItem(new ArrayList<>())
                        );
                    }
                    return Single.merge(singles)
                            .collectInto(
                                new ArrayList<InfoItem>(),
                                (aggregated, items) -> {
                                    for (final InfoItem item : items) {
                                        if (item instanceof StreamInfoItem) {
                                            aggregated.add(item);
                                        }
                                    }
                                }
                            )
                            .map(aggregated -> {
                                final Set<String> seenUrls =
                                        new HashSet<>();
                                final List<InfoItem> deduped =
                                        new ArrayList<>();
                                for (final InfoItem item : aggregated) {
                                    final String url = item.getUrl();
                                    if (url != null
                                            && !seenUrls.contains(url)
                                            && !historyUrls.contains(url)) {
                                        seenUrls.add(url);
                                        deduped.add(item);
                                        if (deduped.size()
                                                >= MAX_RECOMMENDATIONS) {
                                            break;
                                        }
                                    }
                                }
                                final ForYouInfo info = new ForYouInfo();
                                info.setRelatedItems(deduped);
                                return info;
                            });
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    protected Single<ListExtractor.InfoItemsPage<InfoItem>>
            loadMoreItemsLogic() {
        return Single.fromCallable(ListExtractor.InfoItemsPage::emptyPage);
    }

    @Override
    public void handleResult(@NonNull final ForYouInfo result) {
        super.handleResult(result);
        ViewUtils.slideUp(requireView(), 120, 96, 0.06f);
    }

    @Override
    public void setTitle(final String title) {
        // Override - title is set by parent
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
                                    @NonNull final MenuInflater inflater) {
        // Override - no menu
    }

    @Override
    protected ItemViewMode getItemViewMode() {
        ItemViewMode mode = super.getItemViewMode();
        if (mode != ItemViewMode.LIST && mode != ItemViewMode.CARD) {
            mode = ItemViewMode.LIST;
        }
        return mode;
    }
}

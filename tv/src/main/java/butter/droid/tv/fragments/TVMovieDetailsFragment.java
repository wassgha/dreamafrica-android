/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package dream.africa.tv.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import dream.africa.base.content.preferences.Prefs;
import dream.africa.base.providers.media.MediaProvider;
import dream.africa.base.providers.media.VodoProvider;
import dream.africa.base.providers.media.models.Media;
import dream.africa.base.providers.media.models.Movie;
import dream.africa.base.providers.subs.SubsProvider;
import dream.africa.base.torrent.StreamInfo;
import dream.africa.base.utils.NetworkUtils;
import dream.africa.base.utils.PrefUtils;
import dream.africa.base.youtube.YouTubeData;
import dream.africa.tv.R;
import dream.africa.tv.activities.TVStreamLoadingActivity;
import dream.africa.tv.activities.TVTrailerPlayerActivity;
import dream.africa.tv.activities.TVVideoPlayerActivity;
import dream.africa.tv.presenters.MovieDetailsDescriptionPresenter;

public class TVMovieDetailsFragment extends TVBaseDetailsFragment implements MediaProvider.Callback, OnActionClickedListener {

	VodoProvider mMovieProvider = new VodoProvider();

	public static Fragment newInstance(Media media) {
		TVMovieDetailsFragment fragment = new TVMovieDetailsFragment();

		Bundle bundle = new Bundle();
		bundle.putParcelable(EXTRA_ITEM, media);

		fragment.setArguments(bundle);
		return fragment;
	}

	private Movie getMovieItem() {
		return (Movie) getMediaItem();
	}

	@Override
	void loadDetails() {
		ArrayList<Media> mediaList = new ArrayList<>();
		mediaList.add(getMovieItem());

		mMovieProvider.getDetail(mediaList, 0, this);
	}

	@Override
	AbstractDetailsDescriptionPresenter getDetailPresenter() {
		return new MovieDetailsDescriptionPresenter();
	}

	@Override
	void onDetailLoaded() {
		addActions(getMovieItem());
	}

	@Override
	void addActions(Media item) {
		if (item instanceof Movie) {
			Movie movie = (Movie) item;

			List<String> qualities = new ArrayList(movie.torrents.keySet());

            addAction(new TrailerAction(qualities.size() + 1, getResources().getString(R.string.watch), getResources().getString(R.string.trailer)));

			for (String quality : qualities) {

				Media.Torrent torrent = movie.torrents.get(quality);

				//add action
				addAction(new WatchAction((long) qualities.indexOf(quality), getResources().getString(
						R.string.watch), quality, torrent));
			}
		}
	}

	@Override
	ClassPresenterSelector createPresenters(ClassPresenterSelector selector) {
		return null;
	}

	@Override
	public void onActionClicked(Action a) {
        if(a instanceof WatchAction) {
            // check for network
            if (!NetworkUtils.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), R.string.network_message, Toast.LENGTH_SHORT).show();
            } else {
                WatchAction action = (WatchAction) a;
                Media.Torrent torrent = action.getTorrent();
                String subtitleLanguage = PrefUtils.get(getActivity(), Prefs.SUBTITLE_DEFAULT, SubsProvider.SUBTITLE_LANGUAGE_NONE);
                StreamInfo info = new StreamInfo(
                        getMovieItem(),
                        torrent.url,
                        subtitleLanguage,
                        action.getLabel2().toString());

                TVStreamLoadingActivity.startActivity(getActivity(), info);
            }
        } else if(a instanceof TrailerAction) {
            Movie movie = getMovieItem();
            if (!YouTubeData.isYouTubeUrl(movie.trailer)) {
                TVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(movie, null, null, null, null, movie.trailer));
            } else {
                TVTrailerPlayerActivity.startActivity(getActivity(), movie.trailer, movie);
            }
        }
	}

	public static class WatchAction extends android.support.v17.leanback.widget.Action {

		private Media.Torrent mTorrent;

		public WatchAction(long id, CharSequence label, CharSequence label2, Media.Torrent torrent) {
			super(id, label, label2);
			this.mTorrent = torrent;
		}

		public Media.Torrent getTorrent() {
			return mTorrent;
		}

		public void setTorrent(Media.Torrent torrent) {
			mTorrent = torrent;
		}
	}

    public static class TrailerAction extends android.support.v17.leanback.widget.Action {

        public TrailerAction(long id, CharSequence label1, CharSequence label2) {
            super(id, label1, label2);
        }
    }
}
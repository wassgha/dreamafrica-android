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

package dream.africa.base.providers.media;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import dream.africa.base.ButterApplication;
import dream.africa.base.R;
import dream.africa.base.providers.media.models.Genre;
import dream.africa.base.providers.media.models.Media;
import dream.africa.base.providers.media.models.Movie;
import dream.africa.base.utils.LocaleUtils;
import dream.africa.base.utils.StringUtils;
import dream.africa.base.utils.PrefUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.json.*;
import android.text.Html;

public class DreamAfricaProvider extends MediaProvider {

    private static final DreamAfricaProvider sMediaProvider = new DreamAfricaProvider();
    private static Integer CURRENT_API = 0;
    private static final String[] API_URLS = {
            "http://www.wedreamafrica.com/api.php"
    };
    public static String CURRENT_URL = API_URLS[CURRENT_API];

    private static Filters sFilters = new Filters();
    private Integer totalPages = 0;

    @Override
    protected Call enqueue(Request request, com.squareup.okhttp.Callback requestCallback) {
        Context context = ButterApplication.getAppContext();
        PackageInfo pInfo;
        String versionName = "0.0.0";
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        request = request.newBuilder().removeHeader("User-Agent").addHeader("User-Agent", String.format("Mozilla/5.0 (Linux; U; Android %s; %s; %s Build/%s) AppleWebkit/534.30 (KHTML, like Gecko) PT/%s", Build.VERSION.RELEASE, LocaleUtils.getCurrentAsString(), Build.MODEL, Build.DISPLAY, versionName)).build();
        return super.enqueue(request, requestCallback);
    }

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        sFilters = filters;
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("uid", PrefUtils.get(ButterApplication.getAppContext(), "facebook_id", "")));
        params.add(new NameValuePair("email", PrefUtils.get(ButterApplication.getAppContext(), "facebook_email", "")));
        params.add(new NameValuePair("count", "15"));
        if (filters.page != null) {
            params.add(new NameValuePair("page", Integer.toString(filters.page)));
        }

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("search", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new NameValuePair("category", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new NameValuePair("order", "ASC"));
        } else {
            params.add(new NameValuePair("order", "DESC"));
        }

/*        if(filters.langCode != null) {
        params.add(new NameValuePair("lang", filters.langCode));
    }*/

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "popularity";
                break;
            case YEAR:
                sort = "date";
                break;
            case DATE:
                sort = "date";
                break;
            case RATING:
                sort = "popularity";
                break;
            case ALPHABET:
                sort = "title";
                break;
            case TRENDING:
                sort = "popularitys";
                break;
        }

        params.add(new NameValuePair("orderby", sort));
        String query = "?" + buildQuery(params);
        Request.Builder requestBuilder = new Request.Builder()
                .url(CURRENT_URL + query);
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {

                        JSONObject obj = new JSONObject(response.body().string());
                        JSONArray arr = obj.getJSONArray("posts");
                        for(int i = 0; i<arr.length(); i++) {
                            JSONObject post = arr.getJSONObject(i);
                            Movie movie = new Movie(sMediaProvider, null);
                            movie.imdbId = (String) post.getString("id");
                            movie.videoId = (String) post.getString("id");
                            if(isInResults(currentList, movie.videoId)==-1) {
                                movie.title = Html.fromHtml((String) post.getString("title")).toString();
                                String yearStr = (String) post.getString("year").toString();
                                Double year = Double.parseDouble(yearStr);
                                movie.year = Integer.toString(year.intValue());
                                movie.rating = "-1";
                                movie.genre = (String) post.getString("category").toString();
                                movie.image = (String) post.getString("cover_image").toString();
                                movie.headerImage = (String) post.getString("cover_image").toString();
                                movie.trailer = null;
                                String runtimeStr = "7";
                                Double runtime = 7d;
                                if (!runtimeStr.isEmpty())
                                    runtime = Double.parseDouble(runtimeStr);
                                movie.runtime = Integer.toString(runtime.intValue());
                                movie.synopsis = Html.fromHtml((String) post.getString("synopsis").toString()).toString();
                                movie.certification = null;
                                movie.fullImage = movie.image;

                                Media.Torrent torrent = new Media.Torrent();
                                torrent.seeds = 0;
                                torrent.peers = 0;
                                torrent.hash = null;
                                torrent.url = (String) post.getString("torrent");
                                movie.torrents.put("720p", torrent);
                                currentList.add(movie);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    totalPages = 5;
                    callback.onSuccess(sFilters, currentList, true);
                    return;
                }
                onFailure(response.request(), new IOException("Couldn't connect to Vodo"));
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, Callback callback) {
        ArrayList<Media> returnList = new ArrayList<>();
        returnList.add(currentList.get(index));
        callback.onSuccess(null, returnList, true);
        return null;
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.yts_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.ASC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.yts_filter_a_to_z));
        tabs.add(new NavInfo(R.id.yts_filter_release_date,Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.release_date),R.drawable.yts_filter_release_date));
        tabs.add(new NavInfo(R.id.yts_filter_popular_now,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.yts_filter_popular_now));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        return null;
    }

    private int isInResults(ArrayList<Media> results, String id) {
        int i = 0;
        for (Media item : results) {
            if (item.videoId.equals(id)) return i;
            i++;
        }
        return -1;
    }

    public static int getIndexOf(String str, char c, int n)
    {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
        {
            pos = str.indexOf(c, pos + 1);
        }
        return pos;
    }


}

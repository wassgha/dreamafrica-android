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

package dream.africa.tv.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import dream.africa.base.updater.ButterUpdater;
import dream.africa.tv.R;
import dream.africa.tv.activities.base.TVBaseActivity;

public class TVMainActivity extends TVBaseActivity {

    public static Intent startActivity(Activity activity) {
        Intent intent = new Intent(activity, TVMainActivity.class);
        activity.startActivity(intent);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ButterUpdater.getInstance(this, new ButterUpdater.Listener() {
            @Override
            public void updateAvailable(String filePath) {
                TVUpdateActivity.startActivity(TVMainActivity.this);
            }
        }).checkUpdates(false);
    }
}

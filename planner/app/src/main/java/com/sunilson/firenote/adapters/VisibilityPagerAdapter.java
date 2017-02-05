package com.sunilson.firenote.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.sunilson.firenote.R;
import com.sunilson.firenote.fragments.CategoryFragment;
import com.sunilson.firenote.fragments.ColorFragment;

/**
 * @author Linus Weiss
 */

public class VisibilityPagerAdapter extends FragmentPagerAdapter {

    private static int NUM_ITEMS = 2;
    private Context context;

    public VisibilityPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: // Fragment # 0 - This will show FirstFragment
                return CategoryFragment.newInstance(1, "Page # 1");
            case 1: // Fragment # 0 - This will show FirstFragment different title
                return ColorFragment.newInstance("Page # 2");
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: // Fragment # 0 - This will show FirstFragment
                return context.getString(R.string.categories);
            case 1: // Fragment # 0 - This will show FirstFragment different title
                return context.getString(R.string.colors);
            default:
                return null;
        }
    }
}
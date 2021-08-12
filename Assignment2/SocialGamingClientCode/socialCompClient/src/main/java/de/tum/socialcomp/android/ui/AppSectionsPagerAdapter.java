package de.tum.socialcomp.android.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * This class is used to select between the three Fragments 
 * of the game: main screen, map and game statistics.
 * 
 * @author Niklas Klügel
 *
 */

public class AppSectionsPagerAdapter extends FragmentPagerAdapter {

    private FragmentLoadListener listener;

	private String[] sections = new String[]{
			"Inventory",
			"Map",
			"Game"
	};

    public AppSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void changePageTitle(int pageNr, String title)
    {
        sections[pageNr] = title;
    }



    @Override
    public Fragment getItem(int i) {
        Fragment frag;
        switch (i) {
            case 0:
                frag = new MainSectionFragment();
                ((MainSectionFragment)frag).setFragmentLoadListener(listener);
                break;
            case 1:
            	frag = new MapSectionFragment();
                break;
            case 2:
                frag = new GameSectionFragment();
                ((GameSectionFragment)frag).setFragmentLoadListener(listener);
                break;
            default :
                frag = null;
        }
        return frag;
    }

    public void setFragmentLoadListener(FragmentLoadListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {

        return this.sections.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return sections[position];
    }

}

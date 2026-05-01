package com.example.petbuddy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * ViewPager2 adapter for adoption tabs (Dogs and Cats)
 */
public class AdoptionPagerAdapter extends FragmentStateAdapter {
    
    private static final int NUM_TABS = 2;
    
    public AdoptionPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DogsFragment();
            case 1:
                return new CatsFragment();
            default:
                return new DogsFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}
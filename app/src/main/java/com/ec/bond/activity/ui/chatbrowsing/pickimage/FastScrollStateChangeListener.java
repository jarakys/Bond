package com.ec.bond.activity.ui.chatbrowsing.pickimage;



public interface FastScrollStateChangeListener {

    /**
     * Called when fast scrolling begins
     */
    void onFastScrollStart(PickImage fastScroller);

    /**
     * Called when fast scrolling ends
     */
    void onFastScrollStop(PickImage fastScroller);
}


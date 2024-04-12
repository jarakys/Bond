package com.ec.bond.activity.ui.chatbrowsing.pickimage;

import android.view.View;


public interface OnSelectionListener {
    void onClick(Img Img, View view, int position);

    void onLongClick(Img img, View view, int position);
}

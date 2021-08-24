package com.app.tourguide.utils.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

public class SfuiRegularButton extends AppCompatButton {


    private Context context;
    private AttributeSet attrs;
    private int defStyle;

    public SfuiRegularButton(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public SfuiRegularButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        init();
    }

    public SfuiRegularButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        this.attrs = attrs;
        this.defStyle = defStyle;
        init();
    }

    private void init() {
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fonts/SanFranciscoDisplay-Semibold.otf");
        this.setTypeface(font);
    }


}
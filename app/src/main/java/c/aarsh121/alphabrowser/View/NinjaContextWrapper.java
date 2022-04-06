package c.aarsh121.alphabrowser.View;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

import c.aarsh121.alphabrowser.Unit.HelperUnit;

public class NinjaContextWrapper extends ContextWrapper {
    private final Context context;

    public NinjaContextWrapper(Context context) {
        super(context);
        this.context = context;
        HelperUnit.setTheme(context);
    }

    @Override
    public Resources.Theme getTheme() {
        return context.getTheme();
    }
}

package com.bugmonkey.cameraviewx.matisse.internal.utils;

import android.content.Context;
import android.util.Log;

import com.bugmonkey.cameraviewx.matisse.MimeType;
import com.bugmonkey.cameraviewx.matisse.filter.Filter;
import com.bugmonkey.cameraviewx.matisse.internal.entity.IncapableCause;
import com.bugmonkey.cameraviewx.matisse.internal.entity.Item;

import java.util.Set;

/**
 * Created by This on 2018/6/4.
 */

public class VideoFilter extends Filter {

    private static final int MAX_SIZE = 512 * 1024 * 1024;
    private static final int MAX_DURATION = 60 * 5 * 1000;

    @Override
    protected Set<MimeType> constraintTypes() {
        return MimeType.ofVideo();
    }

    @Override
    public IncapableCause filter(Context context, Item item) {
        Log.e("VideoFilter", "item.size:" + item.size + "\n" +
                "item.duration:" + item.duration + "\n" +
                "item.uri:" + item.getContentUri() + "\n");
        if (item.size > MAX_SIZE) {
            return new IncapableCause(IncapableCause.DIALOG, "视频大小不能超过500MB");
        }
        if (item.duration > MAX_DURATION) {
            return new IncapableCause(IncapableCause.DIALOG, "视频长度不能大于5分钟");
        }
        return null;
    }

}

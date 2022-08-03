package com.bugmonkey.cameraviewx.matisse.internal.utils;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import com.bugmonkey.cameraviewx.matisse.MimeType;
import com.bugmonkey.cameraviewx.matisse.filter.Filter;
import com.bugmonkey.cameraviewx.matisse.internal.entity.IncapableCause;
import com.bugmonkey.cameraviewx.matisse.internal.entity.Item;
import com.bugmonkey.cameraviewx.matisse.internal.utils.PhotoMetadataUtils;

import java.util.Set;

/**
 * Created by This on 2018/6/4.
 */

public class PhotoOrVideoFilter extends Filter {

    private static final int MAX_SIZE = 512 * 1024 * 1024;

    private static final int MAX_DURATION = 60 * 5 * 1000;
    private boolean hasSelectImage;


    @Override
    protected Set<MimeType> constraintTypes() {
        return MimeType.ofAll();
    }

    @Override
    public IncapableCause filter(Context context, Item item) {
        Log.e("VideoFilter", "item.size:" + item.size + "\n" +
                "item.duration:" + item.duration + "\n" +
                "item.uri:" + item.getContentUri() + "\n");

        if(MimeType.ofVideo().contains(item.mimeType)){

            if (item.size > MAX_SIZE) {
                return new IncapableCause(IncapableCause.DIALOG, "视频大小不能超过500MB");
            }
            if (item.duration > MAX_DURATION) {
                return new IncapableCause(IncapableCause.DIALOG, "视频长度不能大于5分钟");
            }
        }else {
            hasSelectImage=true;
            Point size = PhotoMetadataUtils.getBitmapBound(context.getContentResolver(), item.getContentUri());
//        if (item.size > mMaxSize) {
//            return new IncapableCause(IncapableCause.DIALOG,"图片大小不能超过"+mMaxSize/1024/1024+"M");
//        }
            if((size.x*1.0/size.y)>=4){
                return new IncapableCause(IncapableCause.DIALOG, "宽高比不能大于4");
            }
        }

        return null;
    }
}

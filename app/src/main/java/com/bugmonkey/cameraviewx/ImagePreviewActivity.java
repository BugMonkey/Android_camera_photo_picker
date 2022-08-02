package com.bugmonkey.cameraviewx;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.bugmonkey.cameraviewx.databinding.ActivityImagePreviewBinding;
import com.bumptech.glide.Glide;
import com.zhihu.matisse.Matisse;


public class ImagePreviewActivity extends AppCompatActivity {
    private ActivityImagePreviewBinding binding;
    public static void start(Activity context, Uri uri, int req) {

        Intent starter = new Intent(context, ImagePreviewActivity.class);
        starter.putExtra("uri",uri);
        context.startActivityForResult(starter,req);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_image_preview);
        binding = ActivityImagePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Glide.with(this).load((Uri) getIntent().getParcelableExtra("uri")).into(binding.image);

        binding.tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

    }
}
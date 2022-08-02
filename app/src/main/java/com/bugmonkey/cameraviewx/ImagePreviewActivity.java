package com.bugmonkey.cameraviewx;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.bugmonkey.cameraviewx.databinding.ActivityImagePreviewBinding;
import com.bumptech.glide.Glide;


public class ImagePreviewActivity extends AppCompatActivity {
    public static String RESULT_URI = "result_uri";

    private ActivityImagePreviewBinding binding;

    private Uri uri;

    public static void start(Activity context, Uri uri, int req) {

        Intent starter = new Intent(context, ImagePreviewActivity.class);
        starter.putExtra("uri",uri);
        context.startActivityForResult(starter,req);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uri = getIntent().getParcelableExtra("uri");

        if (uri == null){
            finish();
            return;
        }

        binding = ActivityImagePreviewBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Glide.with(this).load(uri).into(binding.image);

        binding.tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra(RESULT_URI,uri);
                setResult(RESULT_OK);
                finish();
            }
        });

    }
}
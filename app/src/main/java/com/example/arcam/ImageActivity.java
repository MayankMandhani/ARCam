package com.example.arcam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageActivity extends AppCompatActivity {
    private ViewPager2 viewPager2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().hide();
        Object[] o=(Object[])getIntent().getSerializableExtra("files");
        int position=getIntent().getExtras().getInt("position");
        File[] files= Arrays.copyOf(o,o.length,File[].class);
        List<ImageView> images=new ArrayList<ImageView>();
        for(File file:files){
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            imageView.setImageBitmap(bmp);
            images.add(imageView);
        }
        viewPager2 = (ViewPager2) findViewById(R.id.view_pager2);
        ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter(this,images,viewPager2);
        viewPager2.setAdapter(imagePagerAdapter);
        viewPager2.setCurrentItem(position);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
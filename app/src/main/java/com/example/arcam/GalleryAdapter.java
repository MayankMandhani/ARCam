package com.example.arcam;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private Context context;
    private File[] files;
    private String file;
    public GalleryAdapter(Context context,String file,File[] files){
        this.context=context;
        this.files=files;
        this.file=file;
    }


    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.gridview_item, viewGroup, false);
        return new GalleryAdapter.ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return files.length;
    }

    @Override
    public void onBindViewHolder(GalleryAdapter.ViewHolder viewHolder, int position) {
        viewHolder.mThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bmp = BitmapFactory.decodeFile(files[position].getAbsolutePath());
        viewHolder.mThumbnail.setImageBitmap(bmp);
        String time=files[position].toString().substring(file.length()+1,files[position].toString().indexOf(".j"));
        viewHolder.mTitle.setText(time);
        viewHolder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context,ImageActivity.class);
                intent.putExtra("files",files);
                intent.putExtra("position",position);
                context.startActivity(intent);
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitle;
        private ImageView mThumbnail;
        private LinearLayout mLayout;
        ViewHolder(View view) {
            super(view);
            mThumbnail = view.findViewById(R.id.image);
            mTitle=view.findViewById(R.id.fname);
            mLayout=view.findViewById(R.id.lay);
        }
    }
}

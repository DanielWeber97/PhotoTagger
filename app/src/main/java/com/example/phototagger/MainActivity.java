package com.example.phototagger;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db;
    final int IMAGE_CODE = 888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE if not exists Photos(ID int, Photo text, Size int)");
        db.execSQL("CREATE TABLE if not exists Tags(ID int, Tag text)");
    }


    public void capture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, IMAGE_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent x) {
        Bundle extras = x.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");

        Log.v("MYTAG", "in OnActivityResult");

        ImageView img = (ImageView) findViewById(R.id.display);
        img.setImageBitmap(imageBitmap);
    }
}

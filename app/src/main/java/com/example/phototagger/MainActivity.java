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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db;
    final int IMAGE_CODE = 888;
    private String tag;
    private int size;
    private Bitmap currentImg;
    private ArrayList<Bitmap> loadedImages;
    private int[] testImages;
    private int timesTestPressed;
    private ArrayList<Integer> loadedTestImages;
    private int scrollIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE if not exists Photos(ID int, Photo text, Size int)");
        db.execSQL("CREATE TABLE if not exists Tags(ID int, Tag text)");

        loadedImages = new ArrayList<Bitmap>();
        loadedTestImages = new ArrayList<Integer>();
        timesTestPressed = 0;
        scrollIndex = 0;
        testImages = new int[]{R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four};


    }


    public void capture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(takePictureIntent, IMAGE_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent x) {
        if (resultCode == RESULT_OK) {
            Bundle extras = x.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            Log.v("MYTAG", "in OnActivityResult");

            ImageView img = (ImageView) findViewById(R.id.display);
            img.setImageBitmap(imageBitmap);
            currentImg = imageBitmap;
            this.size = imageBitmap.getByteCount();
            TextView size = (TextView) findViewById(R.id.sizeTextView);
            size.setText(this.size + "");
        }
    }

    public String[] handleTags(String s) {
        String[] data = s.split(";");
        return data;
    }

    public void load() {
        String[] tags = handleTags(this.tag);
        for (int i = 0; i < tags.length; i++) {
            db.rawQuery("SELECT Photo FROM Photos, Tags WHERE Tag = ? AND Photos.ID = Tags.ID", new String[]{tags[i]});
        }
        loadedImages.add(currentImg);
    }

    public void test(View v) {

        Button left = findViewById(R.id.back);
        Button right = findViewById(R.id.forward);
        ImageView image = findViewById(R.id.display);
        if (timesTestPressed > 0) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
        }
        if (timesTestPressed < 4) {
            loadedTestImages.add(testImages[timesTestPressed]);
            timesTestPressed++;
            image.setBackground(getDrawable(loadedTestImages.get(timesTestPressed-1 )));
            scrollIndex++;
        }
    }

    public void scrollLeft(View v) {
        ImageView image = findViewById(R.id.display);
        if (scrollIndex > 0) {
            scrollIndex--;
            image.setBackground(getDrawable(loadedTestImages.get(scrollIndex)));
        }
    }

    public void scrollRight(View v) {
        ImageView image = findViewById(R.id.display);
        if (scrollIndex < loadedTestImages.size()-1) {
            scrollIndex++;
            image.setBackground(getDrawable(loadedTestImages.get(scrollIndex)));
        }
    }

}

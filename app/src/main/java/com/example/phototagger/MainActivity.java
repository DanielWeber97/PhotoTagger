package com.example.phototagger;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db;
    final int IMAGE_CODE = 888;
    private String tag;
    private int size;
    private int scrollIndex;
    private int id;
    private int timesTestPressed;

    private ArrayList<Bitmap> loadedImages;
    private ArrayList<Integer> loadedTestImages;

    private Bitmap currentImg;
    private byte[] currentBlob;
    private int[] testImages;

    private ImageView display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE if not exists Photos(ID int, Photo blob, Size int)");
        db.execSQL("CREATE TABLE if not exists Tags(ID int, Tag text)");


        loadedImages = new ArrayList<Bitmap>();
        loadedTestImages = new ArrayList<Integer>();
        timesTestPressed = 0;
        scrollIndex = 0;
        testImages = new int[]{R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four};
        display = findViewById(R.id.display);

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


            ImageView img = (ImageView) findViewById(R.id.display);
            img.setImageBitmap(imageBitmap);

            currentImg = imageBitmap;
            this.size = imageBitmap.getByteCount();

            TextView size = (TextView) findViewById(R.id.sizeTextView);
            size.setText(this.size + "");

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            currentBlob = stream.toByteArray();


        }
    }

    public String[] handleTags(String s) {
        String[] data = s.split(";");
        return data;
    }

    public void save(View v) {
        Date d = new Date();
        id = (int) d.getTime() / 1000;
        db.execSQL("INSERT INTO Photos values (?, ?, ?)", new Object[]{id, this.currentBlob, this.size});
        EditText t = findViewById(R.id.tagTextView);
        String[] tags = handleTags(t.getText().toString());
        for (int i = 0; i < tags.length; i++) {
            db.execSQL("INSERT INTO Tags values (?, ?)", new Object[]{id, tags[i]});
        }
    }

    public void load(View v) {
        loadedImages.clear();

        EditText t = findViewById(R.id.tagTextView);
        String[] tags = handleTags(t.getText().toString());
        Cursor c;


        Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < tags.length; i++) {
            c = db.rawQuery("SELECT Photo FROM Photos, Tags WHERE Tag = ? AND Photos.ID = Tags.ID", new String[]{tags[i]});
            while (c.moveToNext()) {
                b = BitmapFactory.decodeByteArray(c.getBlob(0), 0, c.getBlob(0).length);
                loadedImages.add(b);
            }
            c.close();
        }
        display.setImageBitmap(b);
        handleButtons(v);
    }


    public void handleButtons(View v) {
        Button left = findViewById(R.id.back);
        Button right = findViewById(R.id.forward);
        ImageView image = findViewById(R.id.display);
        Log.v("mytag", "loaded images size: " + loadedImages.size());
        if (loadedImages.size() > 1) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
        }
    }

    public void scrollLeft(View v) {
        ImageView image = findViewById(R.id.display);
        if (scrollIndex > 0) {
            scrollIndex--;
            image.setImageBitmap(loadedImages.get(scrollIndex));
        }
    }

    public void scrollRight(View v) {
        ImageView image = findViewById(R.id.display);
        if (scrollIndex < loadedImages.size() - 1) {
            scrollIndex++;
            image.setImageBitmap(loadedImages.get(scrollIndex));
        }
    }

}

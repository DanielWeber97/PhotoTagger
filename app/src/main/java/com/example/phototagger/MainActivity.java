package com.example.phototagger;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
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
    private int state; //0 = nothing done, 1 = just captured, 2 = just saved, 3 = just loaded

    private ArrayList<Bitmap> loadedImages;
    private ArrayList<Integer> loadedSizes;
    private ArrayList<String> loadedTags;

    private Bitmap currentImg;
    private byte[] currentBlob;

    private ImageView display;
    private Toast currentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE if not exists Photos(ID int, Photo blob, Size int)");
        db.execSQL("CREATE TABLE if not exists Tags(ID int, Tag text)");


        loadedImages = new ArrayList<Bitmap>();
        loadedTags = new ArrayList<String>();
        loadedSizes = new ArrayList<Integer>();
        timesTestPressed = 0;
        scrollIndex = 0;
        state = 0;
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
            Matrix m = new Matrix();
            m.postRotate(90);
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), m, true);


            ImageView img = (ImageView) findViewById(R.id.display);
            img.setImageBitmap(imageBitmap);

            currentImg = imageBitmap;
            this.size = imageBitmap.getByteCount();

            TextView size = (TextView) findViewById(R.id.sizeTextView);
            size.setText(this.size + "");

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            currentBlob = stream.toByteArray();


            state = 1;
        }
    }

    public String[] handleTags(String s) {
        String[] data = s.split(";");
        return data;
    }

    public void save(View v) {

        if (state == 1) {
            EditText t = findViewById(R.id.tagTextView);
            EditText s = findViewById(R.id.sizeTextView);
            String[] tags = handleTags(t.getText().toString());
            if (t.getText().toString().equals("")) {
                showToast("Please enter a tag");
            } else if (!s.getText().toString().matches("[0-9]+")) {
                showToast("Please enter a valid size");
            } else {
                Date d = new Date();
                id = (int) d.getTime() / 1000;
                db.execSQL("INSERT INTO Photos values (?, ?, ?)", new Object[]{id, this.currentBlob, this.size});
                for (int i = 0; i < tags.length; i++) {
                    db.execSQL("INSERT INTO Tags values (?, ?)", new Object[]{id, tags[i]});
                }
                state = 2;
                showToast("Image Saved!");
            }
            Cursor c = db.rawQuery("SELECT * FROM Photos, Tags WHERE Photos.ID = Tags.ID", null);
            while (c.moveToNext()) {
                Log.v("mytag", c.getInt(0) + " " + c.getBlob(1) + " " + c.getString(2) + " " + c.getString(4));
            }

        }
    }

    public void load(View v) {
        //  if (state != 0) {
        loadedImages.clear();
        loadedSizes.clear();
        loadedTags.clear();

        EditText t = findViewById(R.id.tagTextView);
        String[] tags = handleTags(t.getText().toString());
        EditText s = findViewById(R.id.sizeTextView);


        Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        if (!t.getText().toString().equals("") && !s.getText().toString().equals("")) {
            for (int i = 0; i < tags.length; i++) {
                loadTagAndSize(b, s.getText().toString(), tags, i);
            }
        } else if (!s.getText().toString().equals("") && t.getText().toString().equals("")) {
            if (s.getText().toString().matches("[0-9]*"))
                loadSize(b, s.getText().toString());
        } else if (!t.getText().toString().equals("") && s.getText().toString().equals("")) {
            for (int i = 0; i < tags.length; i++) {
                loadTags(b, tags, i);
            }
        }


        scrollIndex = 0;
        if (loadedImages.size() > 0) {
            display.setImageBitmap(loadedImages.get(scrollIndex));
            String size = loadedSizes.get(scrollIndex) + "";
            t.setText(loadedTags.get(scrollIndex));
            s.setText(size, EditText.BufferType.EDITABLE);
        } else {
            display.setImageBitmap(b);
            s.setText("", EditText.BufferType.EDITABLE);
        }
        handleButtons(v);
        state = 3;


        if (loadedImages.size() > 0) {
            showToast("Image Loaded!");
        } else {
            showToast("No matching images");
        }


        //////////////////////////
        //   }
    }

    public void loadTags(Bitmap b, String[] tags, int i) {
        Log.v("mytag", "in loadTags");
        Cursor c = db.rawQuery("SELECT Photo, Size, Photos.ID FROM Photos, Tags WHERE Tag = ? AND Photos.ID = Tags.ID", new String[]{tags[i]});
        while (c.moveToNext()) {
            b = BitmapFactory.decodeByteArray(c.getBlob(0), 0, c.getBlob(0).length);
            Log.v("mytag", b + "");
            int size = c.getInt(1);
            if (!loadedImages.contains(b)) {
                loadedImages.add(b);
                loadedSizes.add(size);
                loadedTags.add(getAllTags(c.getInt(2)));
            }
        }
        c.close();

    }

    public void loadSize(Bitmap b, String size) {
        String upper = (int) (Integer.parseInt(size) * 1.25) + "";
        String lower = (int) (Integer.parseInt(size) * .75) + "";
        Cursor c = db.rawQuery("SELECT Photo, Size, Photos.ID FROM Photos, Tags WHERE Size Between ? AND ? AND Photos.ID = Tags.ID", new String[]{lower, upper});
        while (c.moveToNext()) {
            b = BitmapFactory.decodeByteArray(c.getBlob(0), 0, c.getBlob(0).length);
            int imgsize = c.getInt(1);
            if (!loadedImages.contains(b)) {
                loadedImages.add(b);
                loadedSizes.add(imgsize);
                loadedTags.add(getAllTags(c.getInt(2)));
            }
        }
        c.close();

    }

    public void loadTagAndSize(Bitmap b, String size, String[] tags, int i) {
        try {
            String upper = (int) (Integer.parseInt(size) * 1.25) + "";
            String lower = (int) (Integer.parseInt(size) * .75) + "";

            Cursor c = db.rawQuery("SELECT Photo, Size,Photos.ID FROM Photos, Tags WHERE Size Between ? AND ? AND Photos.ID = Tags.ID AND Tag = ?", new String[]{lower, upper, tags[i]});
            while (c.moveToNext()) {
                b = BitmapFactory.decodeByteArray(c.getBlob(0), 0, c.getBlob(0).length);
                int imgSize = c.getInt(1);
                Log.v("mytag", getAllTags(c.getInt(2)));
                if (!loadedImages.contains(b)) {
                    loadedImages.add(b);
                    loadedSizes.add(imgSize);
                    loadedTags.add(getAllTags(c.getInt(2)));
                }
            }

        } catch (Exception e) {
            showToast("Enter a valid number");
        }

    }

    public String getAllTags(int i) {
        String toReturn = "";
        Cursor c = db.rawQuery("Select Tag from Tags WHERE ID = ?", new String[]{i + ""});
        while (c.moveToNext()) {
            toReturn += c.getString(0) + ";";
        }
        toReturn = toReturn.substring(0, toReturn.length() - 1);
        Log.v("mytag", toReturn);
        return toReturn;
    }


    public void handleButtons(View v) {
        Button left = findViewById(R.id.back);
        Button right = findViewById(R.id.forward);
        ImageView image = findViewById(R.id.display);
        //Log.v("mytag", "loaded images size: " + loadedImages.size());
        if (loadedImages.size() > 1) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
        } else {
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.INVISIBLE);
        }
    }


    public void showToast(String text) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        currentToast.setGravity(Gravity.CENTER_VERTICAL, 0, -100);
        currentToast.show();

    }

    public void scrollLeft(View v) {
        ImageView image = findViewById(R.id.display);
        EditText s = findViewById(R.id.sizeTextView);
        EditText t = findViewById(R.id.tagTextView);
        if (scrollIndex > 0) {
            scrollIndex--;
        } else {
            scrollIndex = loadedImages.size() - 1;
        }
        image.setImageBitmap(loadedImages.get(scrollIndex));
        s.setText(loadedSizes.get(scrollIndex)+"", TextView.BufferType.EDITABLE);
        t.setText(loadedTags.get(scrollIndex), TextView.BufferType.EDITABLE);
    }

    public void scrollRight(View v) {
        ImageView image = findViewById(R.id.display);
        EditText s = findViewById(R.id.sizeTextView);
        EditText t = findViewById(R.id.tagTextView);
        if (scrollIndex < loadedImages.size() - 1) {
            scrollIndex++;
        } else {
            scrollIndex = 0;
        }
        image.setImageBitmap(loadedImages.get(scrollIndex));
         s.setText(loadedSizes.get(scrollIndex)+"", TextView.BufferType.EDITABLE);
         t.setText(loadedTags.get(scrollIndex),TextView.BufferType.EDITABLE);
    }

}

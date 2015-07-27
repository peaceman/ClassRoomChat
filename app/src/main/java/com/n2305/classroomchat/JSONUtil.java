package com.n2305.classroomchat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peaceman on 27/07/15.
 */
public class JSONUtil {
    public static List<JSONObject> convertToJSONObjectList(List<? extends JSONable> source) {
        List<JSONObject> target = new ArrayList<JSONObject>();

        for (JSONable jsonableObject : source) {
            try {
                target.add(jsonableObject.toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return target;
    }

    public interface JSONable {
        JSONObject toJSONObject() throws JSONException;

        String toJSON() throws JSONException;
    }
}

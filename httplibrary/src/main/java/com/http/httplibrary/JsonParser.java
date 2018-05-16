package com.http.httplibrary;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by qwy on 2017/7/14.
 * json解析
 */
public class JsonParser {

    /**
     * 构造函数
     */
    public JsonParser() {
    }

    /**
     * 获取Json字符串中的属性值
     * @param json     Json字符串
     * @param attrName 属性名称
     * @return 属性值
     * @throws JSONException
     */
    public static Object getPropertyFromJson(String json, String attrName) throws JSONException {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            return jsonObject.get(attrName);
        } catch (JSONException e) {
        }
        return null;
    }

    /**
     * 将对象传换成Json串
     * @param t
     * @return
     */
    public static <T> String bean2Json(T t) {
        try {
            if (t == null)
                return "";
            Gson gson = new Gson();
            return gson.toJson(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /***
     * 转换为T类型
     * @param json
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T json2Bean(String json, Class<T> tClass) {
        try {
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                return gson.fromJson(json, tClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将json转化成对应的JavaBean
     * @param json
     * @return
     */
    public static <T> T getBeanFromJson(String json, Class<T> cl) throws Exception {
        if (TextUtils.isEmpty(json) || cl == null)
            return null;
        Gson gson = new Gson();
        return (T) gson.fromJson(json, cl);
    }

    /**
     * 将json转化成对应的JavaBean
     */
    public static <T> T getBeanFromMap(Map<String, Object> map, Class<T> cl) {
        try {
            return getBeanFromJson(getJsonFromMap(map), cl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将Map<String, String>对象转换为JSON字符串
     * @param params 参数
     * @return Json字符串
     */
    public static String getJsonFromMap(Map<String, Object> params) {
        if (params == null || params.size() <= 0) {
            return "";
        }
        return getJSONObjectFromMap(params).toString();
    }

    /**
     * 将Map<String, String>对象转换为JSON字符串
     * @param params 参数
     * @return Json字符串
     */
    public static <T> String getJsonFromList(ArrayList<T> params) {
        try {
            if (params == null || params.size() <= 0) {
                return "";
            }
            JSONArray array = new JSONArray();
            for (Object o : params) {
                if (o instanceof Map) {
                    array.put(getJSONObjectFromMap((Map<String, Object>) o));
                } else {
                    array.put(o);
                }
            }
            return array.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将Map<String, String>对象转换为JSONObject字符串
     * @param params
     */
    @SuppressWarnings("unchecked")
    private static JSONObject getJSONObjectFromMap(Map<String, Object> params) {
        Set<Entry<String, Object>> entrySet = params.entrySet();
        JSONObject object = new JSONObject();
        for (Entry<String, Object> entry : entrySet) {
            try {
                Object ob = entry.getValue();
                if (ob instanceof List) {
                    List list = (List) ob;
                    if (list.size() == 0) {
                        continue;
                    }
                    JSONArray array = new JSONArray();
                    for (Object o : list) {
                        if (o instanceof Map) {
                            array.put(getJSONObjectFromMap((Map<String, Object>) o));
                        } else {
                            array.put(o);
                        }
                    }
                    object.put(entry.getKey(), array);
                } else if (ob instanceof Map) {
                    object.put(entry.getKey(), getJSONObjectFromMap((Map<String, Object>) ob));
                } else {
                    object.put(entry.getKey(), entry.getValue());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    /**
     * 将json转化为HashMap
     * @param jsonStr 字符串
     */
    public static HashMap<String, Object> getMapFromJson(String jsonStr) {
        HashMap<String, Object> valueMap = new LinkedHashMap<String, Object>();
        if (jsonStr == null || "".equals(jsonStr)) {
            return valueMap;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            valueMap = getMapFromJsonObject(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return valueMap;
    }

    /**
     * 转换JsonArray为List<HashMap<String,Object>>.
     * @param jsonArray JsonArray
     */
    public static List<Object> getListFromJsonArray(JSONArray jsonArray) {
        List<Object> list = new ArrayList<Object>();
        int len = jsonArray.length();
        try {
            for (int i = 0; i < len; i++) {
                Object obj = jsonArray.get(i);
                if (obj instanceof JSONObject) {
                    list.add(getMapFromJsonObject((JSONObject) obj));
                } else if (obj instanceof JSONArray) {
                    list.add(getListFromJsonArray(jsonArray));
                } else {

                    if (obj != null && !TextUtils.isEmpty(obj.toString()) && !"null".equals(obj.toString()))
                        list.add(obj);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 转换JSONObject为HashMap<String,Object>. 注：如果json对象的值为null,则默认转换为"".
     * @param jsonObj
     */
    public static HashMap<String, Object> getMapFromJsonObject(JSONObject jsonObj) {
        HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        Iterator<String> iters = jsonObj.keys();
        try {
            while (iters.hasNext()) {
                String key = iters.next();
                Object obj = jsonObj.get(key);
                if (obj instanceof JSONArray) {
                    map.put(key, getListFromJsonArray((JSONArray) obj));
                } else if (obj instanceof JSONObject) {
                    map.put(key, getMapFromJsonObject((JSONObject) obj));
                } else if (obj instanceof Number) {
                    map.put(key, String.valueOf(obj));
                } else if (jsonObj.isNull(key)) {
                    map.put(key, "");
                } else {
                    if (obj != null && !TextUtils.isEmpty(obj.toString()) && !"null".equals(obj.toString()))
                        map.put(key, obj);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * @param str
     * @param type
     * @param <T>
     * @return
     */
    public static <T> List<T> getListFromJson(String str, Class<T> type) {
        try {
            Type listType = new TypeToken<ArrayList<T>>() {
            }.getType();
            List<T> list = new Gson().fromJson(str, listType);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param str
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T getArrayFromJson(String str, Class<T> type) {
        try {
            Type listType = new TypeToken<ArrayList<T>>() {}.getType();
            return new Gson().fromJson(str, listType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Object> getListFromJson(String json) {
        try {
            JSONArray jsonObject = new JSONArray(json);
            return getListFromJsonArray(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在一个HashMap中查找keyName对应的数据对象，查到的第一个就直接返回了
     * @param users
     * @param keyName
     */
    @SuppressWarnings("unchecked")
    public static Object getObjectFromJSONMap(Map<String, Object> users, String keyName) {
        if (users == null || users.size() <= 0)
            return null;
        try {
            Set<Entry<String, Object>> set = users.entrySet();
            for (Entry<String, Object> entry : set) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equalsIgnoreCase(keyName))
                    return value;

                HashMap<String, Object> map = null;
                if (value instanceof Map) {
                    map = (HashMap<String, Object>) value;
                    Object object = getObjectFromJSONMap(map, keyName);
                    if (object != null)
                        return object;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

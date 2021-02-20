package jp.kcme.assembly.watch;

import android.annotation.SuppressLint;
import android.util.Log;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {

    private static AppUtils INSTANCE;

    public static AppUtils get() {
        if (INSTANCE == null) {
            synchronized (AppUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppUtils();
                }
            }
        }
        return INSTANCE;
    }

    public String tag() {
        StackTraceElement trace = Thread.currentThread().getStackTrace()[3];
        String fileName = trace.getFileName();
        String classPath = trace.getClassName();
        String className = classPath.substring(classPath.lastIndexOf(".") + 1);

        String methodName = trace.getMethodName();
        Pattern ANONYMOUS_METHOD = Pattern.compile("lambda|" + className + "|\\$|\\d");
        Matcher m = ANONYMOUS_METHOD.matcher(methodName);
        if (m.find())
            methodName = m.replaceAll("");

        int lineNumber = trace.getLineNumber();
        return className + "." + methodName + "(" + fileName + ":" + lineNumber + ")";
    }

    public boolean isBlank(String s) {
        if (s == null || s.isEmpty())
            return true;

        for (Character c : s.toCharArray()) {
            if (!Character.isWhitespace(c))
                return false;
        }

        return true;
    }

    public Date dateValue(String dateformat) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date value = null;
        try {
            value = df1.parse(dateformat);
        } catch (ParseException e1) {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            try {
                value = df2.parse(dateformat);
            } catch (ParseException e2) {
                return null;
            }
        }
        return value;
    }

    public void printJson(Object form) {
        if (form != null) {
            Log.i(tag(), form.getClass().getSimpleName());
            printJson(toJson(form));
        }
    }

    public String toJson(Object form) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        return gson.toJson(form);
    }

    public void printJson(String json) {
        String linefeed = System.lineSeparator();
        String[] lines = json.split(linefeed);
        for (String s : lines) {
            Log.i(tag(), s);
        }
    }
}


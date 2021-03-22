package com.h2play.canvas_magic.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.h2play.canvas_magic.R;

public final class FileUtil {


    public static String getJsonFromFile(Context context, String fileName) {
        File fl = new File(context.getFilesDir(),fileName);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            fin.close();
            return ret;
        } catch (FileNotFoundException e) {
            InputStream inputStream = context.getResources().openRawResource(R.raw.base_pattern);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                int i = inputStream.read();
                while (i != -1) {
                    byteArrayOutputStream.write(i);
                    i = inputStream.read();
                }

                String jsonText = new String(byteArrayOutputStream.toByteArray());
                inputStream.close();
                return jsonText;
            } catch (IOException e2) {
                e2.printStackTrace();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static void writeFile(Context context, String fileName, String jsonString) {
        File fl = new File(context.getFilesDir(),fileName);
        FileOutputStream outputStream;

        try {

            outputStream = new FileOutputStream(fl);

            outputStream.write(jsonString.getBytes());

            outputStream.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}

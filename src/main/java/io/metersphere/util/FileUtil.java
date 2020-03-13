package io.metersphere.util;

import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

    public static void saveFile(String fileContent, String filePath, String fileName) {
        String path = filePath + "/" + fileName;

        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(path);
            fwriter.write(fileContent);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}

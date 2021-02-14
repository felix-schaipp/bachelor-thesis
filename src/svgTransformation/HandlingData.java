package svgTransformation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class HandlingData {

    // init the the location of the file
    private static String dir = System.getProperty("user.dir") + "/src/svgTransformation/";

    private static String file_location = dir + "db.json";
    private static Gson gson = new Gson();
    private static Charset utf8 = StandardCharsets.UTF_8;

    // Database Class with two fields
    // - Number
    // - Matrix
    private class DB {

        private String database;
        private ArrayList<Double[][]> entries;

        public void setEntries(ArrayList<Double[][]> entries) {
            this.entries = entries;
        }

        public List getEntries() {
            return entries;
        }

        public void setDBName(String database) {
            this.database = database;
        }

        public String getDBName() {
            return database;
        }

    }

    public void createNewDB(Double[][] matrix) {
        System.out.println(dir);

        // create new DB
        DB db = new DB();

        // fill with parameters
        db.setDBName("DB");
        ArrayList<Double[][]> entries = new ArrayList<>();
        entries.add(0, matrix);
        db.setEntries(entries);

        // save to file
        WriteToFile(gson.toJson(db));

    }

    public void insertNewRecord(Double[][] matrix) {

        ArrayList<Double[][]> newEntries = new ArrayList();

        // get current json
        File dbFile = new File(file_location);
        InputStreamReader insertReader;
        try {
            insertReader = new InputStreamReader(new FileInputStream(dbFile), utf8);

            JsonReader myInsertReader = new JsonReader(insertReader);
            DB db = gson.fromJson(myInsertReader, DB.class);
            // save entries to list
            List oldEntries = db.getEntries();

            // add all old entries and the new one
            newEntries.addAll(oldEntries);
            newEntries.add(matrix);

            // delete old db
            File oldDBFile = new File(file_location);
            try {
                oldDBFile.delete();
            } catch (Exception e) {
                log("File couldn't be deleted");
            }

            // create new Database with updated values
            DB newDB = new DB();
            newDB.setDBName(db.getDBName());
            newDB.setEntries(newEntries);

            // save to database
            WriteToFile(gson.toJson(newDB));

        } catch (Exception e) {
            log("error load cache from file " + e.toString());
        }


    }

    public boolean DBExists() {
        File dbFile = new File(file_location);
        if (!dbFile.exists()) {
            System.out.println("DB does not exists yet.");
            return false;
        } else {
            System.out.println("DB exists under path: " + file_location);
            return true;
        }
    }


    /**
     * @param myData saving to json file
     */
    private static void WriteToFile(String myData) {
        File dbFile = new File(file_location);
        if (!dbFile.exists()) {
            try {
                File directory = new File(dbFile.getParent());
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                dbFile.createNewFile();
            } catch (IOException e) {
                log("Excepton Occured: " + e.toString());
            }
        }

        try {
            // Convenience class for writing character files
            FileWriter dbWriter;
            dbWriter = new FileWriter(dbFile.getAbsoluteFile(), true);

            // Writes text to a character-output stream
            BufferedWriter bufferWriter = new BufferedWriter(dbWriter);
            bufferWriter.write(myData.toString());
            bufferWriter.close();

            log("Data saved at file location: " + file_location + " Data: " + myData + "\n");
        } catch (IOException e) {
            log("Hmm.. Got an error while saving db data to file " + e.toString());
        }
    }

    /**
     * read from file
     */
    public void ReadFromFile() {
        File dbFile = new File(file_location);
        if (!dbFile.exists())
            log("File doesn't exist");

        InputStreamReader isReader;
        try {
            isReader = new InputStreamReader(new FileInputStream(dbFile), utf8);

            JsonReader myReader = new JsonReader(isReader);
            DB db = gson.fromJson(myReader, DB.class);
            List entries = db.getEntries();
            log("Entries: " + entries);

        } catch (Exception e) {
            log("error load cache from file " + e.toString());
        }

        log("\nDatabase loaded successfully from file " + file_location);

    }

    private static void log(String string) {
        System.out.println(string);
    }

}

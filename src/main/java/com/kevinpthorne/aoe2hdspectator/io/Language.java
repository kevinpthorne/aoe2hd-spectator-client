package com.kevinpthorne.aoe2hdspectator.io;

import com.kevinpthorne.aoe2hdspectator.StreamingApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by kevint on 1/19/2017.
 */
public class Language {

    public static final String LANG_DIRECTORY = "/lang/";

    String language = "en";

    Properties map;

    Logger log = StreamingApp.log;

    public Language(String language) {

        this.language = language;

        map = loadLang(language);
    }

    public String getString(String key) {
        return map.getProperty(key);
    }

    private Properties loadLang(String language) {
        Properties map = new Properties();
        try {
            map.load(Language.class.getResourceAsStream(LANG_DIRECTORY + language + ".lang"));
        } catch (IOException e) {
            log.warning("Language file wasn't in resources, checking filesystem.");
            try {
                map.load(new FileInputStream(new File(LANG_DIRECTORY + language + ".lang")));
            } catch (IOException e1) {
                log.severe("Language file wasn't in filesystem, falling back to english.");
                try {
                    map.load(Language.class.getResourceAsStream(LANG_DIRECTORY + "en.lang"));
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }

        }
        return map;
    }
}

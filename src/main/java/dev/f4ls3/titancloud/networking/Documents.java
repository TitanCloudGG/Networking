package dev.f4ls3.titancloud.networking;

import dev.f4ls3.titancloud.file.Document;
import dev.f4ls3.titancloud.file.FileManager;

import java.io.File;

class Documents {

    public static final Document keys = FileManager.loadDocument(new File("./keys.json"));
    public static final Document minions = FileManager.loadDocument(new File("./minions.json"));
}

package fr.openent.presences.model;

import java.nio.file.Paths;
import java.util.UUID;

public class ZIPFile implements Cloneable {

    private final String rootPath;
    private final String name;
    private final String dirPath;
    private final String zipPath;

    public ZIPFile(String name) {
        UUID uuid = UUID.randomUUID();
        this.name = name;
        this.rootPath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString()).normalize().toString();
        this.dirPath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString(), name).normalize().toString();
        this.zipPath = Paths.get(this.dirPath).resolve("..").resolve(this.name + ".zip").normalize().toString();
    }


    public String getName() {
        return this.name;
    }

    public String getRootPath() {
        return this.rootPath;
    }

    public String getDirPath() {
        return this.dirPath;
    }

    public String getZipPath() {
        return this.zipPath;
    }

}

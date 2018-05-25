//   * l10n tests:
// "FileConnection is not available"
//   * class-members:
// private String privateDir_;
    public String getPrivateDir() throws IOException {
        if (null != privateDir_)
            return privateDir_;

        privateDir_ = System.getProperty("fileconn.dir.private");
        if (null != privateDir_)
            return privateDir_;

        String fcapi = System.getProperty("microedition.io.file.FileConnection.version");
        if (fcapi != null) {
            try {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                    final String dir = "file:///" + roots.nextElement().toString() + "/" + getAppProperty("MIDlet-Name");
                    FileConnection fc = (FileConnection) Connector.open(dir);

                    if (null == fc)
                        continue;

                    boolean isExists = fc.exists();
                    if (isExists && !fc.isDirectory()) {
                        fc.delete();
                        isExists = false;
                    }

                    if (!isExists)
                        fc.mkdir();

                    if (fc.canWrite() && fc.canRead()) {
                        fc.close();
                        privateDir_ = dir;
                        return privateDir_;
                    }
                    fc.close();
                }
            }
            catch (IOException e) {
            }
        }
        privateDir_ = null;
        throw new IOException(I18N.get("FileConnection is not available"));
    }


package Persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class HeaderTranslator {
    private Boolean compressedFileTree = null;
    private long headerSize = 0;

    public Boolean fileTreeIsCompressed() {
        return compressedFileTree;
    }

    public long getReadHeaderSize() {
        return headerSize;
    }

    public Folder readFileTree(String path) throws IOException {
        Folder FileTree;
        try {
            FileTree = new Folder("root", null);
            InputStreamWatcher isw = new InputStreamWatcher(new BufferedInputStream(new FileInputStream(path)));
            readCompressedFileTree(isw, FileTree);
            headerSize = isw.getReadBytes();
            compressedFileTree = true;
        } catch (Exception e) {
            FileTree = new Folder("root", null);
            readUncompressedFileTree(new File(path), FileTree);
            compressedFileTree = false;
        }
        
        return FileTree;
    }

    private void readCompressedFileTree(InputStream is, Folder parentFolder) throws Exception {
        int next = is.read();
        while ((next > 0) && (next <= 4)) {
            byte[] buf0 = new byte[8];
            is.read(buf0);
            long index = toLong(buf0);
            int size = is.read();
            byte[] buf1 = new byte[size];
            is.read(buf1);
            String name = new String(buf1, "UTF-8");
            Archive file = new Archive(parentFolder.getPath() + "/" + name);
            file.setHeaderIndex(index);
            parentFolder.addFile(file);
            next = is.read();
        }
        while (next == 0) {
            int size = is.read();
            byte[] buff = new byte[size];
            is.read(buff);
            String name = new String(buff, "UTF-8");
            Folder folder = new Folder(name, parentFolder);
            readCompressedFileTree(is, folder);
            next = is.read();
        }
        if (next == 0x05) {
            return;
        }
        else throw new Exception("Invalid file type");
    }

    private void readUncompressedFileTree(File node, Folder parentFolder) throws IOException {
        if (node.isFile())
            parentFolder.addFile(new Archive(node.getCanonicalPath()));
        else {
            Folder folder = new Folder(node.getName(), parentFolder);
            File[] files = node.listFiles();
            for (File file : files)
                readUncompressedFileTree(file, folder);
        }
    }
    
    public void reserveHeader(OutputStream os, Folder parentFolder) throws Exception {
        reserve(os, parentFolder);
        os.flush();
    }
    
    private void reserve(OutputStream os, Folder parentFolder) throws Exception {
        Archive[] files = parentFolder.getFiles();
        for (Archive file : files) {
            reserveFileHeader(os, file);
        }
        Folder[] folders = parentFolder.getFolders();
        for (Folder folder : folders) {
            os.write(0x00); // Type: Folder
            byte[] name = folder.getName().getBytes("UTF-8");
            os.write(name.length);
            os.write(name);
            reserve(os, folder);
        }
        os.write(0x05);
    }

    private void reserveFileHeader(OutputStream os, Archive file) throws Exception {
        byte type = 0;
        switch (file.getCompressionType()) {
            case LZW:
                type = 0x01;
                break;
            case LZ78:
                type = 0x02;
                break;
            case LZSS:
                type = 0x03;
                break;
            case JPEG:
                type = 0x04;
                break;
            default:
                throw new Exception("Invalid compression type");
        }
        os.write(type);
        for (int i = 0; i < 8; ++i) {
            os.write(0);
        }
        byte[] name = file.getFilename().getBytes("UTF-8");
        os.write(name.length);
        os.write(name);
    }
    

    public void setHeaderValues(String path, Folder parentFolder) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        setHeader(raf, parentFolder);
        raf.close();
    }


    private void setHeader(RandomAccessFile header, Folder parentFolder) throws Exception {
        Archive[] files = parentFolder.getFiles();
        for (Archive file : files) {
            header.skipBytes(1);
            header.write(toArray(file.getHeaderIndex()));
            header.skipBytes(header.read()); // skip filename length and filename
        }
        Folder[] folders = parentFolder.getFolders();
        for (Folder folder : folders) {
            header.skipBytes(1);
            header.skipBytes(header.read()); // skip folder name length and folder name
            setHeader(header, folder);
        }
        header.skipBytes(1);
    }
    

    

    private long toLong(byte[] bytes) {
        long result = 0;
        result |= (long)(bytes[0]) << 56;
        result |= ((long)(bytes[1]) << 48) & 0x00FF000000000000L;
        result |= ((long)(bytes[2]) << 40) & 0x0000FF0000000000L;
        result |= ((long)(bytes[3]) << 32) & 0x000000FF00000000L;
        result |= ((long)(bytes[4]) << 24) & 0x00000000FF000000L;
        result |= ((long)(bytes[5]) << 16) & 0x0000000000FF0000L;
        result |= ((long)(bytes[6]) << 8) & 0x000000000000FF00L;
        result |= (long)(bytes[7]) & 0x00000000000000FFL;
        return result;
    }

    private byte[] toArray(long value) {
        byte[] result = new byte[8];
        result[0] = (byte)((value >> 56) & 0x00000000000000FFL);
        result[1] = (byte)((value >> 48) & 0x00000000000000FFL);
        result[2] = (byte)((value >> 40) & 0x00000000000000FFL);
        result[3] = (byte)((value >> 32) & 0x00000000000000FFL);
        result[4] = (byte)((value >> 24) & 0x00000000000000FFL);
        result[5] = (byte)((value >> 16) & 0x00000000000000FFL);
        result[6] = (byte)((value >> 8) & 0x00000000000000FFL);
        result[7] = (byte)(value & 0x00000000000000FFL);
        return result;
    }

}
import java.io.* ;
import Compressor.LZW;

public class LZWDriver {
    public static void main(String[] args) {
        int DICT_BIT_SIZE = 10;

        if (args.length != 3) {
            printUsage();
            System.exit(1);
        }

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(args[1]));
            OutputStream os = new BufferedOutputStream(new FileOutputStream(args[2]));
            LZW fileW = new LZW();
            if (args[0].equals("compress")) fileW.compress(is,os,DICT_BIT_SIZE);
            else if (args[0].equals("decompress")) fileW.decompress(is,os);
            else {
                printUsage();
            }
            is.close();
            os.close();

        } catch (FileNotFoundException fnfe) {
            System.out.println(args[1] + " Not Found");
			System.exit(1);
        } catch (IOException ioe) {
			System.out.println("IO Error: " + ioe.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void printUsage() {
        System.out.println("Arguments: compress/decompress FromFile ToFile");
    }
}
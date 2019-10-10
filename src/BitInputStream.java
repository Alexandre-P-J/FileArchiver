import java.io.* ;

public class BitInputStream extends FilterInputStream {

	public BitInputStream (InputStream is) { 
		super(is); 
	}

	class BitManager {
		private int[] buf = new int[8];
		private int cnt = -1 ;

		boolean atTheEnd () { 
			return ((buf[7] == 1) && (cnt < 0)); 
		}

		void setTheEnd () { 
			buf[7] = 1;
			cnt = -1;
		}

		boolean noMoreBuffer () { 
			return cnt < 0; 
		}

		void setNext (int next) { 
			for (cnt = 0; cnt < 8; ++cnt) {
				buf[cnt] = next % 2;
				next /= 2;
			}

			if (buf[7] == 1) {
				for (cnt = 7;cnt >= 0; cnt--)
				if (buf[cnt] == 0) break;
				cnt--;
			} 
			else cnt = 6;
		}

		int getNext() {
			return buf[cnt--]; 
		}

		int left() {
			return cnt+1; 
		}
	};

	BitManager bitManager = new BitManager();

	byte[] tempBuf = null;
	int tempBufPtr = 0;
	int tempBufLen = 0;

	private int readNextByte () throws IOException { 
		int val = -1;
		if (tempBufPtr==tempBufLen) val = super.read();
		else {
			byte b = tempBuf[tempBufPtr++];
			if ((b & 0x80) > 0) val = ((int)(b & 0x7F))|0x80;
				else val = b;
		}
		return val;
	}

	public int read () throws IOException {
		if (bitManager.atTheEnd()) return -1;
		if (bitManager.noMoreBuffer()) {
			int i = readNextByte();
			if (i < 0) bitManager.setTheEnd();
			else bitManager.setNext(i);
			return read();
		}
		return bitManager.getNext();
	}
}
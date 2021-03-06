package org.mobicents.media;

import java.io.Serializable;



/**
 * Standard JMF class -- see <a href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/Buffer.html" target="_blank">this class in the JMF Javadoc</a>.
 * Coding complete.
 * An observation on the use of Buffer in JMF: it appears that a Buffer is not considered to be immutable as 
 * it is processed (by a Codec, etc).  JMF will do things like swap the data of two Buffers, or when
 * input is not consumed, update the offset and length.
 * The result of this is that care needs to be taken in implementation code to clone buffers if a buffer
 * is to be used for multiple things (like in a cloned DataSource).
 * @author Ken Larson
 *
 */
public class Buffer implements Serializable {

    protected long timeStamp = -1L;
    protected long duration = -1L;
    protected Format format;
    protected int flags = 0;
    protected Object data = null;
    protected Object header = null;
    protected int length = 0;
    protected int offset = 0;
    protected BufferFactory factory;
    protected long sequenceNumber = SEQUENCE_UNKNOWN;
    public static final int FLAG_EOM = 1;
    public static final int FLAG_DISCARD = 2;
    public static final int FLAG_SILENCE = 4;
    public static final int FLAG_SID = 8;
    public static final int FLAG_KEY_FRAME = 16;
    public static final int FLAG_NO_DROP = 32;
    public static final int FLAG_NO_WAIT = 64;
    public static final int FLAG_NO_SYNC = 96;
    public static final int FLAG_SYSTEM_TIME = 128;
    public static final int FLAG_RELATIVE_TIME = 256;
    public static final int FLAG_FLUSH = 512;
    public static final int FLAG_SYSTEM_MARKER = 1024;
    public static final int FLAG_RTP_MARKER = 2048;
    public static final int FLAG_RTP_TIME = 4096;
    public static final int FLAG_BUF_OVERFLOWN = 8192;
    public static final int FLAG_BUF_UNDERFLOWN = 16384;
    public static final int FLAG_LIVE_DATA = 32768;
    public static final long TIME_UNKNOWN = -1;
    public static final long SEQUENCE_UNKNOWN = Long.MAX_VALUE - 1;
    

    
    
    public Buffer() {
        super();
    }

    public void setFactory(BufferFactory factory) {
        this.factory = factory;
    }
            
    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;	// do not clone.
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isEOM() {
        return (this.flags & FLAG_EOM) != 0;
    }

    public void setEOM(boolean eom) {
        if (eom) {
            this.flags |= FLAG_EOM;
        } else {
            this.flags &= ~FLAG_EOM;
        }
    }

    public boolean isDiscard() {
        return (this.flags & FLAG_DISCARD) != 0;
    }

    public void setDiscard(boolean discard) {
        if (discard) {
            this.flags |= FLAG_DISCARD;
        } else {
            this.flags &= ~FLAG_DISCARD;
        }
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getHeader() {
        return header;
    }

    public void setHeader(Object header) {
        this.header = header;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setSequenceNumber(long number) {
        this.sequenceNumber = number;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void copy(Buffer buffer) {
        copy(buffer, false);
    }

    public void copy(Buffer buffer, boolean swapData) {
        duration = buffer.duration;
        flags = buffer.flags;
        format = buffer.format;	// do not clone.
        header = buffer.header;	// do not clone.
        length = buffer.length;
        offset = buffer.offset;
        sequenceNumber = buffer.sequenceNumber;
        timeStamp = buffer.timeStamp;
        //factory = buffer.factory;
        if (swapData) {
            data = dupData(buffer.data, false);
        } else {
            data = buffer.data;
        }
    }

    public Object clone() {
        // behavior is not the same as copy(this, true).
        // 1. unknown data types results in = copy of data.
        // 2. header is cloned.
        final Buffer result = new Buffer();

        result.duration = duration;
        result.flags = flags;
        result.format = format;	// do not clone.
        result.header = dupData(header, true);	// clone - TODO: 2nd parameter?
        result.length = length;
        result.offset = offset;
        result.sequenceNumber = sequenceNumber;
        result.timeStamp = timeStamp;
        result.data = dupData(data, true);
        result.factory = factory;

        return result;
    }

    /**
     * @param standardTypesOnly if true, only byte[], short[], and int[] are recognized.  Otherwise, all primitive arrays recognized.
     * @return data if type is not recognized.
     */
    private static Object dupData(Object data, boolean standardTypesOnly) {
        // TODO: should we recognize other types of arrays?

        if (data == null) {
            return null;
        }
        final Object result;
        final int len;
        if (!data.getClass().isArray()) {
            throw new IllegalArgumentException();
        }	// TODO: is this right?
        if (data.getClass() == byte[].class) {
            len = ((byte[]) data).length;
            result = new byte[len];
        } else if (data.getClass() == int[].class) {
            len = ((int[]) data).length;
            result = new int[len];
        } else if (data.getClass() == short[].class) {
            len = ((short[]) data).length;
            result = new short[len];
        } else if (!standardTypesOnly && data.getClass() == float[].class) {
            len = ((float[]) data).length;
            result = new float[len];
        } else if (!standardTypesOnly && data.getClass() == double[].class) {
            len = ((double[]) data).length;
            result = new double[len];
        } else if (!standardTypesOnly && data.getClass() == boolean[].class) {
            len = ((boolean[]) data).length;
            result = new boolean[len];
        } else if (!standardTypesOnly && data.getClass() == long[].class) {
            len = ((long[]) data).length;
            result = new long[len];
        } else if (!standardTypesOnly && data.getClass() == char[].class) {
            len = ((char[]) data).length;
            result = new char[len];
        } else {
            return data;	// this appears to be compatible.
        //throw new IllegalArgumentException();	// TODO: support other types?
        }
        System.arraycopy(data, 0, result, 0, len);
        return result;
    }
    
    @Override
    public String toString() {
        return "Buffer[fmt=" + this.format + ", timestamp=" + this.timeStamp +
                ", seq=" + this.sequenceNumber + ", size= " + (length - offset) +
                "]";
    }
    
    public void dispose() {
        if (factory != null) {
            factory.deallocate(this);
        } 
    }
}

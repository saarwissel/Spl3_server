package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.ByteBuffer;

public class StompMessageEncoderDecoderImpl<T> implements MessageEncoderDecoder<T> {
    private static final byte END_OF_MESSAGE = (byte) '^'; // 0x00 הוא תו ה-null \u0000
    private static final byte MESSAGE_DELIMITER = (byte) 0x00; // סימן לסוף ההודעה (בשביל הדוגמה)
    private ByteBuffer byteBuffer;  // ה-ByteBuffer שמאחסן את הנתונים

    public StompMessageEncoderDecoderImpl() {
        byteBuffer = ByteBuffer.allocate(1024);  // אלוקציה ראשונית של 1024 בייטים
    }

    @Override
    public T decodeNextByte(byte nextByte) {
        // הוספת בייט ל-ByteBuffer
        byteBuffer.put(nextByte);
        
        // נבדוק אם הגענו לסוף ההודעה ^@
        if (nextByte == END_OF_MESSAGE) {
            
            byte[] frameData = new byte[byteBuffer.position() - 1];
            byteBuffer.flip();  // מכניס את ה-ByteBuffer למצב קריאה
            byteBuffer.get(frameData);  // קריאת כל הנתונים מה-ByteBuffer

            // המרת המידע ל-String
            String message = new String(frameData);

            // ניקוי ה-ByteBuffer לשימוש הבא
            byteBuffer.clear();

            // החזרת ההודעה המפוענחת
            return (T)message;
        }
        else{
        return null;
        }
    }

    @Override
    public byte[] encode(T message){

        return (message.toString() + "\u0000").getBytes();
    }
}
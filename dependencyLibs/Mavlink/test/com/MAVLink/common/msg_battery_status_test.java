/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */
         
// MESSAGE BATTERY_STATUS PACKING
package com.MAVLink.common;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.ardupilotmega.CRC;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
* Battery information
*/
public class msg_battery_status_test{

public static final int MAVLINK_MSG_ID_BATTERY_STATUS = 147;
public static final int MAVLINK_MSG_LENGTH = 36;
private static final long serialVersionUID = MAVLINK_MSG_ID_BATTERY_STATUS;

private Parser parser = new Parser();

public CRC generateCRC(byte[] packet){
    CRC crc = new CRC();
    for (int i = 1; i < packet.length - 2; i++) {
        crc.update_checksum(packet[i] & 0xFF);
    }
    crc.finish_checksum(MAVLINK_MSG_ID_BATTERY_STATUS);
    return crc;
}

public byte[] generateTestPacket(){
    ByteBuffer payload = ByteBuffer.allocate(6 + MAVLINK_MSG_LENGTH + 2);
    payload.put((byte)MAVLinkPacket.MAVLINK_STX); //stx
    payload.put((byte)MAVLINK_MSG_LENGTH); //len
    payload.put((byte)0); //seq
    payload.put((byte)255); //sysid
    payload.put((byte)190); //comp id
    payload.put((byte)MAVLINK_MSG_ID_BATTERY_STATUS); //msg id
    payload.putInt((int)963497464); //current_consumed
    payload.putInt((int)963497672); //energy_consumed
    payload.putShort((short)17651); //temperature
    //voltages
    payload.putShort((short)17755);
    payload.putShort((short)17756);
    payload.putShort((short)17757);
    payload.putShort((short)17758);
    payload.putShort((short)17759);
    payload.putShort((short)17760);
    payload.putShort((short)17761);
    payload.putShort((short)17762);
    payload.putShort((short)17763);
    payload.putShort((short)17764);
    payload.putShort((short)18795); //current_battery
    payload.put((byte)101); //id
    payload.put((byte)168); //battery_function
    payload.put((byte)235); //type
    payload.put((byte)46); //battery_remaining
    
    CRC crc = generateCRC(payload.array());
    payload.put((byte)crc.getLSB());
    payload.put((byte)crc.getMSB());
    return payload.array();
}

@Test
public void test(){
    byte[] packet = generateTestPacket();
    for(int i = 0; i < packet.length - 1; i++){
        parser.mavlink_parse_char(packet[i] & 0xFF);
    }
    MAVLinkPacket m = parser.mavlink_parse_char(packet[packet.length - 1] & 0xFF);
    byte[] processedPacket = m.encodePacket();
    assertArrayEquals("msg_battery_status", processedPacket, packet);
}
}
        
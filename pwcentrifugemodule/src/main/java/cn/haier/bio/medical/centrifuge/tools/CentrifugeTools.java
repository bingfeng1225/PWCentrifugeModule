package cn.haier.bio.medical.centrifuge.tools;

import cn.qd.peiwen.pwtools.ByteUtils;
import io.netty.buffer.ByteBuf;


public class CentrifugeTools {
    public static final byte[] HEADER = {(byte)0xAA,(byte)0x55};

    public static boolean checkHeader(byte[] header) {
        for (int i = 0; i < HEADER.length; i++) {
            if(HEADER[i] != header[i]){
                return false;
            }
        }
        return true;
    }

    public static boolean checkFrame(byte[] data) {
        byte sum = data[data.length - 1];
        byte check = ByteUtils.computeL8SumCode(data, 2, data.length - 3);
        return (sum == check);
    }

    public static int indexOf(ByteBuf haystack, byte[] needle) {
        //遍历haystack的每一个字节
        for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
            int needleIndex;
            int haystackIndex = i;
            /*haystack是否出现了delimiter，注意delimiter是一个ChannelBuffer（byte[]）
            例如对于haystack="ABC\r\nDEF"，needle="\r\n"
            那么当haystackIndex=3时，找到了“\r”，此时needleIndex=0
            继续执行循环，haystackIndex++，needleIndex++，
            找到了“\n”
            至此，整个needle都匹配到了
            程序然后执行到if (needleIndex == needle.capacity())，返回结果
            */
            for (needleIndex = 0; needleIndex < needle.length; needleIndex++) {
                if (haystack.getByte(haystackIndex) != needle[needleIndex]) {
                    break;
                } else {
                    haystackIndex++;
                    if (haystackIndex == haystack.writerIndex() && needleIndex != needle.length - 1) {
                        return -1;
                    }
                }
            }

            if (needleIndex == needle.length) {
                // Found the needle from the haystack!
                return i - haystack.readerIndex();
            }
        }
        return -1;
    }
}
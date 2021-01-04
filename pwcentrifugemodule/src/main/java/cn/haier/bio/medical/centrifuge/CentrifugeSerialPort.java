package cn.haier.bio.medical.centrifuge;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class CentrifugeSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private HandlerThread thread;
    private CentrifugeHandler handler;
    private PWSerialPortHelper helper;

    private boolean enabled = false;
    private WeakReference<ICentrifugeListener> listener;

    public CentrifugeSerialPort() {
    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
        }
    }

    public void disable() {
        if (this.isInitialized() && this.enabled) {
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void sendData(byte[] data) {
        if (this.isInitialized() && this.enabled) {
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = data;
            this.handler.sendMessage(msg);
        }
    }

    public void changeListener(ICentrifugeListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        if (this.buffer == null) {
            return false;
        }
        return true;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("CentrifugeSerialPort");
            this.helper.setTimeout(3);
            this.helper.setPath(path);
            this.helper.setBaudrate(9600);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("CentrifugeSerialPort");
            this.thread.start();
            this.handler = new CentrifugeHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.writeAndFlush(data);
        CentrifugeSerialPort.this.switchReadModel();
        this.loggerPrint("CentrifugeSerialPort Send:" + CentrifugeTools.bytes2HexString(data, true, ", "));
    }

    public void switchReadModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugeSwitchReadModel();
        }
    }

    public void switchWriteModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugeSwitchWriteModel();
        }
    }

    private void loggerPrint(String message) {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugePrint(message);
        }
    }

    private boolean ignorePackage() {
        int index = CentrifugeTools.indexOf(this.buffer, CentrifugeTools.HEADER);
        if (index != -1) {
            byte[] data = new byte[index];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            this.loggerPrint("CentrifugeSerialPort 指令丢弃:" + CentrifugeTools.bytes2HexString(data, true, ", "));
            return this.processBytesBuffer();
        }
        return false;
    }


    private boolean processBytesBuffer() {
        if (this.buffer.readableBytes() < 3) {
            return true;
        }

        byte[] header = new byte[CentrifugeTools.HEADER.length];
        this.buffer.getBytes(0, header);

        if (!CentrifugeTools.checkHeader(header)) {
            return this.ignorePackage();
        }
        int length = 0xFF & this.buffer.getByte(2);

        if (this.buffer.readableBytes() < length + 3) {
            return true;
        }

        this.buffer.markReaderIndex();

        byte[] data = new byte[length + 3];
        this.buffer.readBytes(data, 0, data.length);

        if (!CentrifugeTools.checkFrame(data)) {
            this.buffer.resetReaderIndex();
            //当前包不合法 丢掉正常的包头以免重复判断
            this.buffer.skipBytes(CentrifugeTools.HEADER.length);
            this.buffer.discardReadBytes();
            return this.ignorePackage();
        }
        this.buffer.discardReadBytes();
        this.loggerPrint("CentrifugeSerialPort Recv:" + CentrifugeTools.bytes2HexString(data, true, ", "));
        this.switchWriteModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugePackageReceived(data);
        }
        return true;
    }

    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.buffer.clear();
        this.switchReadModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugeConnected();
        }
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugePrint("CentrifugeSerialPort read thread released");
        }
    }

    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugeException(throwable);
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onCentrifugePrint("CentrifugeSerialPort state changed: " + state.name());
        }
    }

    @Override
    public boolean onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return false;
        }
        this.buffer.writeBytes(buffer, 0, length);

        return this.processBytesBuffer();
    }

    private class CentrifugeHandler extends Handler {
        public CentrifugeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: {
                    byte[] message = (byte[]) msg.obj;
                    if (null != message && message.length > 0) {
                        CentrifugeSerialPort.this.write(message);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}

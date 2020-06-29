package cn.haier.bio.medical.centrifuge;

/***
 * 超低温变频、T系列、双系统主控板通讯
 *
 */
public class CentrifugeManager {
    private CentrifugeSerialPort serialPort;
    private static CentrifugeManager manager;

    public static CentrifugeManager getInstance() {
        if (manager == null) {
            synchronized (CentrifugeManager.class) {
                if (manager == null)
                    manager = new CentrifugeManager();
            }
        }
        return manager;
    }

    private CentrifugeManager() {

    }

    public void init(String path) {
        if (this.serialPort == null) {
            this.serialPort = new CentrifugeSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if (null != this.serialPort) {
            this.serialPort.enable();
        }
    }

    public void disable() {
        if (null != this.serialPort) {
            this.serialPort.disable();
        }
    }

    public void release() {
        if (null != this.serialPort) {
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void sendData(byte[] data) {
        if (null != this.serialPort) {
            this.serialPort.sendData(data);
        }
    }

    public void changeListener(ICentrifugeListener listener) {
        if (null != this.serialPort) {
            this.serialPort.changeListener(listener);
        }
    }
}


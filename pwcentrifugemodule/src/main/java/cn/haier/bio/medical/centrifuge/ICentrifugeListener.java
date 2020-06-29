package cn.haier.bio.medical.centrifuge;

public interface ICentrifugeListener {
    void onCentrifugeConnected();
    void onCentrifugeSwitchReadModel();
    void onCentrifugeSwitchWriteModel();
    void onCentrifugePrint(String message);
    void onCentrifugeException(Throwable throwable);
    void onCentrifugePackageReceived(byte[] message);
}

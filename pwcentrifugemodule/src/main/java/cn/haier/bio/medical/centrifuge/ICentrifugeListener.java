package cn.haier.bio.medical.centrifuge;

public interface ICentrifugeListener {
    void onCentrifugeConnected();
    void onCentrifugeException();
    void onCentrifugeSwitchReadModel();
    void onCentrifugeSwitchWriteModel();
    void onCentrifugePackageReceived(byte[] message);
}
